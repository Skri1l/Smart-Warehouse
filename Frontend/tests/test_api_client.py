import base64
from collections.abc import Callable

import httpx
import pytest

from app.api.client import ApiClient, ApiError
from app.models.dto import Role, Session

TOKEN = base64.b64encode(b"anna:pw").decode()


def _client(handler: Callable[[httpx.Request], httpx.Response]) -> ApiClient:
    return ApiClient("http://test", transport=httpx.MockTransport(handler))


def test_login_returns_role_when_user_listed() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        assert request.url.path == "/users"
        assert request.headers["Authorization"] == f"Basic {TOKEN}"
        return httpx.Response(200, json=[{"username": "anna", "role": "MANAGER"}])

    session = _client(handler).login("anna", "pw")
    assert session.token == TOKEN
    assert session.role is Role.MANAGER


def test_login_defaults_to_worker_when_user_not_listed() -> None:
    client = _client(lambda request: httpx.Response(200, json=[]))
    assert client.login("anna", "pw").role is Role.WORKER


def test_login_defaults_to_worker_when_users_forbidden() -> None:
    client = _client(lambda request: httpx.Response(403))
    assert client.login("anna", "pw").role is Role.WORKER


def test_login_raises_when_unauthorized() -> None:
    client = _client(lambda request: httpx.Response(401))
    with pytest.raises(ApiError, match="Invalid"):
        client.login("anna", "bad")


def test_error_message_taken_from_backend_body() -> None:
    client = _client(
        lambda request: httpx.Response(500, json={"status": 500, "message": "Boom"})
    )
    with pytest.raises(ApiError, match="Boom"):
        client.login("anna", "pw")


def test_list_products_sums_active_shelf_stock() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        assert request.headers["Authorization"] == "Basic t"
        if request.url.path == "/products":
            return httpx.Response(
                200,
                json=[
                    {"id": 1, "sku": "S", "name": "N", "description": None},
                    {"id": 2, "sku": "E", "name": "Empty", "description": "d"},
                ],
            )
        assert request.url.path == "/shelf-stock"
        return httpx.Response(
            200,
            json=[
                {"productId": 1, "quantity": 3, "status": "ACTIVE"},
                {"productId": 1, "quantity": 4, "status": "ACTIVE"},
                {"productId": 1, "quantity": 9, "status": "EXPIRED"},
            ],
        )

    products = _client(handler).list_products(Session("t", "anna", Role.WORKER))
    assert [(p.sku, p.quantity) for p in products] == [("S", 7), ("E", 0)]
    assert products[0].description == ""
