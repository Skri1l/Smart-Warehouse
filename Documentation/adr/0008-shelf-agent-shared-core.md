# 0008. One Python shelf agent for the Raspberry Pi and the PC emulator

Status: Proposed
Date: 2026-10-07

## Context

Each shelf has a controller board with a camera that sends images to the backend (FR-04, FR-05). The board is a Raspberry Pi 5. [MILESTONES.md](../MILESTONES.md) Milestone 4 requires a "simple device simulator", and the project risks section asks for one early so hardware work does not block the pipeline. The Definition of Done accepts "at least one physical or simulated shelf".

Two further constraints:

- There is one physical shelf prototype, but the demo should show a warehouse with several shelves.
- The labelled test photos (25 so far) have to reach the backend the same way real camera frames do. Otherwise the demo proves nothing about the device.

A simulator written separately from the firmware would have its own HTTP code, its own retry behaviour and its own idea of the protocol. Every protocol change would have to be made twice, and the simulator would drift.

## Decision

We will write the shelf agent once, in Python (`Device/shelf_agent`), with the hardware behind two small interfaces (ports):

- `CameraSource`: `Picamera2Source` on the Pi, `FolderSource` (replays labelled images) on a PC;
- `Trigger`: `GpioTrigger` (shelf sensor) on the Pi, `IntervalTrigger`/console commands on a PC.

Everything else is shared and identical in both modes: `ShelfAgent` (capture → deliver), `BackendClient` (the only wire-protocol code), the outbox and device authentication. The emulator is a runner that creates N `ShelfAgent`s with `FolderSource`s, one per virtual shelf, each with its own device identity. Hardware libraries are imported lazily so the package installs and is tested on any PC.

## Alternatives considered

- **Separate simulator script next to the firmware**: rejected. The demo would show the simulator's behaviour, not the device's, and the two would diverge on the first protocol change.
- **Feeding test images through the real camera stack (a virtual V4L2 camera on the Pi)**: rejected. It needs one Pi per emulated shelf, so it can't run on a laptop and doesn't help the multi-shelf demo.
- **Firmware in C/C++ (or MicroPython on a microcontroller)**: rejected. The Pi 5 runs full Linux, `picamera2` is the supported camera API and it is Python. The team already works in Python (frontend, ML), and nothing on the device needs C-level performance.
- **Keep test photos only in ML unit tests**: rejected as the *only* use. The ML tests remain (see [ADR-0010](0010-labelled-test-images-shared-by-emulator-and-ml.md)), but they cannot demonstrate device → backend → stock → UI.

## Consequences

- One protocol implementation. A change in RFC-0001 is a single edit in `transport/client.py`, and the emulator, tests and Pi pick it up together.
- Several emulated shelves can run next to the physical one on the demo laptop, each registered as its own `Device`, and send heartbeats so they show as online.
- Without the Pi at hand, the backend and ML pipeline can be developed against the emulator, and the emulator against `mock-server` before the backend endpoint exists.
- The Python floor is 3.11 (Raspberry Pi OS Bookworm), one version below the frontend. Newer syntax is off-limits in `Device/`.
- Ports add a little indirection compared with a single script. That is acceptable: there are exactly two implementations of each, and the layering rules in [CODE_STYLE_DEVICE.md](../CODE_STYLE_DEVICE.md) keep hardware code out of the shared core.
- The emulator proves the transport and backend path, not the camera. Image quality, lighting and sensor timing still need testing on the physical shelf.
