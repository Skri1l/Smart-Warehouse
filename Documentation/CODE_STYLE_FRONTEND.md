# Frontend Code Style (Python / Django)

Base style: [PEP 8](https://peps.python.org/pep-0008/), mechanically enforced by **Ruff** (lint + import order) and **Black** (formatting), with **mypy** (+ `django-stubs`) for static typing. Configuration lives in `Frontend/pyproject.toml`.

```bash
cd Frontend
ruff check . --fix     # lint, auto-fix what's safe
black .                 # auto-format
mypy .                  # type-check
pytest                  # run tests
```

> The Django project has not been scaffolded yet (see [MILESTONES.md](MILESTONES.md), Milestone 1/3). The structure below is the target layout to create with `django-admin startproject` / `startapp`.

## Project structure

One Django project, one app per bounded domain area, mirroring the backend's modules where it makes sense (e.g. `inventory`, `products`, `accounts`):

```text
Frontend/
├── config/                 Django project package (settings, root urls, wsgi/asgi)
│   └── settings/
│       ├── base.py         Shared settings
│       ├── dev.py          Local development overrides
│       └── prod.py         Production overrides (reads secrets from env vars)
├── apps/
│   └── <app_name>/
│       ├── models.py
│       ├── views.py
│       ├── urls.py
│       ├── forms.py
│       ├── services.py     Business logic that doesn't belong in a view
│       ├── api_client.py   Typed client for the backend REST API
│       ├── templates/<app_name>/
│       ├── static/<app_name>/
│       └── tests/
├── manage.py
└── pyproject.toml
```

- `DJANGO_SETTINGS_MODULE` points at `config.settings.dev` locally and `config.settings.prod` in deployment; never hardcode secrets in `base.py`.
- The frontend talks to the Java backend over its REST API; it does not access the backend's database directly.

## Naming

| Element | Convention | Example |
|---|---|---|
| Module / package | `lower_snake_case` | `stock_movements.py` |
| Class | `UpperCamelCase` | `ShelfStockView` |
| Function / variable | `lower_snake_case` | `get_current_stock` |
| Constant | `UPPER_SNAKE_CASE` | `DEFAULT_PAGE_SIZE` |
| Django app | `lower_snake_case`, plural for collections | `products`, `inventory` |
| Template | `lower_snake_case.html` | `stock_detail.html` |
| Test file | `test_<module>.py` | `test_services.py` |
| Test function | `test_<behavior>_when_<condition>` | `test_returns_empty_list_when_no_stock` |

## Typing & docstrings

- All new functions and methods have type hints on parameters and return values. `mypy` runs in CI; untyped `def` in new code fails review.
- Docstrings use the **Google style** and are required on modules, views and any function whose behavior isn't obvious from its name/signature:

```python
def calculate_projected_stock(product_id: int, target_date: date) -> int:
    """Estimate stock remaining for a product on a given date.

    Args:
        product_id: Identifier of the product.
        target_date: Date to project stock for.

    Returns:
        Projected quantity, which may be negative if a shortage is expected.
    """
```

- Trivial getters/one-liners don't need a docstring — don't restate the signature in prose.

## Layering rules

- **Views** stay thin: parse request, call a service/API client function, render a response. No business logic or raw HTTP calls to the backend in views.
- Backend API access goes through a typed client module (`api_client.py`), not `requests.get(...)` scattered across views.
- Put non-trivial logic (data shaping, validation beyond form fields) in `services.py`, unit-testable without spinning up the Django test client.

## Templates & static assets

- Use Django's template inheritance (`base.html` + `{% block %}`) — no duplicated page chrome across templates.
- Keep templates free of business logic; use template tags/filters or view context, not inline Python expressions.

## Settings & secrets

- No secrets (API keys, DB passwords, `SECRET_KEY`) committed to the repository, including in `dev.py`. Read them from environment variables (`os.environ` / `django-environ`) with an untracked `.env` for local development.
- `DEBUG = False` by default in `prod.py`; only `dev.py` enables it.

## Testing

- `pytest` + `pytest-django`. One test module per production module under `tests/`.
- Cover: view status codes and context for the happy path, form/validation errors, and any service-layer business logic in isolation (no Django test client needed for pure functions).

## Formatting reference

Ruff and Black handle line length (88, Black default), quote style, import sorting and whitespace automatically. If `ruff check .` and `black --check .` pass, the formatting is correct — don't hand-tune it further.
