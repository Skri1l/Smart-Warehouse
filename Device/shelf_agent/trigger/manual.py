"""Keyboard trigger: press Enter to capture. Handy when bringing up a new device."""

from __future__ import annotations

import threading
from collections.abc import Callable


class ManualTrigger:
    """Fires each time the operator presses Enter; ``q`` quits.

    Args:
        input_fn: Line reader, injectable for tests.
    """

    def __init__(self, input_fn: Callable[[str], str] = input) -> None:
        self._input = input_fn

    def wait(self, stop: threading.Event) -> bool:
        if stop.is_set():
            return False
        try:
            line = self._input("Enter = capture, q = quit > ")
        except EOFError:
            return False
        return line.strip().lower() not in {"q", "quit", "exit"}

    def close(self) -> None:
        """Nothing to release."""
