# Smart Warehouse

Smart Warehouse is an intelligent warehouse management system that combines shelf cameras, computer vision and demand forecasting to keep stock levels accurate in near real time. It is being developed as a diploma project and is structured as a production-style monorepo.

For the full product specification (goals, scope, domain model, business flows) see [Documentation/PROJECT_DOCUMENTATION.md](Documentation/PROJECT_DOCUMENTATION.md) and the delivery plan in [Documentation/MILESTONES.md](Documentation/MILESTONES.md). [Documentation/README.md](Documentation/README.md) is the full documentation index.

## Repository structure

This is a monorepo. Each top-level directory is an independently buildable component with its own dependency manifest and its own conventions document.

```text
Smart-Warehouse/
├── Backend/          Java / Spring Boot REST API, business logic, persistence
├── Frontend/         Python / PySide6 desktop client
├── Documentation/     Product, process and engineering documentation
├── Website/          Static GitHub Pages site: installer download and how-to guide (ADR-0007)
└── .github/          CI workflows, issue/PR templates, code ownership
```

| Component | Language | Framework | Docs |
|---|---|---|---|
| `Backend/` | Java 21 | Spring Boot, Spring Data JPA, PostgreSQL | [CODE_STYLE_BACKEND.md](Documentation/CODE_STYLE_BACKEND.md) |
| `Frontend/` | Python 3.12+ | PySide6 (Qt for Python), desktop | [CODE_STYLE_FRONTEND.md](Documentation/CODE_STYLE_FRONTEND.md) |

## Engineering conventions

| Topic | Document |
|---|---|
| Branching model, branch naming, merge strategy | [Documentation/BRANCHING.md](Documentation/BRANCHING.md) |
| Commit message format | [Documentation/COMMIT_CONVENTION.md](Documentation/COMMIT_CONVENTION.md) |
| Backend (Java) code style | [Documentation/CODE_STYLE_BACKEND.md](Documentation/CODE_STYLE_BACKEND.md) |
| Frontend (Python/PySide6) code style | [Documentation/CODE_STYLE_FRONTEND.md](Documentation/CODE_STYLE_FRONTEND.md) |
| REST API design & OpenAPI documentation | [Documentation/API_GUIDELINES.md](Documentation/API_GUIDELINES.md) |
| How to contribute (PR process, review, DoD) | [CONTRIBUTING.md](CONTRIBUTING.md) |
| Architecture decisions (ADRs) | [Documentation/adr/](Documentation/adr/README.md) |
| Design docs for large changes (RFCs) | [Documentation/rfcs/](Documentation/rfcs/README.md) |
| Operational runbooks | [Backend/runbooks/](Backend/runbooks/README.md) |

## Getting started

### Prerequisites

- Java 21 (JDK)
- Maven (or use the bundled `./Backend/mvnw` wrapper)
- PostgreSQL 15+ running locally, or via Docker
- Python 3.12+ (for the frontend, once scaffolded — see [Frontend/README.md](Frontend/README.md))

### Backend

```bash
cd Backend
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080` by default. Database connection settings are read from `Backend/src/main/resources/application.properties` (do not commit real credentials — use environment variables or an untracked `application-local.properties` profile).

Alternatively, run the backend together with PostgreSQL in Docker:

```bash
cd Backend
cp .env.example .env   # adjust ports/password if 8080 or 5432 are already taken
docker compose up -d --build
```

### API documentation (Swagger)

Once the backend is running, the interactive API docs are available at:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI spec (JSON): `http://localhost:8080/v3/api-docs`

(Replace `8080` with `BACKEND_PORT` from `Backend/.env` if you changed it.)

For now all endpoints are open (no authentication) to simplify development, so requests can be sent straight from Swagger UI via **Try it out**. Access control will be added together with real authentication — do not deploy this configuration publicly.

If you get **HTTP 403** or an unexpected page at that address, another application is most likely occupying the port — check with `docker ps` / `netstat -ano | findstr :8080`.

### Frontend

A minimal PySide6 desktop client is scaffolded in `Frontend/`; run `python -m app --demo` from that directory. See [Frontend/README.md](Frontend/README.md).

## Quality gates

- **Backend:** `./mvnw verify` runs unit/integration tests and checks code formatting (Spotless). Run `./mvnw spotless:apply` to auto-format before committing.
- **Frontend:** `ruff check .`, `black --check .`, `mypy .` and a `pip-licenses` dependency audit (see [ADR-0006](Documentation/adr/0006-frontend-pyside6-desktop.md)) run from `Frontend/`. See [Frontend/pyproject.toml](Frontend/pyproject.toml).
- Both are enforced locally via [pre-commit](.pre-commit-config.yaml) hooks and in CI on every pull request (`.github/workflows/`).

## License

This repository is **proprietary and confidential**. See [LICENSE](LICENSE). No part of this codebase may be copied, used or distributed without prior written permission from the copyright holders. Third-party components (e.g. the LGPL-licensed Qt/PySide6 used by the frontend) keep their own licenses — see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

## Team

| Name | Primary responsibility |
|---|---|
| Ivan Samal | Backend, database, ML/CV integration, forecasting logic |
| Uladzislau Mikhayevich | Frontend, cross-platform desktop UI, cloud deployment, API documentation |
