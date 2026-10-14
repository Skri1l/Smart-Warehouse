from __future__ import annotations

from collections.abc import Callable
from pathlib import Path

from shelf_agent.models import Frame, GroundTruth
from shelf_agent.transport.outbox import Outbox


def test_is_empty_when_directory_does_not_exist(tmp_path: Path) -> None:
    outbox = Outbox(tmp_path / "missing")

    assert len(outbox) == 0
    assert outbox.peek() is None


def test_returns_frames_in_insertion_order(
    tmp_path: Path, make_frame: Callable[..., Frame]
) -> None:
    outbox = Outbox(tmp_path)
    for name in ["1.jpg", "2.jpg", "3.jpg"]:
        outbox.put(make_frame(name=name))

    names = []
    while (frame := outbox.peek()) is not None:
        names.append(frame.filename)
        outbox.pop()

    assert names == ["1.jpg", "2.jpg", "3.jpg"]


def test_restores_frame_exactly_when_reopened(
    tmp_path: Path, make_frame: Callable[..., Frame]
) -> None:
    original = make_frame(data=b"bytes", truth=GroundTruth("MILK-1L", 4))
    Outbox(tmp_path).put(original)

    restored = Outbox(tmp_path).peek()

    assert restored == original


def test_moves_frame_to_rejected_when_rejected(
    tmp_path: Path, make_frame: Callable[..., Frame]
) -> None:
    outbox = Outbox(tmp_path)
    outbox.put(make_frame(name="bad.jpg"))
    outbox.put(make_frame(name="next.jpg"))

    outbox.reject()

    assert len(outbox) == 1
    assert outbox.rejected_count() == 1
    peeked = outbox.peek()
    assert peeked is not None and peeked.filename == "next.jpg"


def test_ignores_half_written_entries(
    tmp_path: Path, make_frame: Callable[..., Frame]
) -> None:
    (tmp_path / "00000000000000000001-x.json.tmp").write_text("{")
    outbox = Outbox(tmp_path)

    assert len(outbox) == 0
    outbox.put(make_frame())
    assert len(outbox) == 1
