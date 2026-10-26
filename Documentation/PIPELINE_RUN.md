# Running the whole pipeline on one PC

How to run the full chain on a laptop and see in Swagger what the backend actually stores:

```text
emulator / webcam ──photo──▶ backend (Docker) ──▶ Vision (host) ──count──▶ backend ──▶ PostgreSQL (Docker)
```

[`scripts/pipeline.sh`](../scripts/pipeline.sh) does every step below. Tested on macOS (Apple M2) with Colima; it works the same with Docker Desktop and on Linux. On Windows, use the manual steps in [Device/USER_GUIDE.md](../Device/USER_GUIDE.md) section 4.

## Why Vision runs on the host

The backend and PostgreSQL run in Docker. The Vision service runs directly on the PC from `Vision/.venv`, and the backend reaches it at `host.docker.internal:8000` (see [Backend/docker-compose.host-vision.yml](../Backend/docker-compose.host-vision.yml)). This avoids the 4-5 GB Vision image, which matters on a laptop with little free disk. The full `docker compose up` in `Backend/` still works when there is enough room.

## One-time setup

- Python 3.11+ (`python3 --version`).
- Docker. On a Mac without Docker Desktop:

  ```bash
  brew install colima docker docker-compose docker-buildx
  ```

  For `docker compose` to find the plugin, put this in `~/.docker/config.json`:

  ```json
  { "cliPluginsExtraDirs": ["/opt/homebrew/lib/docker/cli-plugins"] }
  ```

The script creates `Device/.venv` and `Vision/.venv` on the first run. Vision needs 1-2 GB of packages. On its first start it downloads about 500 MB of model weights into `weights/` and `Vision/`, both gitignored.

## Run everything with one command

From the repository root:

```bash
scripts/pipeline.sh start          # live webcam -> shelf 3
scripts/pipeline.sh start demo     # the 25 labelled Coca-Cola photos -> shelf 2
```

`start` runs `up` (below), picks the camera and prints a summary before the emulator starts: the Swagger links, and which shelf the photos are registered on, with its warehouse, rack, device and current stock:

```text
Smart Warehouse pipeline
  Backend Swagger  http://127.0.0.1:8080/swagger-ui.html
  Vision Swagger   http://127.0.0.1:8000/docs
  Counts per photo http://127.0.0.1:8080/recognition-results
  All devices      http://127.0.0.1:8080/devices

Shelves of scenarios/webcam (webcam 0):
  W1 -> shelf #3 A-3  (rack A, Main warehouse, Minsk)
      device   'webcam-w1' = device #2 on shelf 3, ACTIVE
      stock    COCA-COLA: quantity 1
      quantity  http://127.0.0.1:8080/shelf-stock/shelf/3
      history   http://127.0.0.1:8080/stock-movements/shelf/3
      photos    http://127.0.0.1:8080/image-events/device/2
```

On the very first start the device is not in the database yet. The summary then says `not registered yet, the emulator registers it`, and the emulator's first line confirms the registration (`W1  device key 'webcam-w1' -> device id 2, shelf 3`).

Then press **Enter** to take a photo, `status` for the queue, `q` to quit. The services keep running after `q`. `scripts/pipeline.sh webcam` or `demo` starts the emulator again with the same summary, and `scripts/pipeline.sh down` stops everything.

## Run step by step

```bash
scripts/pipeline.sh up
```

This starts Colima if Docker is not running and creates `Backend/.env` from `.env.example`. It then starts Vision on `127.0.0.1:8000` (log in `.pipeline/vision.log`), PostgreSQL, and the backend on `127.0.0.1:8080`. Finally it seeds the database with [scripts/seed_backend.py](../scripts/seed_backend.py):

| Row | Value |
|---|---|
| Warehouse 1, rack 1 | `Main warehouse`, rack `A` |
| Shelves 1, 2, 3 | 1 is kept for the physical device, 2 for `scenarios/demo`, 3 for `scenarios/webcam` |
| Product 1 | `COCA-COLA` |
| Shelf stock | one record on shelf 2 and one on shelf 3 (quantity 1, overwritten by the first photo) |

Seeding is idempotent: run `scripts/pipeline.sh seed` again at any time and it creates only what is missing.

Then, in a second terminal, send photos:

```bash
scripts/pipeline.sh demo          # the 25 labelled Coca-Cola photos, Enter = next photo
scripts/pipeline.sh webcam        # live webcam photographs shelf 3, Enter = take a photo
scripts/pipeline.sh webcam 1      # the same with camera 1
```

Each Enter prints a line like this:

```text
  [A2] 1.jpg -> sent, imageEvent #1, recognised x16 | expected COCA-COLA x16
```

`recognised` is what the backend stored from Vision. `expected` is the label from `labels.csv`, shown only to you.

Arguments after `demo` / `webcam` go to the emulator, e.g. `scripts/pipeline.sh demo --auto --interval 10` sends a photo every 10 s without Enter.

### Which webcam number

OpenCV numbers cameras in its own order, and the order changes with the devices around. On a Mac with an iPhone nearby, **camera 0 is often the iPhone (Continuity Camera), which delivers black frames**. The built-in FaceTime camera is then 1; without the iPhone it is 0.

So `start` and `webcam` without a number run [scripts/find_webcam.py](../scripts/find_webcam.py). It opens cameras 0, 1, 2… and takes the first one that delivers a picture rather than black, printing e.g. `camera 0: black`, `camera 1: picture`. To choose yourself, pass the number (`webcam 1`) or set `WEBCAM_INDEX=1`.

If every photo counts 0, open the latest photo in `Device/scenarios/webcam/.captures/`. An all-black image means the wrong camera. A picture taken from your phone's angle means the iPhone was picked: pass the other number, or turn off Continuity Camera on the iPhone (Settings → General → AirPlay & Continuity).

On macOS there is no live preview window: OpenCV can open windows only from the main thread, so the emulator turns the preview off and keeps every photo in `.captures/`.

The model counts what matches the prompt in `Vision/detect_yolo_tiled.py` (currently `soda can`). Other objects count as 0.

## See the data

```bash
scripts/pipeline.sh status
```

```text
Devices:
  #1 emulator-a2  shelf 2  ACTIVE   last seen 2026-10-26T04:52:22
Shelf stock:
  shelf 2: quantity 14 (stock #1)
  shelf 3: quantity 1 (stock #2)
Recognition results: 3 in total, last 5:
  id=1  imageEventId=1  detectedQuantity=16  quantityDelta=15  status=CONFIRMED
  id=2  imageEventId=2  detectedQuantity=15  quantityDelta=-1  status=CONFIRMED
  id=3  imageEventId=3  detectedQuantity=14  quantityDelta=-1  status=CONFIRMED
```

The same data in Swagger UI, **http://127.0.0.1:8080/swagger-ui.html**, via **Try it out → Execute**:

| What | Endpoint |
|---|---|
| Current quantity on a shelf | `GET /shelf-stock/shelf/{shelfId}` (2 = demo, 3 = webcam) |
| Count per photo | `GET /recognition-results` |
| Received photos and their status | `GET /image-events`, `GET /image-events/device/{deviceId}` |
| Every stock change | `GET /stock-movements/shelf/{shelfId}` |
| Devices and their heartbeat | `GET /devices` |

Vision has its own Swagger at http://127.0.0.1:8000/docs (`POST /recognize`, upload a photo, get `{"count": N}`).

`lastSeenAt` and other server times are in UTC: the backend container runs in UTC.

## Without Docker

```bash
scripts/pipeline.sh mock
```

This runs the mock backend from `Device/` on port 8080 with Vision behind it (`mock-server --vision-url`). `demo` and `webcam` then print `recognised xN` the same way, but nothing is stored and there is no Swagger. Stop the Docker backend first (`scripts/pipeline.sh down`), because both need port 8080.

## Stop and reset

| Command | Effect |
|---|---|
| `scripts/pipeline.sh logs` | Follow the backend log |
| `scripts/pipeline.sh down` | Stop the containers and the Vision started by the script; data is kept |
| `scripts/pipeline.sh reset` | Asks first, then **deletes the database** and the emulator queue; run `up` again afterwards |
| `colima stop` | Stop the Docker VM itself |

## Troubleshooting

| Symptom | Fix |
|---|---|
| `port 8080 is taken` | A mock server or an old emulator setup is running; stop it (Ctrl-C) |
| Emulator: `device ... is linked to shelf N ..., but configured for shelf M` | The database was seeded differently; `scripts/pipeline.sh reset`, then `up` |
| `-> queued ... HTTP 502 ... Vision service request failed` | Vision is down: `curl 127.0.0.1:8000/docs`, check `.pipeline/vision.log`, then type `flush` in the emulator |
| `-> REJECTED: ... same or a later capture time was already processed` | Expected when the same frames are re-sent after a restart; send new ones |
| Every webcam photo counts 0 | Wrong camera (black frames) or no cans in view; see *Which webcam number* |
| An emulator started before the backend was restarted sends to the wrong device | Quit it with `q` and start it again: device ids are looked up only at start |
