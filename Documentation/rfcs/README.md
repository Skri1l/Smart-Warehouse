# RFCs (Design Docs)

An RFC is a design proposal written **before** implementing a feature that is large, crosses module/service boundaries, or is hard to reverse once built. Its job is to move disagreement and design mistakes to before the code is written, when they cost a paragraph to fix instead of a rewrite.

This is distinct from an [ADR](../adr/README.md): an RFC explores a problem and a design in depth and can be debated and revised while in `Draft`/`In Review`; an ADR is the short, immutable record of the final decision (often an RFC's outcome is summarized as one ADR once accepted).

## When an RFC is required

Write one before starting implementation when a change:

- introduces a new service, external integration, or a new module boundary (e.g. the ML/CV service contract, supplier API integration);
- changes a shared data model or API contract that both backend and frontend depend on;
- affects a non-functional requirement in a hard-to-reverse way (security model, auth scheme, deployment topology);
- was flagged in a PR review as "this needs more design discussion than a code comment."

Routine feature work that fits cleanly within one module and one milestone (a new CRUD endpoint, a UI page for a documented flow) does not need one — go straight to a ticket and implementation.

## Lifecycle

`Draft` → `In Review` → `Accepted` (implementation proceeds) | `Rejected` (kept for record, not implemented) → `Implemented`

## Process

1. Copy [template.md](template.md) to `Documentation/rfcs/00NN-short-title.md`.
2. Open a PR while still in `Draft` — the PR discussion *is* the review.
3. Once both developers agree on the design, set `Status: Accepted`, merge, and create the implementation ticket(s) referencing the RFC number.
4. When implementation lands, update the RFC's status to `Implemented` and add a short "Outcome" note if the final implementation diverged from the design.
5. If accepted, distill the final decision (not the whole exploration) into a new [ADR](../adr/README.md).

## Index

| # | Title | Status |
|---|---|---|
| [0001](0001-image-ingestion-and-recognition-pipeline.md) | Image ingestion and recognition pipeline | Draft |
| [0002](0002-static-site-for-installer-and-onboarding.md) | Static site for desktop installer download and onboarding | Accepted |
