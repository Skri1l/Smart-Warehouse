# 0010. One labelled test-image set, shared by the emulator and the ML tests

Status: Proposed
Date: 2026-10-07

## Context

The team has a first set of 25 shelf photos with known product counts. They are needed in two places with different purposes:

- **ML accuracy tests** (PROJECT_DOCUMENTATION.md §12: "fixed validation image set", "accuracy measurement"). These check that the recognizer counts correctly.
- **The device emulator** ([ADR-0008](0008-shelf-agent-shared-core.md)). It replays the photos as shelf events so the whole device → backend → ML → stock → UI path can be shown and tested end to end.

If each consumer keeps its own copy and its own labels, they drift: a photo gets relabelled in one place, and the demo "expects" a different count than the tests measure.

The backend derives stock changes from consecutive frames of one shelf, so the order in which the emulator sends the photos matters as much as the labels.

## Decision

We will keep the images and their ground truth once, as data rather than code:

- `images/` holds the photos, named `<shelf>_<step>.jpg` so that sorting by name gives the order in which the shelf content changed.
- `labels.csv` has the columns `file,sku,count`, one row per image. It describes the *images*, not any particular scenario.
- A scenario (`scenario.toml`) only says which shelf sends which images in which order. It points to the shared `labels.csv` and adds no labels of its own.

The emulator attaches the label to each frame and prints it next to the upload (`expected MILK-1L x5`), but **ground truth is never sent to the backend**. The ML service must not be able to see the answer. The ML accuracy tests will read the same `labels.csv`.

The set starts in `Device/scenarios/demo/` because the emulator is its first consumer. When the ML service is scaffolded (Milestone 5), the ML tests reference that folder. If more consumers appear, the set moves to a neutral top-level location in one commit.

## Alternatives considered

- **Labels inside the scenario file**: rejected. They would tie ground truth to one demo script, and the ML tests would need to parse emulator config.
- **Labels encoded in file names (`milk_6.jpg`)**: rejected. Renaming a file to fix a label breaks scenario references. It also doesn't scale to several products per image.
- **Send the expected count to the backend as a debug field**: rejected. It risks leaking into recognition logic or making a demo look more accurate than it is.
- **Separate datasets for the demo and the tests**: rejected. Twice the labelling work, and the drift described above.

## Consequences

- Labelling happens once, and fixing a label fixes the demo and the accuracy figure together.
- The presenter sees expected vs. actual during the demo without the system "cheating".
- Photos used to *measure* accuracy must not also be used to *train* the model. If the model is fine-tuned, the training images need a separate set (or a held-out split recorded in `labels.csv`).
- Images are committed to the repository and must stay under the 5 MB pre-commit limit per file (downscale to about 1600 px). If the set grows beyond tens of MB, move it to Git LFS. That would be a new ADR.
- `labels.csv` currently supports one SKU per image. Shelves holding several products need a format change (e.g. one row per SKU per file), coordinated with the ML service contract.
