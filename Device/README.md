# Device — shelf agent and emulator

Software for the shelf controller (Raspberry Pi 5 + camera), plus a PC emulator that pretends to be any number of shelves by replaying labelled test photos.

Both run **the same code**. The emulator only swaps two pieces:

| | Real device | Emulator |
|---|---|---|
| Image source (`CameraSource`) | `Picamera2Source`, camera module | `FolderSource`, files from a scenario |
| Trigger (`Trigger`) | `GpioTrigger` sensor, or `IntervalTrigger` | `IntervalTrigger` (`--auto`) or console commands |
| Agent, HTTP client, outbox, auth | **shared** | **shared** |

Everything a demo shows on the wire is exactly what the Pi will send. See [ADR-0008](../Documentation/adr/0008-shelf-agent-shared-core.md).

```text
Device/
├── shelf_agent/
│   ├── agent.py            ShelfAgent: capture -> deliver, ordered retries (shared)
│   ├── transport/          BackendClient (HTTP, the only wire-protocol code) + Outbox
│   ├── camera/             CameraSource port: picamera.py (Pi), folder.py (emulator)
│   ├── trigger/            Trigger port: gpio.py (Pi), interval.py, manual.py
│   ├── registration.py     Find the device in the backend by key (GET/POST /devices)
│   ├── device.py           Wiring for `run` (config -> camera/trigger/agent)
│   ├── emulator/           Scenario loading + multi-shelf runner/console
│   ├── mock_server.py      Fake backend device endpoints for offline work
│   ├── config.py           Device TOML config
│   └── cli.py              `run` / `emulate` / `mock-server`
├── scenarios/
│   ├── smoke/              Two shelves, tiny placeholder images: checks the upload path
│   └── demo/               Your labelled test photos (images/ + labels.csv)
├── config/device.example.toml
├── deploy/shelf-agent.service   systemd unit for the Pi
└── tests/
```

## Quick start on a PC (emulator)

Python 3.11+.

```bash
cd Device
python -m venv .venv
.venv/Scripts/activate        # Linux/macOS: source .venv/bin/activate
pip install -r requirements.txt -r requirements-dev.txt
```

No backend at hand? Start the mock server in one terminal. It implements the same device endpoints as the backend's Swagger (`/devices`, `/devices/{id}/heartbeat`, `/image-events`) and logs every event:

```bash
python -m shelf_agent mock-server --port 8080
```

In a second terminal, run the smoke scenario. On start every shelf finds its device by `device_key` and registers it if it is missing:

```bash
python -m shelf_agent emulate --scenario scenarios/smoke/scenario.toml
```

```text
  A1       device key 'emulator-a1' -> device id 1, shelf 1
  B1       device key 'emulator-b1' -> device id 2, shelf 2
emulator> A1
  [A1] a1_01.png -> sent, imageEvent #1 | expected SMOKE-RED x3
emulator> all
  [A1] a1_02.png -> sent, imageEvent #2 | expected SMOKE-RED x2
  [B1] b1_01.png -> sent, imageEvent #3 | expected SMOKE-BLUE x2
```

Console commands: `Enter` sends the next shelf's frame (round-robin), `<shelf>` sends from that shelf, then `all`, `flush`, `status`, `help`, `q`. The `expected` part is printed locally from `labels.csv` and is never sent to the backend.

Useful flags:

| Flag | Effect |
|---|---|
| `--auto [--interval 5]` | Every shelf sends on its own timer until its photos run out (unattended demo) |
| `--base-url http://host:8080` | Point at another backend (e.g. the cloud deployment) without editing the scenario |
| `--only A2 B1` | Emulate a subset of shelves |
| `--no-register` | Fail on unknown device keys instead of creating them with `POST /devices` |
| `--reset-outbox` | Drop frames left undelivered by a previous run |
| `-v` / `-vv` | Info / debug logs |

### Against the real backend

1. Start the backend (`Backend/`, see the root README). Swagger UI is at `http://localhost:8080/swagger-ui.html`.
2. Create the warehouse structure the scenario refers to: `POST /warehouses`, `POST /racks/warehouse/{warehouseId}`, then `POST /shelves`. Every `shelf_id` in the scenario must exist. A shelf can hold only one device.
3. Run the scenario. Devices are registered automatically on first start:

```bash
python -m shelf_agent emulate --scenario scenarios/demo/scenario.toml
```

4. Check the result in Swagger: `GET /devices`, then `GET /image-events/device/{deviceId}`.

### The demo photos

`scenarios/demo` holds the 25 labelled Coca-Cola photos. `labels.csv` (`file,sku,count`) is the ground truth and is shared with the ML accuracy tests ([ADR-0010](../Documentation/adr/0010-labelled-test-images-shared-by-emulator-and-ml.md)). The files are named `1.jpg … 25.jpg`, so the scenario lists them explicitly: a glob would sort them as text (`1, 10, 11, …, 2`). To add photos, see [images/README.md](scenarios/demo/images/README.md).

### Scenario format

```toml
images_dir = "images"          # relative to this file
labels = "labels.csv"          # optional
interval_s = 15                # --auto period
heartbeat_interval_s = 30
outbox_dir = ".outbox"         # undelivered frames, one folder per shelf

register_devices = true        # POST /devices for unknown keys (--no-register)

[backend]
base_url = "http://127.0.0.1:8080"
# protocol = "image-events"    # or "multipart" (RFC-0001), see below
# timestamp_zone = "local"     # zone of capturedAt (LocalDateTime): local | utc

[[shelves]]
name = "A2"
shelf_id = 2                   # must exist in the backend
device_key = "emulator-a2"     # default: emulator-<name>
# device_id = 1                # generated by the backend; normally left out
frames = ["shelf-a2_*.jpg"]    # globs are sorted; plain names keep their order
loop = false                   # true = start over after the last photo
```

## What is sent

The agent uses exactly the endpoints the backend publishes in Swagger (`/v3/api-docs`). See [ADR-0009](../Documentation/adr/0009-device-transport-http-with-outbox.md).

**On start:** `GET /devices`, find the row whose `deviceKey` matches, and check that its `shelfId` matches the config. If the key is unknown and registration is on, `POST /devices {"deviceKey", "shelfId"}` (201 + id). The backend generates device ids, which is why `device_id` is normally not configured.

**Per frame** (`protocol = "image-events"`, the default):

```text
POST /image-events
Content-Type: application/json

{"deviceId": 1, "imagePath": "D:/…/scenarios/demo/images/5.jpg", "capturedAt": "2026-10-08T19:56:34.239"}
```

- Response `201` with the new `ImageEvent` id as the body.
- `imagePath` is where the image is stored on the sending machine: the scenario file for the emulator, `[camera] capture_dir` on the Pi. **The backend has no image upload yet**, so the image bytes do not reach the server. Recognition can only read the image if it runs where that path exists (e.g. backend, ML and emulator on the demo laptop).
- `capturedAt` is a `LocalDateTime`, sent without a zone in `timestamp_zone` (`local` by default, the same clock the backend uses for `receivedAt`).

**Heartbeat:** `PATCH /devices/{id}/heartbeat` every `heartbeat_interval_s` (204).

**Errors:**
- `5xx`, `408`, `429` or no connection: the frame stays in the outbox and is retried in order. Any later frame waits behind it.
- Other `4xx`: the frame goes to `<outbox>/rejected/` and is never retried. Fix the config or the device registration, then re-send manually if needed.

`protocol = "multipart"` switches to the RFC-0001 upload of the image itself (`POST /api/v1/device-events/images`, `X-Device-Key` header, `clientEventId`, UTC `capturedAt`). The mock server supports it; the backend does not yet.

## Running on the Raspberry Pi 5

Raspberry Pi OS Bookworm (64-bit), camera module connected.

```bash
sudo apt install -y python3-picamera2 python3-gpiozero
sudo mkdir -p /opt/shelf-agent && sudo chown $USER /opt/shelf-agent
cp -r Device/* /opt/shelf-agent/ && cd /opt/shelf-agent
# --system-site-packages: picamera2 only exists as a system package
python3 -m venv --system-site-packages .venv
.venv/bin/pip install -r requirements.txt
```

Configure:

```bash
sudo mkdir -p /etc/shelf-agent /var/lib/shelf-agent
sudo cp config/device.example.toml /etc/shelf-agent/device.toml   # set base_url, shelf_id, outbox_dir/capture_dir under /var/lib/shelf-agent
echo "SW_DEVICE_KEY=<deviceKey registered with POST /devices>" | sudo tee /etc/shelf-agent.env
sudo chmod 600 /etc/shelf-agent.env
```

Try it by hand first with `type = "manual"` in `[trigger]`, which captures on Enter:

```bash
SW_DEVICE_KEY=... .venv/bin/python -m shelf_agent run --config /etc/shelf-agent/device.toml -v
```

Then install the service (adjust `User=` in the unit file):

```bash
sudo cp deploy/shelf-agent.service /etc/systemd/system/
sudo systemctl daemon-reload && sudo systemctl enable --now shelf-agent
journalctl -u shelf-agent -f
```

The same `run` command works on a PC with `camera.type = "folder"`. That is handy for testing a device config without the hardware.

## Quality gates

Conventions: [CODE_STYLE_DEVICE.md](../Documentation/CODE_STYLE_DEVICE.md). CI: `.github/workflows/device-ci.yml`.

```bash
ruff check . --fix
black .
mypy .
pytest
```
