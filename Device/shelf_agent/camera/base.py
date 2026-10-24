"""Camera port: anything that can produce a :class:`Frame`."""

from __future__ import annotations

from typing import Protocol

from shelf_agent.models import Frame


class SourceExhausted(Exception):
    """Raised by a finite source (a folder of test images) that has no frames left."""


class CameraError(RuntimeError):
    """The camera could not be opened or stopped delivering pictures."""


class CameraSource(Protocol):
    """Produces shelf images. Implemented by the real camera and by the emulator."""

    def capture(self) -> Frame:
        """Take one picture.

        Raises:
            SourceExhausted: If a finite source has no frames left.
        """
        ...

    def close(self) -> None:
        """Release the underlying device or files."""
        ...
