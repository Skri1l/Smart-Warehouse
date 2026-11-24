# Smart Warehouse

Smart Warehouse is an intelligent warehouse management system that combines shelf cameras, computer vision and demand forecasting to keep stock levels accurate in near real time. It is being developed as a diploma project and is structured as a production-style monorepo.

For the full product specification (goals, scope, domain model, business flows) see [Documentation/PROJECT_DOCUMENTATION.md](Documentation/PROJECT_DOCUMENTATION.md) and the delivery plan in [Documentation/MILESTONES.md](Documentation/MILESTONES.md). [Documentation/README.md](Documentation/README.md) is the full documentation index.

## For reviewers: diploma stage 3

**Live:** [demo app](https://skri1l.github.io/Smart-Warehouse/app/) (backend mocked in the browser; sign in as `worker`, `manager` or `admin`, password = the same word) · [API reference](https://skri1l.github.io/Smart-Warehouse/api/). **Run locally:** `cd Frontend && npm ci && npm run dev` → http://localhost:5173 (details in [Getting started](#getting-started)).

### Front-end

| Requirement | Where |
|---|---|
| SPA on a modern framework + bundler | React 18 + Vite 5: [`Frontend/`](Frontend/README.md), [`vite.config.js`](Frontend/vite.config.js) |
| Architecture diagram, user flows, tech stack justification | [Frontend/ARCHITECTURE.md](Frontend/ARCHITECTURE.md), [ADR-0012](Documentation/adr/0012-browser-ui-javascript.md) |
| Components, pages, services for API calls | [`src/components/`](Frontend/src/components), [`src/features/*/…Page.jsx`](Frontend/src/features), [`src/api/`](Frontend/src/api) |
| Forms with validation | [`src/core/validation.js`](Frontend/src/core/validation.js): login, register camera, modify purchase recommendation |
| Global error handling | [`ErrorBoundary.jsx`](Frontend/src/components/ErrorBoundary.jsx), [`core/errors.js`](Frontend/src/core/errors.js), [`api/http.js`](Frontend/src/api/http.js) (ApiError, global 401) |
| Loading states | [`StateView.jsx`](Frontend/src/components/StateView.jsx), busy buttons, `Suspense` fallbacks |
| Unit tests (≥ 3 key components) | `npm test`: `DataTable`, `ErrorBoundary`, `LoginPage`, `DevicesPage`, `ReviewPage`, `ShelvesPage` + logic tests |
| E2E tests | `npm run test:e2e` (Playwright): [`e2e/`](Frontend/e2e) — phone review flow, desktop purchasing flow, adaptive checks |

### API documentation

| Requirement | Where |
|---|---|
| OpenAPI spec in the repository, validated | [Documentation/api/openapi.yaml](Documentation/api/openapi.yaml) (OpenAPI 3.1, Redocly lint in [`api-docs-ci.yml`](.github/workflows/api-docs-ci.yml)) |
| Samples, status codes, error messages, data models | Every operation and schema in `openapi.yaml`; see the [API reference](https://skri1l.github.io/Smart-Warehouse/api/) |
| Authentication | JWT bearer scheme `bearerAuth` in the spec; `POST /auth/login` and `/auth/register` documented; 401 on every protected operation |
| Getting Started + integration tutorial | [GETTING_STARTED.md](Documentation/api/GETTING_STARTED.md), [tutorials/connect-shelf-camera.md](Documentation/api/tutorials/connect-shelf-camera.md) |
| Published docs | Redoc on GitHub Pages; Swagger UI at `http://localhost:8080/swagger-ui.html`. How it is built: [Documentation/api/README.md](Documentation/api/README.md) |

### Adaptive UI

| Requirement | Where |
|---|---|
| Phone, tablet, desktop; orientation; browser zoom | Mobile-first layout, breakpoints and navigation per device: [DESIGN_SYSTEM.md](Documentation/design/DESIGN_SYSTEM.md#devices-and-layout) |
| Responsive grid, relative units, no overlaps, navigation everywhere | CSS Grid/Flex + `rem`; checked on every screen × 6 viewports by [`e2e/adaptive.spec.js`](Frontend/e2e/adaptive.spec.js) |
| 44px touch targets, WCAG 2.1 AA contrast | [`tokens.css`](Frontend/src/styles/tokens.css) `--touch-target`; contrast table in [DESIGN_SYSTEM.md](Documentation/design/DESIGN_SYSTEM.md#text-contrast-wcag-21-aa-needs-451); both asserted by the e2e suite |
| Consistent palette and typography | Single token set [`tokens.css`](Frontend/src/styles/tokens.css), exported for Figma: [`figma-tokens.json`](Documentation/design/figma-tokens.json) |
| Mockups for 3 devices + component library | [Figma: Smart Warehouse — Adaptive UI](https://www.figma.com/design/2cT9dahpeFuGZmCCnzSU1X/Smart-Warehouse-%E2%80%94-Adaptive-UI) (view-only, no sign-in): pages *Foundations* (colours, type, spacing, grid, contrast), *Components* (11 reusable components with variants), *Screens* (Login, Dashboard, Products, Review on 375 / 768 / 1440, plus phone and tablet landscape) |

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
cp .env.example .env   # adjust ports/password if 8080 or 5432 are taken; set your own JWT_SECRET
docker compose up -d --build
```

### API documentation (Swagger)

Once the backend is running, the interactive API docs are available at:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI spec (JSON): `http://localhost:8080/v3/api-docs`

(Replace `8080` with `BACKEND_PORT` from `Backend/.env` if you changed it.)

The docs themselves are public. Every endpoint except `POST /auth/register` and `POST /auth/login` needs a JWT: sign in with `/auth/login`, press **Authorize** in Swagger UI, paste the `accessToken`, then use **Try it out**. The token lives 15 minutes. Step by step: [Documentation/api/GETTING_STARTED.md](Documentation/api/GETTING_STARTED.md#3-get-an-access-token).

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
- **Frontend:** `npm run lint` (ESLint), `npm run format:check` (Prettier), `npm test` (Vitest), `npm run test:e2e` (Playwright), `npm run build` and a `license-checker` audit of runtime dependencies, run from `Frontend/`. See [Frontend/package.json](Frontend/package.json).
- **API docs:** `npm run check` in `Documentation/api/` rebuilds `openapi.yaml`, validates it with Redocly and fails if it is out of date.
- **Device:** `ruff check .`, `black --check .`, `mypy .`, a `pip-licenses` audit and `pytest`, run from `Device/`. See [Device/pyproject.toml](Device/pyproject.toml).
- Java and Python formatting is also enforced locally via [pre-commit](.pre-commit-config.yaml) hooks; every check runs in CI on each pull request (`.github/workflows/`).

## License

This repository is **proprietary and confidential**. See [LICENSE](LICENSE). No part of this codebase may be copied, used or distributed without prior written permission from the copyright holders. Third-party components keep their own licenses — see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

## Team

| Name | Primary responsibility |
|---|---|
| Ivan Samal | Backend, database, ML/CV integration, forecasting logic |
| Uladzislau Mikhayevich | Frontend, responsive web UI, server deployment, API documentation |
