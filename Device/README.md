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

No backend yet? Start the mock server in one terminal. It implements the device endpoints from RFC-0001 and saves every received image to `received/device-<id>/`:

```bash
python -m shelf_agent mock-server --port 8080
```

In a second terminal, run the smoke scenario:

```bash
python -m shelf_agent emulate --scenario scenarios/smoke/scenario.toml
```

```text
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
| `--reset-outbox` | Drop frames left undelivered by a previous run |
| `-v` / `-vv` | Info / debug logs |

### Using your own test photos

1. Copy the photos into `scenarios/demo/images/` and name them `<shelf>_<step>.jpg`. Number the steps in the order the shelf content changed; see [images/README.md](scenarios/demo/images/README.md).
2. Label every photo in `scenarios/demo/labels.csv` (`file,sku,count`). The ML accuracy tests read the same file ([ADR-0010](../Documentation/adr/0010-labelled-test-images-shared-by-emulator-and-ml.md)).
3. Adjust the `[[shelves]]` in `scenarios/demo/scenario.toml`. `device_id`, `shelf_id` and `device_key` must match `Device` rows in the backend. Create them with `POST /devices`.
4. `python -m shelf_agent emulate --scenario scenarios/demo/scenario.toml`

### Scenario format

```toml
images_dir = "images"          # relative to this file
labels = "labels.csv"          # optional
interval_s = 15                # --auto period
heartbeat_interval_s = 30
outbox_dir = ".outbox"         # undelivered frames, one folder per shelf

[backend]
base_url = "http://127.0.0.1:8080"
# upload_path = "/api/v1/device-events/images"
# heartbeat_path = "/devices/{device_id}/heartbeat"

[[shelves]]
name = "A2"
device_id = 2
shelf_id = 2
device_key = "emulator-a2"     # default: emulator-<name>
frames = ["shelf-a2_*.jpg"]    # globs are sorted; plain names keep their order
loop = false                   # true = start over after the last photo
```

## What is sent

One `multipart/form-data` request per frame, as in [RFC-0001](../Documentation/rfcs/0001-image-ingestion-and-recognition-pipeline.md), plus the additions from [ADR-0009](../Documentation/adr/0009-device-transport-http-with-outbox.md):

```text
POST /api/v1/device-events/images
X-Device-Key: <device key>

deviceId=2  shelfId=2  capturedAt=2026-10-07T12:00:00.000Z
clientEventId=<uuid>   image=<file>
```

- Expected response: `202 Accepted` with `{"id": <imageEventId>}` (a bare number also works).
- Heartbeat: `PATCH /devices/{id}/heartbeat` every `heartbeat_interval_s`. This endpoint already exists in `DeviceController`.
- `5xx`, `408`, `429` or no connection: the frame stays in the outbox and is retried in order. Any later frame waits behind it.
- Other `4xx`: the frame goes to `<outbox>/rejected/` and is never retried. Fix the config or the device registration, then re-send manually if needed.

> The backend upload endpoint does not exist yet (see PROJECT_DOCUMENTATION.md §10). Until it lands, use `mock-server`. If the real route differs, set `upload_path` in the scenario/config. No code change is needed.

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
sudo cp config/device.example.toml /etc/shelf-agent/device.toml   # set base_url, ids, outbox_dir=/var/lib/shelf-agent/outbox
echo "SW_DEVICE_KEY=<key from POST /devices>" | sudo tee /etc/shelf-agent.env
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
