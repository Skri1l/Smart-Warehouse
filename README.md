# Smart Warehouse

Smart Warehouse is an intelligent warehouse management system that combines shelf cameras, computer vision and demand forecasting to keep stock levels accurate in near real time. It is being developed as a diploma project and is structured as a production-style monorepo.

For the full product specification (goals, scope, domain model, business flows) see [Documentation/PROJECT_DOCUMENTATION.md](Documentation/PROJECT_DOCUMENTATION.md) and the delivery plan in [Documentation/MILESTONES.md](Documentation/MILESTONES.md). [Documentation/README.md](Documentation/README.md) is the full documentation index.

## Repository structure

This is a monorepo. Each top-level directory is an independently buildable component with its own dependency manifest and its own conventions document.

```text
Smart-Warehouse/
├── Backend/          Java / Spring Boot REST API, business logic, persistence
├── Frontend/         JavaScript / React responsive browser UI (ADR-0012)
├── Device/           Python shelf agent (Raspberry Pi 5) and its multi-shelf PC emulator
├── Documentation/     Product, process and engineering documentation
├── Website/          Static GitHub Pages site: landing page and how-to guide
└── .github/          CI workflows, issue/PR templates, code ownership
```

| Component | Language | Framework | Docs |
|---|---|---|---|
| `Backend/` | Java 21 | Spring Boot, Spring Data JPA, PostgreSQL | [CODE_STYLE_BACKEND.md](Documentation/CODE_STYLE_BACKEND.md) |
| `Frontend/` | JavaScript (Node 20+ for tooling) | React, Vite, browser | [CODE_STYLE_FRONTEND.md](Documentation/CODE_STYLE_FRONTEND.md) |
| `Device/` | Python 3.11+ | httpx; picamera2/gpiozero on the Pi | [CODE_STYLE_DEVICE.md](Documentation/CODE_STYLE_DEVICE.md) |

## Engineering conventions

| Topic | Document |
|---|---|
| Branching model, branch naming, merge strategy | [Documentation/BRANCHING.md](Documentation/BRANCHING.md) |
| Commit message format | [Documentation/COMMIT_CONVENTION.md](Documentation/COMMIT_CONVENTION.md) |
| Backend (Java) code style | [Documentation/CODE_STYLE_BACKEND.md](Documentation/CODE_STYLE_BACKEND.md) |
| Frontend (JavaScript/React) code style | [Documentation/CODE_STYLE_FRONTEND.md](Documentation/CODE_STYLE_FRONTEND.md) |
| Device (shelf agent / emulator) code style | [Documentation/CODE_STYLE_DEVICE.md](Documentation/CODE_STYLE_DEVICE.md) |
| REST API design & OpenAPI documentation | [Documentation/API_GUIDELINES.md](Documentation/API_GUIDELINES.md) |
| How to contribute (PR process, review, DoD) | [CONTRIBUTING.md](CONTRIBUTING.md) |
| Architecture decisions (ADRs) | [Documentation/adr/](Documentation/adr/README.md) |
| Design docs for large changes (RFCs) | [Documentation/rfcs/](Documentation/rfcs/README.md) |
| Operational runbooks | [Backend/runbooks/](Backend/runbooks/README.md) |
| Shelf device hardware (demo shelf, camera, wiring) | [Documentation/hardware/SHELF_DEVICE.md](Documentation/hardware/SHELF_DEVICE.md) |

## Database

Smart Warehouse uses PostgreSQL 17 to store warehouse inventory, stock movements,
recognition results, and procurement data. Liquibase applies database migrations
when the backend starts.

- [Database schema and ER diagrams](Documentation/database/Database-schema.md)
- [Liquibase changelog](Backend/src/main/resources/db.changelog-master.yaml)
- [Local setup instructions](#getting-started)

## Getting started

### Prerequisites

- Java 21 (JDK)
- Maven (or use the bundled `./Backend/mvnw` wrapper)
- PostgreSQL 15+ running locally, or via Docker
- Node.js 20+ (for the frontend — see [Frontend/README.md](Frontend/README.md))
- Python 3.11+ (for the shelf agent / emulator — see [Device/README.md](Device/README.md))

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

The frontend is a responsive browser UI in React ([ADR-0012](Documentation/adr/0012-browser-ui-javascript.md)); in production it is served by our hosted server on the same address as the API ([ADR-0011](Documentation/adr/0011-hosted-server-deployment.md)). Until the backend has `/api/v1` and authentication, the dev server mocks the whole REST protocol in the browser. From `Frontend/`:

```bash
npm ci
npm run dev        # http://localhost:5173, mocked backend; sign in as worker / manager / admin
npm run dev:api    # real backend on http://localhost:8080
```

See [Frontend/README.md](Frontend/README.md).

### Shelf device / emulator

The shelf agent in `Device/` runs on the Raspberry Pi 5 and, with the same code, as an emulator that sends labelled test photos from several virtual shelves. From `Device/`:

```bash
python -m shelf_agent mock-server                                     # fake backend device endpoints
python -m shelf_agent emulate --scenario scenarios/smoke/scenario.toml
```

Step-by-step usage (real backend, diploma demo, troubleshooting): [Device/USER_GUIDE.md](Device/USER_GUIDE.md). Technical reference: [Device/README.md](Device/README.md).

### Whole pipeline in one command

On macOS or Linux, `scripts/pipeline.sh start` starts Vision, PostgreSQL and the backend and seeds the shelves. It then prints the Swagger links and the shelf the photos go to, and starts the webcam emulator (`start demo` uses the labelled photos instead). `scripts/pipeline.sh status` shows what the backend stored. Guide: [Documentation/PIPELINE_RUN.md](Documentation/PIPELINE_RUN.md).

## Quality gates

- **Backend:** `./mvnw verify` runs unit/integration tests and checks code formatting (Spotless). Run `./mvnw spotless:apply` to auto-format before committing.
- **Frontend:** `npm run lint` (ESLint), `npm run format:check` (Prettier), `npm test` (Vitest), `npm run build` and a `license-checker` audit of runtime dependencies, run from `Frontend/`. See [Frontend/package.json](Frontend/package.json).
- **Device:** `ruff check .`, `black --check .`, `mypy .`, a `pip-licenses` audit and `pytest`, run from `Device/`. See [Device/pyproject.toml](Device/pyproject.toml).
- Java and Python formatting is also enforced locally via [pre-commit](.pre-commit-config.yaml) hooks; every check runs in CI on each pull request (`.github/workflows/`).

## License

This repository is **proprietary and confidential**. See [LICENSE](LICENSE). No part of this codebase may be copied, used or distributed without prior written permission from the copyright holders. Third-party components keep their own licenses — see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

## Team

| Name | Primary responsibility |
|---|---|
| Ivan Samal | Backend, database, ML/CV integration, forecasting logic |
| Uladzislau Mikhayevich | Frontend, responsive web UI, server deployment, API documentation |
