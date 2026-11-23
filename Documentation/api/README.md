# API documentation

| Document | For |
|---|---|
| [GETTING_STARTED.md](GETTING_STARTED.md) | Run the backend and make the first calls; conventions and error handling |
| [tutorials/connect-shelf-camera.md](tutorials/connect-shelf-camera.md) | Step-by-step integration: layout → camera → photo upload → stock update |
| [openapi.yaml](openapi.yaml) | **OpenAPI 3.1 specification**: 113 operations, examples for every request and response, every status code with its error body |
| **https://skri1l.github.io/Smart-Warehouse/api/** | The same spec rendered with Redoc (published by [`website-deploy.yml`](../../.github/workflows/website-deploy.yml)) |
| `http://localhost:8080/swagger-ui.html` | Swagger UI with *Try it out*, served by the running backend |

## How the specification is produced

```text
Backend controllers + DTOs ──springdoc──▶ /v3/api-docs ──npm run export──▶ source/openapi.generated.json
                                                                                      │
                                     scripts/build-spec.mjs (npm run build) ◀─────────┘
                                     + overview, conventions, error format
                                     + an example for every schema (request and response)
                                     + real error bodies for 400/404/409/415/500/502 per operation
                                     + tag descriptions and groups
                                                    │
                                                    ▼
                                              openapi.yaml ──redocly lint──▶ valid ✔
                                                    │
                                                    └──redocly build-docs──▶ Redoc HTML (GitHub Pages)
```

- Paths, parameters, schemas and validation rules come **from the code** via springdoc ([API_GUIDELINES.md](../API_GUIDELINES.md)), so they cannot drift from the implementation.
- The documentation layer (`scripts/build-spec.mjs`) adds what the annotations do not have yet. Every example and error message in it was captured from the running backend. The script fails if a schema has no example or if a documented field no longer exists.
- `openapi.yaml` is generated: do not edit it by hand.

## Commands

Node.js 20+:

```bash
cd Documentation/api
npm ci
npm run export   # backend running on :8080 (BACKEND_URL=... to override) → source/openapi.generated.json
npm run build    # → openapi.yaml
npm run lint     # Redocly validation (rules in redocly.yaml)
npm run docs     # → dist/index.html (Redoc)
npm run check    # build + lint + fail if openapi.yaml is out of date (used in CI)
```

After changing a controller or DTO: start the backend, then run `npm run export && npm run build && npm run lint` and commit both JSON and YAML.

## Validation

[`api-docs-ci.yml`](../../.github/workflows/api-docs-ci.yml) runs `npm run check` on every change in this folder:

- the spec is valid OpenAPI 3.1 and passes the Redocly `recommended` ruleset (exceptions with reasons in [`redocly.yaml`](redocly.yaml));
- `openapi.yaml` matches what `build-spec.mjs` produces from the committed source.

## Known gaps in the API (tracked for the backend)

- No `/api/v1` prefix and no authentication yet (both required by [API_GUIDELINES.md](../API_GUIDELINES.md)).
- `ErrorResponse` has `status` and `message` only. The guideline format (`error` code, `path`, `details`) is not implemented, and validation errors use Spring's default body.
- `POST`/`PUT /devices` for a shelf that already has a camera returns **500** instead of 409 (`ShelfAlreadyHasDeviceException` has no handler).
- Lists are not paginated.
