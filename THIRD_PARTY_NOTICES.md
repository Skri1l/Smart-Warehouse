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

### Qt for Python (PySide6, Shiboken6) — version <version>

- License: GNU Lesser General Public License v3.0 (`licenses/LGPL-3.0.txt`,
  which incorporates `licenses/GPL-3.0.txt`)
- Copyright: The Qt Company Ltd. and other contributors
- Source code: https://code.qt.io/cgit/pyside/pyside-setup.git (tag `v<version>`)

### Qt libraries (bundled with PySide6) — version <version>

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

### pyqtgraph — version <version>

- License: MIT (`licenses/pyqtgraph-MIT.txt`)
- Source code: https://github.com/pyqtgraph/pyqtgraph

### httpx — version <version>

- License: BSD 3-Clause (`licenses/httpx-BSD-3-Clause.txt`)
- Source code: https://github.com/encode/httpx

<!-- Add remaining frontend dependencies from the pip-licenses report here. -->

## Backend

<!-- Paste the Maven/Gradle license report summary here: name, version, license, URL. -->

## ML service

<!-- Paste the pip-licenses report for the ML service here. -->
