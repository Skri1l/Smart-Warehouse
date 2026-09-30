# Architecture Decision Records

An ADR is a short, immutable record of one significant architectural or process decision: what was decided, why, and what alternatives were rejected. They are the project's decision log — when someone (including future-you) asks "why is it built this way?", the answer should be an ADR, not a Slack message or a git blame archaeology session.

## Rules

- **Numbered sequentially**, zero-padded: `0001-title-in-kebab-case.md`.
- **Immutable once accepted.** Never edit the Context/Decision of an existing ADR to reflect a new choice. If a decision changes, write a **new** ADR that supersedes the old one, and mark the old one `Superseded by ADR-00NN`.
- **Short.** One screen. If it needs diagrams and multi-week discussion, it started as an [RFC](../rfcs/README.md) and the ADR is just its final, distilled outcome.
- Written **at the time the decision is made** (or immediately after, when backfilling a foundational decision), not months later from memory.

## When to write one

Any decision that would be expensive to reverse or that a new team member would otherwise have to reverse-engineer from the code: choice of framework/library, database, architecture pattern, API style, license, branching model, a rejected alternative worth remembering. Routine implementation choices (variable names, which controller method does what) do not need an ADR.

## Status lifecycle

`Proposed` → `Accepted` → (optionally) `Superseded by ADR-00NN` / `Deprecated`

## Process

1. Copy [template.md](template.md) to `Documentation/adr/00NN-short-title.md` (next sequential number).
2. Fill it in, open a PR (`docs:` commit type per [COMMIT_CONVENTION.md](../COMMIT_CONVENTION.md)).
3. Discuss in the PR itself; once both developers agree, set `Status: Accepted` and merge.

## Index

| # | Title | Status |
|---|---|---|
| [0001](0001-record-architecture-decisions.md) | Record architecture decisions | Accepted |
| [0002](0002-monorepo-repository-structure.md) | Use a single monorepo for Backend, Frontend and Documentation | Accepted |
| [0003](0003-django-for-frontend.md) | Use Django for the frontend | Supersedes |
| [0004](0004-proprietary-license.md) | Use a proprietary license instead of an OSS license | Accepted |
| [0005](0005-gitflow-lite-branching.md) | Use a GitFlow-lite branching model | Accepted |
| [0006](0006-gitflow-lite-branching.md) | Use a GitFlow-lite branching model | Accepted |
