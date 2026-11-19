"""Runs blocking calls on the global thread pool and reports back via signals."""

from collections.abc import Callable
from typing import Any

from PySide6.QtCore import QObject, QRunnable, QThreadPool, Signal


class _Signals(QObject):
    succeeded = Signal(object)
    failed = Signal(str)


class _Task(QRunnable):
    def __init__(self, fn: Callable[[], Any], signals: _Signals) -> None:
        super().__init__()
        self._fn = fn
        self._signals = signals

    def run(self) -> None:
        try:
            result = self._fn()
        except Exception as exc:  # noqa: BLE001 - surfaced to the UI as text
            self._signals.failed.emit(str(exc))
        else:
            self._signals.succeeded.emit(result)


def run_in_background(
    fn: Callable[[], Any],
    on_success: Callable[[Any], None],
    on_error: Callable[[str], None],
) -> _Signals:
    """Run ``fn`` off the UI thread; callbacks are invoked on the main thread.

    Returns:
        The signal holder; callers keep a reference until the task finishes.
    """
    signals = _Signals()
    signals.succeeded.connect(on_success)
    signals.failed.connect(on_error)
    QThreadPool.globalInstance().start(_Task(fn, signals))
    return signals
