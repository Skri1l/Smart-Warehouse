from __future__ import annotations

import threading
from collections.abc import Callable
from pathlib import Path

import pytest

from shelf_agent.agent import Delivery, ShelfAgent, run_heartbeat
from shelf_agent.camera.base import SourceExhausted
from shelf_agent.camera.folder import FolderSource
from shelf_agent.models import Frame
from shelf_agent.transport.client import BackendError, UploadReceipt
from shelf_agent.transport.outbox import Outbox
from shelf_agent.trigger.interval import IntervalTrigger


class FakeClient:
    """Records uploads; ``fail_with`` makes the next uploads raise."""

    def __init__(self) -> None:
        self.sent: list[str] = []
        self.heartbeats = 0
        self.fail_with: BackendError | None = None

    def upload_image(self, frame: Frame) -> UploadReceipt:
        if self.fail_with is not None:
            raise self.fail_with
        self.sent.append(frame.filename)
        return UploadReceipt(status_code=202, image_event_id=len(self.sent))

    def heartbeat(self) -> None:
        if self.fail_with is not None:
            raise self.fail_with
        self.heartbeats += 1

    def close(self) -> None:
        pass


OFFLINE = BackendError("connection refused", retryable=True)
FORBIDDEN = BackendError("HTTP 403", retryable=False, status_code=403)


@pytest.fixture
def client() -> FakeClient:
    return FakeClient()


@pytest.fixture
def make_agent(
    tmp_path: Path, write_images: Callable[..., list[Path]], client: FakeClient
) -> Callable[..., ShelfAgent]:
    def factory(*names: str) -> ShelfAgent:
        camera = FolderSource(write_images(*names))
        return ShelfAgent("A1", camera, client, Outbox(tmp_path / "outbox"))

    return factory


def test_sends_frame_when_backend_accepts(
    make_agent: Callable[..., ShelfAgent], client: FakeClient
) -> None:
    result = make_agent("1.jpg").tick()

    assert result.delivery is Delivery.SENT
    assert result.receipt is not None and result.receipt.image_event_id == 1
    assert client.sent == ["1.jpg"]


def test_queues_frame_when_backend_is_unreachable(
    make_agent: Callable[..., ShelfAgent], client: FakeClient
) -> None:
    agent = make_agent("1.jpg")
    client.fail_with = OFFLINE

    result = agent.tick()

    assert result.delivery is Delivery.QUEUED
    assert agent.queued == 1


def test_keeps_capture_order_when_backend_comes_back(
    make_agent: Callable[..., ShelfAgent], client: FakeClient
) -> None:
    agent = make_agent("1.jpg", "2.jpg", "3.jpg")
    client.fail_with = OFFLINE
    agent.tick()
    agent.tick()

    client.fail_with = None
    result = agent.tick()

    assert client.sent == ["1.jpg", "2.jpg", "3.jpg"]
    assert result.delivery is Delivery.SENT
    assert agent.queued == 0


def test_queues_new_frame_behind_old_ones_when_still_offline(
    make_agent: Callable[..., ShelfAgent], client: FakeClient
) -> None:
    agent = make_agent("1.jpg", "2.jpg")
    client.fail_with = OFFLINE
    agent.tick()

    result = agent.tick()

    assert result.delivery is Delivery.QUEUED
    assert agent.queued == 2
    assert client.sent == []


def test_rejects_frame_without_retry_when_backend_refuses_it(
    make_agent: Callable[..., ShelfAgent], client: FakeClient, tmp_path: Path
) -> None:
    agent = make_agent("1.jpg")
    client.fail_with = FORBIDDEN

    result = agent.tick()

    assert result.delivery is Delivery.REJECTED
    assert agent.queued == 0
    assert Outbox(tmp_path / "outbox").rejected_count() == 1


def test_flush_skips_refused_frame_and_delivers_the_rest(
    tmp_path: Path, client: FakeClient, make_frame: Callable[..., Frame]
) -> None:
    outbox = Outbox(tmp_path / "outbox")
    outbox.put(make_frame(name="bad.jpg"))
    outbox.put(make_frame(name="good.jpg"))
    agent = ShelfAgent("A1", FolderSource([]), client, outbox)
    calls = 0

    def upload(frame: Frame) -> UploadReceipt:
        nonlocal calls
        calls += 1
        if calls == 1:
            raise FORBIDDEN
        client.sent.append(frame.filename)
        return UploadReceipt(202, None)

    client.upload_image = upload  # type: ignore[method-assign]

    assert agent.flush() == 1
    assert client.sent == ["good.jpg"]
    assert outbox.rejected_count() == 1


def test_tick_raises_when_source_is_exhausted(
    make_agent: Callable[..., ShelfAgent],
) -> None:
    agent = make_agent("1.jpg")
    agent.tick()

    with pytest.raises(SourceExhausted):
        agent.tick()


def test_run_sends_every_frame_then_stops_when_source_ends(
    make_agent: Callable[..., ShelfAgent], client: FakeClient
) -> None:
    agent = make_agent("1.jpg", "2.jpg")

    agent.run(IntervalTrigger(0.001), threading.Event())

    assert client.sent == ["1.jpg", "2.jpg"]


def test_run_returns_immediately_when_stop_is_set(
    make_agent: Callable[..., ShelfAgent], client: FakeClient
) -> None:
    stop = threading.Event()
    stop.set()

    make_agent("1.jpg").run(IntervalTrigger(10), stop)

    assert client.sent == []


def test_heartbeat_returns_false_instead_of_raising_when_offline(
    make_agent: Callable[..., ShelfAgent], client: FakeClient
) -> None:
    client.fail_with = OFFLINE

    assert make_agent("1.jpg").heartbeat() is False


def test_heartbeat_loop_flushes_outbox_when_backend_is_back(
    make_agent: Callable[..., ShelfAgent], client: FakeClient
) -> None:
    agent = make_agent("1.jpg")
    client.fail_with = OFFLINE
    agent.tick()
    client.fail_with = None
    stop = threading.Event()
    stop.set()  # one round, then exit

    run_heartbeat([agent], interval_s=10, stop=stop)

    assert client.heartbeats == 1
    assert client.sent == ["1.jpg"]
