"""Trigger port: decides when the next picture is taken."""

from __future__ import annotations

import threading
from typing import Protocol


class Trigger(Protocol):
    """Blocks until a shelf event happens."""

    def wait(self, stop: threading.Event) -> bool:
        """Block until the next capture is due.

        Args:
            stop: Set by the caller to shut the agent down.

        Returns:
            ``True`` to capture now, ``False`` if ``stop`` was set while waiting.
        """
        ...

    def close(self) -> None:
        """Release the underlying sensor, if any."""
        ...
