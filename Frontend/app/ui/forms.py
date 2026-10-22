"""Small modal forms for registering and creating warehouse entities."""

from PySide6.QtWidgets import (
    QComboBox,
    QDialog,
    QDialogButtonBox,
    QFormLayout,
    QLabel,
    QLineEdit,
    QSpinBox,
    QVBoxLayout,
    QWidget,
)

from app.models.dto import Rack, Warehouse
from app.viewmodels.register_viewmodel import RegisterViewModel


class _FormDialog(QDialog):
    """Dialog with a form, an error line and OK/Cancel buttons."""

    def __init__(self, title: str, ok_text: str) -> None:
        super().__init__()
        self.setWindowTitle(title)
        self.setMinimumWidth(340)
        self.form = QFormLayout()
        self.error_label = QLabel()
        self.error_label.setStyleSheet("color: #c62828;")
        self.error_label.setWordWrap(True)
        self.buttons = QDialogButtonBox(
            QDialogButtonBox.StandardButton.Ok | QDialogButtonBox.StandardButton.Cancel
        )
        self.buttons.button(QDialogButtonBox.StandardButton.Ok).setText(ok_text)
        layout = QVBoxLayout(self)
        layout.addLayout(self.form)
        layout.addWidget(self.error_label)
        layout.addWidget(self.buttons)
        self.buttons.accepted.connect(self._submit)
        self.buttons.rejected.connect(self.reject)

    def add_row(self, label: str, widget: QWidget) -> None:
        self.form.addRow(label, widget)

    def _submit(self) -> None:
        error = self.validate()
        if error:
            self.error_label.setText(error)
        else:
            self.accept()

    def validate(self) -> str:
        """Return an error message, or an empty string when the form is valid."""
        return ""


def _required(**fields: str) -> str:
    missing = [name for name, value in fields.items() if not value]
    return f"Fill in: {', '.join(missing)}." if missing else ""


class RegisterDialog(_FormDialog):
    """Creates a new WORKER account via the backend."""

    def __init__(self, viewmodel: RegisterViewModel) -> None:
        super().__init__("Smart Warehouse — Register", "Register")
        self.registered_username: str | None = None
        self._vm = viewmodel
        self.email_edit = QLineEdit()
        self.username_edit = QLineEdit()
        self.password_edit = QLineEdit()
        self.password_edit.setEchoMode(QLineEdit.EchoMode.Password)
        self.repeat_edit = QLineEdit()
        self.repeat_edit.setEchoMode(QLineEdit.EchoMode.Password)
        self.add_row("Email", self.email_edit)
        self.add_row("Username", self.username_edit)
        self.add_row("Password", self.password_edit)
        self.add_row("Repeat password", self.repeat_edit)
        self._vm.busyChanged.connect(lambda busy: self.buttons.setEnabled(not busy))
        self._vm.registered.connect(self._on_registered)
        self._vm.registerFailed.connect(self.error_label.setText)

    def _submit(self) -> None:
        self.error_label.clear()
        self._vm.register(
            self.email_edit.text().strip(),
            self.username_edit.text().strip(),
            self.password_edit.text(),
            self.repeat_edit.text(),
        )

    def _on_registered(self, username: str) -> None:
        self.registered_username = username
        self.accept()


class ProductDialog(_FormDialog):
    def __init__(self) -> None:
        super().__init__("New product", "Create")
        self.sku_edit = QLineEdit()
        self.name_edit = QLineEdit()
        self.description_edit = QLineEdit()
        self.add_row("SKU", self.sku_edit)
        self.add_row("Name", self.name_edit)
        self.add_row("Description", self.description_edit)

    def values(self) -> tuple[str, str, str]:
        return (
            self.sku_edit.text().strip(),
            self.name_edit.text().strip(),
            self.description_edit.text().strip(),
        )

    def validate(self) -> str:
        sku, name, _ = self.values()
        return _required(SKU=sku, Name=name)


class WarehouseDialog(_FormDialog):
    def __init__(self) -> None:
        super().__init__("New warehouse", "Create")
        self.name_edit = QLineEdit()
        self.address_edit = QLineEdit()
        self.add_row("Name", self.name_edit)
        self.add_row("Address", self.address_edit)

    def values(self) -> tuple[str, str]:
        return self.name_edit.text().strip(), self.address_edit.text().strip()

    def validate(self) -> str:
        name, address = self.values()
        return _required(Name=name, Address=address)


class RackDialog(_FormDialog):
    def __init__(self, warehouses: list[Warehouse]) -> None:
        super().__init__("New rack", "Create")
        self.label_edit = QLineEdit()
        self.warehouse_combo = QComboBox()
        for w in warehouses:
            self.warehouse_combo.addItem(f"{w.name} (#{w.id})", w.id)
        self.add_row("Label", self.label_edit)
        self.add_row("Warehouse", self.warehouse_combo)

    def values(self) -> tuple[str, int]:
        return self.label_edit.text().strip(), self.warehouse_combo.currentData()

    def validate(self) -> str:
        if self.warehouse_combo.count() == 0:
            return "Create a warehouse first."
        return _required(Label=self.values()[0])


class ShelfDialog(_FormDialog):
    def __init__(self, racks: list[Rack]) -> None:
        super().__init__("New shelf", "Create")
        self.label_edit = QLineEdit()
        self.capacity_spin = QSpinBox()
        self.capacity_spin.setRange(1, 1_000_000)
        self.capacity_spin.setValue(50)
        self.rack_combo = QComboBox()
        for r in racks:
            self.rack_combo.addItem(f"{r.label} (#{r.id})", r.id)
        self.add_row("Label", self.label_edit)
        self.add_row("Capacity", self.capacity_spin)
        self.add_row("Rack", self.rack_combo)

    def values(self) -> tuple[str, int, int]:
        return (
            self.label_edit.text().strip(),
            self.capacity_spin.value(),
            self.rack_combo.currentData(),
        )

    def validate(self) -> str:
        if self.rack_combo.count() == 0:
            return "Create a rack first."
        return _required(Label=self.values()[0])
