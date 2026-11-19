# Runbooks

A runbook is an operational how-to for a specific symptom or alert: what to check, in what order, and how to mitigate — written so that whoever is on call (or defending the diploma demo) can follow it under time pressure without first understanding the whole system.

Runbooks live next to the service they operate, not in `Documentation/`, because they need to stay next to the code/config they reference and change on the same PRs. `Frontend/runbooks/` should follow the same [template](template.md) once the frontend is a deployed, operated service.

This is deliberately narrower than a how-to guide: a how-to guide teaches a user task; a runbook is written for whoever is firefighting a specific failure, assumes they're already under pressure, and optimizes for "what do I do right now" over explanation.

## Index

| Symptom / alert | Runbook |
|---|---|
| Image recognition pipeline stuck or failing | [image-recognition-pipeline-degraded.md](image-recognition-pipeline-degraded.md) |

## Process

1. Copy [template.md](template.md) for a new symptom/alert.
2. Keep steps concrete and copy-pasteable (exact commands, exact log queries) — not "check if the service is healthy."
3. Update the runbook whenever an incident reveals a step that was missing or wrong; that update is part of the incident's follow-up, not optional cleanup.
