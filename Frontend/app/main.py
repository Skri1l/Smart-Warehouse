"""Application entry point."""

import argparse
import os
import sys

from PySide6.QtWidgets import QApplication, QDialog

from app.api.client import DEFAULT_BASE_URL
from app.api.factory import ApiMode, create_api
from app.api.mock import MockApiClient
from app.ui.login_dialog import LoginDialog
from app.ui.main_window import MainWindow
from app.viewmodels.login_viewmodel import LoginViewModel
from app.viewmodels.products_viewmodel import ProductsViewModel
from app.viewmodels.register_viewmodel import RegisterViewModel
from app.viewmodels.storage_viewmodel import StorageViewModel


def main() -> int:
    parser = argparse.ArgumentParser(description="Smart Warehouse desktop client")
    parser.add_argument(
        "--api-mode",
        type=ApiMode,
        choices=list(ApiMode),
        default=ApiMode(os.environ.get("SW_API_MODE", ApiMode.AUTO)),
        help="http: real backend, mock: in-memory data, auto: backend or mock",
    )
    parser.add_argument(
        "--api-url", default=os.environ.get("SW_API_URL", DEFAULT_BASE_URL)
    )
    args, qt_args = parser.parse_known_args()

    app = QApplication([sys.argv[0], *qt_args])
    app.setApplicationName("Smart Warehouse")
    api = create_api(args.api_mode, args.api_url)

    dialog = LoginDialog(LoginViewModel(api), RegisterViewModel(api))
    if isinstance(api, MockApiClient):
        dialog.setWindowTitle(dialog.windowTitle() + " [mock data]")
    if dialog.exec() != QDialog.DialogCode.Accepted or dialog.session is None:
        return 0

    session = dialog.session
    window = MainWindow(
        session, ProductsViewModel(api, session), StorageViewModel(api, session)
    )
    window.show()
    return app.exec()


if __name__ == "__main__":
    sys.exit(main())
