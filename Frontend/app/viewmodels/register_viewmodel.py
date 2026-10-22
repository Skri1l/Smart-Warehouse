"""State and actions for the registration dialog."""

from typing import Any

from PySide6.QtCore import QObject, Signal

from app.api.client import WarehouseApi
from app.viewmodels.worker import run_in_background


class RegisterViewModel(QObject):
    """Creates a new account on the backend."""

    busyChanged = Signal(bool)
    registered = Signal(str)
    registerFailed = Signal(str)

    def __init__(self, api: WarehouseApi) -> None:
        super().__init__()
        self._api = api
        self._pending: Any = None

    def register(self, email: str, username: str, password: str, repeat: str) -> None:
        if not email or not username or not password:
            self.registerFailed.emit("Fill in all fields.")
            return
        if "@" not in email:
            self.registerFailed.emit("Enter a valid email.")
            return
        if password != repeat:
            self.registerFailed.emit("Passwords do not match.")
            return
        self.busyChanged.emit(True)
        self._pending = run_in_background(
            lambda: self._api.register(email, username, password),
            lambda _: self._on_success(username),
            self._on_error,
        )

    def _on_success(self, username: str) -> None:
        self.busyChanged.emit(False)
        self.registered.emit(username)

    def _on_error(self, message: str) -> None:
        self.busyChanged.emit(False)
        self.registerFailed.emit(message)
