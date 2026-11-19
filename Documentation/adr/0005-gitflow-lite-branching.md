# 0005. Use a GitFlow-lite branching model

Status: Accepted
Date: 2026-09-28

## Context

The repository already had `main` and `develop` branches and ticket-referencing feature branches (`sw-102-DB`, `sw-105-backend-structure`) before conventions were formally written down. The team needs `main` to always reflect a demoable/deployable state for milestone demos, while day-to-day work happens on short-lived branches.

## Decision

We will use a lightweight GitFlow variant: `main` (always deployable) and `develop` (integration) as protected long-lived branches, with `feature/`, `fix/`, `hotfix/`, `release/` and `chore/` short-lived branches named `<type>/<TICKET-ID>-<description>`. Full rules in [BRANCHING.md](../BRANCHING.md).

## Alternatives considered

- **Trunk-based development (single `main`, short-lived branches only)** — rejected for now: with milestone demos happening every 1-2 weeks and a diploma defense at the end, having a stable `main` that is never broken by in-progress `develop` work is more valuable than trunk-based development's faster integration cadence, which mainly pays off with larger teams and continuous deployment.
- **Full GitFlow (with `support/` branches, formal release managers)** — rejected: unnecessary process overhead for a two-person team.

## Consequences

Every feature branch merges to `develop` first and is validated there before a `release/*` branch promotes a batch of work to `main` for a milestone demo. This costs one extra merge step compared to trunk-based development, but means a broken `develop` never blocks preparing a demo from the last good `main`.
