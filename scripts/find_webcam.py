"""Print the index of the first camera that delivers a real (non-black) picture.

OpenCV numbers cameras in its own order. On a Mac with an iPhone nearby,
camera 0 is often the iPhone (Continuity Camera), which opens but sends only
black frames until it is in use; the built-in camera is then 1. Run with the
Device venv, which has OpenCV:

    Device/.venv/bin/python scripts/find_webcam.py
"""

from __future__ import annotations

import os
import sys
import time

# OpenCV probes the Orbbec depth-camera SDK and prints its log for every index.
os.environ.setdefault("OPENCV_LOG_LEVEL", "ERROR")

import cv2  # noqa: E402 - after the log level is set

MAX_INDEX = 4
SAMPLE_S = 2.0
# Mean pixel value (0-255) above which a frame is a picture, not darkness.
MIN_BRIGHTNESS = 5.0


def brightness(index: int) -> float | None:
    camera = cv2.VideoCapture(index)
    if not camera.isOpened():
        return None
    best = 0.0
    deadline = time.monotonic() + SAMPLE_S
    try:
        while time.monotonic() < deadline:
            ok, frame = camera.read()
            if ok:
                best = max(best, float(frame.mean()))
                if best > MIN_BRIGHTNESS:
                    break
    finally:
        camera.release()
    return best


def main() -> None:
    for index in range(MAX_INDEX):
        value = brightness(index)
        if value is None:
            break
        verdict = "picture" if value > MIN_BRIGHTNESS else "black"
        print(f"camera {index}: {verdict} (brightness {value:.0f})", file=sys.stderr)
        if value > MIN_BRIGHTNESS:
            print(index)
            return
    sys.exit("no camera delivers a picture: is one connected and allowed?")


if __name__ == "__main__":
    main()
