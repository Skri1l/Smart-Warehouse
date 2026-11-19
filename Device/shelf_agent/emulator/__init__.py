"""PC emulator: several virtual shelves replaying labeled test images or
photographing a real scene with a PC webcam.

The emulator only swaps the camera for :class:`FolderSource` or
:class:`WebcamSource`; every frame still goes through :class:`ShelfAgent` and
:class:`BackendClient`.
"""

from shelf_agent.emulator.runner import Emulator
from shelf_agent.emulator.scenario import (
    EmulatedShelf,
    Scenario,
    WebcamSettings,
    load_scenario,
)

__all__ = ["EmulatedShelf", "Emulator", "Scenario", "WebcamSettings", "load_scenario"]
