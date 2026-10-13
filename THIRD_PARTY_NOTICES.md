# Third-Party Notices

The "Smart Warehouse" software includes the third-party components listed
below. Each component is licensed under its own terms, reproduced in the
`licenses/` directory. The proprietary license in `LICENSE` applies only to
the portions of the software authored by its copyright holders and does not
restrict any rights granted by the licenses below.

<!--
Keep this file up to date:
- Frontend: `pip-licenses --format=markdown --with-urls` in the frontend venv
- ML service: same command in its venv
- Backend: Maven/Gradle license plugin report
Replace every <version> placeholder with the exact version that ships in the build.
Reject any new dependency licensed under GPL or AGPL (see ADR 0004).
-->

## Frontend (desktop client)

### Qt for Python (PySide6, Shiboken6) — version 6.11.2

- PyPI license classifier: `LGPL-3.0-only OR GPL-2.0-only OR GPL-3.0-only`
  (Qt/PySide6 is multi-licensed; this project uses it under the **LGPL-3.0-only**
  branch of that choice — see [ADR-0006](Documentation/adr/0006-frontend-pyside6-desktop.md))
- License: GNU Lesser General Public License v3.0 (`licenses/LGPL-3.0.txt`,
  which incorporates `licenses/GPL-3.0.txt`)
- Copyright: The Qt Company Ltd. and other contributors
- Source code: https://code.qt.io/cgit/pyside/pyside-setup.git (tag `v6.11.2`)

### Qt libraries (bundled with PySide6) — version 6.11.2

- License: GNU Lesser General Public License v3.0
- Copyright: The Qt Company Ltd. and other contributors
- Source code: https://download.qt.io/official_releases/qt/

**Replacing the Qt/PySide6 libraries.** The Qt and PySide6 libraries are
shipped as separate shared library files (`.dll` on Windows, `.so` on Linux,
`.dylib` / frameworks on macOS) inside the application directory, in the
`_internal/PySide6/` folder. You may replace them with a compatible modified
version of the same release. The source code of the exact versions listed
above is available at the links provided; the copyright holders will also
provide it on request for at least three years from the date of distribution.

**Qt modules that must not be used.** Qt Charts, Qt Data Visualization and any
other Qt module whose PyPI/Qt licensing is GPL-only (not LGPL) must not be
added to this project — see [ADR-0006](Documentation/adr/0006-frontend-pyside6-desktop.md).
Use `pyqtgraph` or `matplotlib` for charting instead.

### pyqtgraph — version 0.14.0

- License: MIT (`licenses/pyqtgraph-MIT.txt`)
- Copyright: 2012 University of North Carolina at Chapel Hill, Luke Campagnola
- Source code: https://github.com/pyqtgraph/pyqtgraph

### httpx — version 0.28.1

- License: BSD 3-Clause (`licenses/httpx-BSD-3-Clause.txt`)
- Copyright: 2019, Encode OSS Ltd.
- Source code: https://github.com/encode/httpx

<!-- Add remaining frontend dependencies (including transitive ones, e.g.
httpcore) from the pip-licenses report here once the project is scaffolded
and requirements.txt reflects the actual installed set. -->

## Device (shelf agent / emulator)

### httpx — version 0.28.1

Same component and license as listed under Frontend (BSD 3-Clause,
`licenses/httpx-BSD-3-Clause.txt`).

### picamera2 and gpiozero (Raspberry Pi only, not bundled)

- picamera2 — BSD 2-Clause, Copyright Raspberry Pi Ltd —
  https://github.com/raspberrypi/picamera2
- gpiozero — BSD 3-Clause, Copyright Ben Nuttall and contributors —
  https://github.com/gpiozero/gpiozero

These are installed on the shelf controller from Raspberry Pi OS packages
(`apt install python3-picamera2 python3-gpiozero`). They are not
redistributed with this software and are not in `Device/requirements.txt`
(see [ADR-0008](Documentation/adr/0008-shelf-agent-shared-core.md)).

<!-- Add transitive Device dependencies (httpcore, h11, anyio, certifi, idna)
from `pip-licenses` in the Device venv when the agent is packaged for release. -->

## Backend

<!-- Paste the Maven/Gradle license report summary here: name, version, license, URL. -->

## ML service

<!-- Paste the pip-licenses report for the ML service here. -->
