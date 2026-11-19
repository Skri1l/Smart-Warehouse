# 0001. Record architecture decisions

Status: Accepted
Date: 2026-09-28

## Context

The team is two people building a system over 12 weeks. Significant technical and process decisions (framework choices, license, branching model) are being made quickly, often under deadline pressure, and are easy to forget the reasoning behind a few weeks later — both for the two developers and for anyone assessing the diploma who asks "why this and not X?".

## Decision

We will record every significant architectural or process decision as a short Architecture Decision Record (ADR) in `Documentation/adr/`, following the process in [README.md](README.md). ADRs are numbered sequentially and are immutable — a changed decision gets a new ADR that supersedes the old one.

## Alternatives considered

- **No formal record, rely on commit history / memory** — rejected: commit messages explain *what* changed, rarely *why*, and reasoning is the first thing lost over a 12-week project with exam pressure at the end.
- **A single running "decisions log" document** — rejected: a single mutable file loses the point-in-time context of each decision and invites silent edits instead of superseding records.

## Consequences

Every non-trivial decision costs a few extra minutes to write down. In exchange, the project has a searchable, dated record of *why* it looks the way it does — useful for the diploma defense, for onboarding a buyer's engineering team if the project is sold, and for the two developers themselves after time away from the code.
