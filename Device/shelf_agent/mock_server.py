"""Stand-in for the backend's device endpoints, for development without Spring Boot.

Implements the device side of RFC-0001 only: image upload (202 + event id) and
heartbeat (204). Uploaded images are written to disk so you can check exactly
what a device sent. Standard library only.
"""

from __future__ import annotations

import json
import logging
import re
import threading
from dataclasses import dataclass, field
from datetime import datetime
from email.message import EmailMessage
from email.parser import BytesParser
from email.policy import HTTP
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

from shelf_agent.transport.client import DEVICE_KEY_HEADER

logger = logging.getLogger(__name__)

UPLOAD_ROUTE = re.compile(r"^(/api/v1)?/device-events/images/?$")
HEARTBEAT_ROUTE = re.compile(r"^(/api/v1)?/devices/(?P<device_id>\d+)/heartbeat/?$")
REQUIRED_FIELDS = ("deviceId", "shelfId", "capturedAt")


@dataclass(frozen=True)
class ReceivedImage:
    """One accepted upload, as the backend would store it in ``image_events``."""

    id: int
    device_id: int
    shelf_id: int
    captured_at: str
    client_event_id: str | None
    device_key: str
    filename: str
    content_type: str
    size: int
    path: Path
    fields: dict[str, str] = field(default_factory=dict)


class MockBackend(ThreadingHTTPServer):
    """HTTP server recording what devices send.

    Args:
        address: ``(host, port)``; port ``0`` picks a free one (tests).
        out_dir: Where received images are saved, one folder per device.
    """

    daemon_threads = True

    def __init__(self, address: tuple[str, int], out_dir: Path) -> None:
        super().__init__(address, _Handler)
        self.out_dir = out_dir
        self.images: list[ReceivedImage] = []
        self.heartbeats: list[int] = []
        self._lock = threading.Lock()

    @property
    def base_url(self) -> str:
        host, port = self.server_address[:2]
        return f"http://{host!s}:{port}"

    def record_image(
        self, fields: dict[str, str], device_key: str, image: EmailMessage
    ) -> ReceivedImage:
        data = image.get_payload(decode=True)
        assert isinstance(data, bytes)
        filename = image.get_filename() or "image.bin"
        with self._lock:
            event_id = len(self.images) + 1
            folder = self.out_dir / f"device-{fields['deviceId']}"
            folder.mkdir(parents=True, exist_ok=True)
            path = folder / f"{event_id:04d}_{Path(filename).name}"
            path.write_bytes(data)
            received = ReceivedImage(
                id=event_id,
                device_id=int(fields["deviceId"]),
                shelf_id=int(fields["shelfId"]),
                captured_at=fields["capturedAt"],
                client_event_id=fields.get("clientEventId"),
                device_key=device_key,
                filename=filename,
                content_type=image.get_content_type(),
                size=len(data),
                path=path,
                fields=fields,
            )
            self.images.append(received)
        return received

    def record_heartbeat(self, device_id: int) -> None:
        with self._lock:
            self.heartbeats.append(device_id)


class _Handler(BaseHTTPRequestHandler):
    server: MockBackend

    def do_POST(self) -> None:
        # Always drain the body first: replying early while the client is still
        # uploading makes the OS reset the connection instead of delivering
        # the error status.
        body = self.rfile.read(int(self.headers.get("Content-Length", 0)))
        if not UPLOAD_ROUTE.match(self.path):
            self._reply(404, {"error": "NOT_FOUND", "message": self.path})
            return
        device_key = self.headers.get(DEVICE_KEY_HEADER, "")
        if not device_key:
            self._reply(401, {"error": "UNAUTHORIZED", "message": "missing device key"})
            return
        try:
            fields, image = self._parse_multipart(body)
            missing = [name for name in REQUIRED_FIELDS if name not in fields]
            if missing or image is None:
                raise ValueError(f"missing field(s): {missing or ['image']}")
            datetime.fromisoformat(fields["capturedAt"])
            received = self.server.record_image(fields, device_key, image)
        except ValueError as exc:
            self._reply(400, {"error": "VALIDATION_ERROR", "message": str(exc)})
            return
        logger.info(
            "image #%d device=%d shelf=%d %s (%d bytes) captured %s",
            received.id,
            received.device_id,
            received.shelf_id,
            received.filename,
            received.size,
            received.captured_at,
        )
        self._reply(202, {"id": received.id, "processingStatus": "RECEIVED"})

    def do_PATCH(self) -> None:
        match = HEARTBEAT_ROUTE.match(self.path)
        if not match:
            self._reply(404, {"error": "NOT_FOUND", "message": self.path})
            return
        device_id = int(match["device_id"])
        self.server.record_heartbeat(device_id)
        logger.debug("heartbeat device=%d", device_id)
        self._reply(204, None)

    def log_message(self, format: str, *args: object) -> None:
        logger.debug("%s - %s", self.address_string(), format % args)

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

    def _reply(self, status: int, body: object) -> None:
        payload = b"" if body is None else json.dumps(body).encode()
        self.send_response(status)
        if payload:
            self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)
