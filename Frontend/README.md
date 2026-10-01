# Frontend

Python / PySide6 (Qt for Python) cross-platform desktop client for Smart Warehouse — see [ADR-0006](../Documentation/adr/0006-frontend-pyside6-desktop.md) for why it's a desktop app rather than a web app. **Not scaffolded yet** — tracked under Milestone 1/3 in [../Documentation/MILESTONES.md](../Documentation/MILESTONES.md).

Conventions (project layout, naming, typing, testing, threading, packaging) are defined in [../Documentation/CODE_STYLE_FRONTEND.md](../Documentation/CODE_STYLE_FRONTEND.md) — read it before writing the first widget.

## Tooling already configured

- `pyproject.toml` — Ruff, Black, mypy and pytest-qt configuration.
- `requirements.txt` — runtime dependencies (PySide6, httpx, pyqtgraph).
- `requirements-dev.txt` — lint/type-check/test/license-audit dependencies.

```bash
pip install -r requirements.txt -r requirements-dev.txt
ruff check .
black --check .
mypy .
QT_QPA_PLATFORM=offscreen pytest
```

## Licensing

PySide6/Qt is LGPL-3.0; this project is proprietary. See [ADR-0006](../Documentation/adr/0006-frontend-pyside6-desktop.md) for the compliance rules (dynamic linking, `--onedir` packaging, no GPL-only Qt modules) and [../THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md) for the current third-party component list. Run `pip-licenses` (installed via `requirements-dev.txt`) before adding any new dependency.
