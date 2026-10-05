# How to run the desktop client

Step-by-step guide to running the Smart Warehouse PySide6 client locally, with or without the backend. For architecture and conventions see [ADR-0006](../Documentation/adr/0006-frontend-pyside6-desktop.md) and [CODE_STYLE_FRONTEND.md](../Documentation/CODE_STYLE_FRONTEND.md).

## 1. Prerequisites

- Python **3.12+** (`python --version`)
- Git
- Optional: the backend running locally (see the root [README](../README.md#backend)). Not needed in mock mode.

## 2. Set up the environment

All commands run from the `Frontend/` directory.

```bash
cd Frontend
python -m venv .venv
```

Activate the virtual environment:

| OS | Command |
|---|---|
| Windows (PowerShell) | `.venv\Scripts\Activate.ps1` |
| Windows (Git Bash) | `source .venv/Scripts/activate` |
| macOS / Linux | `source .venv/bin/activate` |

Install dependencies:

```bash
pip install -r requirements.txt -r requirements-dev.txt
```

`requirements-dev.txt` is only needed for linting and tests.

## 3. Run the application

```bash
python -m app
```

### Choosing the API: backend or mock

The client talks to the API through a selection layer ([app/api/factory.py](app/api/factory.py)) that picks the implementation at startup:

| Mode | What it does | When to use |
|---|---|---|
| `auto` (default) | Uses the backend if it responds at the API URL, otherwise falls back to mock data | Everyday development |
| `http` | Always uses the real backend; errors are shown if it is down | Testing against the backend |
| `mock` | Always uses in-memory mock data, no network | UI work, demos, no backend available |

Set the mode with a flag or an environment variable (the flag wins):

```bash
python -m app --api-mode mock
python -m app --api-mode http --api-url http://localhost:8080
```

| Setting | Flag | Environment variable | Default |
|---|---|---|---|
| API mode | `--api-mode` | `SW_API_MODE` | `auto` |
| Backend URL | `--api-url` | `SW_API_URL` | `http://localhost:8080` |

When mock data is in use, the login window title ends with **[mock data]**.

### Signing in

- **Mock mode:** username `worker` (warehouse worker) or `manager` (manager / administrator), any non-empty password. Managers see the extra *Purchase recommendations* section.
- **Backend mode:** the backend uses HTTP Basic auth. Until real users are implemented, sign in with `APP_USER` / `APP_PASSWORD` from `Backend/.env` (see `Backend/.env.example`; the backend reads the BCrypt hash in `APP_PASSWORD_BCRYPT`). There is no login endpoint, so the client checks the credentials with `GET /users` and takes the role from the matching user (accounts not in the list, like `user`, get `WORKER`). Products come from `GET /products`; quantities are the sum of `ACTIVE` entries from `GET /shelf-stock`. The backend has no per-product minimum yet, so the dashboard's low-stock list shows only products with zero stock. API reference: `http://localhost:8080/swagger-ui.html`.

## 4. Lint, type-check and test

The same checks run in CI ([frontend-ci.yml](../.github/workflows/frontend-ci.yml)):

```bash
ruff check .
black --check .
mypy .
pytest
```

Widget tests need a display. To run them headless (as CI does), set `QT_QPA_PLATFORM=offscreen`:

| Shell | Command |
|---|---|
| macOS / Linux / Git Bash | `QT_QPA_PLATFORM=offscreen pytest` |
| PowerShell | `$env:QT_QPA_PLATFORM="offscreen"; pytest` |

## 5. Troubleshooting

| Problem | Fix |
|---|---|
| `ModuleNotFoundError: No module named 'app'` | Run from `Frontend/`, not from the repository root or `Frontend/app/`. |
| `ModuleNotFoundError: No module named 'PySide6'` | The virtual environment is not active, or dependencies are not installed. Repeat step 2. |
| PowerShell refuses to run `Activate.ps1` | Run `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned` once, or use Git Bash. |
| "Cannot reach server" on login | The backend is not running at the API URL. Start it, fix `--api-url`, or use `--api-mode mock`. |
| App silently shows mock data although the backend should be used | In `auto` mode the backend did not respond at startup. Use `--api-mode http` to see the actual error. |
| Linux: `qt.qpa.plugin: Could not load the Qt platform plugin "xcb"` | Install the system Qt dependencies, e.g. `sudo apt install libxcb-cursor0 libxkbcommon-x11-0`. |
