"""PC webcam source, with a fake ``cv2`` standing in for OpenCV and the camera."""

from __future__ import annotations

import sys
import threading
import time
import types
from collections.abc import Callable, Iterator, Sequence
from pathlib import Path

import pytest

from shelf_agent.agent import Delivery
from shelf_agent.camera.base import CameraError, CameraSource
from shelf_agent.camera.webcam import WebcamSource
from shelf_agent.config import (
    PROTOCOL_DEVICE_IMAGES,
    BackendConfig,
    DeviceIdentity,
    load_device_config,
)
from shelf_agent.device import build_camera
from shelf_agent.emulator.runner import Emulator
from shelf_agent.emulator.scenario import EmulatedShelf, Scenario, WebcamSettings
from shelf_agent.mock_server import MockBackend

DEVICE_ROOT = Path(__file__).parents[1]


class FakeImage:
    """Stands in for a numpy frame; remembers which read produced it."""

    shape = (720, 1280, 3)

    def __init__(self, number: int) -> None:
        self.number = number

    def copy(self) -> FakeImage:
        return FakeImage(self.number)


class FakeEncoded:
    def __init__(self, image: FakeImage) -> None:
        self._image = image

    def tobytes(self) -> bytes:
        return b"\xff\xd8 frame %d" % self._image.number


class FakeCamera:
    """A webcam producing numbered frames at ~200 fps; can be made to fail."""

    def __init__(self, opened: bool = True) -> None:
        self.opened = opened
        self.broken = False
        self.released = False
        self.reads = 0
        self.props: dict[int, float] = {}

    def isOpened(self) -> bool:  # noqa: N802 - OpenCV API
        return self.opened

    def set(self, prop: int, value: float) -> bool:
        self.props[prop] = value
        return True

    def get(self, prop: int) -> float:
        return self.props.get(prop, 0.0)

    def read(self) -> tuple[bool, FakeImage | None]:
        time.sleep(0.005)
        if self.broken:
            return False, None
        self.reads += 1
        return True, FakeImage(self.reads)

    def release(self) -> None:
        self.released = True


@pytest.fixture
def camera() -> FakeCamera:
    return FakeCamera()


@pytest.fixture
def fake_cv2(
    monkeypatch: pytest.MonkeyPatch, camera: FakeCamera
) -> types.SimpleNamespace:
    module = types.SimpleNamespace(
        CAP_ANY=0,
        CAP_DSHOW=700,
        CAP_PROP_FRAME_WIDTH=3,
        CAP_PROP_FRAME_HEIGHT=4,
        IMWRITE_JPEG_QUALITY=1,
        FONT_HERSHEY_SIMPLEX=0,
        opened_with=[],
    )

    def video_capture(index: int, backend: int) -> FakeCamera:
        module.opened_with.append((index, backend))
        return camera

    module.VideoCapture = video_capture
    module.imencode = lambda ext, image, params: (True, FakeEncoded(image))
    module.shown = []
    module.imshow = lambda title, image: module.shown.append(title)
    module.waitKey = lambda delay: -1
    module.rectangle = module.putText = lambda *args: None
    module.destroyAllWindows = lambda: None
    monkeypatch.setitem(sys.modules, "cv2", module)
    return module


@pytest.fixture
def webcam(fake_cv2: types.SimpleNamespace, tmp_path: Path) -> Iterator[WebcamSource]:
    source = WebcamSource(0, tmp_path / "captures", timeout_s=2)
    yield source
    source.close()


def test_capture_returns_a_jpeg_kept_in_capture_dir(
    webcam: WebcamSource, tmp_path: Path
) -> None:
    frame = webcam.capture()

    assert frame.content_type == "image/jpeg"
    assert frame.filename.endswith(".jpg")
    assert frame.source_path == (tmp_path / "captures" / frame.filename).resolve()
    assert frame.source_path.read_bytes() == frame.data
    assert frame.data.startswith(b"\xff\xd8")
    assert frame.ground_truth is None


def test_capture_waits_for_a_frame_read_after_the_call(
    webcam: WebcamSource, camera: FakeCamera
) -> None:
    time.sleep(0.05)
    reads_before = camera.reads

    frame = webcam.capture()

    number = int(frame.data.rsplit(b" ", 1)[1])
    assert number > reads_before


def test_requests_the_configured_resolution(
    fake_cv2: types.SimpleNamespace, camera: FakeCamera, tmp_path: Path
) -> None:
    source = WebcamSource(1, tmp_path, width=640, height=480, timeout_s=2)
    source.close()

    assert fake_cv2.opened_with[0][0] == 1
    assert camera.props == {3: 640, 4: 480}


def test_close_releases_the_camera_once(
    webcam: WebcamSource, camera: FakeCamera
) -> None:
    webcam.close()
    webcam.close()

    assert camera.released


def test_unopenable_camera_is_a_camera_error(
    fake_cv2: types.SimpleNamespace, camera: FakeCamera, tmp_path: Path
) -> None:
    camera.opened = False

    with pytest.raises(CameraError, match="cannot open webcam 0"):
        WebcamSource(0, tmp_path)


def test_camera_that_stops_sending_frames_fails_the_capture(
    webcam: WebcamSource, camera: FakeCamera
) -> None:
    camera.broken = True

    with pytest.raises(CameraError, match="webcam 0"):
        webcam.capture()


def test_missing_opencv_is_a_camera_error(
    monkeypatch: pytest.MonkeyPatch, tmp_path: Path
) -> None:
    monkeypatch.setitem(sys.modules, "cv2", None)

    with pytest.raises(CameraError, match="requirements-webcam.txt"):
        WebcamSource(0, tmp_path)


def test_preview_is_off_on_macos(
    fake_cv2: types.SimpleNamespace, tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    monkeypatch.setattr(sys, "platform", "darwin")
    source = WebcamSource(0, tmp_path / "captures", preview=True, timeout_s=2)
    try:
        source.capture()
    finally:
        source.close()

    assert fake_cv2.shown == []


def test_failing_preview_does_not_stop_the_captures(
    fake_cv2: types.SimpleNamespace, tmp_path: Path, monkeypatch: pytest.MonkeyPatch
) -> None:
    monkeypatch.setattr(sys, "platform", "linux")

    def broken_imshow(title: str, image: FakeImage) -> None:
        raise RuntimeError("cannot open display")

    fake_cv2.imshow = broken_imshow
    source = WebcamSource(0, tmp_path / "captures", preview=True, timeout_s=2)
    try:
        first = source.capture()
        second = source.capture()
    finally:
        source.close()

    assert first.data != second.data


def test_webcam_example_config_builds_the_device_camera(
    fake_cv2: types.SimpleNamespace,
) -> None:
    config = load_device_config(DEVICE_ROOT / "config" / "webcam.example.toml", env={})

    camera = build_camera(config.camera)
    try:
        assert isinstance(camera, WebcamSource)
        assert config.trigger.type == "manual"
        assert config.backend.protocol == PROTOCOL_DEVICE_IMAGES
    finally:
        camera.close()


class StubWebcam:
    """Emulator-side camera double: counts captures, never runs out."""

    def __init__(self, folder: Path) -> None:
        self._folder = folder
        self.captures = 0
        self.closed = 0

    def capture(self):  # type: ignore[no-untyped-def]
        from shelf_agent.models import Frame, utc_now

        self.captures += 1
        path = self._folder / f"cam_{self.captures}.jpg"
        path.write_bytes(b"\xff\xd8 cam %d" % self.captures)
        return Frame(
            data=path.read_bytes(),
            filename=path.name,
            content_type="image/jpeg",
            captured_at=utc_now(),
            source_path=path,
        )

    def close(self) -> None:
        self.closed += 1


@pytest.fixture
def server(tmp_path: Path) -> Iterator[MockBackend]:
    backend = MockBackend(("127.0.0.1", 0), tmp_path / "received")
    thread = threading.Thread(target=backend.serve_forever, daemon=True)
    thread.start()
    yield backend
    backend.shutdown()
    backend.server_close()


@pytest.fixture
def opened() -> list[tuple[int, Sequence[str]]]:
    return []


@pytest.fixture
def webcam_factory(
    tmp_path: Path, opened: list[tuple[int, Sequence[str]]]
) -> Callable[[int, WebcamSettings, Sequence[str]], CameraSource]:
    def factory(
        index: int, settings: WebcamSettings, names: Sequence[str]
    ) -> CameraSource:
        opened.append((index, list(names)))
        return StubWebcam(tmp_path)

    return factory


def webcam_scenario(server: MockBackend, tmp_path: Path) -> Scenario:
    return Scenario(
        backend=BackendConfig(
            base_url=server.base_url, timeout_s=2, protocol=PROTOCOL_DEVICE_IMAGES
        ),
        shelves=(
            EmulatedShelf("W1", DeviceIdentity(None, 31, "cam-w1"), (), webcam=0),
            EmulatedShelf("W2", DeviceIdentity(None, 32, "cam-w2"), (), webcam=0),
        ),
        labels={},
        outbox_dir=tmp_path / "outbox",
        heartbeat_interval_s=60,
    )


def test_emulator_uploads_webcam_photos_through_the_device_path(
    server: MockBackend,
    tmp_path: Path,
    webcam_factory: Callable[..., CameraSource],
    opened: list[tuple[int, Sequence[str]]],
) -> None:
    emulator = Emulator(
        webcam_scenario(server, tmp_path),
        output=lambda _: None,
        webcam_factory=webcam_factory,
    )
    emulator.connect()

    results = [emulator.step_next() for _ in range(5)]

    assert opened == [(0, ["W1", "W2"])]
    assert all(r is not None and r[1].delivery is Delivery.SENT for r in results)
    assert [e.protocol for e in server.events] == ["device-images"] * 5
    assert {e.shelf_id for e in server.events} == {31, 32}
    assert Path(server.events[0].image_path).read_bytes().startswith(b"\xff\xd8")
    assert "webcam 0" in emulator.status_lines()[0]


def test_emulator_console_reports_camera_errors_and_keeps_running(
    server: MockBackend, tmp_path: Path
) -> None:
    class BrokenWebcam(StubWebcam):
        def capture(self):  # type: ignore[no-untyped-def]
            raise CameraError("webcam 0 stopped sending frames")

    lines: list[str] = []
    emulator = Emulator(
        webcam_scenario(server, tmp_path),
        output=lines.append,
        webcam_factory=lambda index, settings, names: BrokenWebcam(tmp_path),
    )
    emulator.connect()
    commands = iter(["W1", "status", "q"])

    emulator.run_interactive(threading.Event(), input_fn=lambda _: next(commands))

    assert any("camera error" in line for line in lines)
    assert any("webcam 0" in line for line in lines)
