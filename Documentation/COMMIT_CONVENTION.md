# Commit Message Convention

Smart Warehouse follows [Conventional Commits](https://www.conventionalcommits.org/) v1.0.0. This keeps history searchable, lets tooling generate changelogs automatically, and makes the intent of every change obvious without opening the diff.

## Format

```text
<type>(<scope>): <subject>

<body>

<footer>
```

Only `<type>` and `<subject>` are mandatory.

### Type

| Type | When to use |
|---|---|
| `feat` | A new user-facing feature or capability |
| `fix` | A bug fix |
| `docs` | Documentation only changes |
| `style` | Formatting, whitespace, missing semicolons — no code logic change |
| `refactor` | Code change that neither fixes a bug nor adds a feature |
| `perf` | Performance improvement |
| `test` | Adding or correcting tests |
| `build` | Changes to build system or dependencies (Maven, pip, Docker) |
| `ci` | Changes to CI configuration/scripts |
| `chore` | Everything else (tooling, repo maintenance) |
| `revert` | Reverts a previous commit |

### Scope

The affected component, lower-case, one word:

`backend`, `frontend`, `docs`, `ci`, `infra`, or a narrower module name once they exist (e.g. `inventory`, `auth`, `ml`). Omit the scope if the change is repo-wide.

### Subject

- Imperative mood, as if finishing the sentence "This commit will …": `add`, not `added`/`adds`.
- No capital letter at the start, no period at the end.
- Max ~72 characters.

### Body (optional)

- Explains **why**, not what (the diff already shows what). Use when the reasoning isn't obvious from the subject.
- Wrap at ~72 characters per line.

### Footer (optional)

- Ticket reference: `Refs: SW-118`
- Breaking change: start a paragraph with `BREAKING CHANGE:` followed by a description of the impact and migration.

## Examples

```text
feat(backend): add shelf stock endpoint

Refs: SW-118
```

```text
fix(frontend): correct pagination offset on stock list

The offset was computed from the current page index instead of the
previous one, causing the last row of every page to be skipped.

Refs: SW-142
```

```text
docs: describe branching and commit conventions
```

```text
build(backend): bump spring-boot-starter-parent to 4.1.1
```

```text
feat(backend): change stock movement DTO to use ISO-8601 timestamps

BREAKING CHANGE: `createdAt` is now a string in ISO-8601 format
instead of an epoch millisecond number. Update any frontend code that
parses this field.

Refs: SW-160
```

## Enforcement

- Commit messages are validated locally by the `commitizen` [pre-commit](../.pre-commit-config.yaml) hook on the `commit-msg` stage.
- PR titles are checked the same way, since `feature/*` → `develop` merges use squash-merge and the PR title becomes the final commit message (see [BRANCHING.md](BRANCHING.md)).

## Practical rules

- One logical change per commit. Do not bundle an unrelated fix with a feature.
- Never commit generated files, build output (`target/`, `__pycache__/`), IDE settings, or secrets — see the relevant `.gitignore`.
- Do not commit directly to `main` or `develop`; commit on a branch and open a PR.
