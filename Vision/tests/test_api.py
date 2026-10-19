"""HTTP contract of POST /recognize, with the YOLO model replaced by a fake."""

from __future__ import annotations

import cv2
import numpy as np
import pytest
from fastapi.testclient import TestClient

import api


def make_detection(x1, y1, x2, y2):
    return {"bbox": (x1, y1, x2, y2), "class_name": "soda can", "confidence": 0.9}


@pytest.fixture
def client(monkeypatch):
    detections = [make_detection(0, 0, 10, 10), make_detection(20, 0, 30, 10)]

    monkeypatch.setattr(api, "load_model", lambda: object())
    monkeypatch.setattr(
        api,
        "detect_products_tiled",
        lambda image, model: (image, detections),
    )

    with TestClient(api.app) as test_client:
        yield test_client


def jpeg_bytes():
    ok, encoded = cv2.imencode(".jpg", np.zeros((8, 8, 3), dtype=np.uint8))
    assert ok
    return encoded.tobytes()


def test_returns_detected_count(client):
    response = client.post(
        "/recognize", files={"file": ("shelf.jpg", jpeg_bytes(), "image/jpeg")}
    )

    assert response.status_code == 200
    assert response.json() == {"count": 2}


def test_rejects_empty_file(client):
    response = client.post("/recognize", files={"file": ("e.jpg", b"", "image/jpeg")})

    assert response.status_code == 400


def test_rejects_undecodable_image(client):
    response = client.post(
        "/recognize", files={"file": ("x.jpg", b"not an image", "image/jpeg")}
    )

    assert response.status_code == 400


def test_rejects_oversized_file(client):
    payload = b"\0" * (api.MAX_FILE_SIZE + 1)
    response = client.post("/recognize", files={"file": ("big.jpg", payload)})

    assert response.status_code == 413
