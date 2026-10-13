# Vision Service

A Python service that detects and counts objects in photographs.
The Java Backend sends an image over HTTP and receives the detected count.

## Running with Docker

From the `Backend` directory:

```bash
docker compose up -d --build
```

During the first build, Docker downloads the `yolov8x.pt` model weights.
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

## Current Limitations

- Supported object classes: `bottle`, `cup`, and `wine glass`.
- The detected count may differ from the actual number of products.
- Result confidence and requests for another photograph are not implemented yet.
- The Backend automatically updates stock without checking recognition confidence.
- Stock updates require an active device and exactly one ShelfStock record
  on its assigned shelf.
