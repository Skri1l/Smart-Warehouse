# 0012. Replace the desktop client with a responsive browser UI in JavaScript

Status: Accepted — supersedes [0006](0006-frontend-pyside6-desktop.md) and [0007](0007-static-site-for-installer-and-onboarding.md)
Date: 2026-11-17

## Context

[ADR-0006](0006-frontend-pyside6-desktop.md) chose a PySide6 desktop client, and [ADR-0007](0007-static-site-for-installer-and-onboarding.md) added a static site for downloading its installers. Two things have changed since:

1. **Hosted deployment.** Per [ADR-0011](0011-hosted-server-deployment.md) we host the whole server side and a customer company should only connect shelves. A client that every employee must download, install and update contradicts that goal.
2. **Diploma criteria.** The diploma criteria for the project document require an **adaptive (responsive) UI**, and Milestone 5 already lists "responsive base UI" as a deliverable. A Qt desktop window does not run on phones or tablets, so NFR-07 in its desktop-only form cannot satisfy the criterion. Warehouse workers on the floor naturally use a phone or tablet.

Other facts:

- The existing PySide6 scaffold in `Frontend/` is small (login, dashboard with low-stock products, product search, mock API). Little work is lost.
- The desktop client brought per-OS PyInstaller builds, macOS signing and LGPL compliance work (ADR-0006 "Licensing"), none of which has been done yet.
- The original reason for staying in Python ([ADR-0003](0003-django-for-frontend.md), ADR-0006) was the frontend developer's skill set. The team now accepts learning JavaScript as a trade-off for a UI that runs everywhere without installation.
- The UI is dashboards, tables, forms and a few charts with role-based navigation (FR-01, FR-17), backed by the existing REST API.

## Decision

We will build the frontend as a **single-page web application in JavaScript** that runs in the browser on desktop, tablet and phone, and talks to the backend only over its REST API.

- **Stack:** React 18, Vite (build and dev server), React Router. Plain JavaScript (ES2022 modules) with JSDoc type comments; no TypeScript for now.
- **Responsive design:** mobile-first CSS (CSS Modules + shared design tokens as CSS custom properties) with fixed breakpoints; every screen must work from 360 px phone width to wide desktop monitors. Details in [CODE_STYLE_FRONTEND.md](../CODE_STYLE_FRONTEND.md).
- **Charts:** Chart.js via `react-chartjs-2` (both MIT).
- **HTTP:** a small `fetch` wrapper in `src/api/`; no direct `fetch` calls from components.
- **Hosting:** `vite build` produces static files that the reverse proxy from ADR-0011 serves on the **same origin** as `/api`, so no CORS is needed. In development the Vite dev server proxies `/api` to `http://localhost:8080`.
- **Auth:** the session credential is never stored in `localStorage`/`sessionStorage`. Preferred: an `HttpOnly`, `Secure`, `SameSite=Strict` cookie issued by the backend. The exact mechanism is fixed together with backend authentication in Milestone 5. Role-based hiding in the UI is UX only; the backend enforces authorization.
- **Supported browsers:** the two latest versions of Chrome, Edge, Firefox and Safari (desktop and mobile).
- **Quality tooling:** ESLint + Prettier, Vitest + React Testing Library, run in `frontend-ci.yml`.
- **Licensing:** only permissive dependencies (MIT, BSD, Apache-2.0, ISC); GPL/AGPL packages are rejected. CI checks this with `license-checker`.
- **Website:** the GitHub Pages site from ADR-0007 stays as a landing page and how-to guide, but its download buttons are replaced by an "Open the app" link and shelf-connection instructions. There are no installers anymore.

## Alternatives considered

- **Keep the PySide6 desktop client (ADR-0006):** rejected. It cannot run on phones/tablets, does not meet the responsive-UI criterion, and needs installation on every workstation, which contradicts ADR-0011.
- **Desktop client plus a separate mobile web UI:** rejected. Two frontends with two REST clients and two auth flows is the cost ADR-0007 already refused to pay.
- **Server-rendered Django (+ HTMX) — reviving ADR-0003:** responsive and in Python, but adds a third server application to deploy and operate next to the Spring backend, duplicates session and auth handling, and interactive dashboards with charts are more natural as a client of the existing REST API.
- **Vue 3:** equally suitable. React was chosen for the larger body of documentation, examples and component libraries, which matters most while the developer is learning.
- **TypeScript:** rejected for now to keep the toolchain and learning curve small. JSDoc comments keep types readable; migrating file by file to TypeScript later is possible without rewriting.
- **Flutter Web / Flet:** rejected. Not JavaScript, heavy initial download, weak accessibility and text selection in the browser.
- **Native mobile app or PWA-first:** out of scope for the timeline. A responsive SPA can later be made installable as a PWA without changing the architecture.

## Consequences

- The UI works on any device with a browser and no installation; NFR-07 is rewritten as a responsive-web requirement, and the diploma's adaptive-UI criterion can be demonstrated directly by resizing the window or opening the site on a phone.
- The system's languages become Java, Python (Vision, Device) and JavaScript. The frontend developer must learn JS and React during Milestone 5, which is the main schedule risk; scope per screen stays minimal until the base layout works.
- `Frontend/` is re-scaffolded as a Vite project. The PySide6 code is removed in the same PR, together with its `licenses/` LGPL texts and the PySide6/pyqtgraph entries in `THIRD_PARTY_NOTICES.md` once no shipped component uses them. `frontend-ci.yml` switches from Python to Node.
- No per-OS packaging, code signing or LGPL dynamic-linking obligations; the Milestone 8 installer work disappears.
- Responsive layouts must be designed and tested at several widths for every screen, which is extra work per screen compared to a fixed desktop window.
- [CODE_STYLE_FRONTEND.md](../CODE_STYLE_FRONTEND.md), [API_GUIDELINES.md](../API_GUIDELINES.md), [PROJECT_DOCUMENTATION.md](../PROJECT_DOCUMENTATION.md) are updated to describe the browser UI together with this ADR; `Website/` content follows in its own PR.
