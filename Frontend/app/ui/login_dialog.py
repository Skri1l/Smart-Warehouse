"""Login dialog shown at application start."""

from PySide6.QtWidgets import (
    QDialog,
    QDialogButtonBox,
    QFormLayout,
    QLabel,
    QLineEdit,
    QVBoxLayout,
)

from app.models.dto import Session
from app.viewmodels.login_viewmodel import LoginViewModel


class LoginDialog(QDialog):
    """Collects credentials and accepts once the backend returns a session."""

    def __init__(self, viewmodel: LoginViewModel) -> None:
        super().__init__()
        self.setWindowTitle("Smart Warehouse — Sign in")
        self.setMinimumWidth(320)
        self.session: Session | None = None
        self._vm = viewmodel

        self.username_edit = QLineEdit()
        self.password_edit = QLineEdit()
        self.password_edit.setEchoMode(QLineEdit.EchoMode.Password)
        self.error_label = QLabel()
        self.error_label.setStyleSheet("color: #c62828;")
        self.error_label.setWordWrap(True)

        self.buttons = QDialogButtonBox(
            QDialogButtonBox.StandardButton.Ok | QDialogButtonBox.StandardButton.Cancel
        )
        self.buttons.button(QDialogButtonBox.StandardButton.Ok).setText("Sign in")

        form = QFormLayout()
        form.addRow("Username", self.username_edit)
        form.addRow("Password", self.password_edit)
        layout = QVBoxLayout(self)
        layout.addLayout(form)
        layout.addWidget(self.error_label)
        layout.addWidget(self.buttons)

        self.buttons.accepted.connect(self._submit)
        self.buttons.rejected.connect(self.reject)
        self._vm.busyChanged.connect(self._set_busy)
        self._vm.loggedIn.connect(self._on_logged_in)
        self._vm.loginFailed.connect(self.error_label.setText)

    def _submit(self) -> None:
        self.error_label.clear()
        self._vm.login(self.username_edit.text().strip(), self.password_edit.text())

    def _set_busy(self, busy: bool) -> None:
        self.buttons.setEnabled(not busy)

    def _on_logged_in(self, session: Session) -> None:
        self.session = session
        self.accept()
