# 0002. Use a single monorepo for Backend, Frontend and Documentation

Status: Accepted
Date: 2026-09-28

## Context

Smart Warehouse consists of a Java backend, a Python frontend, and (later) a Python ML service, developed by a two-person team on a fixed 12-week timeline. The components are tightly coupled during development: the frontend is built directly against the backend's evolving API, and both need to move in lockstep with the milestone plan in [MILESTONES.md](../MILESTONES.md).

## Decision

We will keep `Backend/`, `Frontend/` and `Documentation/` as top-level directories in a single Git repository (`Skri1l/Smart-Warehouse`), each independently buildable with its own dependency manifest, rather than splitting them into separate repositories.

## Alternatives considered

- **Polyrepo (separate repo per component)** — rejected for this project's scale: two developers would pay coordination overhead (cross-repo PRs, version pinning between backend and frontend, duplicated CI/documentation setup) with no corresponding benefit, since there is no independent release cadence or separate ownership team to isolate.
- **Single repo, no subdirectory separation** — rejected: mixing Java and Python source trees without clear boundaries would make per-component CI, linting and dependency management harder to configure correctly.

## Consequences

A single PR can span backend and frontend changes when a feature genuinely requires it, and documentation always describes the state of the whole system at one commit. The trade-off is that CI must be scoped with path filters (see `.github/workflows/`) so a frontend-only change doesn't trigger an unnecessary backend build, and the two components must agree on shared conventions (commit format, branching) even though they use different languages.
