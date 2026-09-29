# Frontend

Python / Django web application for Smart Warehouse. **Not scaffolded yet** — tracked under Milestone 1/3 in [../Documentation/MILESTONES.md](../Documentation/MILESTONES.md).

Conventions (project layout, naming, typing, testing) are defined in [../Documentation/CODE_STYLE_FRONTEND.md](../Documentation/CODE_STYLE_FRONTEND.md) — read it before running `django-admin startproject`, since the expected `config/` + `apps/` layout differs from the Django default.

## Tooling already configured

- `pyproject.toml` — Ruff, Black and mypy (+ django-stubs) configuration.
- `requirements-dev.txt` — lint/type-check/test dependencies. Runtime dependencies (`django`, ...) go in a `requirements.txt` created alongside the scaffolded project.

```bash
pip install -r requirements-dev.txt
ruff check .
black --check .
mypy .
pytest
```
