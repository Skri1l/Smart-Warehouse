"""Main window: navigation sidebar with dashboard and product search pages."""

from PySide6.QtWidgets import (
    QDialog,
    QGroupBox,
    QHBoxLayout,
    QLabel,
    QLineEdit,
    QListWidget,
    QMainWindow,
    QMessageBox,
    QPushButton,
    QStackedWidget,
    QTableWidget,
    QTableWidgetItem,
    QVBoxLayout,
    QWidget,
)

from app.models.dto import Product, Rack, Role, Session, Shelf, Warehouse
from app.ui.forms import ProductDialog, RackDialog, ShelfDialog, WarehouseDialog
from app.viewmodels.products_viewmodel import ProductsViewModel
from app.viewmodels.storage_viewmodel import StorageViewModel

_COLUMNS = ["SKU", "Name", "Description", "Quantity"]


def _fill_table(table: QTableWidget, products: list[Product]) -> None:
    table.setRowCount(len(products))
    for row, p in enumerate(products):
        values = [p.sku, p.name, p.description, str(p.quantity)]
        for col, value in enumerate(values):
            table.setItem(row, col, QTableWidgetItem(value))
    table.resizeColumnsToContents()


def _fill_rows(table: QTableWidget, rows: list[list[str]]) -> None:
    table.setRowCount(len(rows))
    for row, values in enumerate(rows):
        for col, value in enumerate(values):
            table.setItem(row, col, QTableWidgetItem(value))
    table.resizeColumnsToContents()


def _make_table(columns: list[str] = _COLUMNS) -> QTableWidget:
    table = QTableWidget(0, len(columns))
    table.setHorizontalHeaderLabels(columns)
    table.setEditTriggers(QTableWidget.EditTrigger.NoEditTriggers)
    table.verticalHeader().setVisible(False)
    return table


class MainWindow(QMainWindow):
    """Top-level window; navigation entries depend on the user's role."""

    def __init__(
        self,
        session: Session,
        viewmodel: ProductsViewModel,
        storage_viewmodel: StorageViewModel,
    ) -> None:
        super().__init__()
        self.setWindowTitle(f"Smart Warehouse — {session.username}")
        self.resize(900, 560)
        self._vm = viewmodel
        self._storage_vm = storage_viewmodel
        self._warehouses: list[Warehouse] = []
        self._racks: list[Rack] = []

        self.nav = QListWidget()
        self.nav.setFixedWidth(180)
        self.pages = QStackedWidget()

        self._add_page("Dashboard", self._build_dashboard())
        self._add_page("Products", self._build_products())
        self._add_page("Warehouses & shelves", self._build_storage())
        if session.role in (Role.MANAGER, Role.ADMIN):
            # Role-based hiding is UX only; the backend enforces access (ADR-0006).
            self._add_page("Purchase recommendations", self._build_placeholder())

        central = QWidget()
        layout = QHBoxLayout(central)
        layout.addWidget(self.nav)
        layout.addWidget(self.pages, 1)
        self.setCentralWidget(central)

        self.nav.currentRowChanged.connect(self.pages.setCurrentIndex)
        self.nav.setCurrentRow(0)
        self._vm.productsChanged.connect(self._on_products)
        self._vm.lowStockChanged.connect(self._on_low_stock)
        self._vm.errorOccurred.connect(self._show_error)
        self._vm.created.connect(self.statusBar().showMessage)
        self._storage_vm.warehousesChanged.connect(self._on_warehouses)
        self._storage_vm.racksChanged.connect(self._on_racks)
        self._storage_vm.shelvesChanged.connect(self._on_shelves)
        self._storage_vm.errorOccurred.connect(self._show_error)
        self._storage_vm.created.connect(self.statusBar().showMessage)
        self.statusBar().showMessage(
            f"Signed in as {session.username} ({session.role})"
        )
        self._vm.refresh()
        self._storage_vm.refresh()

    def _add_page(self, title: str, page: QWidget) -> None:
        self.nav.addItem(title)
        self.pages.addWidget(page)

    def _build_dashboard(self) -> QWidget:
        page = QWidget()
        layout = QVBoxLayout(page)
        header = QHBoxLayout()
        header.addWidget(QLabel("<h2>Dashboard</h2>"))
        header.addStretch()
        refresh = QPushButton("Refresh")
        refresh.clicked.connect(self._vm.refresh)
        header.addWidget(refresh)
        layout.addLayout(header)
        self.low_stock_label = QLabel("Low-stock products: —")
        layout.addWidget(self.low_stock_label)
        self.low_stock_table = _make_table()
        layout.addWidget(self.low_stock_table)
        return page

    def _build_products(self) -> QWidget:
        page = QWidget()
        layout = QVBoxLayout(page)
        header = QHBoxLayout()
        header.addWidget(QLabel("<h2>Products</h2>"))
        header.addStretch()
        self.add_product_button = QPushButton("Add product")
        self.add_product_button.clicked.connect(self._add_product)
        header.addWidget(self.add_product_button)
        layout.addLayout(header)
        self.search_edit = QLineEdit()
        self.search_edit.setPlaceholderText("Search by SKU or name…")
        self.search_edit.textChanged.connect(self._vm.set_query)
        layout.addWidget(self.search_edit)
        self.products_table = _make_table()
        layout.addWidget(self.products_table)
        return page

    def _build_storage(self) -> QWidget:
        page = QWidget()
        layout = QVBoxLayout(page)
        header = QHBoxLayout()
        header.addWidget(QLabel("<h2>Warehouses &amp; shelves</h2>"))
        header.addStretch()
        refresh = QPushButton("Refresh")
        refresh.clicked.connect(self._storage_vm.refresh)
        header.addWidget(refresh)
        layout.addLayout(header)

        self.warehouses_table = _make_table(["ID", "Name", "Address"])
        self.racks_table = _make_table(["ID", "Label", "Warehouse"])
        self.shelves_table = _make_table(["ID", "Label", "Capacity", "Status", "Rack"])
        sections = [
            ("Warehouses", self.warehouses_table, "Add warehouse", self._add_warehouse),
            ("Racks", self.racks_table, "Add rack", self._add_rack),
            ("Shelves", self.shelves_table, "Add shelf", self._add_shelf),
        ]
        row = QHBoxLayout()
        for title, table, button_text, handler in sections:
            box = QGroupBox(title)
            box_layout = QVBoxLayout(box)
            box_layout.addWidget(table)
            button = QPushButton(button_text)
            button.clicked.connect(handler)
            box_layout.addWidget(button)
            row.addWidget(box)
        layout.addLayout(row)
        return page

    def _add_product(self) -> None:
        dialog = ProductDialog()
        if dialog.exec() == QDialog.DialogCode.Accepted:
            self._vm.create_product(*dialog.values())

    def _add_warehouse(self) -> None:
        dialog = WarehouseDialog()
        if dialog.exec() == QDialog.DialogCode.Accepted:
            self._storage_vm.create_warehouse(*dialog.values())

    def _add_rack(self) -> None:
        dialog = RackDialog(self._warehouses)
        if dialog.exec() == QDialog.DialogCode.Accepted:
            self._storage_vm.create_rack(*dialog.values())

    def _add_shelf(self) -> None:
        dialog = ShelfDialog(self._racks)
        if dialog.exec() == QDialog.DialogCode.Accepted:
            self._storage_vm.create_shelf(*dialog.values())

    def _show_error(self, message: str) -> None:
        self.statusBar().showMessage(message)
        if self.isVisible():
            QMessageBox.warning(self, "Smart Warehouse", message)

    def _on_warehouses(self, warehouses: list[Warehouse]) -> None:
        self._warehouses = warehouses
        _fill_rows(
            self.warehouses_table, [[str(w.id), w.name, w.address] for w in warehouses]
        )

    def _on_racks(self, racks: list[Rack]) -> None:
        self._racks = racks
        names = {w.id: w.name for w in self._warehouses}
        _fill_rows(
            self.racks_table,
            [
                [str(r.id), r.label, names.get(r.warehouse_id, f"#{r.warehouse_id}")]
                for r in racks
            ],
        )

    def _on_shelves(self, shelves: list[Shelf]) -> None:
        labels = {r.id: r.label for r in self._racks}
        _fill_rows(
            self.shelves_table,
            [
                [
                    str(s.id),
                    s.label,
                    str(s.capacity),
                    s.status,
                    labels.get(s.rack_id, f"#{s.rack_id}"),
                ]
                for s in shelves
            ],
        )

    def _build_placeholder(self) -> QWidget:
        page = QWidget()
        layout = QVBoxLayout(page)
        layout.addWidget(QLabel("<h2>Purchase recommendations</h2><p>Coming soon.</p>"))
        layout.addStretch()
        return page

    def _on_products(self, products: list[Product]) -> None:
        _fill_table(self.products_table, products)

    def _on_low_stock(self, products: list[Product]) -> None:
        self.low_stock_label.setText(f"Low-stock products: {len(products)}")
        _fill_table(self.low_stock_table, products)
