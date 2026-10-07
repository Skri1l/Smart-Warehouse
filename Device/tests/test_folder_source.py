from __future__ import annotations

from collections.abc import Callable
from pathlib import Path

import pytest

from shelf_agent.camera.base import SourceExhausted
from shelf_agent.camera.folder import FolderSource
from shelf_agent.models import GroundTruth
from tests.conftest import CAPTURED_AT


def test_returns_images_in_given_order(write_images: Callable[..., list[Path]]) -> None:
    source = FolderSource(write_images("b.jpg", "a.jpg"))

    assert [source.capture().filename for _ in range(2)] == ["b.jpg", "a.jpg"]


def test_frame_carries_file_bytes_and_content_type(
    write_images: Callable[..., list[Path]],
) -> None:
    frame = FolderSource(write_images("shot.png")).capture()

    assert frame.data == b"shot.png"
    assert frame.content_type == "image/png"


def test_raises_when_frames_run_out(write_images: Callable[..., list[Path]]) -> None:
    source = FolderSource(write_images("a.jpg"))
    source.capture()

    with pytest.raises(SourceExhausted):
        source.capture()
    assert source.remaining == 0


def test_starts_over_when_loop_is_enabled(
    write_images: Callable[..., list[Path]]
) -> None:
    source = FolderSource(write_images("a.jpg", "b.jpg"), loop=True)

    names = [source.capture().filename for _ in range(3)]

    assert names == ["a.jpg", "b.jpg", "a.jpg"]


def test_attaches_ground_truth_when_label_exists(
    write_images: Callable[..., list[Path]],
) -> None:
    labels = {"a.jpg": GroundTruth("MILK-1L", 6)}
    source = FolderSource(write_images("a.jpg", "b.jpg"), labels=labels)

    assert source.capture().ground_truth == GroundTruth("MILK-1L", 6)
    assert source.capture().ground_truth is None


def test_stamps_capture_time_from_clock(
    write_images: Callable[..., list[Path]]
) -> None:
    source = FolderSource(write_images("a.jpg"), clock=lambda: CAPTURED_AT)

    assert source.capture().captured_at == CAPTURED_AT


def test_gives_each_frame_a_unique_event_id(
    write_images: Callable[..., list[Path]],
) -> None:
    source = FolderSource(write_images("a.jpg"), loop=True)

    assert source.capture().event_id != source.capture().event_id
