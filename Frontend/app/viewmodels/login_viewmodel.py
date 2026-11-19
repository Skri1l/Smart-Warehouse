"""State and actions for the login dialog."""

from typing import Any

from PySide6.QtCore import QObject, Signal

from app.api.client import WarehouseApi
from app.models.dto import Session
from app.viewmodels.worker import run_in_background


class LoginViewModel(QObject):
    """Authenticates the user against the backend."""

    busyChanged = Signal(bool)
    loggedIn = Signal(object)
    loginFailed = Signal(str)

    def __init__(self, api: WarehouseApi) -> None:
        super().__init__()
        self._api = api
        self._pending: Any = None

    def login(self, username: str, password: str) -> None:
        if not username or not password:
            self.loginFailed.emit("Enter username and password.")
            return
        self.busyChanged.emit(True)
        self._pending = run_in_background(
            lambda: self._api.login(username, password),
            self._on_success,
            self._on_error,
        )

    def _on_success(self, session: Session) -> None:
        self.busyChanged.emit(False)
        self.loggedIn.emit(session)

    def _on_error(self, message: str) -> None:
        self.busyChanged.emit(False)
        self.loginFailed.emit(message)
