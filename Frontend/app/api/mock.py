"""In-memory mock of the backend API, used while the real API is not ready."""

from app.api.client import ApiError
from app.models.dto import Product, Rack, Role, Session, Shelf, Warehouse

_USERS = {"worker": Role.WORKER, "manager": Role.MANAGER}

_PRODUCTS = [
    Product(1, "SKU-1001", "Milk 1L", "Dairy", 42, 20),
    Product(2, "SKU-1002", "Butter 200g", "Dairy", 8, 10),
    Product(3, "SKU-2001", "Rice 1kg", "Grocery", 120, 30),
    Product(4, "SKU-2002", "Pasta 500g", "Grocery", 15, 25),
    Product(5, "SKU-3001", "Apple juice 1L", "Drinks", 64, 20),
]


class MockApiClient:
    """Accepts users ``worker`` and ``manager`` with any non-empty password.

    Created data lives only as long as the client instance.
    """

    def __init__(self) -> None:
        self._users: dict[str, Role] = dict(_USERS)
        self._products: list[Product] = list(_PRODUCTS)
        self._warehouses: list[Warehouse] = [Warehouse(1, "Main", "Minsk")]
        self._racks: list[Rack] = [Rack(1, "R-01", 1)]
        self._shelves: list[Shelf] = [Shelf(1, "R-01-S1", 50, "ACTIVE", 1)]

    def login(self, username: str, password: str) -> Session:
        role = self._users.get(username)
        if role is None or not password:
            raise ApiError("Invalid username or password.")
        return Session(token="mock-token", username=username, role=role)

    def register(self, email: str, username: str, password: str) -> None:
        if username in self._users:
            raise ApiError("User with that username already exists", 409)
        self._users[username] = Role.WORKER

    def list_products(self, session: Session) -> list[Product]:
        return list(self._products)

    def create_product(
        self, session: Session, sku: str, name: str, description: str
    ) -> int:
        if any(p.sku == sku for p in self._products):
            raise ApiError("Product with this SKU already exists", 409)
        product = Product(len(self._products) + 1, sku, name, description, 0)
        self._products.append(product)
        return product.id

    def list_warehouses(self, session: Session) -> list[Warehouse]:
        return list(self._warehouses)

    def create_warehouse(self, session: Session, name: str, address: str) -> int:
        item = Warehouse(len(self._warehouses) + 1, name, address)
        self._warehouses.append(item)
        return item.id

    def list_racks(self, session: Session) -> list[Rack]:
        return list(self._racks)

    def create_rack(self, session: Session, label: str, warehouse_id: int) -> int:
        item = Rack(len(self._racks) + 1, label, warehouse_id)
        self._racks.append(item)
        return item.id

    def list_shelves(self, session: Session) -> list[Shelf]:
        return list(self._shelves)

    def create_shelf(
        self, session: Session, label: str, capacity: int, rack_id: int
    ) -> int:
        item = Shelf(len(self._shelves) + 1, label, capacity, "ACTIVE", rack_id)
        self._shelves.append(item)
        return item.id
