"""End-to-end: emulator -> shared agent/client -> real HTTP -> mock backend."""

from __future__ import annotations

import json
import threading
from collections.abc import Callable, Iterator
from dataclasses import replace
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

import pytest

from shelf_agent.agent import Delivery
from shelf_agent.config import (
    PROTOCOL_DEVICE_IMAGES,
    PROTOCOL_MULTIPART,
    BackendConfig,
    DeviceIdentity,
)
from shelf_agent.emulator.runner import Emulator
from shelf_agent.emulator.scenario import EmulatedShelf, Scenario
from shelf_agent.mock_server import MockBackend
from shelf_agent.models import Frame, GroundTruth
from shelf_agent.registration import RegistrationError
from shelf_agent.transport.client import BackendClient, BackendError


@pytest.fixture
def server(tmp_path: Path) -> Iterator[MockBackend]:
    backend = MockBackend(("127.0.0.1", 0), tmp_path / "received")
    thread = threading.Thread(target=backend.serve_forever, daemon=True)
    thread.start()
    yield backend
    backend.shutdown()
    backend.server_close()


@pytest.fixture
def scenario(
    tmp_path: Path, server: MockBackend, write_images: Callable[..., list[Path]]
) -> Scenario:
    a_frames = write_images("a_01.jpg", "a_02.jpg")
    b_frames = write_images("b_01.jpg")
    return Scenario(
        backend=BackendConfig(base_url=server.base_url, timeout_s=2),
        shelves=(
            EmulatedShelf("A1", DeviceIdentity(None, 11, "key-a1"), tuple(a_frames)),
            EmulatedShelf("B1", DeviceIdentity(None, 22, "key-b1"), tuple(b_frames)),
        ),
        labels={"a_01.jpg": GroundTruth("MILK-1L", 6)},
        outbox_dir=tmp_path / "outbox",
        heartbeat_interval_s=60,
    )


def connected(scenario: Scenario) -> Emulator:
    emulator = Emulator(scenario, output=lambda _: None)
    emulator.connect()
    return emulator


def test_registers_each_shelf_as_its_own_device(
    scenario: Scenario, server: MockBackend
) -> None:
    connected(scenario)

    devices = {d.device_key: d.shelf_id for d in server.devices.values()}
    assert devices == {"key-a1": 11, "key-b1": 22}


def test_reuses_existing_devices_instead_of_registering_twice(
    scenario: Scenario, server: MockBackend
) -> None:
    connected(scenario)
    connected(scenario)

    assert len(server.devices) == 2


def test_sends_image_events_as_published_in_swagger(
    scenario: Scenario, server: MockBackend, tmp_path: Path
) -> None:
    emulator = connected(scenario)

    emulator.step("A1")
    emulator.step("B1")

    first, second = server.events
    assert (first.device_id, first.shelf_id, first.protocol) == (1, 11, "image-events")
    assert (second.device_id, second.shelf_id) == (2, 22)
    assert Path(first.image_path) == (tmp_path / "images" / "a_01.jpg").resolve()
    assert "Z" not in first.captured_at and "+" not in first.captured_at


def test_fails_when_device_missing_and_registration_disabled(
    scenario: Scenario,
) -> None:
    with pytest.raises(RegistrationError):
        connected(replace(scenario, register_devices=False))


def test_fails_fast_when_backend_is_down_and_ids_unknown(scenario: Scenario) -> None:
    offline = replace(
        scenario, backend=BackendConfig(base_url="http://127.0.0.1:9", timeout_s=0.5)
    )

    with pytest.raises(BackendError):
        connected(offline)


def test_step_next_round_robins_until_all_frames_sent(
    scenario: Scenario, server: MockBackend
) -> None:
    emulator = connected(scenario)

    while emulator.step_next() is not None:
        pass

    assert [Path(e.image_path).name for e in server.events] == [
        "a_01.jpg",
        "b_01.jpg",
        "a_02.jpg",
    ]


def test_step_next_continues_after_explicitly_stepped_shelf(
    scenario: Scenario, server: MockBackend
) -> None:
    emulator = connected(scenario)

    emulator.step("A1")
    emulator.step_next()

    assert [Path(e.image_path).name for e in server.events] == ["a_01.jpg", "b_01.jpg"]


def test_run_auto_sends_everything_and_heartbeats(
    scenario: Scenario, server: MockBackend
) -> None:
    emulator = connected(scenario)

    emulator.run_auto(threading.Event(), interval_s=0.01)

    assert len(server.events) == 3
    assert set(server.heartbeats) == {1, 2}


def test_queues_frames_while_backend_is_down_and_delivers_later(
    scenario: Scenario, server: MockBackend
) -> None:
    registered = server.add_device("key-a1", 11)  # e.g. by an administrator
    shelf = replace(
        scenario.shelves[0], identity=DeviceIdentity(registered.id, 11, "key-a1")
    )
    known = replace(scenario, shelves=(shelf,))
    offline = replace(
        known, backend=BackendConfig(base_url="http://127.0.0.1:9", timeout_s=0.5)
    )
    down = connected(offline)  # starts anyway: device_id is configured
    first = down.step("A1")
    down.close()
    assert first is not None and first.delivery is Delivery.QUEUED

    up = connected(known)  # same outbox directory, backend reachable again
    up.step("A1")

    assert [Path(e.image_path).name for e in server.events] == ["a_01.jpg", "a_01.jpg"]


def test_interactive_console_runs_commands_until_quit(
    scenario: Scenario, server: MockBackend
) -> None:
    lines: list[str] = []
    commands = iter(["B1", "", "status", "bogus", "q"])
    emulator = Emulator(scenario, output=lines.append)
    emulator.connect()

    emulator.run_interactive(threading.Event(), input_fn=lambda _: next(commands))

    assert [Path(e.image_path).name for e in server.events] == ["b_01.jpg", "a_01.jpg"]
    output = "\n".join(lines)
    assert "device key 'key-a1' -> device id 1, shelf 11" in output
    assert "[A1] a_01.jpg -> sent, imageEvent #2 | expected MILK-1L x6" in output
    assert "unknown command or shelf: 'bogus'" in output
    assert "1/2 left" in output


def test_multipart_protocol_uploads_the_image_bytes(
    scenario: Scenario, server: MockBackend
) -> None:
    multipart = replace(scenario.backend, protocol=PROTOCOL_MULTIPART)
    emulator = connected(replace(scenario, backend=multipart))

    emulator.step("A1")

    (event,) = server.events
    assert event.protocol == "multipart"
    assert event.device_key == "key-a1"
    assert event.captured_at.endswith("Z")
    assert Path(event.image_path).read_bytes() == b"a_01.jpg"


def test_device_images_protocol_uploads_the_image_to_the_device(
    scenario: Scenario, server: MockBackend
) -> None:
    device_images = replace(scenario.backend, protocol=PROTOCOL_DEVICE_IMAGES)
    emulator = connected(replace(scenario, backend=device_images))

    emulator.step("A1")

    (event,) = server.events
    assert (event.protocol, event.device_id, event.shelf_id) == ("device-images", 1, 11)
    assert "Z" not in event.captured_at and "+" not in event.captured_at
    assert Path(event.image_path).read_bytes() == b"a_01.jpg"


def device_images_client(server: MockBackend, shelf_id: int = 11) -> BackendClient:
    device = server.add_device(f"key-{shelf_id}", shelf_id)
    config = BackendConfig(
        base_url=server.base_url, timeout_s=2, protocol=PROTOCOL_DEVICE_IMAGES
    )
    return BackendClient(config, DeviceIdentity(device.id, shelf_id, device.device_key))


def test_device_images_queues_frame_when_vision_fails(
    scenario: Scenario, server: MockBackend
) -> None:
    server.vision_failures = 1
    device_images = replace(scenario.backend, protocol=PROTOCOL_DEVICE_IMAGES)
    emulator = connected(replace(scenario, backend=device_images))

    first = emulator.step("A1")

    assert first is not None and first.delivery is Delivery.QUEUED
    assert server.events == []
    emulator.step("A1")  # the queued frame goes first
    assert server.events[0].image_path.endswith("a_01.jpg")


def test_device_images_vision_failure_is_retryable_502(
    server: MockBackend, make_frame: Callable[..., Frame]
) -> None:
    server.vision_failures = 1
    client = device_images_client(server)

    with pytest.raises(BackendError) as failed:
        client.upload_image(make_frame())

    assert (failed.value.status_code, failed.value.retryable) == (502, True)
    client.upload_image(make_frame())
    assert len(server.events) == 1


def test_device_images_rejects_frame_not_newer_than_processed_one(
    server: MockBackend, make_frame: Callable[..., Frame]
) -> None:
    client = device_images_client(server)
    client.upload_image(make_frame())

    with pytest.raises(BackendError) as failed:
        client.upload_image(make_frame())  # same capturedAt, e.g. a retry

    assert (failed.value.status_code, failed.value.retryable) == (409, False)
    assert len(server.events) == 1


def test_device_images_rejects_inactive_device(
    server: MockBackend, make_frame: Callable[..., Frame]
) -> None:
    client = device_images_client(server)
    server.devices[1].status = "INACTIVE"

    with pytest.raises(BackendError) as failed:
        client.upload_image(make_frame())

    assert (failed.value.status_code, failed.value.retryable) == (409, False)
    assert server.events == []


class FakeVision(ThreadingHTTPServer):
    """``POST /recognize`` answering ``counts`` in turn and keeping the uploads."""

    def __init__(self, counts: list[int]) -> None:
        super().__init__(("127.0.0.1", 0), _FakeVisionHandler)
        self.counts = counts
        self.uploads: list[bytes] = []

    @property
    def base_url(self) -> str:
        host, port = self.server_address[:2]
        return f"http://{host!s}:{port}"


class _FakeVisionHandler(BaseHTTPRequestHandler):
    server: FakeVision

    def do_POST(self) -> None:
        body = self.rfile.read(int(self.headers.get("Content-Length", 0)))
        self.server.uploads.append(body)
        payload = json.dumps({"count": self.server.counts.pop(0)}).encode()
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)

    def log_message(self, format: str, *args: object) -> None:
        pass


@pytest.fixture
def vision() -> Iterator[FakeVision]:
    fake = FakeVision([16, 15])
    thread = threading.Thread(target=fake.serve_forever, daemon=True)
    thread.start()
    yield fake
    fake.shutdown()
    fake.server_close()


def test_device_images_returns_vision_count_as_quantity(
    scenario: Scenario, server: MockBackend, vision: FakeVision
) -> None:
    server.vision_url = vision.base_url
    device_images = replace(scenario.backend, protocol=PROTOCOL_DEVICE_IMAGES)
    emulator = connected(replace(scenario, backend=device_images))

    first = emulator.step("A1")
    second = emulator.step("A1")

    assert first is not None and first.receipt is not None
    assert second is not None and second.receipt is not None
    assert (first.receipt.quantity, second.receipt.quantity) == (16, 15)
    assert b"a_01.jpg" in vision.uploads[0] and b"a_02.jpg" in vision.uploads[1]
    assert server.set_quantity(11, 0) == 15


def test_device_images_unreachable_vision_is_retryable_502(
    server: MockBackend, make_frame: Callable[..., Frame]
) -> None:
    server.vision_url = "http://127.0.0.1:9"  # nothing listens on the discard port
    client = device_images_client(server)

    with pytest.raises(BackendError) as failed:
        client.upload_image(make_frame())

    assert (failed.value.status_code, failed.value.retryable) == (502, True)
    assert server.events == []
