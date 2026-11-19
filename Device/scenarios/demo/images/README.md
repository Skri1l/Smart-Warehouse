# Demo test photos

Put the labelled shelf photos here. One file per shot, named
`<shelf>_<step>.jpg`, for example:

```text
shelf-a2_01.jpg   6 cartons
shelf-a2_02.jpg   5 cartons   (one taken)
shelf-a2_03.jpg   3 cartons   (two more taken)
shelf-b1_01.jpg   ...
```

- **Order matters.** The scenario sends each shelf's photos sorted by name. The backend computes the stock change from consecutive frames, so number the steps in the order the shelf content changed.
- **Label every photo** in [`../labels.csv`](../labels.csv) (`file,sku,count`). The emulator prints the expected value next to each upload, and the ML accuracy tests read the same file.
- **Include a few hard cases**: a blurred shot, an empty shelf, a foreign product. They show the low-confidence → manual-review flow (FR-08).
- **Keep files small.** The repository's pre-commit hook rejects files over 5 MB. Downscale to about 1600 px on the long side (≈300–600 KB JPEG) before committing.
