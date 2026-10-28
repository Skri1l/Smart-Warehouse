"""Does detection confidence tell a bad photo from a good one?

Runs the production detector on the labelled photos and on degraded copies
of them (blur, darkness, noise, occlusion, ...), then reports how well
confidence and simple image statistics predict a wrong count.
See Documentation/analysis/VISION_CONFIDENCE.md for the results.

Usage (from Vision/):
    python experiments/confidence.py           # run the model (~15 min on CPU), then report
    python experiments/confidence.py --report  # report from the saved results only
"""
import csv
import json
import os
import sys
from collections import defaultdict

import cv2
import numpy as np

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from detect_yolo_tiled import BASE_DIR, detect, load_model  # noqa: E402


DATA_DIR = os.path.join(BASE_DIR, "tests", "data")
OUTPUT_DIR = os.path.join(BASE_DIR, "outputs", "confidence")
RESULTS_PATH = os.path.join(OUTPUT_DIR, "results.json")

# A box below this confidence counts as "uncertain".
UNCERTAIN = 0.30

# The retake rule being evaluated.
MIN_MEAN_CONFIDENCE = 0.50
MIN_BRIGHTNESS = 40
MAX_BRIGHTNESS = 220


def motion_blur(image, size):
    kernel = np.zeros((size, size), np.float32)
    kernel[size // 2, :] = 1.0 / size
    return cv2.filter2D(image, -1, kernel)


def gamma(image, value):
    lut = np.array([((i / 255.0) ** value) * 255 for i in range(256)], np.uint8)
    return cv2.LUT(image, lut)


def noise(image, sigma, seed):
    rng = np.random.default_rng(seed)
    noisy = image.astype(np.float32) + rng.normal(0, sigma, image.shape)
    return np.clip(noisy, 0, 255).astype(np.uint8)


def low_resolution(image, factor):
    h, w = image.shape[:2]
    small = cv2.resize(image, (w // factor, h // factor), interpolation=cv2.INTER_AREA)
    return cv2.resize(small, (w, h), interpolation=cv2.INTER_LINEAR)


def jpeg(image, quality):
    _, buffer = cv2.imencode(".jpg", image, [cv2.IMWRITE_JPEG_QUALITY, quality])
    return cv2.imdecode(buffer, cv2.IMREAD_COLOR)


def occlude(image, share, seed):
    """A skin-coloured ellipse over ``share`` of the frame, like a hand."""
    rng = np.random.default_rng(seed)
    h, w = image.shape[:2]
    bw, bh = int(w * np.sqrt(share)), int(h * np.sqrt(share))
    x = int(rng.integers(0, w - bw))
    y = int(rng.integers(0, h - bh))

    result = image.copy()
    cv2.ellipse(result, (x + bw // 2, y + bh // 2), (bw // 2, bh // 2),
                0, 0, 360, (120, 150, 200), -1)
    return result


def glare(image):
    h, w = image.shape[:2]
    yy, xx = np.mgrid[0:h, 0:w]
    radius = min(h, w) * 0.35
    mask = np.clip(1 - np.hypot(xx - w * 0.55, yy - h * 0.4) / radius, 0, 1)
    bright = image.astype(np.float32) + mask[..., None] * 230
    return np.clip(bright, 0, 255).astype(np.uint8)


VARIANTS = {
    "original": lambda image, seed: image,
    "blur_g15": lambda image, seed: cv2.GaussianBlur(image, (15, 15), 0),
    "blur_g41": lambda image, seed: cv2.GaussianBlur(image, (41, 41), 0),
    "motion_31": lambda image, seed: motion_blur(image, 31),
    "dark_g2.5": lambda image, seed: gamma(image, 2.5),
    "dark_g4": lambda image, seed: gamma(image, 4.0),
    "bright_x2.5": lambda image, seed: cv2.convertScaleAbs(image, alpha=2.5, beta=40),
    "noise_40": lambda image, seed: noise(image, 40, seed),
    "lowres_8": lambda image, seed: low_resolution(image, 8),
    "jpeg_q5": lambda image, seed: jpeg(image, 5),
    "occlude_25": lambda image, seed: occlude(image, 0.25, seed),
    "glare": lambda image, seed: glare(image),
}


def load_labels():
    with open(os.path.join(DATA_DIR, "labels.csv"), newline="", encoding="utf-8") as f:
        rows = csv.DictReader(line for line in f if not line.startswith("#"))
        return [(r["file"], int(r["count"])) for r in rows]


def run():
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    model = load_model()
    results = []

    for seed, (file, truth) in enumerate(load_labels()):
        image = cv2.imread(os.path.join(DATA_DIR, "images", file))

        for name, degrade in VARIANTS.items():
            degraded = degrade(image, seed)
            gray = cv2.cvtColor(degraded, cv2.COLOR_BGR2GRAY)

            results.append({
                "file": file,
                "variant": name,
                "truth": truth,
                "confidences": sorted(
                    (d["confidence"] for d in detect(model, degraded)),
                    reverse=True
                ),
                "sharpness": float(cv2.Laplacian(gray, cv2.CV_64F).var()),
                "brightness": float(gray.mean()),
            })

        print(f"{file}: done", flush=True)

    with open(RESULTS_PATH, "w", encoding="utf-8") as f:
        json.dump(results, f)

    return results


def add_features(r):
    c = np.array(r["confidences"])
    r["count"] = len(c)
    r["error"] = len(c) - r["truth"]
    r["mean"] = float(c.mean()) if len(c) else 0.0
    r["p10"] = float(np.percentile(c, 10)) if len(c) else 0.0
    r["uncertain"] = float((c < UNCERTAIN).mean()) if len(c) else 1.0


def auroc(scores, wrong):
    """Chance that a wrong run scores lower than a correct one (0.5 = useless)."""
    scores, wrong = np.array(scores), np.array(wrong, bool)
    bad, good = scores[wrong], scores[~wrong]
    if not len(bad) or not len(good):
        return float("nan")
    return float(np.mean([(b < g) + 0.5 * (b == g) for b in bad for g in good]))


def retake(r):
    return (r["mean"] < MIN_MEAN_CONFIDENCE
            or not MIN_BRIGHTNESS <= r["brightness"] <= MAX_BRIGHTNESS)


def report(results):
    for r in results:
        add_features(r)

    by_variant = defaultdict(list)
    for r in results:
        by_variant[r["variant"]].append(r)

    print("| Variant | Exact | Mean error | Mean conf | p10 conf | Uncertain | Brightness | Retake: wrong caught | Retake: correct flagged |")
    print("|---|---|---|---|---|---|---|---|---|")
    for name, rs in by_variant.items():
        errors = np.array([r["error"] for r in rs])
        wrong = [r for r in rs if r["error"]]
        right = [r for r in rs if not r["error"]]
        print(f"| {name} | {np.mean(errors == 0):.0%} | {errors.mean():+.1f} "
              f"| {np.mean([r['mean'] for r in rs]):.2f} | {np.mean([r['p10'] for r in rs]):.2f} "
              f"| {np.mean([r['uncertain'] for r in rs]):.0%} | {np.mean([r['brightness'] for r in rs]):.0f} "
              f"| {sum(map(retake, wrong))}/{len(wrong)} | {sum(map(retake, right))}/{len(right)} |")

    for tolerance in (0, 2):
        wrong = [abs(r["error"]) > tolerance for r in results]
        print(f"\nAUROC, wrong = |error| > {tolerance} ({sum(wrong)}/{len(results)} runs)")
        for key, sign in (("mean", 1), ("p10", 1), ("uncertain", -1), ("sharpness", 1)):
            print(f"  {key:10} {auroc([sign * r[key] for r in results], wrong):.2f}")

    print("\nOriginal photos: lowest mean confidence "
          f"{min(r['mean'] for r in by_variant['original']):.2f}, "
          f"lowest p10 {min(r['p10'] for r in by_variant['original']):.2f}")

    missed = [r for r in results if r["error"] and not retake(r)]
    print(f"Wrong runs the retake rule misses: {len(missed)}, errors {sorted(r['error'] for r in missed)}")


def main():
    if "--report" in sys.argv:
        with open(RESULTS_PATH, encoding="utf-8") as f:
            results = json.load(f)
    else:
        results = run()

    report(results)


if __name__ == "__main__":
    main()
