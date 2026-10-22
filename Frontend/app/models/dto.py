"""Plain data classes mirroring backend API schemas (see /v3/api-docs)."""

from dataclasses import dataclass
from enum import StrEnum


class Role(StrEnum):
    """User role, mirrors the backend ``UserRole`` enum."""

    ADMIN = "ADMIN"
    WORKER = "WORKER"
    MANAGER = "MANAGER"


@dataclass(frozen=True)
class Session:
    """Authenticated session kept in memory only.

    ``token`` holds the base64 ``username:password`` pair sent as HTTP Basic
    credentials; the backend has no token-issuing login endpoint yet.
    """

    token: str
    username: str
    role: Role


@dataclass(frozen=True)
class Product:
    """Product with its current total quantity across shelves."""

    id: int
    sku: str
    name: str
    description: str
    quantity: int
    min_quantity: int = 0
    """The backend has no per-product minimum yet, so only empty stock is low."""


@dataclass(frozen=True)
class Warehouse:
    id: int
    name: str
    address: str


@dataclass(frozen=True)
class Rack:
    id: int
    label: str
    warehouse_id: int


@dataclass(frozen=True)
class Shelf:
    id: int
    label: str
    capacity: int
    status: str
    rack_id: int
