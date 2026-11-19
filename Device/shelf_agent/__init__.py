"""Smart Warehouse shelf agent.

One code base runs on the Raspberry Pi 5 shelf controller and on a developer PC
as an emulator. Only the image source (camera vs. folder) and the trigger
(sensor vs. timer/keyboard) differ; capture delivery, retries and device
authentication are shared. See Documentation/adr/0008-shelf-agent-shared-core.md.
"""

__version__ = "0.1.0"
