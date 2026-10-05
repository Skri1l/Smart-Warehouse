"""Typed REST client for the Smart Warehouse backend."""

from typing import Any, Protocol

import httpx

from app.models.dto import Product, Role, Session

DEFAULT_BASE_URL = "http://localhost:8080"


class ApiError(Exception):
    """Raised when the backend returns an error or cannot be reached."""


class WarehouseApi(Protocol):
    """Operations the UI needs from the backend."""

    def login(self, username: str, password: str) -> Session: ...

    def list_products(self, session: Session) -> list[Product]: ...


class ApiClient:
    """httpx-based implementation of :class:`WarehouseApi`."""

    def __init__(
        self,
        base_url: str = DEFAULT_BASE_URL,
        transport: httpx.BaseTransport | None = None,
    ) -> None:
        self._http = httpx.Client(base_url=base_url, timeout=10.0, transport=transport)

    def login(self, username: str, password: str) -> Session:
        data = self._request(
            "POST",
            "/api/v1/auth/login",
            json={"username": username, "password": password},
        )
        return Session(token=data["token"], username=username, role=Role(data["role"]))

    def list_products(self, session: Session) -> list[Product]:
        data = self._request(
            "GET",
            "/api/v1/products",
            headers={"Authorization": f"Bearer {session.token}"},
        )
        items = data["content"] if isinstance(data, dict) else data
        return [
            Product(
                id=item["id"],
                sku=item["sku"],
                name=item["name"],
                category=item.get("category", ""),
                unit=item.get("unit", ""),
                quantity=item.get("quantity", 0),
                min_quantity=item.get("minQuantity", 0),
            )
            for item in items
        ]

    def _request(self, method: str, path: str, **kwargs: Any) -> Any:
        try:
            response = self._http.request(method, path, **kwargs)
        except httpx.HTTPError as exc:
            raise ApiError(f"Cannot reach server: {exc}") from exc
        if response.status_code == 401:
            raise ApiError("Invalid username or password.")
        if response.is_error:
            try:
                detail = response.json().get("detail", response.text)
            except ValueError:
                detail = response.text
            raise ApiError(f"Server error {response.status_code}: {detail}")
        return response.json()
