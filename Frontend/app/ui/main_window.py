"""Main window: navigation sidebar with dashboard and product search pages."""

from PySide6.QtWidgets import (
    QHBoxLayout,
    QLabel,
    QLineEdit,
    QListWidget,
    QMainWindow,
    QPushButton,
    QStackedWidget,
    QTableWidget,
    QTableWidgetItem,
    QVBoxLayout,
    QWidget,
)

from app.models.dto import Product, Role, Session
from app.viewmodels.products_viewmodel import ProductsViewModel

_COLUMNS = ["SKU", "Name", "Category", "Quantity", "Unit"]


def _fill_table(table: QTableWidget, products: list[Product]) -> None:
    table.setRowCount(len(products))
    for row, p in enumerate(products):
        values = [p.sku, p.name, p.category, str(p.quantity), p.unit]
        for col, value in enumerate(values):
            table.setItem(row, col, QTableWidgetItem(value))
    table.resizeColumnsToContents()


def _make_table() -> QTableWidget:
    table = QTableWidget(0, len(_COLUMNS))
    table.setHorizontalHeaderLabels(_COLUMNS)
    table.setEditTriggers(QTableWidget.EditTrigger.NoEditTriggers)
    table.verticalHeader().setVisible(False)
    return table


class MainWindow(QMainWindow):
    """Top-level window; navigation entries depend on the user's role."""

    def __init__(self, session: Session, viewmodel: ProductsViewModel) -> None:
        super().__init__()
        self.setWindowTitle(f"Smart Warehouse — {session.username}")
        self.resize(900, 560)
        self._vm = viewmodel

        self.nav = QListWidget()
        self.nav.setFixedWidth(180)
        self.pages = QStackedWidget()

        self._add_page("Dashboard", self._build_dashboard())
        self._add_page("Products", self._build_products())
        if session.role is Role.MANAGER:
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
        self._vm.errorOccurred.connect(self.statusBar().showMessage)
        self.statusBar().showMessage(
            f"Signed in as {session.username} ({session.role})"
        )
        self._vm.refresh()

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
        layout.addWidget(QLabel("<h2>Products</h2>"))
        self.search_edit = QLineEdit()
        self.search_edit.setPlaceholderText("Search by SKU or name…")
        self.search_edit.textChanged.connect(self._vm.set_query)
        layout.addWidget(self.search_edit)
        self.products_table = _make_table()
        layout.addWidget(self.products_table)
        return page

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
