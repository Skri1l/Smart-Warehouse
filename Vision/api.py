from contextlib import asynccontextmanager
from threading import Lock

import cv2
import numpy as np
from fastapi import FastAPI, HTTPException, UploadFile
from ultralytics import YOLO

from detect_yolo_tiled import detect_products_tiled


model_lock = Lock()
MAX_FILE_SIZE = 10 * 1024 * 1024


@asynccontextmanager
async def lifespan(app: FastAPI):
    app.state.model = YOLO("yolov8x.pt")
    yield
    del app.state.model


app = FastAPI(lifespan=lifespan)


@app.post("/recognize")
def recognize(file: UploadFile):
    try:
        contents = file.file.read(MAX_FILE_SIZE + 1)
    finally:
        file.file.close()

    if len(contents) > MAX_FILE_SIZE:
        raise HTTPException(413, "Image exceeds 10 MB")

    if not contents:
        raise HTTPException(400, "Empty file")

    image = cv2.imdecode(
        np.frombuffer(contents, dtype=np.uint8),
        cv2.IMREAD_COLOR
    )

    if image is None:
        raise HTTPException(400, "Cannot decode image")

    with model_lock:
        _, detections = detect_products_tiled(
            image,
            app.state.model
        )

    return {"count": len(detections)}