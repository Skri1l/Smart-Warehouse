"""In-memory mock of the backend API, used while the real API is not ready."""

from app.api.client import ApiError
from app.models.dto import Product, Role, Session

_USERS = {"worker": Role.WORKER, "manager": Role.MANAGER}

_PRODUCTS = [
    Product(1, "SKU-1001", "Milk 1L", "Dairy", "pcs", 42, 20),
    Product(2, "SKU-1002", "Butter 200g", "Dairy", "pcs", 8, 10),
    Product(3, "SKU-2001", "Rice 1kg", "Grocery", "pcs", 120, 30),
    Product(4, "SKU-2002", "Pasta 500g", "Grocery", "pcs", 15, 25),
    Product(5, "SKU-3001", "Apple juice 1L", "Drinks", "pcs", 64, 20),
]


class MockApiClient:
    """Accepts users ``worker`` and ``manager`` with any non-empty password."""

    def login(self, username: str, password: str) -> Session:
        role = _USERS.get(username)
        if role is None or not password:
            raise ApiError("Invalid username or password.")
        return Session(token="mock-token", username=username, role=role)

    def list_products(self, session: Session) -> list[Product]:
        return list(_PRODUCTS)
