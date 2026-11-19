# 0011. Host all server-side components on a server we operate

Status: Accepted
Date: 2026-11-17

## Context

Until now the system has only run on a developer laptop: `Backend/docker-compose.yml` starts PostgreSQL, the Vision service and the backend locally, the shelf agent is pointed at `http://localhost:8080`, and the frontend was planned as a desktop client that every employee installs ([ADR-0006](0006-frontend-pyside6-desktop.md)). "Cloud deployment" was listed in the MVP scope and in Milestone 8, but nobody had decided what is deployed where or what the customer has to run.

After the BA review the recommendation was explicit: a warehouse company that buys the product should **only have to mount and connect the smart shelves**, and everything else should already work. The company must not install or operate a server, a database or a client application.

Constraints:

- Two developers and a fixed diploma deadline (Milestone 8: 08.12.26 – 22.12.26).
- Shelves already push images over HTTP with an on-device outbox that survives server outages ([ADR-0009](0009-device-transport-http-with-outbox.md)), so they only need a reachable URL and a device key.
- All server components are already containerised (`Backend/Dockerfile`, `Vision/Dockerfile`, `Planning/Dockerfile`).
- The backend currently permits all requests (`SecurityConfig`), which is not acceptable on a public address.

## Decision

We will **host every server-side component ourselves** on a Linux server (a cloud VM / VPS) and give each customer company a URL. The customer only connects shelves and opens the web UI in a browser ([ADR-0012](0012-browser-ui-javascript.md)).

- **Runtime:** one Docker Compose stack per deployment: `proxy`, `backend`, `vision`, `planning`, `postgres`. A production compose file (`deploy/docker-compose.prod.yml`) is separate from the local development one.
- **Single public entry point:** a reverse proxy (**Caddy**, Apache-2.0) is the only container with published ports (80/443). It obtains and renews TLS certificates automatically, serves the built web UI as static files and forwards `/api/**` (including device uploads) to the backend. Backend, Vision, Planning and PostgreSQL are reachable only on the internal Docker network.
- **Same origin:** UI and API live under one host name (`https://<company>.<our-domain>/` and `/api/...`), so the browser needs no CORS configuration.
- **Tenancy for the MVP:** one isolated stack and database **per customer company**, on its own subdomain. For the diploma there is one instance with the demo company. A shared multi-tenant deployment is a separate future decision (RFC).
- **Shelf onboarding:** an administrator registers the shelf in the web UI and receives its device key; the shelf is configured with the company URL and that key and pushes over **HTTPS only**. The customer's network needs outbound HTTPS and nothing else — no port forwarding, no VPN.
- **Secrets and config:** `.env` on the server (outside git), following NFR-03. Nothing environment-specific is committed.
- **Data safety:** nightly `pg_dump` of the database and of the image volume to storage outside the VM; a retention period for stored camera images is set before real customers are onboarded.
- **Precondition:** the stack is not exposed publicly until authentication and role-based authorization (Milestone 5) and device-key validation are in place.

## Alternatives considered

- **On-premise install at each customer** (our compose stack on their hardware): rejected. It needs customer IT staff, a machine that is always on, and remote support from us — exactly what the BA recommendation asks to avoid.
- **One shared multi-tenant deployment for all companies:** deferred, not rejected. It is cheaper to run at scale, but needs a tenant id on every entity, tenant-scoped authorization and queries, and careful testing against data leaks between companies. Too much for the remaining timeline; one stack per company gives isolation for free and can be migrated later.
- **Kubernetes or a managed PaaS per service:** rejected. Operational complexity and cost far beyond one VM for diploma-scale load; Compose is already what the team uses locally.
- **nginx + certbot instead of Caddy:** viable, more widespread. Rejected only because Caddy handles certificate issuance and renewal with no extra container or cron job, which is one less thing to break during the demo.
- **Expose each service on its own port:** rejected. Every extra public port is extra attack surface, and the browser would need CORS for UI ↔ API.

## Consequences

- The customer's setup is reduced to: mount shelves, enter URL and device key, open the site. No desktop installers, no per-OS builds.
- **We become the operator.** Uptime, OS and image updates, certificates, backups, logging and monitoring are now our responsibility (Milestone 8: "logging and basic monitoring", "installation/deployment documentation"). Runbooks for the deployed stack go next to each service (`Backend/runbooks/`, later `Frontend/runbooks/`).
- Security moves from "nice to have" to a release blocker: auth, device keys, TLS, input validation and rate limits must be finished before the first public deployment.
- Running cost of at least one VM plus a domain name; per-company stacks multiply it, which is acceptable for a handful of customers and is the main argument for revisiting multi-tenancy later.
- If the server or the customer's internet link is down, shelves keep photos in their outbox (ADR-0009) and the UI is unavailable; there is no offline mode.
- A new `deploy/` directory (production compose file, Caddyfile, backup script, `.env.example`) and a deployment workflow are added; this extends the layout from [ADR-0002](0002-monorepo-repository-structure.md).
