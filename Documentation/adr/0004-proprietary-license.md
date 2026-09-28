# 0004. Use a proprietary license instead of an OSS license

Status: Accepted
Date: 2026-09-28

## Context

Smart Warehouse is a diploma project, but the team intends to keep the option of selling it (or licensing it commercially) after graduation. Permissive OSS licenses (MIT, Apache-2.0) grant anyone the right to copy, use, modify and redistribute the code — including commercially — which would let a buyer's competitor, or anyone else, legally obtain the same system for free from the public repository, undermining any future sale.

## Decision

We will license the repository under a proprietary "All Rights Reserved" license (see [LICENSE](../../LICENSE)), granting no usage rights by default beyond academic evaluation of the diploma work, with all other use requiring a separate written agreement.

## Alternatives considered

- **MIT / Apache-2.0** — rejected: incompatible with the stated goal of a future commercial sale; anyone could legally fork and use the code once published.
- **Source-available with a non-commercial clause (e.g. BUSL, Elastic License)** — considered, not chosen for now: adds legal complexity (defining "commercial use", a conversion date) that isn't needed while the repository is private; revisit via a new ADR if the project is ever open-sourced with commercial restrictions instead of sold outright.
- **No LICENSE file** — rejected: in the absence of a license, default copyright law technically already reserves all rights, but an explicit LICENSE file removes ambiguity for examiners, collaborators and prospective buyers.

## Consequences

The repository must stay private (a public GitHub repo with this license still technically protects the code, but a private repo avoids any accidental exposure). Any future decision to open-source part of the system, or to change the terms for a specific buyer, should be recorded as a new ADR rather than by editing this one or the LICENSE file silently.

Note: some universities' academic policies claim partial rights over student diploma work; this ADR does not address that — see the note in the repository README/LICENSE follow-up regarding checking the university's IP policy before an actual sale.
