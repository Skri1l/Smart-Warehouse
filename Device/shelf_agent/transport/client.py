"""HTTP client for the device-facing backend API (RFC-0001, ADR-0009).

This is the single implementation of the wire protocol. The real device and
the emulator both send through :class:`BackendClient`, so whatever the
emulator demonstrates is exactly what the Raspberry Pi will send.
"""

from __future__ import annotations

from dataclasses import dataclass
from datetime import UTC, datetime

import httpx

from shelf_agent import __version__
from shelf_agent.config import BackendConfig, DeviceIdentity
from shelf_agent.models import Frame

DEVICE_KEY_HEADER = "X-Device-Key"

# Status codes worth retrying: the request may succeed later without changes.
_RETRYABLE_STATUS = {408, 425, 429}


class BackendError(Exception):
    """A request to the backend failed.

    Attributes:
        retryable: ``True`` for network errors, timeouts and 5xx/429 — the
            frame is kept in the outbox. ``False`` for other 4xx: the request
            itself is wrong (unknown device, bad key) and repeating it won't help.
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
    """Backend's answer to an accepted upload."""

    status_code: int
    image_event_id: int | None


def format_timestamp(moment: datetime) -> str:
    """ISO-8601 in UTC with millisecond precision, e.g. ``2026-10-07T12:00:00.000Z``."""
    if moment.tzinfo is None:
        raise ValueError("captured_at must be timezone-aware")
    return (
        moment.astimezone(UTC).isoformat(timespec="milliseconds").replace("+00:00", "Z")
    )


class BackendClient:
    """Sends images and heartbeats on behalf of one device.

    Args:
        config: Backend address and endpoint paths.
        identity: Device and shelf ids plus the device key.
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

    def upload_image(self, frame: Frame) -> UploadReceipt:
        """Upload one frame as ``multipart/form-data``.

        Only the image and its device metadata are sent; a test image's ground
        truth stays on the client.

        Raises:
            BackendError: If the backend did not accept the image.
        """
        fields = {
            "deviceId": str(self._identity.device_id),
            "shelfId": str(self._identity.shelf_id),
            "capturedAt": format_timestamp(frame.captured_at),
            "clientEventId": frame.event_id,
        }
        files = {"image": (frame.filename, frame.data, frame.content_type)}
        response = self._send(
            "POST", self._config.upload_path, data=fields, files=files
        )
        return UploadReceipt(
            status_code=response.status_code,
            image_event_id=_parse_event_id(response),
        )

    def heartbeat(self) -> None:
        """Tell the backend the device is alive (updates ``Device.lastSeenAt``).

        Raises:
            BackendError: If the heartbeat was not accepted.
        """
        path = self._config.heartbeat_path.format(device_id=self._identity.device_id)
        self._send("PATCH", path)

    def close(self) -> None:
        self._http.close()

    def _send(self, method: str, path: str, **kwargs: object) -> httpx.Response:
        try:
            response = self._http.request(method, path, **kwargs)  # type: ignore[arg-type]
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
    """Read the created ``ImageEvent`` id from either ``42`` or ``{"id": 42}``."""
    try:
        body = response.json()
    except ValueError:
        return None
    if isinstance(body, dict):
        body = body.get("id")
    if isinstance(body, int) and not isinstance(body, bool):
        return body
    return None
