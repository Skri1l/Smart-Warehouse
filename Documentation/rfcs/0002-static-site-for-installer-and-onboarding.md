# 0002. Static site for desktop installer download and onboarding

Status: Draft
Author(s): Uladzislau Mikhayevich
Date: 2026-10-01
Related: NFR-07 (desktop support) in [PROJECT_DOCUMENTATION.md](../PROJECT_DOCUMENTATION.md), [ADR-0006](../adr/0006-frontend-pyside6-desktop.md), Milestone 8

## Summary

[ADR-0006](../adr/0006-frontend-pyside6-desktop.md) moved the frontend from a web app to a PySide6 desktop client, which dropped mobile/tablet access to the product (NFR-07 was narrowed to desktop-only as a result). This RFC does **not** propose rebuilding mobile access to live stock data. It proposes a small, static HTML site — no backend, no build pipeline, no accounts — whose only job is: let a visitor (on phone, tablet or desktop) download the correct installer for their OS and read a short guide on how to install and use the application. This gives the product a reachable-from-anywhere front door without taking on a second real UI.

## Motivation

Right now, anyone wanting to get or evaluate Smart Warehouse — a new warehouse worker, an examiner, a prospective buyer — has no entry point except cloning the repository and reading `Frontend/README.md`. That's fine for the two developers, not for anyone else, and directly undermines the diploma's stated goal of being demoable and potentially sold ([LICENSE](../../LICENSE), [ADR-0004](../adr/0004-proprietary-license.md)).

Separately, dropping mobile/tablet support in NFR-07 leaves a real gap: *some* reachable-from-a-phone surface is still worth having, even if it isn't the operational UI. A static download-and-docs page is the cheapest thing that closes that gap honestly, without re-opening the "do we need a second frontend" question that [ADR-0003](../adr/0003-django-for-frontend.md) and ADR-0006 already answered (no).

## Detailed design

**What it is:** a static site, plain HTML/CSS (+ a few lines of vanilla JS for OS-detection on the download button), with two kinds of pages:

- **Download / home** — big "Download for Windows / macOS / Linux" buttons, each pointing at the latest matching asset on the project's GitHub Releases page. Optional: detect the visitor's OS and pre-highlight the right button.
- **How to use** — a short guide: installing the app, first login, and a walkthrough of the core flows already described in `PROJECT_DOCUMENTATION.md` (find a product, read the dashboard, confirm a recognition review, etc.), with screenshots.

**Why plain HTML/CSS, not a static-site generator or framework:** the same reasoning as [ADR-0003](../adr/0003-django-for-frontend.md)/[ADR-0006](../adr/0006-frontend-pyside6-desktop.md) — a two-person team on a fixed deadline shouldn't add a toolchain (Jekyll/Hugo/Next.js/...) for a handful of pages that change rarely. If the page count or update frequency grows enough to justify it, that's a future ADR, not a default.

**Where it lives:** a new top-level directory, `Website/`, alongside `Backend/`, `Frontend/` and `Documentation/`. It is intentionally **not** part of the `Frontend/` desktop client — different release cadence, different audience, no shared code.

**Hosting:** GitHub Pages, built from `Website/` via a dedicated `.github/workflows/website-deploy.yml` (deploy on push to `main` when `Website/**` changes). No server to operate, no cost, consistent with [NFR-05](../PROJECT_DOCUMENTATION.md) (nothing new to scale) and the project's "no infrastructure we don't need" pattern so far.

**Download links:** point at GitHub Releases, populated by the PyInstaller packaging CI described in ADR-0006's Consequences (Milestone 8). This RFC's page can ship earlier than that CI with a "coming soon" placeholder — the page and the packaging pipeline are sequenced independently.

**Docs-as-code:** same rules as the rest of the repo ([Documentation/README.md](../README.md)) — content changes through a PR, link-checked by the existing `docs-ci.yml` (extend its glob to include `Website/**/*.html` or add a second lychee pass over it).

## Alternatives considered

- **Do nothing — just link to the GitHub Releases page.** Rejected: fine for the two developers, not for a warehouse manager, examiner or prospective buyer evaluating the product; a dedicated page with OS-specific buttons and a usage guide looks like a real product, which matters given the stated goal of a future sale.
- **A real responsive web dashboard mirroring the desktop client (read-only live stock view).** Rejected for this RFC: this is the full "second frontend" problem ADR-0003/0006 already decided against — its own REST client, its own auth handling, its own ongoing maintenance. If validated floor-mobile access to *live* data becomes a real requirement, that deserves its own RFC and should be evaluated on its own cost, not smuggled in as a side effect of a download page.
- **A native mobile app or PWA.** Rejected: same ecosystem-sprawl argument as ADR-0006's alternatives (Tauri/Electron/Compose Multiplatform) — far too expensive for the remaining diploma timeline.
- **Serve the static page from the Spring Boot backend.** Rejected: couples a marketing/docs concern to the backend's deploy lifecycle and uptime for no benefit; the site should work even if the backend is down for maintenance.

## Drawbacks

- **Must not be mistaken for mobile access to the product.** The biggest risk of this RFC is presentation, not engineering: if NFR-07's documentation or the diploma defense implies this page *is* mobile support, that's a misrepresentation. Any write-up referencing this must say explicitly: this is a download/onboarding page, not a mobile client.
- Adds a small new deployable surface to keep accurate — screenshots and the how-to-use guide will drift if not updated alongside real UI changes in `Frontend/`.
- One more GitHub Pages/DNS surface to set up, however small.

## Unresolved questions

- Build this now (cheap, but nothing to download yet — ships as a placeholder) or wait for Milestone 8 packaging to exist first? Leaning toward: skeleton now, wire real download links once CI produces installers.
- Is a custom domain worth it for credibility if the project is actually offered for sale, versus the default `*.github.io` URL? Out of scope here — a cost/branding decision for later, possibly its own ADR once there's a buyer conversation.
- Does accepting this RFC warrant updating [ADR-0002](../adr/0002-monorepo-repository-structure.md) (monorepo structure) to mention the new `Website/` directory, or is a line in this RFC's Outcome enough? Default: update ADR-0002's... no — ADRs are immutable, so this would need its own small ADR ("add a Website/ directory to the monorepo") once this RFC is accepted, not an edit to ADR-0002.

## Outcome

_(filled in after implementation)_
