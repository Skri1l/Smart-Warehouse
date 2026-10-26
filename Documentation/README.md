# Documentation

Index of everything documentation-related. Product and process documentation for backend/frontend-specific concerns are kept next to their code ([Frontend/README.md](../Frontend/README.md), [Backend/runbooks/](../Backend/runbooks/)) rather than duplicated here.

## Docs-as-code

All documentation is Markdown, lives in this repository, and changes go through a pull request with review — the same process as code (see [CONTRIBUTING.md](../CONTRIBUTING.md)). Two consequences of treating docs this way:

- **Docs and code change together.** A PR that changes an API contract updates [API_GUIDELINES.md](API_GUIDELINES.md) or the relevant RFC/runbook in the same PR, not in a follow-up "update docs" ticket that may never happen.
- **Broken links are a CI failure, not a slow rot.** Every PR touching a `.md` file is checked by [`.github/workflows/docs-ci.yml`](../.github/workflows/docs-ci.yml) (link checker); a PR with a dead link doesn't merge.

## Product & delivery

| Document | Purpose |
|---|---|
| [PROJECT_DOCUMENTATION.md](PROJECT_DOCUMENTATION.md) | Product spec: goals, scope, domain model, requirements, architecture |
| [MILESTONES.md](MILESTONES.md) | 12-week delivery plan |
| [BA_SUMMARY.md](BA_SUMMARY.md) | Summary of answers for the business analyst: vision, problem, goals, high-level solution |
| [Device/USER_GUIDE.md](../Device/USER_GUIDE.md) | How to run the shelf emulator / device against the backend and for the demo |
| [PIPELINE_RUN.md](PIPELINE_RUN.md) | The whole chain on one PC with `scripts/pipeline.sh`: backend + PostgreSQL in Docker, Vision, emulator or webcam, data in Swagger |

## Hardware

| Document | Purpose |
|---|---|
| [hardware/SHELF_DEVICE.md](hardware/SHELF_DEVICE.md) | Demo shelf module: dimensions, camera placement, components, lighting, wiring, printing, transport |
| [hardware/shelf_fov.py](hardware/shelf_fov.py) | Camera field-of-view calculator the shelf dimensions are derived from |

## Engineering conventions

| Document | Purpose |
|---|---|
| [BRANCHING.md](BRANCHING.md) | Branching model and naming |
| [COMMIT_CONVENTION.md](COMMIT_CONVENTION.md) | Commit message format |
| [CODE_STYLE_BACKEND.md](CODE_STYLE_BACKEND.md) | Java/Spring Boot conventions |
| [CODE_STYLE_FRONTEND.md](CODE_STYLE_FRONTEND.md) | Python/PySide6 desktop client conventions |
| [CODE_STYLE_DEVICE.md](CODE_STYLE_DEVICE.md) | Python shelf agent (Raspberry Pi + emulator) conventions |
| [API_GUIDELINES.md](API_GUIDELINES.md) | REST design & OpenAPI documentation standard |

## Decisions & design

| Document | Purpose | Written |
|---|---|---|
| [adr/](adr/README.md) | Architecture Decision Records — short, immutable, numbered records of *why* a significant decision was made | at decision time |
| [rfcs/](rfcs/README.md) | Design docs for features/changes big enough to design before coding | before implementation |

Rule of thumb: an **RFC** explores a problem and can change while in review; an **ADR** is the short, final, immutable record of the decision (often the accepted outcome of an RFC, or a smaller standalone choice). Routine day-to-day implementation choices need neither — a PR description is enough.

## Operations

Runbooks (what to do when a specific alert/symptom happens) live next to the service they operate, not here: [Backend/runbooks/](../Backend/runbooks/). `Frontend/runbooks/` follows once the frontend is a deployed, operated service.

## Reference documentation (generated, not hand-written)

Hand-maintained API reference drifts from the code the moment someone changes a method signature and forgets the doc. Reference material is instead generated from the source of truth:

- **Backend REST API:** [springdoc-openapi](https://springdoc.org/) generates the OpenAPI spec and Swagger UI directly from controller/DTO annotations — see [API_GUIDELINES.md](API_GUIDELINES.md). Once the first controllers exist (Milestone 2), `GET /v3/api-docs` and `/swagger-ui.html` are the canonical, always-current API reference; nothing about request/response shapes should be hand-documented elsewhere.
- **Backend internals:** `./mvnw javadoc:javadoc` generates browsable Javadoc from the class/method comments described in [CODE_STYLE_BACKEND.md](CODE_STYLE_BACKEND.md), for internal (non-API) reference.
- **Frontend:** the PySide6 desktop client is a consumer of the backend API, not a library with its own public API surface, so it does not need a separate generated reference (no Sphinx/mkdocstrings doc site) at this stage — its `api/client.py` (see [CODE_STYLE_FRONTEND.md](CODE_STYLE_FRONTEND.md)) is written against the generated OpenAPI spec instead of a hand-maintained one (see [API_GUIDELINES.md](API_GUIDELINES.md)). Doxygen/DocFX/TypeDoc don't apply — those target C++/.NET/TypeScript codebases, none of which are in this stack.
- **Third-party components:** [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md) is regenerated from each component's actual installed version via `pip-licenses` (frontend/ML) and the Maven license plugin (backend) rather than hand-maintained from memory — see [ADR-0006](adr/0006-frontend-pyside6-desktop.md).

CI does not yet publish the generated OpenAPI spec anywhere, because there are no controllers to generate it from yet. Once Milestone 2 adds real endpoints, add a CI step that fails the build if `/v3/api-docs` can't be generated, so the reference can never silently go stale.
