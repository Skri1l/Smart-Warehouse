# 0007. Use a static GitHub Pages site for installer download and onboarding

Status: Accepted
Date: 2026-10-03

## Context

[ADR-0006](0006-frontend-pyside6-desktop.md) made the frontend a PySide6 desktop client, and NFR-07 in [PROJECT_DOCUMENTATION.md](../PROJECT_DOCUMENTATION.md) was narrowed to desktop-only. As a result the product has no entry point for anyone outside the team: a warehouse worker, an examiner or a prospective buyer can only get the application by cloning the repository and reading `Frontend/README.md`. This conflicts with the goal of a demoable and potentially sellable product ([ADR-0004](0004-proprietary-license.md)).

Constraints:

- Two developers and a fixed diploma deadline. No capacity for a second real UI or a new toolchain.
- [ADR-0003](0003-django-for-frontend.md) and ADR-0006 already rejected a second, web-based frontend.
- Installers will be produced by the per-OS PyInstaller CI from ADR-0006 (Milestone 8), which does not exist yet.

The design was explored in [RFC-0002](../rfcs/0002-static-site-for-installer-and-onboarding.md).

## Decision

We will publish a **static site written in plain HTML/CSS** (plus a few lines of vanilla JS to detect the visitor's OS), hosted on **GitHub Pages**. The site has two purposes only:

- **Download page** with Windows / macOS / Linux buttons that link to the latest assets on GitHub Releases.
- **How-to-use guide** covering installation, first login and the core flows from `PROJECT_DOCUMENTATION.md`, with screenshots.

Details:

- **Location:** a new top-level `Website/` directory next to `Backend/`, `Frontend/` and `Documentation/`. This extends the monorepo layout from [ADR-0002](0002-monorepo-repository-structure.md). The site shares no code with `Frontend/` and has its own release cadence.
- **Deployment:** `.github/workflows/website-deploy.yml` deploys on every push to `main` that touches `Website/**`.
- **Domain:** the default `*.github.io` address. A custom domain is a separate decision for later, if the product is offered for sale.
- **Sequencing:** we build the skeleton now, with "coming soon" placeholders in place of download links. Real links are added once the Milestone 8 packaging CI publishes installers to GitHub Releases.
- **Docs-as-code:** content changes go through PRs, and `docs-ci.yml` link-checks `Website/**/*.html` as well.
- **Positioning:** the site is a download and onboarding page, **not** mobile access to the product. Documentation and the diploma defense must not present it as fulfilling mobile/tablet support.

## Alternatives considered

- **Link to the GitHub Releases page only.** Rejected because it works for developers but not for a warehouse manager, examiner or buyer. There are no OS-specific buttons and no usage guide.
- **A static-site generator or framework (Jekyll, Hugo, Next.js, ...).** Rejected because it adds a toolchain for a handful of rarely changing pages. If the site grows, revisit this in a new ADR.
- **A responsive read-only web dashboard mirroring the desktop client.** Rejected because it is the "second frontend" that ADR-0003/0006 already ruled out, with its own REST client, auth and maintenance. Live mobile access needs its own RFC.
- **A native mobile app or PWA.** Rejected as far too expensive for the remaining timeline, for the same reasons as the Tauri/Electron/Compose alternatives in ADR-0006.
- **Serving the page from the Spring Boot backend.** Rejected because it ties a docs/marketing concern to the backend's deploy lifecycle and uptime. The site should stay available while the backend is down.

## Consequences

- The product gets a public front door that works from any device, at no hosting cost and with no server to operate.
- The repository gains a fourth top-level directory and a second deploy workflow to maintain.
- Screenshots and the how-to-use guide will drift unless they are updated together with UI changes in `Frontend/`. PRs that change user-visible flows should also update `Website/`.
- Until Milestone 8 lands, the site cannot offer real downloads and shows placeholders.
- There is a presentation risk: the site could be mistaken for mobile support. Any write-up that mentions it must say explicitly what it is and what it is not.
