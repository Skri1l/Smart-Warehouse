# Device Code Style (Python / Raspberry Pi shelf agent)

The shelf agent in `Device/` runs on the Raspberry Pi 5 shelf controller and, unchanged, as a multi-shelf emulator on a PC. See [ADR-0008](adr/0008-shelf-agent-shared-core.md) for why it is one code base, [ADR-0009](adr/0009-device-transport-http-with-outbox.md) for the transport, and [Device/README.md](../Device/README.md) for usage.

Base style is the same as the frontend: [PEP 8](https://peps.python.org/pep-0008/) enforced by **Ruff** (lint + import order) and **Black** (formatting), with **mypy** for typing. Configuration lives in `Device/pyproject.toml`.

```bash
cd Device
ruff check . --fix
black .
mypy .
pytest
```

Naming, type hints and Google-style docstrings follow [CODE_STYLE_FRONTEND.md](CODE_STYLE_FRONTEND.md#naming), minus the Qt-specific rules. Everything below is specific to the device.

## Python version

`requires-python >= 3.11`, because Raspberry Pi OS Bookworm ships Python 3.11 and `picamera2` exists only as a system package there. Don't use 3.12+ syntax or stdlib APIs (`type X = ...` aliases, PEP 695 generics, `itertools.batched`). Ruff/mypy target `py311` to catch this.

## Layering rules

The point of the package layout is that **the emulator cannot drift from the device**. These rules keep it that way:

| Module | May import | Must not import |
|---|---|---|
| `models.py`, `config.py` | stdlib | anything else in the package |
| `camera/`, `trigger/` | `models` | `transport`, `agent`, `httpx` |
| `transport/` | `models`, `config`, `httpx` | `camera`, `trigger`, `emulator` |
| `registration.py` | `config`, `transport.client` | `httpx` directly, `agent`, `emulator` |
| `agent.py` | the ports (`CameraSource`, `Trigger`, `Uploader`), `transport.outbox` | concrete hardware, `httpx` |
| `device.py`, `emulator/` | everything above | each other |
| `mock_server.py` | stdlib, `transport.client` constants | `agent`, `emulator` |

- **`transport/client.py` is the only wire-protocol code.** Endpoint paths, field names, headers, timestamp format and retry classification live there and nowhere else. It follows the backend's published OpenAPI spec (`/v3/api-docs`). When a controller or DTO changes, update the client and `mock_server.py` together and check the shapes against Swagger, not against the RFC. The emulator and `mock_server` never build a request by hand. A protocol change is one edit, and the emulator demo stays honest.
- **The agent depends on ports, not implementations.** `ShelfAgent` takes a `CameraSource`, an `Uploader` and an `Outbox`. A new camera or sensor is a new class in `camera/`/`trigger/` plus one branch in `device.build_camera`/`build_trigger`. The agent itself does not change.
- **Hardware libraries are imported lazily**, inside the constructor of `Picamera2Source`/`GpioTrigger`, never at module top level. The package must import, and its tests must pass, on a PC without `picamera2`/`gpiozero`.
- **Ground truth never crosses the wire.** `Frame.ground_truth` exists for console output and verification. `BackendClient` must not send it (there is a test for this), because the backend/ML side must not be able to "see the answer" ([ADR-0010](adr/0010-labelled-test-images-shared-by-emulator-and-ml.md)).

## Reliability rules

These follow from NFR-02 and the stock-delta model ([ADR-0009](adr/0009-device-transport-http-with-outbox.md)):

- **Device ids come from the backend.** Configure `device_key` + `shelf_id`; `registration.resolve_identity` looks the id up (`GET /devices`). Never invent ids in scenarios or configs, because they differ between databases.
- **Never drop a frame silently.** A retryable failure goes to the outbox. A final (4xx) failure goes to `outbox/rejected/` with an `ERROR` log.
- **Never reorder frames of one shelf.** While the outbox is non-empty, new frames queue behind it. Don't add "send the fresh one first" shortcuts.
- **`captured_at` is set at capture time** and stays the same through retries. Inside the agent, timestamps are always timezone-aware (UTC). Conversion to the wire format (zone-less `LocalDateTime` for `image-events`, `Z`-suffixed UTC for `multipart`) happens only in `transport/client.py`. A naive datetime reaching the client is a bug (the formatters raise).
- **Classify errors in one place** (`BackendClient._send`). Callers branch on `BackendError.retryable`, never on status codes.
- **Background loops stop on a `threading.Event`.** Use `stop.wait(timeout)` instead of `time.sleep` so `SIGTERM` (systemd) and Ctrl+C stop promptly.

## Configuration & secrets

- Device config and scenarios are **TOML** (`tomllib`, stdlib), and labels are **CSV**. No YAML, so there is no extra dependency on the Pi.
- Relative paths in a config file resolve against the file's directory, not the working directory (`config.resolve_path`).
- The device key of a real device comes from an environment variable (`SW_DEVICE_KEY` by default), never from a committed file (NFR-03). `config/*.toml` is gitignored; only `*.example.toml` templates are committed. Emulator scenarios may contain demo keys in plain text, because those are test values for emulated devices.

## Logging

- `logging.getLogger(__name__)` per module. The CLI configures handlers; library code never calls `basicConfig` or `print`.
- Prefix agent messages with the shelf name (`[A1] ...`) so interleaved multi-shelf output stays readable.
- The emulator's interactive console is a UI. It writes through an injected `output` callable (testable), not through logging.

## Testing

- `pytest`, no hardware. Hardware adapters (`picamera.py`, `gpio.py`) stay thin enough that their logic doesn't need a device. Everything else is tested on a PC and in CI.
- `transport/` is tested against `httpx.MockTransport`. End-to-end tests start `MockBackend` on port `0` and drive the real `Emulator` → `ShelfAgent` → `BackendClient` over loopback HTTP.
- Test names follow the frontend convention: `test_<behavior>_when_<condition>`.
- Bundled configs and scenarios (`config/device.example.toml`, `scenarios/smoke`) are loaded by tests, so a broken template fails CI.

## Dependencies & licensing

Same policy as the rest of the project ([ADR-0004](adr/0004-proprietary-license.md)): MIT/BSD/Apache/LGPL only, every runtime dependency listed in [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md), and `pip-licenses` runs in CI. Keep `requirements.txt` minimal: the Pi installs it over Wi-Fi, and every package is something that can break on ARM. Today it is only `httpx`. `picamera2` and `gpiozero` come from Raspberry Pi OS packages (`apt`) and are deliberately not in `requirements.txt`.
