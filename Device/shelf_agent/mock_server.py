"""Stand-in for the backend's device endpoints, for development without Spring Boot.

Mirrors what the backend publishes in Swagger (``/v3/api-docs``) for devices:

* ``GET /devices``, ``GET /devices/{id}``, ``POST /devices`` (201 + id)
* ``PATCH /devices/{id}/heartbeat`` (204)
* ``POST /image-events`` with JSON ``{deviceId, imagePath, capturedAt}`` (201 + id)

plus the RFC-0001 multipart upload (``POST /api/v1/device-events/images``),
which the backend does not have yet. Unknown devices get ``404`` with the
backend's ``{"status", "message"}`` body. Standard library only.
"""

from __future__ import annotations

import json
import logging
import re
import threading
from dataclasses import dataclass
from datetime import datetime
from email.message import EmailMessage
from email.parser import BytesParser
from email.policy import HTTP
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Any

from shelf_agent.transport.client import DEVICE_KEY_HEADER

logger = logging.getLogger(__name__)

DEVICES_ROUTE = re.compile(r"^/devices/?$")
DEVICE_ROUTE = re.compile(r"^/devices/(?P<id>\d+)/?$")
HEARTBEAT_ROUTE = re.compile(r"^/devices/(?P<id>\d+)/heartbeat/?$")
IMAGE_EVENTS_ROUTE = re.compile(r"^/image-events/?$")
UPLOAD_ROUTE = re.compile(r"^/api/v1/device-events/images/?$")
MULTIPART_FIELDS = ("deviceId", "shelfId", "capturedAt")


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

    def __init__(self, address: tuple[str, int], out_dir: Path) -> None:
        super().__init__(address, _Handler)
        self.out_dir = out_dir
        self.devices: dict[int, MockDevice] = {}
        self.events: list[ReceivedEvent] = []
        self.heartbeats: list[int] = []
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
                    raise ValueError(f"Shelf {shelf_id} already has a device")
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
        folder = self.server.out_dir / f"device-{fields['deviceId']}"
        folder.mkdir(parents=True, exist_ok=True)
        path = folder / f"{len(self.server.events) + 1:04d}_{image.get_filename()}"
        path.write_bytes(data)
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
        self, body: bytes
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
            if name == "image":
                image = part
            else:
                payload = part.get_payload(decode=True)
                if isinstance(payload, bytes):
                    fields[name] = payload.decode("utf-8")
        return fields, image

    def _not_found(self, message: str) -> None:
        self._reply(404, {"status": 404, "message": message})

    def _reply(self, status: int, body: object) -> None:
        payload = b"" if body is None else json.dumps(body).encode()
        self.send_response(status)
        if payload:
            self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)
