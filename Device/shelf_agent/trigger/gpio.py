"""Sensor trigger on a Raspberry Pi GPIO pin (IR barrier, door or reed switch).

``gpiozero`` is imported inside the constructor so the package imports on a PC.
"""

from __future__ import annotations

import threading


class GpioTrigger:
    """Fires after a shelf sensor was activated and released again.

    Capturing on release, plus a short settle delay, keeps the worker's hand
    out of the picture.

    Args:
        pin: BCM pin number of the sensor.
        settle_s: Delay between release and capture.
        poll_s: How often ``stop`` is re-checked while waiting for the sensor.
    """

    def __init__(self, pin: int, *, settle_s: float = 2.0, poll_s: float = 0.5) -> None:
        from gpiozero import Button

        self._sensor = Button(pin)
        self._settle_s = settle_s
        self._poll_s = poll_s

    def wait(self, stop: threading.Event) -> bool:
        while not stop.is_set():
            if self._sensor.wait_for_press(timeout=self._poll_s):
                break
        while not stop.is_set():
            if self._sensor.wait_for_release(timeout=self._poll_s):
                break
        return not stop.wait(self._settle_s)

    def close(self) -> None:
        self._sensor.close()
