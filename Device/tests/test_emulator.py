"""End-to-end: emulator -> shared agent/client -> real HTTP -> mock backend."""

from __future__ import annotations

import threading
from collections.abc import Callable, Iterator
from dataclasses import replace
from pathlib import Path

import pytest

from shelf_agent.agent import Delivery
from shelf_agent.config import BackendConfig, DeviceIdentity
from shelf_agent.emulator.runner import Emulator
from shelf_agent.emulator.scenario import EmulatedShelf, Scenario
from shelf_agent.mock_server import MockBackend
from shelf_agent.models import GroundTruth


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
            EmulatedShelf("A1", DeviceIdentity(1, 11, "key-a1"), tuple(a_frames)),
            EmulatedShelf("B1", DeviceIdentity(2, 22, "key-b1"), tuple(b_frames)),
        ),
        labels={"a_01.jpg": GroundTruth("MILK-1L", 6)},
        outbox_dir=tmp_path / "outbox",
        heartbeat_interval_s=60,
    )


def test_each_shelf_uploads_as_its_own_device(
    scenario: Scenario, server: MockBackend
) -> None:
    emulator = Emulator(scenario)

    emulator.step("A1")
    emulator.step("B1")

    first, second = server.images
    assert (first.device_id, first.shelf_id, first.device_key) == (1, 11, "key-a1")
    assert (second.device_id, second.shelf_id, second.device_key) == (2, 22, "key-b1")
    assert first.path.read_bytes() == b"a_01.jpg"
    assert first.captured_at.endswith("Z")
    assert first.client_event_id


def test_step_next_round_robins_until_all_frames_sent(
    scenario: Scenario, server: MockBackend
) -> None:
    emulator = Emulator(scenario)

    while emulator.step_next() is not None:
        pass

    assert [i.filename for i in server.images] == ["a_01.jpg", "b_01.jpg", "a_02.jpg"]


def test_run_auto_sends_everything_and_heartbeats(
    scenario: Scenario, server: MockBackend
) -> None:
    emulator = Emulator(scenario)

    emulator.run_auto(threading.Event(), interval_s=0.01)

    assert len(server.images) == 3
    assert set(server.heartbeats) == {1, 2}


def test_queues_frames_while_backend_is_down_and_delivers_later(
    scenario: Scenario, server: MockBackend
) -> None:
    offline = replace(
        scenario, backend=BackendConfig(base_url="http://127.0.0.1:9", timeout_s=0.5)
    )
    down = Emulator(offline)
    first = down.step("A1")
    down.close()
    assert first is not None and first.delivery is Delivery.QUEUED

    up = Emulator(scenario)  # same outbox directory, backend reachable again
    up.step("A1")

    assert [i.filename for i in server.images] == ["a_01.jpg", "a_01.jpg"]


def test_interactive_console_runs_commands_until_quit(
    scenario: Scenario, server: MockBackend
) -> None:
    lines: list[str] = []
    commands = iter(["B1", "", "status", "bogus", "q"])
    emulator = Emulator(scenario, output=lines.append)

    emulator.run_interactive(threading.Event(), input_fn=lambda _: next(commands))

    assert [i.filename for i in server.images] == ["b_01.jpg", "a_01.jpg"]
    output = "\n".join(lines)
    assert "[A1] a_01.jpg -> sent, imageEvent #2 | expected MILK-1L x6" in output
    assert "unknown command or shelf: 'bogus'" in output
    assert "1/2 left" in output


def test_mock_backend_refuses_upload_without_device_key(
    scenario: Scenario, server: MockBackend
) -> None:
    shelf = replace(scenario.shelves[0], identity=DeviceIdentity(1, 11, ""))
    emulator = Emulator(replace(scenario, shelves=(shelf,)))

    result = emulator.step("A1")

    assert result is not None and result.delivery is Delivery.REJECTED
    assert server.images == []


def test_step_next_continues_after_explicitly_stepped_shelf(
    scenario: Scenario, server: MockBackend
) -> None:
    emulator = Emulator(scenario)

    emulator.step("A1")
    emulator.step_next()

    assert [i.filename for i in server.images] == ["a_01.jpg", "b_01.jpg"]
