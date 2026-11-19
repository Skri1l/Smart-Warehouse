from collections.abc import Callable

import httpx
import pytest

from app.api.client import ApiClient, ApiError
from app.models.dto import Role, Session


def _client(handler: Callable[[httpx.Request], httpx.Response]) -> ApiClient:
    return ApiClient("http://test", transport=httpx.MockTransport(handler))


def test_login_returns_session_when_credentials_valid() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        assert request.url.path == "/api/v1/auth/login"
        return httpx.Response(200, json={"token": "t", "role": "MANAGER"})

    session = _client(handler).login("anna", "pw")
    assert session.token == "t"
    assert session.role is Role.MANAGER


def test_login_raises_when_unauthorized() -> None:
    client = _client(lambda request: httpx.Response(401))
    with pytest.raises(ApiError, match="Invalid"):
        client.login("anna", "bad")


def test_list_products_sends_bearer_token() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        assert request.headers["Authorization"] == "Bearer t"
        item = {"id": 1, "sku": "S", "name": "N", "quantity": 3, "minQuantity": 1}
        return httpx.Response(200, json={"content": [item]})

    products = _client(handler).list_products(Session("t", "anna", Role.WORKER))
    assert products[0].sku == "S"
    assert products[0].min_quantity == 1
