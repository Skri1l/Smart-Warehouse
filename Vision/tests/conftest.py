from __future__ import annotations

import csv
from dataclasses import dataclass
from pathlib import Path

import pytest

DATA_DIR = Path(__file__).parent / "data"
IMAGES_DIR = DATA_DIR / "images"
LABELS_FILE = DATA_DIR / "labels.csv"


@dataclass(frozen=True)
class LabelledImage:
    file: str
    sku: str
    count: int

    @property
    def path(self) -> Path:
        return IMAGES_DIR / self.file


def load_labels() -> list[LabelledImage]:
    with LABELS_FILE.open(newline="", encoding="utf-8") as f:
        rows = csv.DictReader(line for line in f if not line.startswith("#"))
        return [LabelledImage(r["file"], r["sku"], int(r["count"])) for r in rows]


@pytest.fixture(scope="session")
def model():
    """The production YOLO model, loaded once for the whole test run."""
    pytest.importorskip("ultralytics")
    from detect_yolo_tiled import load_model

    return load_model()
