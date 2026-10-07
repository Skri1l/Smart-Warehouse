from __future__ import annotations

from collections.abc import Callable
from datetime import datetime, timedelta, timezone
from email.message import EmailMessage
from email.parser import BytesParser
from email.policy import HTTP

import httpx
import pytest

from shelf_agent.config import BackendConfig, DeviceIdentity
from shelf_agent.models import Frame, GroundTruth
from shelf_agent.transport.client import (
    DEVICE_KEY_HEADER,
    BackendClient,
    BackendError,
    format_timestamp,
)


def parse_multipart(request: httpx.Request) -> dict[str, EmailMessage]:
    header = f"Content-Type: {request.headers['content-type']}\r\n\r\n".encode()
    message = BytesParser(policy=HTTP).parsebytes(header + request.read())
    assert isinstance(message, EmailMessage)
    parts: dict[str, EmailMessage] = {}
    for part in message.iter_parts():
        assert isinstance(part, EmailMessage)
        name = part.get_param("name", header="content-disposition")
        assert isinstance(name, str)
        parts[name] = part
    return parts


def text(part: EmailMessage) -> str:
    payload = part.get_payload(decode=True)
    assert isinstance(payload, bytes)
    return payload.decode()


def client_with(
    handler: Callable[[httpx.Request], httpx.Response],
    backend_config: BackendConfig,
    identity: DeviceIdentity,
) -> BackendClient:
    return BackendClient(
        backend_config, identity, transport=httpx.MockTransport(handler)
    )


def test_upload_sends_rfc_0001_multipart_fields(
    backend_config: BackendConfig,
    identity: DeviceIdentity,
    make_frame: Callable[..., Frame],
) -> None:
    requests: list[httpx.Request] = []

    def handler(request: httpx.Request) -> httpx.Response:
        requests.append(request)
        return httpx.Response(202, json={"id": 41})

    frame = make_frame(name="shelf.jpg", data=b"JPEGDATA")
    receipt = client_with(handler, backend_config, identity).upload_image(frame)

    request = requests[0]
    assert request.method == "POST"
    assert request.url.path == "/api/v1/device-events/images"
    assert request.headers[DEVICE_KEY_HEADER] == "test-key"
    parts = parse_multipart(request)
    assert text(parts["deviceId"]) == "7"
    assert text(parts["shelfId"]) == "3"
    assert text(parts["capturedAt"]) == "2026-10-07T12:30:00.000Z"
    assert text(parts["clientEventId"]) == frame.event_id
    assert parts["image"].get_filename() == "shelf.jpg"
    assert parts["image"].get_content_type() == "image/jpeg"
    assert parts["image"].get_payload(decode=True) == b"JPEGDATA"
    assert receipt.status_code == 202
    assert receipt.image_event_id == 41


def test_upload_never_sends_ground_truth(
    backend_config: BackendConfig,
    identity: DeviceIdentity,
    make_frame: Callable[..., Frame],
) -> None:
    requests: list[httpx.Request] = []

    def handler(request: httpx.Request) -> httpx.Response:
        requests.append(request)
        return httpx.Response(202)

    frame = make_frame(truth=GroundTruth("SECRET-SKU", 99))
    client_with(handler, backend_config, identity).upload_image(frame)

    assert b"SECRET-SKU" not in requests[0].read()
    assert set(parse_multipart(requests[0])) == {
        "deviceId",
        "shelfId",
        "capturedAt",
        "clientEventId",
        "image",
    }


@pytest.mark.parametrize(
    ("body", "expected"),
    [({"id": 5}, 5), (5, 5), ({"other": 1}, None), (None, None)],
)
def test_reads_event_id_when_backend_returns_it(
    body: object,
    expected: int | None,
    backend_config: BackendConfig,
    identity: DeviceIdentity,
    make_frame: Callable[..., Frame],
) -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        if body is None:
            return httpx.Response(202)
        return httpx.Response(202, json=body)

    receipt = client_with(handler, backend_config, identity).upload_image(make_frame())

    assert receipt.image_event_id == expected


@pytest.mark.parametrize("status", [500, 502, 503, 429, 408])
def test_marks_error_retryable_when_server_side(
    status: int,
    backend_config: BackendConfig,
    identity: DeviceIdentity,
    make_frame: Callable[..., Frame],
) -> None:
    client = client_with(lambda r: httpx.Response(status), backend_config, identity)

    with pytest.raises(BackendError) as error:
        client.upload_image(make_frame())

    assert error.value.retryable
    assert error.value.status_code == status


@pytest.mark.parametrize("status", [400, 401, 403, 404, 409])
def test_marks_error_final_when_request_is_wrong(
    status: int,
    backend_config: BackendConfig,
    identity: DeviceIdentity,
    make_frame: Callable[..., Frame],
) -> None:
    client = client_with(lambda r: httpx.Response(status), backend_config, identity)

    with pytest.raises(BackendError) as error:
        client.upload_image(make_frame())

    assert not error.value.retryable


def test_marks_error_retryable_when_backend_unreachable(
    backend_config: BackendConfig,
    identity: DeviceIdentity,
    make_frame: Callable[..., Frame],
) -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        raise httpx.ConnectError("connection refused", request=request)

    with pytest.raises(BackendError) as error:
        client_with(handler, backend_config, identity).upload_image(make_frame())

    assert error.value.retryable
    assert error.value.status_code is None


def test_heartbeat_patches_device_path(
    backend_config: BackendConfig, identity: DeviceIdentity
) -> None:
    requests: list[httpx.Request] = []

    def handler(request: httpx.Request) -> httpx.Response:
        requests.append(request)
        return httpx.Response(204)

    client_with(handler, backend_config, identity).heartbeat()

    assert requests[0].method == "PATCH"
    assert requests[0].url.path == "/devices/7/heartbeat"
    assert requests[0].headers[DEVICE_KEY_HEADER] == "test-key"


def test_formats_timestamp_as_utc_with_z() -> None:
    moment = datetime(2026, 10, 7, 15, 0, tzinfo=timezone(timedelta(hours=3)))

    assert format_timestamp(moment) == "2026-10-07T12:00:00.000Z"


def test_rejects_naive_timestamp() -> None:
    with pytest.raises(ValueError):
        format_timestamp(datetime(2026, 10, 7, 12, 0))
