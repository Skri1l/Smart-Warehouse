# Smart Warehouse

Smart Warehouse is an intelligent warehouse management system that combines shelf cameras, computer vision and demand forecasting to keep stock levels accurate in near real time. It is being developed as a diploma project and is structured as a production-style monorepo.

For the full product specification (goals, scope, domain model, business flows) see [Documentation/PROJECT_DOCUMENTATION.md](Documentation/PROJECT_DOCUMENTATION.md) and the delivery plan in [Documentation/MILESTONES.md](Documentation/MILESTONES.md). [Documentation/README.md](Documentation/README.md) is the full documentation index.

## Repository structure

This is a monorepo. Each top-level directory is an independently buildable component with its own dependency manifest and its own conventions document.

```text
Smart-Warehouse/
├── Backend/          Java / Spring Boot REST API, business logic, persistence
├── Frontend/         Python / Django web application
├── Documentation/     Product, process and engineering documentation
└── .github/          CI workflows, issue/PR templates, code ownership
```

| Component | Language | Framework | Docs |
|---|---|---|---|
| `Backend/` | Java 21 | Spring Boot, Spring Data JPA, PostgreSQL | [CODE_STYLE_BACKEND.md](Documentation/CODE_STYLE_BACKEND.md) |
| `Frontend/` | Python 3.12+ | Django | [CODE_STYLE_FRONTEND.md](Documentation/CODE_STYLE_FRONTEND.md) |

## Engineering conventions

| Topic | Document |
|---|---|
| Branching model, branch naming, merge strategy | [Documentation/BRANCHING.md](Documentation/BRANCHING.md) |
| Commit message format | [Documentation/COMMIT_CONVENTION.md](Documentation/COMMIT_CONVENTION.md) |
| Backend (Java) code style | [Documentation/CODE_STYLE_BACKEND.md](Documentation/CODE_STYLE_BACKEND.md) |
| Frontend (Python/Django) code style | [Documentation/CODE_STYLE_FRONTEND.md](Documentation/CODE_STYLE_FRONTEND.md) |
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

### Frontend

The Django application has not been scaffolded yet (tracked under Milestone 1/3 — see [MILESTONES.md](Documentation/MILESTONES.md)). Once created, run instructions will live in [Frontend/README.md](Frontend/README.md).

## Quality gates

- **Backend:** `./mvnw verify` runs unit/integration tests and checks code formatting (Spotless). Run `./mvnw spotless:apply` to auto-format before committing.
- **Frontend:** `ruff check .`, `black --check .` and `mypy .` run from `Frontend/`. See [Frontend/pyproject.toml](Frontend/pyproject.toml).
- Both are enforced locally via [pre-commit](.pre-commit-config.yaml) hooks and in CI on every pull request (`.github/workflows/`).

## License

This repository is **proprietary and confidential**. See [LICENSE](LICENSE). No part of this codebase may be copied, used or distributed without prior written permission from the copyright holders.

## Team

| Name | Primary responsibility |
|---|---|
| Ivan Samal | Backend, database, ML/CV integration, forecasting logic |
| Uladzislau Mikhayevich | Frontend, responsive UI, cloud deployment, API documentation |
