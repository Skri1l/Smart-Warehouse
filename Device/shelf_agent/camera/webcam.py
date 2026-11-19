"""PC webcam via OpenCV: a stand-in for the shelf camera when no Pi is at hand.

Behaves like :class:`~shelf_agent.camera.picamera.Picamera2Source`: every
capture is a fresh JPEG, kept in ``capture_dir`` and delivered by the same
agent, outbox and backend client. ``cv2`` is imported inside the constructor,
so the package still imports where OpenCV is not installed.
"""

from __future__ import annotations

import logging
import sys
import threading
import time
from pathlib import Path
from typing import Any

from shelf_agent.camera.base import CameraError
from shelf_agent.models import Frame, utc_now

logger = logging.getLogger(__name__)


class WebcamSource:
    """Takes JPEG stills from a webcam (``cv2.VideoCapture``).

    A background thread reads the stream continuously and keeps only the
    latest frame. Webcams buffer several frames, so reading on demand would
    return a picture from before the shelf changed; :meth:`capture` instead
    waits for a frame read after it was called.

    Args:
        index: Camera number as OpenCV sees it (0 = default webcam).
        capture_dir: Where stills are kept; their path is what
            ``image-events`` reports.
        width: Requested resolution width; the driver may pick the nearest.
        height: Requested resolution height.
        preview: Show a live window, so the presenter can aim the camera.
        title: Preview window title.
        jpeg_quality: JPEG quality, 0-100.
        timeout_s: How long to wait for the camera to open or deliver a frame.
    """

    def __init__(
        self,
        index: int,
        capture_dir: Path,
        *,
        width: int = 1280,
        height: int = 720,
        preview: bool = False,
        title: str | None = None,
        jpeg_quality: int = 90,
        timeout_s: float = 10.0,
    ) -> None:
        try:
            import cv2
        except ImportError as exc:
            raise CameraError(
                "webcam needs OpenCV: pip install -r requirements-webcam.txt"
            ) from exc

        self._cv2 = cv2
        self._index = index
        self._capture_dir = capture_dir
        self._capture_dir.mkdir(parents=True, exist_ok=True)
        # Cocoa lets only the main thread open windows, and the preview is
        # drawn from the reader thread, so on macOS it can never work.
        if preview and sys.platform == "darwin":
            logger.warning(
                "webcam %d: no live preview on macOS; photos are kept in %s",
                index,
                capture_dir,
            )
            preview = False
        self._preview = preview
        self._title = title or f"webcam {index}"
        self._jpeg_quality = jpeg_quality
        self._timeout_s = timeout_s

        # DirectShow opens in about a second on Windows; the default MSMF
        # backend can take ten.
        backend = cv2.CAP_DSHOW if sys.platform == "win32" else cv2.CAP_ANY
        self._camera = cv2.VideoCapture(index, backend)
        if not self._camera.isOpened():
            raise CameraError(
                f"cannot open webcam {index}: is it connected, free (not used by "
                "another app) and allowed in the OS camera privacy settings?"
            )
        self._camera.set(cv2.CAP_PROP_FRAME_WIDTH, width)
        self._camera.set(cv2.CAP_PROP_FRAME_HEIGHT, height)

        self._cond = threading.Condition()
        self._latest: Any = None
        self._sequence = 0
        self._error: str | None = None
        self._flash_until = 0.0
        self._closed = threading.Event()
        self._reader = threading.Thread(
            target=self._read_loop, name=f"webcam-{index}", daemon=True
        )
        self._reader.start()
        # Fail at start-up, not on the first shelf event.
        try:
            self._wait_for_frame(after=0)
        except CameraError:
            self.close()
            raise
        logger.info(
            "webcam %d opened at %dx%d",
            index,
            int(self._camera.get(cv2.CAP_PROP_FRAME_WIDTH)),
            int(self._camera.get(cv2.CAP_PROP_FRAME_HEIGHT)),
        )

    def capture(self) -> Frame:
        with self._cond:
            requested = self._sequence
        image = self._wait_for_frame(after=requested)
        captured_at = utc_now()
        ok, encoded = self._cv2.imencode(
            ".jpg", image, [self._cv2.IMWRITE_JPEG_QUALITY, self._jpeg_quality]
        )
        if not ok:
            raise CameraError(f"webcam {self._index}: JPEG encoding failed")
        data = encoded.tobytes()
        filename = f"{captured_at:%Y%m%dT%H%M%S%fZ}.jpg"
        path = self._capture_dir / filename
        path.write_bytes(data)
        self._flash_until = time.monotonic() + 0.4
        return Frame(
            data=data,
            filename=filename,
            content_type="image/jpeg",
            captured_at=captured_at,
            source_path=path.resolve(),
        )

    def close(self) -> None:
        """Stop the reader and release the camera; safe to call more than once."""
        if self._closed.is_set():
            return
        self._closed.set()
        self._reader.join(timeout=2)
        self._camera.release()

    def _wait_for_frame(self, after: int) -> Any:
        deadline = time.monotonic() + self._timeout_s
        with self._cond:
            while self._sequence <= after:
                if self._error is not None:
                    raise CameraError(self._error)
                remaining = deadline - time.monotonic()
                if remaining <= 0 or self._closed.is_set():
                    raise CameraError(
                        f"webcam {self._index} delivered no frame "
                        f"in {self._timeout_s:g} s"
                    )
                self._cond.wait(remaining)
            return self._latest.copy()

    def _read_loop(self) -> None:
        # The preview is drawn here too: an OpenCV window belongs to the
        # thread that created it.
        try:
            self._read_frames()
        finally:
            if self._preview:
                self._cv2.destroyAllWindows()

    def _read_frames(self) -> None:
        failures = 0
        while not self._closed.is_set():
            ok, image = self._camera.read()
            if not ok:
                failures += 1
                if failures >= 50:
                    with self._cond:
                        self._error = f"webcam {self._index} stopped sending frames"
                        self._cond.notify_all()
                    return
                time.sleep(0.05)
                continue
            failures = 0
            with self._cond:
                self._latest = image
                self._sequence += 1
                self._cond.notify_all()
            if self._preview:
                try:
                    self._show(image)
                except Exception as exc:  # noqa: BLE001 - any GUI failure
                    # No display, or a GUI the reader thread may not use:
                    # keep capturing without the window.
                    logger.warning("webcam %d: preview disabled: %s", self._index, exc)
                    self._preview = False

    def _show(self, image: Any) -> None:
        cv2 = self._cv2
        if time.monotonic() < self._flash_until:
            image = image.copy()
            height, width = image.shape[:2]
            cv2.rectangle(image, (0, 0), (width - 1, height - 1), (0, 255, 0), 12)
            cv2.putText(
                image,
                "CAPTURED",
                (20, 50),
                cv2.FONT_HERSHEY_SIMPLEX,
                1.4,
                (0, 255, 0),
                3,
            )
        cv2.imshow(self._title, image)
        cv2.waitKey(1)
