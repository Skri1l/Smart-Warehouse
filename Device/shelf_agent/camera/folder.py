"""Emulated camera: returns pre-recorded images in a fixed order."""

from __future__ import annotations

import mimetypes
from collections.abc import Callable, Mapping, Sequence
from datetime import datetime
from pathlib import Path

from shelf_agent.camera.base import SourceExhausted
from shelf_agent.models import Frame, GroundTruth, utc_now


class FolderSource:
    """Replays a sequence of image files as if a camera had just taken them.

    The order matters: the backend derives the stock delta from consecutive
    frames of the same shelf, so a scenario lists images in the order the
    shelf content changed (6 items -> 5 -> 3 ...).

    Args:
        images: Image files in capture order.
        labels: Ground truth by file name; attached to frames for logging only.
        loop: Start over after the last image instead of raising
            :class:`SourceExhausted`.
        clock: Source of ``captured_at``; the real wall clock by default.
    """

    def __init__(
        self,
        images: Sequence[Path],
        *,
        labels: Mapping[str, GroundTruth] | None = None,
        loop: bool = False,
        clock: Callable[[], datetime] = utc_now,
    ) -> None:
        self._images = list(images)
        self._labels = dict(labels or {})
        self._loop = loop
        self._clock = clock
        self._index = 0

    @property
    def remaining(self) -> int:
        """Frames left before the source is exhausted (ignores looping)."""
        return len(self._images) - self._index

    @property
    def total(self) -> int:
        """Number of images in one pass of the sequence."""
        return len(self._images)

    def capture(self) -> Frame:
        if self._index >= len(self._images):
            if not (self._loop and self._images):
                raise SourceExhausted("no frames left")
            self._index = 0

        path = self._images[self._index]
        self._index += 1
        content_type = mimetypes.guess_type(path.name)[0] or "application/octet-stream"
        return Frame(
            data=path.read_bytes(),
            filename=path.name,
            content_type=content_type,
            captured_at=self._clock(),
            ground_truth=self._labels.get(path.name),
        )

    def close(self) -> None:
        """Nothing to release; files are read on demand."""
