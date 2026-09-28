# Contributing

This repository is private and worked on by a fixed two-person diploma team, but it follows the same process a production team would, so it survives handover, grading and (if applicable) a future sale.

## Before you start

1. Every change is tied to a ticket `SW-<number>` in the team's tracker. No ticket, no branch (except trivial `chore/` docs fixes).
2. Read the conventions relevant to what you're touching:
   - [Documentation/BRANCHING.md](Documentation/BRANCHING.md)
   - [Documentation/COMMIT_CONVENTION.md](Documentation/COMMIT_CONVENTION.md)
   - [Documentation/CODE_STYLE_BACKEND.md](Documentation/CODE_STYLE_BACKEND.md) or [Documentation/CODE_STYLE_FRONTEND.md](Documentation/CODE_STYLE_FRONTEND.md)
   - [Documentation/API_GUIDELINES.md](Documentation/API_GUIDELINES.md) if you're adding/changing an endpoint
3. If the change crosses module/service boundaries, changes a shared contract, or is otherwise hard to reverse, write an [RFC](Documentation/rfcs/README.md) first and get it `Accepted` before implementing. Once a significant decision (design or otherwise) is made — via an RFC or on its own — record it as an [ADR](Documentation/adr/README.md).

## Local setup

```bash
git clone git@github.com:Skri1l/Smart-Warehouse.git
cd Smart-Warehouse
pip install pre-commit && pre-commit install --hook-type pre-commit --hook-type commit-msg
```

This installs the git hooks defined in [.pre-commit-config.yaml](.pre-commit-config.yaml): formatting/lint checks run before each commit, commit message format is checked on `commit-msg`.

## Workflow

1. Branch from `develop` (or `main` for a `hotfix/`): `git checkout -b feature/SW-118-shelf-stock-endpoint develop`.
2. Make focused commits following the [commit convention](Documentation/COMMIT_CONVENTION.md).
3. Keep the branch in sync: `git fetch && git rebase origin/develop`.
4. Before opening a PR, run the full local quality gate:
   - Backend: `./Backend/mvnw -f Backend/pom.xml verify`
   - Frontend: `ruff check . && black --check . && mypy . && pytest` (from `Frontend/`)
5. Open a PR into `develop` using the [PR template](.github/pull_request_template.md). Link the ticket.
6. Address review comments with new commits (don't force-push during review unless asked); squash-merge once approved and CI is green.

## Definition of Done for a pull request

- [ ] Code follows the relevant style guide (formatter/linter pass locally and in CI).
- [ ] New/changed behavior has tests (unit, and integration where it touches the database or another module).
- [ ] New/changed endpoints are documented per [API_GUIDELINES.md](Documentation/API_GUIDELINES.md) and visible in Swagger UI.
- [ ] No secrets, credentials or generated files committed.
- [ ] Commit messages and PR title follow [COMMIT_CONVENTION.md](Documentation/COMMIT_CONVENTION.md).
- [ ] At least one reviewer approved.

## Code review

- Reviewers check correctness, adherence to the style guides above, and test coverage — not personal formatting preference (the formatter already decided that).
- Author resolves conversations after addressing them; reviewer re-approves after the final push.
- Prefer small PRs (one ticket) over large ones — easier to review, easier to revert.
