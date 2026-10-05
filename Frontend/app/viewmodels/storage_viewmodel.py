"""State and actions for warehouses, racks and shelves."""

from collections.abc import Callable
from typing import Any

from PySide6.QtCore import QObject, Signal

from app.api.client import WarehouseApi
from app.models.dto import Session
from app.viewmodels.worker import run_in_background


class StorageViewModel(QObject):
    """Loads the warehouse → rack → shelf hierarchy and creates new entries."""

    warehousesChanged = Signal(list)
    racksChanged = Signal(list)
    shelvesChanged = Signal(list)
    created = Signal(str)
    errorOccurred = Signal(str)

    def __init__(self, api: WarehouseApi, session: Session) -> None:
        super().__init__()
        self._api = api
        self._session = session
        self._pending: list[Any] = []

    def refresh(self) -> None:
        s = self._session
        self._run(lambda: self._api.list_warehouses(s), self.warehousesChanged.emit)
        self._run(lambda: self._api.list_racks(s), self.racksChanged.emit)
        self._run(lambda: self._api.list_shelves(s), self.shelvesChanged.emit)

    def create_warehouse(self, name: str, address: str) -> None:
        self._create(
            lambda: self._api.create_warehouse(self._session, name, address),
            f"Warehouse '{name}' created.",
        )

    def create_rack(self, label: str, warehouse_id: int) -> None:
        self._create(
            lambda: self._api.create_rack(self._session, label, warehouse_id),
            f"Rack '{label}' created.",
        )

    def create_shelf(self, label: str, capacity: int, rack_id: int) -> None:
        self._create(
            lambda: self._api.create_shelf(self._session, label, capacity, rack_id),
            f"Shelf '{label}' created.",
        )

    def _create(self, fn: Callable[[], Any], message: str) -> None:
        def done(_: Any) -> None:
            self.created.emit(message)
            self.refresh()

        self._run(fn, done)

    def _run(self, fn: Callable[[], Any], on_success: Callable[[Any], None]) -> None:
        # Keep recent signal holders alive until their tasks report back.
        task = run_in_background(fn, on_success, self.errorOccurred.emit)
        self._pending = [*self._pending[-20:], task]
