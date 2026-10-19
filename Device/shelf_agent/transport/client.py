"""HTTP client for the backend's device-facing API.

This is the single implementation of the wire protocol. The real device and
the emulator both send through :class:`BackendClient`, so whatever the
emulator demonstrates is exactly what the Raspberry Pi will send.

Two upload protocols (``BackendConfig.protocol``, ADR-0009):

* ``image-events`` (default) follows the backend's published OpenAPI spec:
  ``POST /image-events`` with JSON ``{deviceId, imagePath, capturedAt}``.
* ``device-images`` uploads the image to ``POST /devices/{id}/images``
  (``file`` part, ``capturedAt`` query parameter). The backend sends it to the
  Vision service and writes the counted quantity to the shelf stock.
* ``multipart`` is the RFC-0001 upload of the image bytes, for when the
  backend implements ``POST /api/v1/device-events/images``.
"""

from __future__ import annotations

from dataclasses import dataclass
from datetime import UTC, datetime
from typing import Any

import httpx

from shelf_agent import __version__
from shelf_agent.config import (
    PROTOCOL_DEVICE_IMAGES,
    PROTOCOL_MULTIPART,
    BackendConfig,
    DeviceIdentity,
)
from shelf_agent.models import Frame

DEVICE_KEY_HEADER = "X-Device-Key"

# Status codes worth retrying: the request may succeed later without changes.
_RETRYABLE_STATUS = {408, 425, 429}


class BackendError(Exception):
    """A request to the backend failed.

    Attributes:
        retryable: ``True`` for network errors, timeouts and 5xx/429 — the
            frame is kept in the outbox. ``False`` for other 4xx: the request
            itself is wrong (unknown device, bad data) and repeating it won't help.
        status_code: HTTP status, or ``None`` if no response was received.
    """

    def __init__(
        self, message: str, *, retryable: bool, status_code: int | None = None
    ) -> None:
        super().__init__(message)
        self.retryable = retryable
        self.status_code = status_code


@dataclass(frozen=True)
class UploadReceipt:
    """Backend's answer to an accepted upload.

    Attributes:
        quantity: Stock quantity the backend recognised in the image
            (``device-images`` only), ``None`` otherwise.
    """

    status_code: int
    image_event_id: int | None
    quantity: int | None = None


@dataclass(frozen=True)
class RemoteDevice:
    """A ``DeviceResponseDto`` as returned by ``GET /devices``."""

    id: int
    device_key: str
    shelf_id: int | None
    status: str | None


def format_timestamp(moment: datetime) -> str:
    """ISO-8601 in UTC with millisecond precision, e.g. ``2026-10-07T12:00:00.000Z``."""
    if moment.tzinfo is None:
        raise ValueError("captured_at must be timezone-aware")
    return (
        moment.astimezone(UTC).isoformat(timespec="milliseconds").replace("+00:00", "Z")
    )


def format_local_datetime(moment: datetime, zone: str) -> str:
    """Zone-less ISO-8601 for a Java ``LocalDateTime`` (``2026-10-07T15:00:00.000``).

    Args:
        moment: Timezone-aware capture time.
        zone: ``local`` (this machine's zone) or ``utc``.
    """
    if moment.tzinfo is None:
        raise ValueError("captured_at must be timezone-aware")
    converted = moment.astimezone(UTC if zone == "utc" else None)
    return converted.replace(tzinfo=None).isoformat(timespec="milliseconds")


class BackendClient:
    """Talks to the backend on behalf of one device.

    Args:
        config: Backend address, protocol and endpoint paths.
        identity: Device key and shelf id; ``device_id`` may still be unknown
            until :meth:`bind` is called after registration lookup.
        transport: Custom httpx transport; tests pass ``httpx.MockTransport``.
    """

    def __init__(
        self,
        config: BackendConfig,
        identity: DeviceIdentity,
        *,
        transport: httpx.BaseTransport | None = None,
    ) -> None:
        self._config = config
        self._identity = identity
        self._http = httpx.Client(
            base_url=config.base_url,
            timeout=config.timeout_s,
            transport=transport,
            headers={
                DEVICE_KEY_HEADER: identity.device_key,
                "User-Agent": f"smart-warehouse-shelf-agent/{__version__}",
            },
        )

    @property
    def identity(self) -> DeviceIdentity:
        return self._identity

    def bind(self, identity: DeviceIdentity) -> None:
        """Use a resolved identity (with the backend-generated ``device_id``)."""
        self._identity = identity

    def upload_image(self, frame: Frame) -> UploadReceipt:
        """Report one captured frame to the backend.

        Only device metadata (and, for ``multipart``, the image) is sent; a
        test image's ground truth stays on the client.

        Raises:
            BackendError: If the backend did not accept the frame.
        """
        device_id = self._require_device_id()
        if self._config.protocol == PROTOCOL_MULTIPART:
            response = self._upload_multipart(frame, device_id)
        elif self._config.protocol == PROTOCOL_DEVICE_IMAGES:
            response = self._upload_device_image(frame, device_id)
        else:
            response = self._post_image_event(frame, device_id)
        return UploadReceipt(
            status_code=response.status_code,
            image_event_id=_parse_event_id(response),
            quantity=_parse_quantity(response),
        )

    def heartbeat(self) -> None:
        """Tell the backend the device is alive (updates ``Device.lastSeenAt``).

        Raises:
            BackendError: If the heartbeat was not accepted.
        """
        path = self._config.heartbeat_path.format(device_id=self._require_device_id())
        self._send("PATCH", path)

    def list_devices(self) -> list[RemoteDevice]:
        """All devices known to the backend (``GET /devices``)."""
        body = self._send("GET", self._config.devices_path).json()
        if not isinstance(body, list):
            raise BackendError("GET /devices: expected a JSON array", retryable=False)
        return [_parse_device(item) for item in body if isinstance(item, dict)]

    def register_device(self) -> int:
        """Create this device in the backend (``POST /devices``).

        Returns:
            The id the backend assigned.

        Raises:
            BackendError: ``404`` if the shelf does not exist, ``409`` if the key
                is taken or the shelf already has a device.
        """
        payload = {
            "deviceKey": self._identity.device_key,
            "shelfId": self._identity.shelf_id,
        }
        response = self._send("POST", self._config.devices_path, json=payload)
        device_id = _parse_event_id(response)
        if device_id is None:
            raise BackendError("POST /devices: response has no id", retryable=False)
        return device_id

    def close(self) -> None:
        self._http.close()

    def _post_image_event(self, frame: Frame, device_id: int) -> httpx.Response:
        if frame.source_path is None:
            raise BackendError(
                f"{frame.filename}: no stored image path to report", retryable=False
            )
        payload = {
            "deviceId": device_id,
            "imagePath": frame.source_path.as_posix(),
            "capturedAt": format_local_datetime(
                frame.captured_at, self._config.timestamp_zone
            ),
        }
        return self._send("POST", self._config.image_events_path, json=payload)

    def _upload_device_image(self, frame: Frame, device_id: int) -> httpx.Response:
        path = self._config.device_images_path.format(device_id=device_id)
        params = {
            "capturedAt": format_local_datetime(
                frame.captured_at, self._config.timestamp_zone
            )
        }
        files = {"file": (frame.filename, frame.data, frame.content_type)}
        return self._send("POST", path, params=params, files=files)

    def _upload_multipart(self, frame: Frame, device_id: int) -> httpx.Response:
        fields = {
            "deviceId": str(device_id),
            "shelfId": str(self._identity.shelf_id),
            "capturedAt": format_timestamp(frame.captured_at),
            "clientEventId": frame.event_id,
        }
        files = {"image": (frame.filename, frame.data, frame.content_type)}
        return self._send("POST", self._config.upload_path, data=fields, files=files)

    def _require_device_id(self) -> int:
        if self._identity.device_id is None:
            raise BackendError(
                f"device '{self._identity.device_key}' has no id yet "
                "(registration lookup has not succeeded)",
                retryable=True,
            )
        return self._identity.device_id

    def _send(self, method: str, path: str, **kwargs: Any) -> httpx.Response:
        try:
            response = self._http.request(method, path, **kwargs)
        except httpx.TransportError as exc:
            raise BackendError(
                f"{method} {path}: {type(exc).__name__}: {exc}", retryable=True
            ) from exc

        if response.is_success:
            return response
        status = response.status_code
        raise BackendError(
            f"{method} {path}: HTTP {status} {response.text[:200]}",
            retryable=status >= 500 or status in _RETRYABLE_STATUS,
            status_code=status,
        )


def _parse_event_id(response: httpx.Response) -> int | None:
    """Read a created id from ``42``, ``{"id": 42}`` or ``{"imageEventId": 42}``."""
    body = _json(response)
    if isinstance(body, dict):
        body = body.get("imageEventId", body.get("id"))
    return _as_int(body)


def _parse_quantity(response: httpx.Response) -> int | None:
    """Recognised quantity from a ``CameraImageResponseDto``, if present."""
    body = _json(response)
    return _as_int(body.get("quantity")) if isinstance(body, dict) else None


def _json(response: httpx.Response) -> Any:
    try:
        return response.json()
    except ValueError:
        return None


def _as_int(value: Any) -> int | None:
    if isinstance(value, int) and not isinstance(value, bool):
        return value
    return None


def _parse_device(item: dict[str, Any]) -> RemoteDevice:
    shelf_id = item.get("shelfId")
    return RemoteDevice(
        id=int(item["id"]),
        device_key=str(item.get("deviceKey", "")),
        shelf_id=int(shelf_id) if shelf_id is not None else None,
        status=item.get("status"),
    )
