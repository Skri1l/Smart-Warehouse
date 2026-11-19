from __future__ import annotations

import json
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
    RemoteDevice,
    format_local_datetime,
    format_timestamp,
)

Handler = Callable[[httpx.Request], httpx.Response]


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
    handler: Handler, config: BackendConfig, identity: DeviceIdentity
) -> BackendClient:
    return BackendClient(config, identity, transport=httpx.MockTransport(handler))


def recording(
    response: httpx.Response,
) -> tuple[list[httpx.Request], Handler]:
    requests: list[httpx.Request] = []

    def handler(request: httpx.Request) -> httpx.Response:
        requests.append(request)
        return response

    return requests, handler


# --- image-events (Swagger: POST /image-events) ---------------------------------


def test_posts_image_event_json_as_published_in_swagger(
    backend_config: BackendConfig,
    identity: DeviceIdentity,
    make_frame: Callable[..., Frame],
) -> None:
    requests, handler = recording(httpx.Response(201, json=41))

    receipt = client_with(handler, backend_config, identity).upload_image(make_frame())

    request = requests[0]
    assert request.method == "POST"
    assert request.url.path == "/image-events"
    assert request.headers["content-type"] == "application/json"
    assert json.loads(request.read()) == {
        "deviceId": 7,
        "imagePath": "/data/captures/shot.jpg",
        "capturedAt": "2026-10-07T12:30:00.000",
    }
    assert receipt.status_code == 201
    assert receipt.image_event_id == 41


def test_image_event_never_contains_ground_truth_or_extra_fields(
    backend_config: BackendConfig,
    identity: DeviceIdentity,
    make_frame: Callable[..., Frame],
) -> None:
    requests, handler = recording(httpx.Response(201, json=1))

    frame = make_frame(truth=GroundTruth("SECRET-SKU", 99))
    client_with(handler, backend_config, identity).upload_image(frame)

    body = requests[0].read()
    assert b"SECRET-SKU" not in body
    assert set(json.loads(body)) == {"deviceId", "imagePath", "capturedAt"}


def test_rejects_frame_without_stored_path_when_sending_image_event(
    backend_config: BackendConfig,
    identity: DeviceIdentity,
    make_frame: Callable[..., Frame],
) -> None:
    _, handler = recording(httpx.Response(201, json=1))

    with pytest.raises(BackendError) as error:
        client_with(handler, backend_config, identity).upload_image(
            make_frame(source_path=None)
        )

    assert not error.value.retryable


def test_waits_with_retryable_error_when_device_id_is_unknown(
    backend_config: BackendConfig, make_frame: Callable[..., Frame]
) -> None:
    requests, handler = recording(httpx.Response(201, json=1))
    unresolved = DeviceIdentity(device_id=None, shelf_id=3, device_key="k")

    with pytest.raises(BackendError) as error:
        client_with(handler, backend_config, unresolved).upload_image(make_frame())

    assert error.value.retryable
    assert requests == []


def test_formats_local_datetime_without_offset() -> None:
    moment = datetime(
        2026, 10, 7, 15, 0, 1, 250000, tzinfo=timezone(timedelta(hours=3))
    )

    assert format_local_datetime(moment, "utc") == "2026-10-07T12:00:01.250"
    assert "+" not in format_local_datetime(moment, "local")


# --- device-images (POST /devices/{id}/images) --------------------------------


def test_uploads_image_to_device_images_endpoint(
    device_images_config: BackendConfig,
    identity: DeviceIdentity,
    make_frame: Callable[..., Frame],
) -> None:
    body = {
        "imageEventId": 41,
        "recognitionResultId": 9,
        "shelfId": 3,
        "previousQuantity": 16,
        "quantity": 15,
        "quantityDelta": -1,
    }
    requests, handler = recording(httpx.Response(200, json=body))

    frame = make_frame(name="5.jpg", data=b"JPEGDATA")
    receipt = client_with(handler, device_images_config, identity).upload_image(frame)

    request = requests[0]
    assert request.method == "POST"
    assert request.url.path == "/devices/7/images"
    assert request.url.params["capturedAt"] == "2026-10-07T12:30:00.000"
    parts = parse_multipart(request)
    assert set(parts) == {"file"}
    assert parts["file"].get_filename() == "5.jpg"
    assert parts["file"].get_payload(decode=True) == b"JPEGDATA"
    assert (receipt.image_event_id, receipt.quantity) == (41, 15)


def test_device_images_never_sends_ground_truth(
    device_images_config: BackendConfig,
    identity: DeviceIdentity,
    make_frame: Callable[..., Frame],
) -> None:
    requests, handler = recording(httpx.Response(200, json={"imageEventId": 1}))

    frame = make_frame(truth=GroundTruth("SECRET-SKU", 99))
    client_with(handler, device_images_config, identity).upload_image(frame)

    assert b"SECRET-SKU" not in requests[0].read()
    assert b"SECRET-SKU" not in str(requests[0].url).encode()


# --- multipart (RFC-0001) ------------------------------------------------------


def test_upload_sends_rfc_0001_multipart_fields(
    multipart_config: BackendConfig,
    identity: DeviceIdentity,
    make_frame: Callable[..., Frame],
) -> None:
    requests, handler = recording(httpx.Response(202, json={"id": 41}))

    frame = make_frame(name="shelf.jpg", data=b"JPEGDATA")
    receipt = client_with(handler, multipart_config, identity).upload_image(frame)

    request = requests[0]
    assert request.url.path == "/api/v1/device-events/images"
    assert request.headers[DEVICE_KEY_HEADER] == "test-key"
    parts = parse_multipart(request)
    assert text(parts["deviceId"]) == "7"
    assert text(parts["shelfId"]) == "3"
    assert text(parts["capturedAt"]) == "2026-10-07T12:30:00.000Z"
    assert text(parts["clientEventId"]) == frame.event_id
    assert parts["image"].get_filename() == "shelf.jpg"
    assert parts["image"].get_payload(decode=True) == b"JPEGDATA"
    assert receipt.image_event_id == 41


def test_multipart_never_sends_ground_truth(
    multipart_config: BackendConfig,
    identity: DeviceIdentity,
    make_frame: Callable[..., Frame],
) -> None:
    requests, handler = recording(httpx.Response(202))

    frame = make_frame(truth=GroundTruth("SECRET-SKU", 99))
    client_with(handler, multipart_config, identity).upload_image(frame)

    assert b"SECRET-SKU" not in requests[0].read()
    assert set(parse_multipart(requests[0])) == {
        "deviceId",
        "shelfId",
        "capturedAt",
        "clientEventId",
        "image",
    }


def test_formats_timestamp_as_utc_with_z() -> None:
    moment = datetime(2026, 10, 7, 15, 0, tzinfo=timezone(timedelta(hours=3)))

    assert format_timestamp(moment) == "2026-10-07T12:00:00.000Z"


def test_rejects_naive_timestamp() -> None:
    with pytest.raises(ValueError):
        format_timestamp(datetime(2026, 10, 7, 12, 0))
    with pytest.raises(ValueError):
        format_local_datetime(datetime(2026, 10, 7, 12, 0), "utc")


# --- responses and errors ------------------------------------------------------


@pytest.mark.parametrize(
    ("body", "expected"),
    [
        ({"id": 5}, 5),
        ({"imageEventId": 5}, 5),
        (5, 5),
        ({"other": 1}, None),
        (None, None),
    ],
)
def test_reads_event_id_when_backend_returns_it(
    body: object,
    expected: int | None,
    backend_config: BackendConfig,
    identity: DeviceIdentity,
    make_frame: Callable[..., Frame],
) -> None:
    response = httpx.Response(201) if body is None else httpx.Response(201, json=body)
    _, handler = recording(response)

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


# --- devices -------------------------------------------------------------------


def test_heartbeat_patches_device_path(
    backend_config: BackendConfig, identity: DeviceIdentity
) -> None:
    requests, handler = recording(httpx.Response(204))

    client_with(handler, backend_config, identity).heartbeat()

    assert requests[0].method == "PATCH"
    assert requests[0].url.path == "/devices/7/heartbeat"


def test_lists_devices_from_device_response_dto(
    backend_config: BackendConfig, identity: DeviceIdentity
) -> None:
    body = [
        {
            "id": 4,
            "deviceKey": "k4",
            "status": "ACTIVE",
            "lastSeenAt": None,
            "shelfId": 9,
        }
    ]
    requests, handler = recording(httpx.Response(200, json=body))

    devices = client_with(handler, backend_config, identity).list_devices()

    assert requests[0].url.path == "/devices"
    assert devices == [RemoteDevice(id=4, device_key="k4", shelf_id=9, status="ACTIVE")]


def test_registers_device_with_key_and_shelf(
    backend_config: BackendConfig, identity: DeviceIdentity
) -> None:
    requests, handler = recording(httpx.Response(201, json=12))

    device_id = client_with(handler, backend_config, identity).register_device()

    assert requests[0].method == "POST"
    assert requests[0].url.path == "/devices"
    assert json.loads(requests[0].read()) == {"deviceKey": "test-key", "shelfId": 3}
    assert device_id == 12
