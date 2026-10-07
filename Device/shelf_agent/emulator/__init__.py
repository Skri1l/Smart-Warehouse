"""PC emulator: several virtual shelves replaying labeled test images.

The emulator only swaps the camera for :class:`FolderSource`; every frame
still goes through :class:`ShelfAgent` and :class:`BackendClient`.
"""

from shelf_agent.emulator.runner import Emulator
from shelf_agent.emulator.scenario import EmulatedShelf, Scenario, load_scenario

__all__ = ["EmulatedShelf", "Emulator", "Scenario", "load_scenario"]
