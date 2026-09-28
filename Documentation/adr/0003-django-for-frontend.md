# 0003. Use Django for the frontend

Status: Accepted
Date: 2026-09-28

## Context

`PROJECT_DOCUMENTATION.md` originally left the frontend framework open ("React or another SPA framework selected by the frontend developer"). The frontend developer's stronger existing skill set is Python rather than JavaScript, and the diploma timeline (12 weeks, two people) does not leave room for ramping up a second language ecosystem (Node tooling, a JS build pipeline, a separate deployment target) in addition to Java on the backend and Python for the ML service.

## Decision

We will build the frontend as a Django application (Python) that consumes the backend's REST API, instead of a JavaScript SPA framework.

## Alternatives considered

- **React / Vue SPA** — rejected for this project: would require standing up a separate JS build/deploy pipeline and CI job, and the frontend developer would be learning the framework and the domain simultaneously, which is the highest-risk combination on a fixed deadline.
- **FastAPI + a JS/HTMX frontend** — rejected: still splits the frontend into two toolchains (Python API layer + separate templating/JS layer) for no benefit over Django's built-in templating for this project's UI complexity (dashboards and forms, not a highly interactive SPA).
- **Streamlit/NiceGUI dashboard** — rejected as the primary frontend: fast to build but not designed for multi-role authenticated web apps with custom navigation/forms (FR-01, FR-17); could be revisited later for an internal analytics view if needed.

## Consequences

The system now has three languages (Java, Python x2 for frontend and ML) instead of four, and the frontend developer works in one ecosystem end-to-end. The trade-off is a more traditional server-rendered UI rather than a fully interactive SPA — acceptable given the actors and flows in `PROJECT_DOCUMENTATION.md` (dashboards, forms, lists) don't require SPA-level interactivity. [CODE_STYLE_FRONTEND.md](../CODE_STYLE_FRONTEND.md) defines the resulting project layout and conventions.
