# Shelf emulator — user guide

How to send shelf photos to the backend as if they came from real shelf devices. Covers a first check without a backend, the real backend, the diploma demo and the Raspberry Pi.

[README.md](README.md) is the technical reference (architecture, protocol, configuration). This guide is the step-by-step version.

## Contents

1. [What you can do](#1-what-you-can-do)
2. [One-time setup](#2-one-time-setup)
3. [Quick check without a backend](#3-quick-check-without-a-backend-2-minutes)
4. [Emulator against the real backend](#4-emulator-against-the-real-backend)
5. [Running the diploma demo](#5-running-the-diploma-demo)
6. [Your own photos and shelves](#6-your-own-photos-and-shelves)
7. [The real Raspberry Pi shelf](#7-the-real-raspberry-pi-shelf)
8. [Command reference](#8-command-reference)
9. [Troubleshooting](#9-troubleshooting)
10. [FAQ](#10-faq)

---

## 1. What you can do

| Goal | What runs | Section |
|---|---|---|
| Check that everything works, no backend needed | `mock-server` + emulator | [3](#3-quick-check-without-a-backend-2-minutes) |
| Fill the real backend with image events | Backend + emulator | [4](#4-emulator-against-the-real-backend) |
| Show several shelves at the defense | Backend + emulator (+ the physical shelf) | [5](#5-running-the-diploma-demo) |
| Play a real shelf with a PC webcam, no Pi needed | Backend + emulator or `run` with a webcam | [6.4](#64-live-webcam-instead-of-photos) |
| Run a real shelf | Raspberry Pi 5 + camera | [7](#7-the-real-raspberry-pi-shelf) |

The emulator and the Raspberry Pi run **the same program**. The only difference is where pictures come from: files on disk or the camera. So everything the emulator sends is exactly what a real shelf will send.

Each emulated shelf is a separate device in the backend, with its own `deviceKey`, its own shelf and its own heartbeat.

## 2. One-time setup

You need Python 3.11 or newer (`python --version`).

**Windows (PowerShell):**

```powershell
cd D:\sdc\diploma\Smart-Warehouse\Device
python -m venv .venv
.venv\Scripts\python -m pip install -r requirements.txt
```

**Linux / macOS:**

```bash
cd Smart-Warehouse/Device
python3 -m venv .venv
.venv/bin/python -m pip install -r requirements.txt
```

All commands below are run from the `Device` folder. They use `python` for short. If you don't activate the virtual environment, write `.venv\Scripts\python` (Windows) or `.venv/bin/python` (Linux/macOS) instead.

> On Windows, `.venv\Scripts\Activate.ps1` may be blocked by the execution policy. Calling `.venv\Scripts\python` directly avoids that; no activation is needed.

## 3. Quick check without a backend (2 minutes)

The mock server pretends to be the backend: it has the same device endpoints as in Swagger and prints every event it receives.

**Terminal 1** starts the mock server:

```powershell
python -m shelf_agent mock-server
```

```text
mock backend on http://127.0.0.1:8080, saving images to ...\Device\received
```

**Terminal 2** starts the emulator with the small built-in scenario (two shelves, five placeholder images):

```powershell
python -m shelf_agent emulate --scenario scenarios/smoke/scenario.toml
```

```text
  A1       device key 'emulator-a1' -> device id 1, shelf 1
  B1       device key 'emulator-b1' -> device id 2, shelf 2
Emulating 2 shelves -> http://127.0.0.1:8080
emulator>
```

Press **Enter** a few times, then type **q**:

```text
emulator>
  [A1] a1_01.png -> sent, imageEvent #1 | expected SMOKE-RED x3
emulator>
  [B1] b1_01.png -> sent, imageEvent #2 | expected SMOKE-BLUE x2
emulator> q
```

Terminal 1 shows the same events arriving. That's it: the whole sending path works.

> Port 8080 busy (the real backend is running)? Use another port for the mock:
> `python -m shelf_agent mock-server --port 8099`, and add `--base-url http://127.0.0.1:8099` to the `emulate` command.

## 4. Emulator against the real backend

### 4.1 Start the backend

From the repository root, either with Docker (PostgreSQL included):

```powershell
cd Backend
copy .env.example .env      # Linux/macOS: cp .env.example .env
docker compose up -d --build
```

or with Maven against your own PostgreSQL (`cd Backend; ./mvnw spring-boot:run`).

Open Swagger UI: **http://localhost:8080/swagger-ui.html**. Everything below can be done there with **Try it out → Execute**.

### 4.2 Create the shelves (once per database)

Devices are created by the emulator automatically, but **the shelves they sit on must exist**. Create a warehouse, a rack and as many shelves as the scenario needs. Each `POST` returns the new id in the response body; write the ids down.

| Step | Endpoint | Request body | Returns |
|---|---|---|---|
| 1 | `POST /warehouses` | `{"name": "Main warehouse", "address": "Minsk"}` | warehouse id, e.g. `1` |
| 2 | `POST /racks/warehouse/{warehouseId}` with `warehouseId = 1` | `{"label": "A", "warehouseId": 1}` | rack id, e.g. `1` |
| 3 | `POST /shelves` | `{"label": "A-1", "capacity": 20, "rackId": 1}` | shelf id, e.g. `1` |
| 4 | `POST /shelves` again for every further shelf | `{"label": "A-2", "capacity": 20, "rackId": 1}` | `2`, `3`, … |

A shelf can hold **only one device**. Plan one shelf per emulated shelf, plus one for the physical prototype.

The demo scenario uploads the photos for recognition (`protocol = "device-images"`). The backend writes the counted quantity to the shelf's stock record, so every emulated shelf also needs **exactly one** `ShelfStock` with the product on it:

| Step | Endpoint | Request body | Returns |
|---|---|---|---|
| 5 | `POST /products` | `{"sku": "COCA-COLA", "name": "Coca-Cola can 0.33 l"}` | product id, e.g. `1` |
| 6 | `POST /shelf-stock` | `{"shelfId": 2, "productId": 1, "quantity": 1, "expirationDate": "2027-12-31"}` | stock id |

`quantity` must be positive; the first photo overwrites it with the recognised count.

The same with `curl` (bash or Git Bash):

```bash
curl -X POST localhost:8080/warehouses -H "Content-Type: application/json" -d '{"name":"Main warehouse","address":"Minsk"}'
curl -X POST localhost:8080/racks/warehouse/1 -H "Content-Type: application/json" -d '{"label":"A","warehouseId":1}'
curl -X POST localhost:8080/shelves -H "Content-Type: application/json" -d '{"label":"A-1","capacity":20,"rackId":1}'
curl -X POST localhost:8080/shelves -H "Content-Type: application/json" -d '{"label":"A-2","capacity":20,"rackId":1}'
curl -X POST localhost:8080/products -H "Content-Type: application/json" -d '{"sku":"COCA-COLA","name":"Coca-Cola can 0.33 l"}'
curl -X POST localhost:8080/shelf-stock -H "Content-Type: application/json" -d '{"shelfId":2,"productId":1,"quantity":1,"expirationDate":"2027-12-31"}'
```

The Vision service must be running too (`http://127.0.0.1:8000/docs`, see [Vision/README.md](../Vision/README.md)). With Docker Compose it starts together with the backend.

### 4.3 Point the scenario at your shelves

Open `scenarios/demo/scenario.toml` and set `shelf_id` to an id from step 4.2:

```toml
[[shelves]]
name = "A2"
shelf_id = 2                  # <- id returned by POST /shelves
device_key = "emulator-a2"
frames = ["1.jpg", "2.jpg", ...]
```

Don't set `device_id`. The backend generates it, and the emulator finds it by `device_key`.

### 4.4 Run

```powershell
python -m shelf_agent emulate --scenario scenarios/demo/scenario.toml
```

On the first start the emulator registers the device (`POST /devices`). On later starts it finds the existing one:

```text
  A2       device key 'emulator-a2' -> device id 1, shelf 2
```

Send a few frames with **Enter**.

### 4.5 Check the result in Swagger

| What | Endpoint |
|---|---|
| The device exists, `lastSeenAt` updates (heartbeat every 30 s) | `GET /devices` |
| The events of this device | `GET /image-events/device/{deviceId}` |
| All events | `GET /image-events` |

| The recognised quantity of the shelf | `GET /shelf-stock/shelf/{shelfId}` |
| Detected count per photo | `GET /recognition-results` |

With `protocol = "device-images"` every event is `PROCESSED`, `imagePath` is the copy saved by the backend, and the console shows the recognised count next to the label:

```text
  [A2] 5.jpg -> sent, imageEvent #5, recognised x12 | expected COCA-COLA x12
```

After the whole demo scenario the shelf stock is `3` (the label of `25.jpg`).

## 5. Running the diploma demo

### Before the defense

1. Create shelves for every emulated shelf (section 4.2). If you also show the physical prototype, give it its own shelf.
2. Add more shelves to `scenarios/demo/scenario.toml` if you want several (see section 6.3). Several shelves can reuse the same photos.
3. Run through the whole demo once. Then decide whether to start from a clean database (see [FAQ](#10-faq)).
4. Start with an empty queue: add `--reset-outbox` to the first run of the day.

### During the defense

```powershell
python -m shelf_agent emulate --scenario scenarios/demo/scenario.toml
```

| Type | What happens |
|---|---|
| **Enter** | Next photo from the next shelf (shelves take turns) |
| `A2` (a shelf name) | Next photo from that shelf |
| `all` | One photo from every shelf at once |
| `status` | Photos left and queued per shelf |
| `flush` | Retry queued photos now |
| `help` | List of commands |
| `q` | Quit |

Each line shows what the photo should contain, for example `expected COCA-COLA x15`. The audience can compare it with what the system recognised. The expected value is shown only in your console and is **never** sent to the backend.

**Unattended mode.** Every shelf sends on its own timer until its photos run out:

```powershell
python -m shelf_agent emulate --scenario scenarios/demo/scenario.toml --auto --interval 5
```

**Showing reliability** (optional, good for questions about failures):

1. Stop the backend while the emulator is running and send a frame. You'll see `queued (backend unreachable, will retry)`.
2. Send another one. It queues behind the first; order is preserved.
3. Start the backend again and type `flush` (or just wait for the next heartbeat). The queued frames arrive in the original order with their original capture time.

## 6. Your own photos and shelves

### 6.1 Where things are

```text
scenarios/demo/
├── images/          the photos (1.jpg … 25.jpg)
├── labels.csv       what is on each photo
└── scenario.toml    which shelf sends which photos, in which order
```

### 6.2 Add photos

1. Copy the photos to `scenarios/demo/images/`. Keep each file under **5 MB**; the repository rejects larger files. Downscale to about 1600 px on the long side.
2. Add one line per photo to `labels.csv`:

   ```csv
   file,sku,count
   26.jpg,COCA-COLA,7
   ```

   `count` is how many items are really on the photo. The same file is used to measure recognition accuracy, so take care that it is correct.
3. Add the file names to `frames` in `scenario.toml`, **in the order the shelf changed**. The backend computes "how many were taken/added" by comparing consecutive photos of the same shelf.

> With numbered names (`1.jpg`, `2.jpg`, … `10.jpg`), list files explicitly. A pattern like `"*.jpg"` sorts them as text: `1, 10, 11, …, 2`. Patterns are fine for zero-padded names (`shelf-b_01.jpg`, `shelf-b_02.jpg`).

### 6.3 Add another shelf

Create a shelf in the backend (section 4.2), then append to `scenario.toml`:

```toml
[[shelves]]
name = "A3"                   # any short name, used in console commands
shelf_id = 3                  # id from POST /shelves
device_key = "emulator-a3"    # unique per shelf
frames = ["10.jpg", "11.jpg", "12.jpg"]
loop = false                  # true = start over after the last photo
```

Run only some shelves with `--only A2 A3`.

### 6.4 Live webcam instead of photos

No Raspberry Pi yet? A PC webcam can play the shelf camera. Put products in front of it, press Enter, change them, press Enter again: every press takes a fresh photo and sends it through exactly the same code as the device (registration, heartbeat, queue and retries, upload to `POST /devices/{id}/images`). The photos are also kept in `.captures/`.

Install OpenCV once:

```powershell
.venv\Scripts\python -m pip install -r requirements-webcam.txt
```

Create a shelf with one stock record for it in the backend (section 4.2; the bundled scenario uses `shelf_id = 3`), then either:

**a) Emulator console**, a ready scenario with one webcam shelf `W1`:

```powershell
python -m shelf_agent emulate --scenario scenarios/webcam/scenario.toml
```

A live window opens so you can aim the camera; it flashes green on every capture. Type `W1` or press Enter to take a photo, then `status`, `q` as usual. `--auto --interval 20` takes photos on a timer instead.

Any scenario can switch to the webcam: `--webcam` (camera 0) or `--webcam 1` (a second, e.g. USB, camera) makes every shelf photograph instead of replaying its files. `--no-preview` hides the window. In your own scenario, give a shelf `webcam = 0` instead of `frames`; several shelves may share one camera.

**b) The device program itself**: `run`, as on the Pi, only with `[camera] type = "webcam"`:

```powershell
copy config\webcam.example.toml config\webcam.toml
python -m shelf_agent -v run --config config/webcam.toml
```

`[trigger] type = "manual"` takes a photo on Enter, like the shelf sensor firing; `"interval"` takes one every `interval_s` seconds.

## 7. The real Raspberry Pi shelf

Full installation steps are in [README.md → Running on the Raspberry Pi 5](README.md#running-on-the-raspberry-pi-5). In short:

1. In the backend, create the shelf and **register the device yourself**: `POST /devices` with `{"deviceKey": "shelf-a1-pi", "shelfId": 1}`. On a real device the agent does not register itself by default.
2. On the Pi, copy `config/device.example.toml` to `/etc/shelf-agent/device.toml` and set `base_url` (the backend's address in your network) and `shelf_id`.
3. Put the key in `/etc/shelf-agent.env`: `SW_DEVICE_KEY=shelf-a1-pi`.
4. First run with `type = "manual"` in `[trigger]`, which takes a photo on Enter:

   ```bash
   SW_DEVICE_KEY=shelf-a1-pi .venv/bin/python -m shelf_agent run --config /etc/shelf-agent/device.toml -v
   ```

5. When the photos look right, switch `[trigger]` to `gpio` (shelf sensor) or `interval`, and install the systemd service.

You can test a device config on a PC without the Pi camera: set `[camera] type = "webcam"` (see 6.4), or `type = "folder"` and `folder = "../scenarios/demo/images"`.

## 8. Command reference

```text
python -m shelf_agent mock-server [--host 127.0.0.1] [--port 8080] [--out received]
python -m shelf_agent emulate --scenario FILE [options]
python -m shelf_agent run --config FILE
```

`emulate` options:

| Option | Meaning |
|---|---|
| `--auto` | Send on a timer instead of the interactive console |
| `--interval N` | Seconds between photos of one shelf in `--auto` (default: `interval_s` from the scenario) |
| `--base-url URL` | Use another backend, e.g. `http://192.168.1.10:8080`, without editing the scenario |
| `--only A2 B1` | Emulate only these shelves |
| `--no-register` | Don't create missing devices; stop with an error instead |
| `--reset-outbox` | Throw away photos still queued from a previous run |
| `--webcam [N]` | Every shelf photographs with webcam N (default 0) instead of replaying files |
| `--no-preview` | Don't open the live webcam window |
| `-v` / `-vv` | More logs / debug logs (put before the command: `python -m shelf_agent -v emulate ...`) |

Exit codes: `0` ok, `2` configuration or registration problem, `3` backend unreachable or refused a request, `4` webcam could not be opened.

## 9. Troubleshooting

| Message | Cause | Fix |
|---|---|---|
| `backend: GET /devices: ConnectError ...` | Backend not running, or wrong address/port | Start the backend; check `base_url` or pass `--base-url` |
| `backend: POST /devices: HTTP 404 ... Shelf with id N is not found` | The scenario's `shelf_id` doesn't exist in this database | Create the shelf (section 4.2) and put its id in `shelf_id` |
| `backend: POST /devices: HTTP 409 ... already has a device` / `... already exists` | That shelf already has another device, or the key is taken | Use a free shelf, or delete the old device: `DELETE /devices/{id}` |
| `device key '...' is not registered in the backend` | Registration is off (`--no-register`, or `register = false` on a device) | Register it with `POST /devices`, or drop `--no-register` |
| `device '...' is linked to shelf N in the backend, but configured for shelf M` | The key was registered on another shelf earlier | Set `shelf_id = N`, or delete that device and run again |
| `device_id = N is configured, but key '...' is not registered` / `... has id N in the backend, but device_id = M is configured` | A `device_id` in the config doesn't match this database | Remove `device_id` from the config; ids are found automatically |
| `shelf 'A2': image not found: ...` / `'...' matched no files` | A file in `frames` doesn't exist | Check names and that the photo is in `images/` |
| `-> queued (backend unreachable, will retry)` | Backend went down while sending | Nothing is lost. Start the backend, then `flush` or wait. Old queue unwanted? Use `--reset-outbox` |
| `-> REJECTED: ...` | Backend refused this photo for good (4xx) | The reason follows the colon. The photo is kept in `.outbox/<shelf>/rejected/` |
| `all shelves are out of frames` | Every photo was sent | `q` and start again, or set `loop = true` |
| `cannot listen on 127.0.0.1:8080 ...` (mock server) | Port taken, often by the backend | `mock-server --port 8099` + `emulate --base-url http://127.0.0.1:8099` |
| `-> REJECTED: ... HTTP 409 ... exactly one stock record on the shelf` | The shelf has no `ShelfStock`, or more than one | Create one stock record for the shelf (section 4.2, step 6) |
| `-> REJECTED: ... HTTP 409 ... same or a later capture time was already processed` | A newer photo of this shelf is already in the database | Expected when re-sending old frames; send newer ones |
| `-> queued ... HTTP 502 ... Vision service request failed` | Vision service is not running | Start it, then `flush` |
| `camera: webcam needs OpenCV` | OpenCV not installed | `pip install -r requirements-webcam.txt` |
| `camera: cannot open webcam 0 ...` | No camera, another app (Zoom, Teams, a browser) holds it, or Windows blocks it | Close the other app; Settings → Privacy & security → Camera → allow desktop apps; try `--webcam 1` |
| `capturedAt` looks shifted by some hours | PC and backend use different time zones | Set `timestamp_zone = "utc"` in `[backend]` if the backend runs in UTC (e.g. in a container) |

## 10. FAQ

**Does the photo itself reach the server?**
Yes, with `protocol = "device-images"` in `[backend]` (set in `scenarios/demo`). The photo is uploaded to `POST /devices/{id}/images`, the backend sends it to the Vision service and writes the count to the shelf stock. The default `image-events` protocol sends only the **path** to the photo (`imagePath`) and does not trigger recognition.

**How do I start the demo from a clean state?**
`--reset-outbox` clears the emulator's own queue. Events already in the database stay there. Delete them with `DELETE /image-events/{id}`, or reset the whole database with `docker compose down -v` in `Backend/`. **That command deletes all data**, including shelves, so you then repeat section 4.2.

**Can two shelves use the same photos?**
Yes. List the same files in both `frames`. Each shelf sends them under its own device.

**Can I run several emulators at once?**
Yes, as long as they use different shelves and `device_key`s. Each scenario keeps its own queue folder.

**Where does the expected value (`expected COCA-COLA x15`) come from?**
From `labels.csv`. It is printed only for you and never sent, so the system can't "peek" at the answer.

**Why isn't `device_id` in the scenario?**
The database assigns device ids, and they differ between databases (your PC, Docker, the cloud). The emulator finds the id by `device_key` every time it starts.
