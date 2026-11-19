from __future__ import annotations

from pathlib import Path

import pytest

from tests.conftest import DATA_DIR, IMAGES_DIR, load_labels

DEVICE_DEMO_DIR = Path(__file__).parents[2] / "Device" / "scenarios" / "demo"


def test_every_image_is_labelled_exactly_once():
    labelled = [label.file for label in load_labels()]
    images = sorted(p.name for p in IMAGES_DIR.iterdir() if p.suffix == ".jpg")

    assert len(labelled) == len(set(labelled))
    assert sorted(labelled) == images


def test_counts_are_non_negative():
    assert all(label.count >= 0 for label in load_labels())


@pytest.mark.skipif(not DEVICE_DEMO_DIR.exists(), reason="Device folder absent")
def test_labels_match_device_emulator_copy():
    """The emulator replays the same photos; ground truth must not drift."""
    ours = (DATA_DIR / "labels.csv").read_bytes()
    theirs = (DEVICE_DEMO_DIR / "labels.csv").read_bytes()

    assert ours == theirs
