# Frontend Code Style (Python / PySide6 Desktop Client)

The frontend is a cross-platform desktop application (Windows, macOS, Linux) built with **PySide6 (Qt for Python)**, consuming the backend exclusively over its REST API — see [ADR-0006](adr/0006-frontend-pyside6-desktop.md) and [ADR-0003](adr/0003-django-for-frontend.md) (superseded) for why.

Base style: [PEP 8](https://peps.python.org/pep-0008/), mechanically enforced by **Ruff** (lint + import order) and **Black** (formatting), with **mypy** for static typing. Configuration lives in `Frontend/pyproject.toml`.

```bash
cd Frontend
ruff check . --fix     # lint, auto-fix what's safe
black .                 # auto-format
mypy .                  # type-check
pytest                  # run tests (pytest-qt for widget tests)
```

> The project has not been scaffolded yet (see [MILESTONES.md](MILESTONES.md)). The structure below is the target layout.

## Project structure

One importable package (`app/`), organized by responsibility, not by Qt widget type:

```text
Frontend/
├── app/
│   ├── main.py              Entry point: creates QApplication, shows the main window
│   ├── ui/                  Qt Designer .ui sources + hand-written widgets/views
│   │   ├── designer/*.ui    Qt Designer source files (committed)
│   │   ├── main_window.py   Hand-written widget/window classes
│   │   └── ...              *_ui.py (generated from .ui, gitignored — see below)
│   ├── viewmodels/          Mediates between views and api/core — no Qt imports needed beyond Signal/Slot
│   ├── core/                Business logic that isn't Qt- or API-specific (formatting, validation)
│   ├── api/                 Typed REST client for the backend (httpx-based)
│   │   └── client.py
│   ├── models/              Plain data classes (DTOs) mirroring backend API_GUIDELINES.md schemas
│   └── resources/           Icons, .qss stylesheets, resources.qrc
├── tests/
├── main.py or app/__main__.py
├── requirements.txt          Runtime deps (PySide6, httpx, pyqtgraph, ...)
├── requirements-dev.txt      Lint/type-check/test/license-audit tooling
└── pyproject.toml
```

### Generated files are never committed

`.ui` files (Qt Designer) and `.qrc` (resources) are source; the `*_ui.py` and `resources_rc.py` files `pyside6-uic`/`pyside6-rcc` generate from them are **not** committed (see `.gitignore`) — regenerate them as a build step:

```bash
pyside6-uic app/ui/designer/main_window.ui -o app/ui/main_window_ui.py
pyside6-rcc app/resources/resources.qrc -o app/resources/resources_rc.py
```

Committing generated UI code invites drift: someone edits the `.ui` file, forgets to regenerate, and the reviewer can't tell from the diff. A `scripts/build_ui.py` (or Makefile target) regenerating all of them should exist before Milestone 3 and run in CI before tests.

## Naming

Qt's own API uses `camelCase` (`mousePressEvent`, `clicked.connect`) because it's a C++ library — don't fight that when overriding Qt methods or calling Qt APIs. Everything **we** write follows PEP 8:

| Element | Convention | Example |
|---|---|---|
| Module / package | `lower_snake_case` | `stock_view.py` |
| Our classes | `UpperCamelCase` | `StockTableModel` |
| Our functions / variables / methods | `lower_snake_case` | `load_current_stock` |
| Qt method overrides | `camelCase` (Qt's convention, not ours) | `def closeEvent(self, event): ...` |
| Constant | `UPPER_SNAKE_CASE` | `DEFAULT_PAGE_SIZE` |
| Qt signal | `camelCase` + past tense/noun, matches Qt convention | `stockChanged = Signal(int)` |
| Test file | `test_<module>.py` | `test_api_client.py` |
| Test function | `test_<behavior>_when_<condition>` | `test_shows_error_when_login_fails` |

## Typing & docstrings

- All new functions/methods have type hints. `mypy` runs in CI; untyped `def` in new code fails review. PySide6 ships its own type stubs, so Qt classes are typed out of the box.
- Docstrings use **Google style**, required on modules, viewmodels and any function whose behavior isn't obvious from its name/signature:

```python
def project_stock(product_id: int, target_date: date) -> int:
    """Estimate stock remaining for a product on a given date.

    Args:
        product_id: Identifier of the product.
        target_date: Date to project stock for.

    Returns:
        Projected quantity, which may be negative if a shortage is expected.
    """
```

## Layering rules

- **Views** (`ui/`) contain only widget setup, layout and signal/slot wiring. No HTTP calls, no business logic in a view class.
- **ViewModels** (`viewmodels/`) hold UI state and expose Qt `Signal`s the view connects to; they call `api/` and `core/`, never touch widgets directly. This keeps views testable-by-inspection and viewmodels testable without a running `QApplication` event loop where possible.
- **`api/`** is the only place that imports `httpx` / knows REST endpoint paths — written against the backend's OpenAPI spec (see [API_GUIDELINES.md](API_GUIDELINES.md)), not hand-guessed shapes.
- **`core/`** has zero Qt and zero HTTP imports — pure functions/classes, the cheapest things in the codebase to unit test.

## Threading

Qt's UI must run on the main thread; a slow network call on that thread freezes the window. Per [ADR-0006](adr/0006-frontend-pyside6-desktop.md):

- Network calls go through `QThreadPool`/`QRunnable` (or `qasync` if async/await is preferred) — never call `api/` methods directly from a slot connected to a UI signal without offloading.
- A `QRunnable`/worker reports back to the main thread via `Signal`, never by mutating a widget from a worker thread directly.

## Authentication & authorization

The client stores the session token in memory only (not on disk) and shows/hides navigation by role for UX purposes. The backend is the only authority on permissions — the client hiding a button is not a security control, see [API_GUIDELINES.md](API_GUIDELINES.md).

## Dependency licensing

Every new dependency must be usable in closed-source software (project is proprietary, see [LICENSE](../LICENSE)):

- Allowed: MIT, BSD, Apache-2.0, LGPL (dynamically linked — see ADR-0006 for what that requires in practice).
- Rejected outright: GPL-only or AGPL packages, and any GPL-only Qt module (Qt Charts, Qt Data Visualization, PyQt5/PyQt6).
- Before adding a dependency, check its license and add it to [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md). CI runs `pip-licenses` to catch violations (see `.github/workflows/frontend-ci.yml`).

## Testing

- `pytest` + `pytest-qt` (`qtbot` fixture) for widget-level tests; run headless in CI via `QT_QPA_PLATFORM=offscreen` (no real display needed).
- Pure `core/` logic is tested without `qtbot` at all — it has no Qt dependency.
- `api/` is tested against a mocked HTTP layer (`httpx` supports a `MockTransport`), not a live backend, for unit tests; a small number of end-to-end tests against a real running backend belong in integration testing, not this suite.

## Packaging

PyInstaller, `--onedir` mode (never `--onefile` — see [ADR-0006](adr/0006-frontend-pyside6-desktop.md) for the LGPL dynamic-linking reason), built per OS. Packaging scripts/CI matrix land with Milestone 8 cloud/deployment work; not needed before there's an application to package.

## Formatting reference

Ruff and Black handle line length (88), quote style, import sorting and whitespace automatically; both exclude generated `*_ui.py`/`resources_rc.py` files. If `ruff check .` and `black --check .` pass, the formatting is correct — don't hand-tune it further.
