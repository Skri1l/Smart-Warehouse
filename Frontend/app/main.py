"""Application entry point."""

import argparse
import os
import sys

from PySide6.QtWidgets import QApplication, QDialog

from app.api.client import DEFAULT_BASE_URL, ApiClient, WarehouseApi
from app.api.demo import DemoApiClient
from app.ui.login_dialog import LoginDialog
from app.ui.main_window import MainWindow
from app.viewmodels.login_viewmodel import LoginViewModel
from app.viewmodels.products_viewmodel import ProductsViewModel


def main() -> int:
    parser = argparse.ArgumentParser(description="Smart Warehouse desktop client")
    parser.add_argument("--demo", action="store_true", help="use in-memory demo data")
    parser.add_argument(
        "--api-url", default=os.environ.get("SW_API_URL", DEFAULT_BASE_URL)
    )
    args, qt_args = parser.parse_known_args()

    app = QApplication([sys.argv[0], *qt_args])
    app.setApplicationName("Smart Warehouse")
    api: WarehouseApi = DemoApiClient() if args.demo else ApiClient(args.api_url)

    dialog = LoginDialog(LoginViewModel(api))
    if dialog.exec() != QDialog.DialogCode.Accepted or dialog.session is None:
        return 0

    window = MainWindow(dialog.session, ProductsViewModel(api, dialog.session))
    window.show()
    return app.exec()


if __name__ == "__main__":
    sys.exit(main())
