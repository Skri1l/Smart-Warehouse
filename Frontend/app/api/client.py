"""Typed REST client for the Smart Warehouse backend.

Shapes follow the live OpenAPI spec at ``/v3/api-docs``. Authentication is
HTTP Basic: there is no login endpoint, so :meth:`ApiClient.login` validates
the credentials with an authenticated request.
"""

import base64
from collections import defaultdict
from typing import Any, Protocol

import httpx

from app.models.dto import Product, Role, Session

DEFAULT_BASE_URL = "http://localhost:8080"


class ApiError(Exception):
    """Raised when the backend returns an error or cannot be reached."""

    def __init__(self, message: str, status: int | None = None) -> None:
        super().__init__(message)
        self.status = status


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
        token = base64.b64encode(f"{username}:{password}".encode()).decode("ascii")
        session = Session(token=token, username=username, role=Role.WORKER)
        try:
            users = self._request("GET", "/users", headers=_auth(session))
        except ApiError as exc:
            # Credentials are valid but this account may not list users.
            if exc.status == 403:
                return session
            raise
        role = next(
            (Role(u["role"]) for u in users if u.get("username") == username),
            Role.WORKER,
        )
        return Session(token=token, username=username, role=role)

    def list_products(self, session: Session) -> list[Product]:
        products = self._request("GET", "/products", headers=_auth(session))
        stock = self._request("GET", "/shelf-stock", headers=_auth(session))
        quantities: defaultdict[int, int] = defaultdict(int)
        for item in stock:
            if item.get("status", "ACTIVE") == "ACTIVE":
                quantities[item["productId"]] += item.get("quantity") or 0
        return [
            Product(
                id=item["id"],
                sku=item["sku"],
                name=item["name"],
                description=item.get("description") or "",
                quantity=quantities[item["id"]],
            )
            for item in products
        ]

    def _request(self, method: str, path: str, **kwargs: Any) -> Any:
        try:
            response = self._http.request(method, path, **kwargs)
        except httpx.HTTPError as exc:
            raise ApiError(f"Cannot reach server: {exc}") from exc
        if response.status_code == 401:
            raise ApiError("Invalid username or password.", 401)
        if response.is_error:
            try:
                body = response.json()
                detail = body.get("message") or body.get("detail") or response.text
            except (ValueError, AttributeError):
                detail = response.text
            raise ApiError(
                f"Server error {response.status_code}: {detail}", response.status_code
            )
        return response.json()


def _auth(session: Session) -> dict[str, str]:
    return {"Authorization": f"Basic {session.token}"}
