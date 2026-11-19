"""ML accuracy on the labelled photos (ADR-0010).

Runs the production model, so it is slow on CPU (several seconds per photo).
Skip with ``pytest -m "not slow"``.
"""

from __future__ import annotations

import pytest

from detect_yolo_tiled import detect_products_tiled
from tests.conftest import load_labels

LABELS = load_labels()

pytestmark = pytest.mark.slow


@pytest.mark.parametrize("label", LABELS, ids=[label.file for label in LABELS])
def test_counts_products_on_labelled_photo(model, label):
    _, detections = detect_products_tiled(str(label.path), model)

    assert len(detections) == label.count
