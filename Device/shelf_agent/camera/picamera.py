"""Real camera on the Raspberry Pi 5, via picamera2.

``picamera2`` is imported inside the constructor so this module (and the
whole package) can be imported on a PC where the library does not exist.
"""

from __future__ import annotations

import io
import time
from pathlib import Path

from shelf_agent.models import Frame, utc_now


class Picamera2Source:
    """Takes JPEG stills with a Raspberry Pi camera module.

    Every still is also written to ``capture_dir``: the ``image-events``
    protocol sends its path rather than the bytes.

    Args:
        width: Still resolution width in pixels.
        height: Still resolution height in pixels.
        capture_dir: Where stills are kept on the device.
        warmup_s: Time to let auto-exposure and white balance settle after start.
    """

    def __init__(
        self, width: int, height: int, capture_dir: Path, *, warmup_s: float = 2.0
    ) -> None:
        from picamera2 import Picamera2

        self._capture_dir = capture_dir
        self._capture_dir.mkdir(parents=True, exist_ok=True)
        self._camera = Picamera2()
        config = self._camera.create_still_configuration(main={"size": (width, height)})
        self._camera.configure(config)
        self._camera.start()
        time.sleep(warmup_s)

    def capture(self) -> Frame:
        captured_at = utc_now()
        buffer = io.BytesIO()
        self._camera.capture_file(buffer, format="jpeg")
        filename = f"{captured_at:%Y%m%dT%H%M%S%fZ}.jpg"
        path = self._capture_dir / filename
        path.write_bytes(buffer.getvalue())
        return Frame(
            data=buffer.getvalue(),
            filename=filename,
            content_type="image/jpeg",
            captured_at=captured_at,
            source_path=path,
        )

    def close(self) -> None:
        self._camera.stop()
        self._camera.close()
