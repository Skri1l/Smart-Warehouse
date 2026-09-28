# Branching Model

Smart Warehouse uses a lightweight **GitFlow** variant: two long-lived branches, short-lived work branches named after their tracking ticket.

## Long-lived branches

| Branch | Purpose | Protected |
|---|---|---|
| `main` | Always deployable. Every commit on `main` corresponds to a tagged release / demo state. | Yes |
| `develop` | Integration branch. All finished feature work lands here first. | Yes |

Protection rules (configure in GitHub → Settings → Branches):

- No direct pushes; changes only via pull request.
- At least 1 approving review required before merge.
- Required status checks: backend CI, frontend CI (see `.github/workflows/`).
- Linear history preferred — squash merge for `feature/*` → `develop`.

## Short-lived branches

Format:

```text
<type>/<TICKET-ID>-<short-kebab-case-description>
```

| Type | Used for | Branches from | Merges into |
|---|---|---|---|
| `feature/` | New functionality | `develop` | `develop` |
| `fix/` | Bug fix found on `develop` | `develop` | `develop` |
| `hotfix/` | Urgent fix for a bug already on `main` | `main` | `main` **and** `develop` |
| `release/` | Stabilizing a batch of features before a milestone demo | `develop` | `main` **and** `develop` |
| `chore/` | Tooling, CI, dependency bumps, docs-only changes | `develop` | `develop` |

`TICKET-ID` is the project tracker key in the form `SW-<number>` (e.g. `SW-102`). If a change has no tracked ticket, omit the ID: `chore/update-readme`.

### Examples

```text
feature/SW-118-shelf-stock-endpoint
fix/SW-142-negative-quantity-bug
hotfix/SW-201-recognition-service-crash
release/SW-milestone-2
chore/SW-99-bump-spring-boot
```

### Rules

- Branch names are lowercase, words separated by hyphens, no spaces or underscores.
- One branch = one logical change. Do not mix unrelated fixes into a feature branch.
- Rebase (`git rebase develop`) instead of merging `develop` into your branch, to keep history readable. Never rewrite history on `main` or `develop`.
- Delete the branch after it is merged (GitHub can do this automatically on merge).

## Pull requests

- Open a PR as soon as the branch exists if you want early feedback (mark it a draft).
- PR title should follow the [commit convention](COMMIT_CONVENTION.md) format, since squash-merge uses the PR title as the commit message.
- Use the PR template (`.github/pull_request_template.md`) — link the ticket, describe the change, list how it was tested.
- CI must pass (build, tests, lint/format checks) before merge.

## Releases

- A `release/*` branch is cut from `develop` at each milestone (see [MILESTONES.md](MILESTONES.md)).
- Only bug fixes and documentation changes are allowed on a `release/*` branch — no new features.
- On completion, merge into `main`, tag (`vX.Y.0`), then merge `main` back into `develop` to sync any release-branch fixes.
