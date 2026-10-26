"""Stand-in for the backend's device endpoints, for development without Spring Boot.

Mirrors what the backend publishes in Swagger (``/v3/api-docs``) for devices:

* ``GET /devices``, ``GET /devices/{id}``, ``POST /devices`` (201 + id)
* ``PATCH /devices/{id}/heartbeat`` (204)
* ``POST /image-events`` with JSON ``{deviceId, imagePath, capturedAt}`` (201 + id)
* ``POST /devices/{id}/images?capturedAt=...`` with a multipart ``file`` (200);
  the mock stores the image but does not recognise it, so ``quantity`` is null.
  Like the backend it answers ``502`` when recognition fails (simulated with
  ``vision_failures``) and ``409`` for an inactive device or a frame captured
  no later than the shelf's last processed one. With ``vision_url`` set, the
  image is forwarded to the Vision service (``POST /recognize``) like the
  backend does, and its count comes back as ``quantity``

plus the RFC-0001 multipart upload (``POST /api/v1/device-events/images``),
which the backend does not have yet. Unknown devices get ``404`` with the
backend's ``{"status", "message"}`` body. Standard library only.
"""

from __future__ import annotations

import json
import logging
import re
import sys
import threading
import uuid
from dataclasses import dataclass
from datetime import datetime
from email.message import EmailMessage
from email.parser import BytesParser
from email.policy import HTTP
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Any
from urllib.parse import parse_qs, urlsplit
from urllib.request import Request, urlopen

from shelf_agent.transport.client import DEVICE_KEY_HEADER

logger = logging.getLogger(__name__)

DEVICES_ROUTE = re.compile(r"^/devices/?$")
DEVICE_ROUTE = re.compile(r"^/devices/(?P<id>\d+)/?$")
HEARTBEAT_ROUTE = re.compile(r"^/devices/(?P<id>\d+)/heartbeat/?$")
DEVICE_IMAGES_ROUTE = re.compile(r"^/devices/(?P<id>\d+)/images/?$")
IMAGE_EVENTS_ROUTE = re.compile(r"^/image-events/?$")
UPLOAD_ROUTE = re.compile(r"^/api/v1/device-events/images/?$")
MULTIPART_FIELDS = ("deviceId", "shelfId", "capturedAt")
# The backend's VisionConfig read timeout.
VISION_TIMEOUT_S = 180


@dataclass
class MockDevice:
    """A row of the backend's ``devices`` table (``DeviceResponseDto``)."""

    id: int
    device_key: str
    shelf_id: int
    status: str = "ACTIVE"
    last_seen_at: str | None = None

    def to_json(self) -> dict[str, Any]:
        return {
            "id": self.id,
            "deviceKey": self.device_key,
            "status": self.status,
            "lastSeenAt": self.last_seen_at,
            "shelfId": self.shelf_id,
        }


@dataclass(frozen=True)
class ReceivedEvent:
    """One accepted image event, as the backend would store it in ``image_events``."""

    id: int
    device_id: int
    shelf_id: int
    image_path: str
    captured_at: str
    protocol: str
    device_key: str = ""
    client_event_id: str | None = None
    size: int | None = None


class MockBackend(ThreadingHTTPServer):
    """HTTP server recording what devices send.

    Args:
        address: ``(host, port)``; port ``0`` picks a free one (tests).
        out_dir: Where multipart uploads are saved, one folder per device.
    """

    daemon_threads = True
    # On Windows SO_REUSEADDR lets a second server bind a port that is already
    # in use (e.g. by the real backend on 8080) and requests get split between
    # them. Fail loudly instead.
    allow_reuse_address = sys.platform != "win32"

    def __init__(self, address: tuple[str, int], out_dir: Path) -> None:
        super().__init__(address, _Handler)
        self.out_dir = out_dir
        self.devices: dict[int, MockDevice] = {}
        self.events: list[ReceivedEvent] = []
        self.heartbeats: list[int] = []
        # The next N device-images uploads fail as if the Vision service were
        # down (backend: 502 "Vision service request failed").
        self.vision_failures = 0
        # Vision service base URL; ``None`` leaves ``quantity`` null.
        self.vision_url: str | None = None
        self._last_processed: dict[int, datetime] = {}
        self._quantities: dict[int, int] = {}
        self._lock = threading.Lock()

    @property
    def base_url(self) -> str:
        host, port = self.server_address[:2]
        return f"http://{host!s}:{port}"

    def add_device(self, device_key: str, shelf_id: int) -> MockDevice:
        """Create a device, enforcing the backend's uniqueness rules.

        Raises:
            ValueError: Key already used or shelf already has a device (409).
        """
        with self._lock:
            for device in self.devices.values():
                if device.device_key == device_key:
                    raise ValueError(f"Device with key {device_key} already exists")
                if device.shelf_id == shelf_id:
                    raise ValueError(f"Shelf with id {shelf_id} already has a device")
            device = MockDevice(len(self.devices) + 1, device_key, shelf_id)
            self.devices[device.id] = device
            return device

    def heartbeat(self, device_id: int) -> bool:
        with self._lock:
            device = self.devices.get(device_id)
            if device is None:
                return False
            device.last_seen_at = datetime.now().isoformat(timespec="seconds")
            self.heartbeats.append(device_id)
            return True

    def take_vision_failure(self) -> bool:
        with self._lock:
            if self.vision_failures <= 0:
                return False
            self.vision_failures -= 1
            return True

    def mark_processed(self, shelf_id: int, captured_at: datetime) -> bool:
        """Record a processed frame; ``False`` if the shelf already has a later one."""
        with self._lock:
            last = self._last_processed.get(shelf_id)
            if last is not None and captured_at <= last:
                return False
            self._last_processed[shelf_id] = captured_at
            return True

    def recognize(self, filename: str, content_type: str, data: bytes) -> int:
        """Count the products in an image with the Vision service.

        Raises:
            OSError: Vision is unreachable, timed out or answered an error.
            ValueError: Vision answered without a numeric ``count``.
        """
        assert self.vision_url is not None
        boundary = uuid.uuid4().hex
        safe_name = filename.replace('"', "")
        body = (
            (
                f"--{boundary}\r\n"
                'Content-Disposition: form-data; name="file"; '
                f'filename="{safe_name}"\r\n'
                f"Content-Type: {content_type}\r\n\r\n"
            ).encode()
            + data
            + f"\r\n--{boundary}--\r\n".encode()
        )
        request = Request(
            f"{self.vision_url}/recognize",
            data=body,
            headers={"Content-Type": f"multipart/form-data; boundary={boundary}"},
            method="POST",
        )
        with urlopen(request, timeout=VISION_TIMEOUT_S) as response:
            count = json.load(response).get("count")
        if not isinstance(count, int):
            raise ValueError(f"Vision answered without a count: {count!r}")
        return count

    def set_quantity(self, shelf_id: int, quantity: int) -> int | None:
        """Store the shelf's recognised quantity; returns the previous one."""
        with self._lock:
            previous = self._quantities.get(shelf_id)
            self._quantities[shelf_id] = quantity
            return previous

    def add_event(self, **fields: Any) -> ReceivedEvent:
        with self._lock:
            event = ReceivedEvent(id=len(self.events) + 1, **fields)
            self.events.append(event)
        logger.info(
            "image event #%d device=%d shelf=%d %s captured %s",
            event.id,
            event.device_id,
            event.shelf_id,
            event.image_path,
            event.captured_at,
        )
        return event


class _Handler(BaseHTTPRequestHandler):
    server: MockBackend

    def do_GET(self) -> None:
        if DEVICES_ROUTE.match(self.path):
            self._reply(200, [d.to_json() for d in self.server.devices.values()])
        elif match := DEVICE_ROUTE.match(self.path):
            device = self.server.devices.get(int(match["id"]))
            if device is None:
                self._not_found(f"Device with id {match['id']} is not found")
            else:
                self._reply(200, device.to_json())
        else:
            self._not_found(self.path)

    def do_POST(self) -> None:
        # Always drain the body first: replying early while the client is still
        # uploading makes the OS reset the connection instead of delivering
        # the error status.
        body = self.rfile.read(int(self.headers.get("Content-Length", 0)))
        try:
            if DEVICES_ROUTE.match(self.path):
                self._create_device(body)
            elif IMAGE_EVENTS_ROUTE.match(self.path):
                self._create_image_event(body)
            elif UPLOAD_ROUTE.match(self.path):
                self._upload_multipart(body)
            elif match := DEVICE_IMAGES_ROUTE.match(urlsplit(self.path).path):
                self._upload_device_image(int(match["id"]), body)
            else:
                self._not_found(self.path)
        except (ValueError, KeyError, TypeError) as exc:
            self._reply(400, {"status": 400, "message": f"Invalid request: {exc}"})

    def do_PATCH(self) -> None:
        match = HEARTBEAT_ROUTE.match(self.path)
        if not match:
            self._not_found(self.path)
        elif self.server.heartbeat(int(match["id"])):
            self._reply(204, None)
        else:
            self._not_found(f"Device with id {match['id']} is not found")

    def log_message(self, format: str, *args: object) -> None:
        logger.debug("%s - %s", self.address_string(), format % args)

    def _create_device(self, body: bytes) -> None:
        data = json.loads(body)
        key, shelf_id = str(data["deviceKey"]), int(data["shelfId"])
        if not key:
            raise ValueError("deviceKey must not be blank")
        try:
            device = self.server.add_device(key, shelf_id)
        except ValueError as exc:
            self._reply(409, {"status": 409, "message": str(exc)})
            return
        logger.info("device #%d registered: %s on shelf %d", device.id, key, shelf_id)
        self._reply(201, device.id)

    def _create_image_event(self, body: bytes) -> None:
        data = json.loads(body)
        device = self.server.devices.get(int(data["deviceId"]))
        if device is None:
            self._not_found(f"Device with id {data['deviceId']} is not found")
            return
        image_path = str(data["imagePath"])
        if not image_path:
            raise ValueError("imagePath must not be blank")
        captured_at = str(data["capturedAt"])
        if datetime.fromisoformat(captured_at).tzinfo is not None:
            raise ValueError("capturedAt is a LocalDateTime and must have no offset")
        event = self.server.add_event(
            device_id=device.id,
            shelf_id=device.shelf_id,
            image_path=image_path,
            captured_at=captured_at,
            protocol="image-events",
            device_key=self.headers.get(DEVICE_KEY_HEADER, ""),
        )
        self._reply(201, event.id)

    def _upload_device_image(self, device_id: int, body: bytes) -> None:
        device = self.server.devices.get(device_id)
        if device is None:
            self._not_found(f"Device not found: {device_id}")
            return
        captured_at = parse_qs(urlsplit(self.path).query)["capturedAt"][0]
        captured = datetime.fromisoformat(captured_at)
        if captured.tzinfo is not None:
            raise ValueError("capturedAt is a LocalDateTime and must have no offset")
        _, image = self._parse_multipart(body, file_field="file")
        if image is None:
            raise ValueError("missing field(s): ['file']")
        data = image.get_payload(decode=True)
        assert isinstance(data, bytes)
        # Same order as the backend: recognition runs before the stock checks.
        if self.server.take_vision_failure():
            self._error(502, "Vision service request failed")
            return
        quantity: int | None = None
        if self.server.vision_url is not None:
            try:
                quantity = self.server.recognize(
                    image.get_filename() or "image.jpg", image.get_content_type(), data
                )
            except (OSError, ValueError) as exc:
                logger.warning("Vision request failed: %s", exc)
                self._error(502, "Vision service request failed")
                return
        if device.status != "ACTIVE":
            self._error(409, "Device is inactive")
            return
        if not self.server.mark_processed(device.shelf_id, captured):
            self._error(
                409,
                "An image with the same or a later capture time was already processed",
            )
            return
        path = self._save_image(device_id, image.get_filename(), data)
        event = self.server.add_event(
            device_id=device.id,
            shelf_id=device.shelf_id,
            image_path=str(path),
            captured_at=captured_at,
            protocol="device-images",
            device_key=self.headers.get(DEVICE_KEY_HEADER, ""),
            size=len(data),
        )
        previous: int | None = None
        delta: int | None = None
        if quantity is not None:
            previous = self.server.set_quantity(device.shelf_id, quantity)
            delta = None if previous is None else quantity - previous
            logger.info(
                "shelf %d: Vision counted %d (was %s)",
                device.shelf_id,
                quantity,
                "unknown" if previous is None else previous,
            )
        self._reply(
            200,
            {
                "imageEventId": event.id,
                "recognitionResultId": None,
                "shelfId": device.shelf_id,
                "previousQuantity": previous,
                "quantity": quantity,
                "quantityDelta": delta,
            },
        )

    def _save_image(
        self, device_id: int | str, filename: str | None, data: bytes
    ) -> Path:
        folder = self.server.out_dir / f"device-{device_id}"
        folder.mkdir(parents=True, exist_ok=True)
        path = folder / f"{len(self.server.events) + 1:04d}_{filename}"
        path.write_bytes(data)
        return path

    def _upload_multipart(self, body: bytes) -> None:
        device_key = self.headers.get(DEVICE_KEY_HEADER, "")
        if not device_key:
            self._reply(401, {"status": 401, "message": "missing device key"})
            return
        fields, image = self._parse_multipart(body)
        missing = [name for name in MULTIPART_FIELDS if name not in fields]
        if missing or image is None:
            raise ValueError(f"missing field(s): {missing or ['image']}")
        datetime.fromisoformat(fields["capturedAt"])
        data = image.get_payload(decode=True)
        assert isinstance(data, bytes)
        path = self._save_image(fields["deviceId"], image.get_filename(), data)
        event = self.server.add_event(
            device_id=int(fields["deviceId"]),
            shelf_id=int(fields["shelfId"]),
            image_path=str(path),
            captured_at=fields["capturedAt"],
            protocol="multipart",
            device_key=device_key,
            client_event_id=fields.get("clientEventId"),
            size=len(data),
        )
        self._reply(202, {"id": event.id, "processingStatus": "RECEIVED"})

    def _parse_multipart(
        self, body: bytes, file_field: str = "image"
    ) -> tuple[dict[str, str], EmailMessage | None]:
        content_type = self.headers.get("Content-Type", "")
        if not content_type.startswith("multipart/form-data"):
            raise ValueError("expected multipart/form-data")
        message = BytesParser(policy=HTTP).parsebytes(
            f"Content-Type: {content_type}\r\n\r\n".encode() + body
        )
        assert isinstance(message, EmailMessage)
        fields: dict[str, str] = {}
        image: EmailMessage | None = None
        for part in message.iter_parts():
            assert isinstance(part, EmailMessage)
            name = part.get_param("name", header="content-disposition")
            if not isinstance(name, str):
                continue
            if name == file_field:
                image = part
            else:
                payload = part.get_payload(decode=True)
                if isinstance(payload, bytes):
                    fields[name] = payload.decode("utf-8")
        return fields, image

    def _not_found(self, message: str) -> None:
        self._error(404, message)

    def _error(self, status: int, message: str) -> None:
        self._reply(status, {"status": status, "message": message})

    def _reply(self, status: int, body: object) -> None:
        payload = b"" if body is None else json.dumps(body).encode()
        self.send_response(status)
        if payload:
            self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)
