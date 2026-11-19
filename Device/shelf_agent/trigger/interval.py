"""Time-based trigger: periodic snapshots, also used by the emulator's auto mode."""

from __future__ import annotations

import threading


class IntervalTrigger:
    """Fires every ``interval_s`` seconds.

    Args:
        interval_s: Pause between captures.
        fire_immediately: Fire on the first call without waiting.
    """

    def __init__(self, interval_s: float, *, fire_immediately: bool = True) -> None:
        if interval_s <= 0:
            raise ValueError("interval_s must be positive")
        self._interval_s = interval_s
        self._pending_first = fire_immediately

    def wait(self, stop: threading.Event) -> bool:
        if self._pending_first:
            self._pending_first = False
            return not stop.is_set()
        return not stop.wait(self._interval_s)

    def close(self) -> None:
        """Nothing to release."""
