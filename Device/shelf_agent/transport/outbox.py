"""Disk-backed FIFO of frames that have not been delivered yet (NFR-02, ADR-0009).

The outbox survives restarts, so a shelf that loses Wi-Fi or power keeps its
pictures and sends them, in capture order, once the backend is reachable.
"""

from __future__ import annotations

import json
import os
import time
from datetime import datetime
from pathlib import Path

from shelf_agent.models import Frame, GroundTruth

_META_SUFFIX = ".json"
_DATA_SUFFIX = ".img"


class Outbox:
    """Ordered queue of frames stored as ``<seq>.img`` + ``<seq>.json`` pairs.

    The metadata file is written last via an atomic rename, so an entry is
    either complete or invisible; a crash mid-write never yields a half frame.

    Args:
        directory: Where entries are stored; created on demand.
    """

    def __init__(self, directory: Path) -> None:
        self._directory = directory
        self._rejected = directory / "rejected"

    @property
    def directory(self) -> Path:
        return self._directory

    def __len__(self) -> int:
        return len(self._entries())

    def put(self, frame: Frame) -> None:
        """Append a frame to the end of the queue."""
        _write(self._directory, frame)

    def peek(self) -> Frame | None:
        """Oldest frame, or ``None`` if the queue is empty."""
        entries = self._entries()
        return _read(entries[0]) if entries else None

    def pop(self) -> None:
        """Remove the oldest frame (after it was delivered)."""
        entries = self._entries()
        if entries:
            _delete(entries[0])

    def reject(self) -> None:
        """Move the oldest frame to ``rejected/`` (see :meth:`add_rejected`)."""
        frame = self.peek()
        if frame is not None:
            self.add_rejected(frame)
            self.pop()

    def add_rejected(self, frame: Frame) -> None:
        """Keep a frame the backend refused for good (4xx) for manual inspection.

        The image is stored in ``rejected/`` instead of being silently lost;
        it is never retried automatically.
        """
        _write(self._rejected, frame)

    def rejected_count(self) -> int:
        """Frames in ``rejected/``."""
        return len(Outbox(self._rejected))

    def _entries(self) -> list[Path]:
        if not self._directory.is_dir():
            return []
        return sorted(self._directory.glob(f"*{_META_SUFFIX}"))


def _write(directory: Path, frame: Frame) -> None:
    directory.mkdir(parents=True, exist_ok=True)
    stem = f"{_next_sequence(directory):020d}-{frame.event_id}"
    meta = {
        "filename": frame.filename,
        "content_type": frame.content_type,
        "captured_at": frame.captured_at.isoformat(),
        "event_id": frame.event_id,
        "source_path": str(frame.source_path) if frame.source_path else None,
        "ground_truth": (
            {"sku": frame.ground_truth.sku, "count": frame.ground_truth.count}
            if frame.ground_truth
            else None
        ),
    }
    (directory / f"{stem}{_DATA_SUFFIX}").write_bytes(frame.data)
    tmp = directory / f"{stem}{_META_SUFFIX}.tmp"
    tmp.write_text(json.dumps(meta), encoding="utf-8")
    os.replace(tmp, directory / f"{stem}{_META_SUFFIX}")


def _next_sequence(directory: Path) -> int:
    """Strictly increasing key, even with a coarse (Windows) or backward clock."""
    existing = sorted(directory.glob(f"*{_META_SUFFIX}"))
    last = int(existing[-1].name.split("-", 1)[0]) if existing else 0
    return max(time.time_ns(), last + 1)


def _read(meta_path: Path) -> Frame:
    meta = json.loads(meta_path.read_text(encoding="utf-8"))
    truth = meta.get("ground_truth")
    source_path = meta.get("source_path")
    return Frame(
        data=meta_path.with_suffix(_DATA_SUFFIX).read_bytes(),
        filename=meta["filename"],
        content_type=meta["content_type"],
        captured_at=datetime.fromisoformat(meta["captured_at"]),
        event_id=meta["event_id"],
        ground_truth=GroundTruth(**truth) if truth else None,
        source_path=Path(source_path) if source_path else None,
    )


def _delete(meta_path: Path) -> None:
    meta_path.unlink(missing_ok=True)
    meta_path.with_suffix(_DATA_SUFFIX).unlink(missing_ok=True)
