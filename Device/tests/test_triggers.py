from __future__ import annotations

import threading

import pytest

from shelf_agent.trigger.interval import IntervalTrigger
from shelf_agent.trigger.manual import ManualTrigger


def test_interval_fires_immediately_then_waits() -> None:
    trigger = IntervalTrigger(0.01)
    stop = threading.Event()

    assert trigger.wait(stop) is True
    assert trigger.wait(stop) is True


def test_interval_returns_false_when_stopped() -> None:
    trigger = IntervalTrigger(10, fire_immediately=False)
    stop = threading.Event()
    stop.set()

    assert trigger.wait(stop) is False


def test_interval_rejects_non_positive_period() -> None:
    with pytest.raises(ValueError):
        IntervalTrigger(0)


@pytest.mark.parametrize(
    ("line", "expected"), [("", True), ("q", False), ("QUIT", False)]
)
def test_manual_fires_on_enter_and_quits_on_q(line: str, expected: bool) -> None:
    trigger = ManualTrigger(input_fn=lambda _: line)

    assert trigger.wait(threading.Event()) is expected


def test_manual_stops_on_end_of_input() -> None:
    def closed_stdin(_: str) -> str:
        raise EOFError

    assert ManualTrigger(input_fn=closed_stdin).wait(threading.Event()) is False
