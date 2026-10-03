# 0006. Use a PySide6 desktop client for the frontend

Status: Accepted — supersedes [0003](0003-django-for-frontend.md)
Date: 2026-09-30

## Context

[ADR 0003](0003-django-for-frontend.md) chose a server-rendered Django web application as the frontend. The team has since decided that the frontend must be a **desktop application** that connects to the backend's REST API and runs on Windows, macOS and Linux from a single codebase.

<!-- TODO: state the concrete reason for moving to desktop (e.g. offline work, access to local files/hardware such as scanners or printers, customer requirement). A web UI is already cross-platform, so the ADR should record what desktop gives us that the browser does not. -->

The constraints recorded in ADR 0003 still apply:

- The frontend developer's stronger skill set is Python rather than JavaScript.
- The diploma timeline (12 weeks, two people) leaves no room for ramping up an additional language ecosystem on top of Java (backend) and Python (ML service).
- The UI consists of dashboards, forms and lists with multi-role authentication (FR-01, FR-17), not highly interactive custom graphics.

The project is distributed under a proprietary license (`LICENSE`), so every UI dependency must be usable in closed-source software.

## Decision

We will build the frontend as a cross-platform desktop application in **Python using PySide6 (Qt for Python)**, communicating with the backend exclusively over its REST API.

- **UI toolkit:** PySide6 widgets; forms may be designed in Qt Designer (`.ui` files).
- **Charts:** `pyqtgraph` (MIT) or `matplotlib` embedded via its Qt backend. **Qt Charts, Qt Data Visualization and any other Qt module licensed only under GPL must not be used.**
- **HTTP client:** `httpx`. Network calls run off the UI thread (`QThreadPool` / `QRunnable` or `qasync`) so the interface never blocks.
- **Authentication:** the client obtains a token from the backend at login, keeps it in memory for the session, and shows or hides navigation sections according to the user's role returned by the backend. Authorization is always enforced by the backend; role-based hiding in the client is a UX concern only.
- **Packaging:** PyInstaller in `--onedir` mode, built per OS in CI (matrix job for Windows, macOS, Linux). `--onefile` must not be used (see Licensing).
- **Bindings:** PySide6 only. PyQt5/PyQt6 must not be used — they are licensed under GPL or a commercial license and are incompatible with our proprietary license.

## Licensing

PySide6 and the Qt libraries it ships are licensed under the **GNU LGPL v3**, which permits use in proprietary applications under the following conditions, which we adopt as requirements:

1. **Dynamic linking and replaceability.** Qt/PySide6 shared libraries are shipped as separate files next to the executable (`--onedir`), so a user can replace them with a modified version.
2. **License terms.** `LICENSE` contains a *Third-party components* clause stating that it does not restrict the rights granted by third-party licenses, and explicitly permits modifying LGPL components and reverse engineering to the extent necessary to debug such modifications (LGPLv3 §4).
3. **Notices.** Every distributed build includes `THIRD_PARTY_NOTICES.md` and the full text of the LGPLv3 and GPLv3 under `licenses/`, together with the Qt/PySide6 versions used and where to obtain their source code.
4. **Dependency audit.** New frontend dependencies are checked with `pip-licenses` before being added; anything under GPL or AGPL is rejected. The same check is applied to backend (Maven/Gradle license plugin) and ML-service dependencies.

## Alternatives considered

- **Keep Django (ADR 0003)** — rejected: does not meet the new requirement for a desktop application.
- **Flet (Python, Flutter-based renderer)** — a strong alternative: Apache 2.0 license (no LGPL obligations), modern look, low learning curve. Rejected as the primary choice because the project is younger, has fewer ready-made components for tables/forms and a smaller body of documentation and answered questions, which raises risk on a fixed deadline. Remains the fallback if PySide6 proves too heavy.
- **JavaFX** — would allow sharing DTOs with the Java backend, but moves the frontend developer out of Python, contradicting the main constraint of ADR 0003.
- **Tauri / Electron** — rejected for the same reason as the React/Vue SPA in ADR 0003: requires a JS/TS toolchain and build pipeline.
- **Compose Multiplatform (Kotlin)** — mature, but introduces a new language and ecosystem on a fixed deadline.
- **PyQt6** — functionally equivalent to PySide6 but GPL/commercial-licensed; incompatible with our proprietary license.

## Consequences

- The system keeps three languages (Java, Python for the frontend, Python for ML); the frontend developer stays in one ecosystem end-to-end.
- The backend becomes a pure REST API consumer target; there is no server-side rendering. API contracts and error formats must be documented well enough for a separate client.
- We now distribute binaries, so we take on per-OS build and packaging work in CI and the LGPL compliance steps above. macOS builds may additionally need code signing/notarization to launch without warnings.
- Users must install and update the client; there is no auto-update mechanism in scope for the diploma.
- [CODE_STYLE_FRONTEND.md](../CODE_STYLE_FRONTEND.md) must be rewritten for the PySide6 project layout and conventions, and `PROJECT_DOCUMENTATION.md` updated to describe a desktop client.
