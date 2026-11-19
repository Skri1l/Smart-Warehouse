"""State and actions for the dashboard and product search."""

from typing import Any

from PySide6.QtCore import QObject, Signal

from app.api.client import WarehouseApi
from app.core.stock import filter_products, is_low_stock
from app.models.dto import Product, Session
from app.viewmodels.worker import run_in_background


class ProductsViewModel(QObject):
    """Loads products and exposes filtered and low-stock views of them."""

    productsChanged = Signal(list)
    lowStockChanged = Signal(list)
    errorOccurred = Signal(str)

    def __init__(self, api: WarehouseApi, session: Session) -> None:
        super().__init__()
        self._api = api
        self._session = session
        self._products: list[Product] = []
        self._query = ""
        self._pending: Any = None

    def refresh(self) -> None:
        self._pending = run_in_background(
            lambda: self._api.list_products(self._session),
            self._on_loaded,
            self.errorOccurred.emit,
        )

    def set_query(self, query: str) -> None:
        self._query = query
        self.productsChanged.emit(filter_products(self._products, query))

    def _on_loaded(self, products: list[Product]) -> None:
        self._products = products
        self.productsChanged.emit(filter_products(products, self._query))
        self.lowStockChanged.emit([p for p in products if is_low_stock(p)])
