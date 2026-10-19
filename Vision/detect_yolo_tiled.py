import os
import sys

import cv2
import numpy as np
from ultralytics import YOLOWorld


BASE_DIR = os.path.dirname(os.path.abspath(__file__))

OUTPUT_DIR = os.path.join(
    BASE_DIR,
    "outputs",
    "detect_yolo_tiled"
)

# COCO-trained YOLO has no "can" class: it saw cans as low-confidence
# "bottle"/"cup" and counted 1 of 25 labelled photos correctly. YOLO-World
# is open-vocabulary, so the product is described with a text prompt.
MODEL_PATH = "yolov8x-worldv2.pt"
PRODUCT_PROMPTS = ["soda can"]
CONFIDENCE = 0.10
IMAGE_SIZE = 1280
NMS_IOU = 0.5

# A box that covers most of two or more other boxes is a whole group of
# cans detected as one object, not another can.
GROUP_COVERAGE = 0.8


def load_model(model_path=MODEL_PATH):
    model = YOLOWorld(model_path)
    model.set_classes(PRODUCT_PROMPTS)
    return model


def coverage(inner, outer):
    """Share of the ``inner`` box area that lies inside ``outer``."""
    x1 = max(inner[0], outer[0])
    y1 = max(inner[1], outer[1])
    x2 = min(inner[2], outer[2])
    y2 = min(inner[3], outer[3])

    intersection = max(0, x2 - x1) * max(0, y2 - y1)
    area = (inner[2] - inner[0]) * (inner[3] - inner[1])

    return intersection / area if area > 0 else 0.0


def remove_group_boxes(detections):
    result = []

    for d in detections:
        covered = sum(
            1 for other in detections
            if other is not d
            and coverage(other["bbox"], d["bbox"]) >= GROUP_COVERAGE
        )

        if covered < 2:
            result.append(d)

    return result


def detect(model, image):
    results = model.predict(
        image,
        conf=CONFIDENCE,
        iou=NMS_IOU,
        imgsz=IMAGE_SIZE,
        agnostic_nms=True,
        verbose=False
    )[0]

    detections = []

    if results.boxes is None:
        return detections

    boxes = results.boxes.xyxy.cpu().numpy()
    confs = results.boxes.conf.cpu().numpy()
    classes = results.boxes.cls.cpu().numpy().astype(int)

    for box, conf, cls_id in zip(boxes, confs, classes):
        x1, y1, x2, y2 = box.astype(int)

        detections.append({
            "bbox": (x1, y1, x2, y2),
            "class_name": results.names[cls_id],
            "confidence": float(conf)
        })

    return remove_group_boxes(detections)


def detect_products_tiled(image_path, model):
    if isinstance(image_path, np.ndarray):
        image = image_path
    else:
        image = cv2.imread(image_path)

    if image is None:
        raise ValueError("Cannot read image")

    return image, detect(model, image)


def draw_detections(image, detections):
    result = image.copy()

    for i, d in enumerate(detections, start=1):
        x1, y1, x2, y2 = d["bbox"]
        label = f'{i}: {d["class_name"]} {d["confidence"]:.2f}'

        cv2.rectangle(result, (x1, y1), (x2, y2), (0, 255, 0), 2)

        cv2.putText(
            result,
            label,
            (x1, max(y1 - 5, 20)),
            cv2.FONT_HERSHEY_SIMPLEX,
            0.45,
            (0, 255, 0),
            1
        )

    return result


def main():
    """Usage: python detect_yolo_tiled.py <image or folder>"""
    if len(sys.argv) != 2:
        sys.exit(main.__doc__)

    source = sys.argv[1]

    if os.path.isdir(source):
        image_paths = [
            os.path.join(source, file)
            for file in sorted(os.listdir(source))
            if file.lower().endswith((".png", ".jpg", ".jpeg"))
        ]
    else:
        image_paths = [source]

    os.makedirs(OUTPUT_DIR, exist_ok=True)

    model = load_model()

    for image_path in image_paths:
        image, detections = detect_products_tiled(image_path, model)
        output_image = draw_detections(image, detections)

        image_name = os.path.basename(image_path)
        name_without_ext = os.path.splitext(image_name)[0]
        output_path = os.path.join(
            OUTPUT_DIR,
            f"{name_without_ext}_detected.png"
        )

        cv2.imwrite(output_path, output_image)

        print(f"{image_name}: detected {len(detections)} products")

    print("Done.")


if __name__ == "__main__":
    main()
