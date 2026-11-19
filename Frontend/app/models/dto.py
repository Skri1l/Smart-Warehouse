"""Plain data classes mirroring backend API schemas (see API_GUIDELINES.md)."""

from dataclasses import dataclass
from enum import StrEnum


class Role(StrEnum):
    """User role returned by the backend at login."""

    WORKER = "WORKER"
    MANAGER = "MANAGER"


@dataclass(frozen=True)
class Session:
    """Authenticated session kept in memory only."""

    token: str
    username: str
    role: Role


@dataclass(frozen=True)
class Product:
    """Product with its current total quantity across shelves."""

    id: int
    sku: str
    name: str
    category: str
    unit: str
    quantity: int
    min_quantity: int
