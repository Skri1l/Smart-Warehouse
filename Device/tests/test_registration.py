from __future__ import annotations

import json
import threading
from typing import Any

import httpx
import pytest

from shelf_agent.config import BackendConfig, DeviceIdentity
from shelf_agent.registration import RegistrationError, connect, resolve_identity
from shelf_agent.transport.client import BackendClient, BackendError


class FakeDevicesApi:
    """``GET``/``POST /devices`` over ``httpx.MockTransport``."""

    def __init__(self, devices: list[dict[str, Any]] | None = None) -> None:
        self.devices = devices or []
        self.created: list[dict[str, Any]] = []
        self.offline = False

    def __call__(self, request: httpx.Request) -> httpx.Response:
        if self.offline:
            raise httpx.ConnectError("connection refused", request=request)
        if request.method == "GET":
            return httpx.Response(200, json=self.devices)
        payload = json.loads(request.read())
        self.created.append(payload)
        new_id = 100 + len(self.created)
        self.devices.append({"id": new_id, "status": "ACTIVE", **payload})
        return httpx.Response(201, json=new_id)


def make_client(
    api: FakeDevicesApi,
    *,
    device_id: int | None = None,
    shelf_id: int = 3,
) -> BackendClient:
    identity = DeviceIdentity(device_id=device_id, shelf_id=shelf_id, device_key="k1")
    return BackendClient(
        BackendConfig(base_url="http://backend.test"),
        identity,
        transport=httpx.MockTransport(api),
    )


def device(id: int, key: str = "k1", shelf: int = 3, status: str = "ACTIVE") -> dict:
    return {"id": id, "deviceKey": key, "shelfId": shelf, "status": status}


def test_binds_backend_id_when_key_is_registered() -> None:
    client = make_client(FakeDevicesApi([device(5, key="other", shelf=1), device(8)]))

    resolved = resolve_identity(client, register=False)

    assert resolved.device_id == 8
    assert client.identity.device_id == 8


def test_registers_device_when_missing_and_registration_enabled() -> None:
    api = FakeDevicesApi()
    client = make_client(api)

    resolved = resolve_identity(client, register=True)

    assert api.created == [{"deviceKey": "k1", "shelfId": 3}]
    assert resolved.device_id == 101


def test_explains_how_to_register_when_missing_and_registration_disabled() -> None:
    client = make_client(FakeDevicesApi())

    with pytest.raises(RegistrationError, match="POST /devices"):
        resolve_identity(client, register=False)


def test_fails_when_key_belongs_to_another_shelf() -> None:
    client = make_client(FakeDevicesApi([device(8, shelf=4)]))

    with pytest.raises(RegistrationError, match="shelf 4"):
        resolve_identity(client, register=True)


def test_fails_when_configured_id_differs_from_backend() -> None:
    client = make_client(FakeDevicesApi([device(8)]), device_id=2)

    with pytest.raises(RegistrationError, match="id 8"):
        resolve_identity(client, register=False)


def test_accepts_inactive_device_with_a_warning(
    caplog: pytest.LogCaptureFixture,
) -> None:
    client = make_client(FakeDevicesApi([device(8, status="INACTIVE")]))

    assert resolve_identity(client, register=False).device_id == 8
    assert "INACTIVE" in caplog.text


def test_connect_starts_offline_when_device_id_is_configured() -> None:
    api = FakeDevicesApi()
    api.offline = True
    client = make_client(api, device_id=8)

    assert connect(
        client, register=False, stop=threading.Event(), retry_s=0, wait=False
    )
    assert client.identity.device_id == 8


def test_connect_fails_fast_offline_without_id_when_not_waiting() -> None:
    api = FakeDevicesApi()
    api.offline = True

    with pytest.raises(BackendError):
        connect(
            make_client(api),
            register=True,
            stop=threading.Event(),
            retry_s=0,
            wait=False,
        )


def test_connect_gives_up_when_stopped_while_waiting_for_backend() -> None:
    api = FakeDevicesApi()
    api.offline = True
    stop = threading.Event()
    stop.set()

    assert connect(make_client(api), register=True, stop=stop, retry_s=0) is False


def test_refuses_to_register_when_an_id_is_configured_for_unknown_key() -> None:
    api = FakeDevicesApi()

    with pytest.raises(RegistrationError, match="remove device_id"):
        resolve_identity(make_client(api, device_id=2), register=True)
    assert api.created == []
