# Vision Service

A Python service that detects and counts objects in photographs.
The Java Backend sends an image over HTTP and receives the detected count.

## Running with Docker

From the `Backend` directory:

```bash
docker compose up -d --build
```

During the first build, Docker downloads the `yolov8x-worldv2.pt` model weights
and the CLIP text encoder.
You do not need to download them manually or commit them to Git.

Swagger UI: http://localhost:8000/docs

## API

### POST /recognize

Accepts an image as `multipart/form-data`.

Required field:
- `file` — an image file, up to 10 MiB.

PowerShell example:

```powershell
curl.exe -X POST http://localhost:8000/recognize -F "file=@C:\images\shelf.jpg"
```

Example response:

```json
{
  "count": 14
}
```

Error responses:
- `400` — the file is empty or cannot be decoded as an image.
- `413` — the file exceeds the size limit.

## Backend Integration

When running in Docker Compose, the Backend uses:

```text
http://vision:8000
```

When running the Backend locally, outside Docker, use:

```text
http://127.0.0.1:8000
```

The Vision service returns only the detected object count.
The Java Backend updates stock quantities and stores recognition results
and stock movement history.

## Model

The service uses YOLO-World (`yolov8x-worldv2.pt`), an open-vocabulary detector.
The product is described with a text prompt (`PRODUCT_PROMPTS` in
`detect_yolo_tiled.py`, currently `soda can`), so no training is needed.
A box that covers two or more other detections is treated as a group of
products detected as one object and is not counted.

The COCO model used before has no "can" class and counted 1 of the 25
labelled photos correctly. YOLO-World counts all 25 correctly.

## Tests

The labelled photos live in `tests/data` (`images/` and `labels.csv`, see
ADR-0010). They are a copy of `Device/scenarios/demo`; a test checks that
the two `labels.csv` files stay identical. These photos are only for measuring
accuracy and must not be used for training.

```bash
python -m venv .venv
.venv/Scripts/pip install torch torchvision --index-url https://download.pytorch.org/whl/cpu
.venv/Scripts/pip install -r requirements-dev.txt
.venv/Scripts/python -m pytest                 # all tests, ~4 min on CPU
.venv/Scripts/python -m pytest -m "not slow"   # without the real model
```

To see the boxes: `python detect_yolo_tiled.py tests/data/images`
(writes to `outputs/`).

## Current Limitations

- Only products matching `PRODUCT_PROMPTS` are counted; other products need
  their own prompt.
- The detected count may differ from the actual number of products,
  especially for photos unlike the labelled set (other angles, lighting,
  tightly packed shelves).
- Result confidence and requests for another photograph are not implemented yet.
- The Backend automatically updates stock without checking recognition confidence.
- Stock updates require an active device and exactly one ShelfStock record
  on its assigned shelf.
