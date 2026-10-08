"""Field-of-view calculator for the Smart Warehouse demo shelf.

Checks whether a camera mounted above the front edge of a shelf sees the whole
product volume (a W x D floor area plus products up to height h), and searches
for the lowest mounting height that works. All numbers in SHELF_DEVICE.md come
from this script, so re-run it whenever the shelf, the product envelope or the
camera changes.

Coordinate system (millimetres):
    x - across the shelf, 0 = centre line, +x = right when facing the shelf
    y - into the shelf, 0 = front edge of the working area, +y = towards the back
    z - up, 0 = top surface of the shelf floor

The camera looks towards +y and is pitched down by `pitch` degrees from the
horizontal. The sensor's long side is parallel to x. A simple pinhole model is
used; the real lens has some barrel distortion, which the default 5 % edge
margin absorbs.

Usage:
    python shelf_fov.py                      # report for the reference design
    python shelf_fov.py --width 600 --depth 300 --height 180
"""

from __future__ import annotations

import argparse
import itertools
import math
from dataclasses import dataclass


@dataclass(frozen=True)
class Camera:
    name: str
    hfov_deg: float
    vfov_deg: float
    width_px: int
    height_px: int
    focal_mm: float
    f_number: float
    pixel_um: float


CAMERAS = {
    "cm3-wide": Camera("Raspberry Pi Camera Module 3 Wide (IMX708)", 102.0, 67.0, 4608, 2592, 2.75, 2.2, 1.4),
    "cm3": Camera("Raspberry Pi Camera Module 3 (IMX708)", 66.0, 41.0, 4608, 2592, 4.74, 1.8, 1.4),
    "v2": Camera("Raspberry Pi Camera Module v2 (IMX219)", 62.2, 48.8, 3280, 2464, 3.04, 2.0, 1.12),
    "ov5647": Camera("OV5647 5 MP clone", 53.5, 41.4, 2592, 1944, 3.6, 2.9, 1.4),
}

Vec = tuple[float, float, float]


def _sub(a: Vec, b: Vec) -> Vec:
    return (a[0] - b[0], a[1] - b[1], a[2] - b[2])


def _dot(a: Vec, b: Vec) -> float:
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]


def _axes(pitch_deg: float) -> tuple[Vec, Vec, Vec]:
    p = math.radians(pitch_deg)
    forward = (0.0, math.cos(p), -math.sin(p))
    up = (0.0, math.sin(p), math.cos(p))
    right = (1.0, 0.0, 0.0)
    return forward, up, right


def project(cam: Camera, cam_pos: Vec, pitch_deg: float, point: Vec) -> tuple[float, float, float]:
    """Return (u, v, depth): pixel coordinates (0,0 = top-left) and depth along the optical axis."""
    forward, up, right = _axes(pitch_deg)
    d = _sub(point, cam_pos)
    depth = _dot(d, forward)
    fx = (cam.width_px / 2) / math.tan(math.radians(cam.hfov_deg / 2))
    fy = (cam.height_px / 2) / math.tan(math.radians(cam.vfov_deg / 2))
    u = cam.width_px / 2 + fx * _dot(d, right) / depth
    v = cam.height_px / 2 - fy * _dot(d, up) / depth
    return u, v, depth


def volume_corners(width: float, depth: float, height: float) -> list[Vec]:
    return [(x, y, z) for x, y, z in itertools.product((-width / 2, width / 2), (0.0, depth), (0.0, height))]


def fits(cam: Camera, cam_pos: Vec, pitch_deg: float, corners: list[Vec], margin: float) -> bool:
    """True if every corner lands inside the frame, keeping `margin` (fraction of the half-frame) free at each edge."""
    forward, _, _ = _axes(pitch_deg)
    for c in corners:
        if _dot(_sub(c, cam_pos), forward) <= 0:
            return False
        u, v, _ = project(cam, cam_pos, pitch_deg, c)
        if abs(u - cam.width_px / 2) > (cam.width_px / 2) * (1 - margin):
            return False
        if abs(v - cam.height_px / 2) > (cam.height_px / 2) * (1 - margin):
            return False
    return True


def pitch_range(cam: Camera, cam_pos: Vec, corners: list[Vec], margin: float) -> tuple[float, float] | None:
    ok = [p / 4 for p in range(20 * 4, 89 * 4) if fits(cam, cam_pos, p / 4, corners, margin)]
    return (min(ok), max(ok)) if ok else None


def min_height(cam: Camera, cam_y: float, corners: list[Vec], margin: float) -> tuple[int, tuple[float, float]] | None:
    for h in range(100, 1500, 5):
        rng = pitch_range(cam, (0.0, cam_y, float(h)), corners, margin)
        if rng:
            return h, rng
    return None


def depth_of_field(cam: Camera, focus_mm: float, coc_px: float) -> tuple[float, float]:
    coc_mm = coc_px * cam.pixel_um / 1000
    hyperfocal = cam.focal_mm**2 / (cam.f_number * coc_mm)
    near = hyperfocal * focus_mm / (hyperfocal + focus_mm)
    far = math.inf if focus_mm >= hyperfocal else hyperfocal * focus_mm / (hyperfocal - focus_mm)
    return near, far


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--camera", choices=CAMERAS, default="cm3-wide")
    ap.add_argument("--width", type=float, default=400, help="working area width, mm")
    ap.add_argument("--depth", type=float, default=300, help="working area depth, mm")
    ap.add_argument("--height", type=float, default=180, help="max product height, mm")
    ap.add_argument("--cam-y", type=float, default=-30, help="lens position relative to the front edge, mm")
    ap.add_argument("--cam-z", type=float, default=400, help="lens height above the floor, mm")
    ap.add_argument("--pitch", type=float, default=60, help="camera pitch below horizontal, degrees")
    ap.add_argument("--focus", type=float, default=380, help="focus distance, mm")
    ap.add_argument("--margin", type=float, default=0.05, help="free frame edge, fraction of half-frame")
    args = ap.parse_args()

    cam = CAMERAS[args.camera]
    corners = volume_corners(args.width, args.depth, args.height)
    pos = (0.0, args.cam_y, args.cam_z)

    print(f"Working volume {args.width:.0f} x {args.depth:.0f} x {args.height:.0f} mm, margin {args.margin:.0%}")
    print("\nMinimum lens height per camera (lens at y = %.0f mm):" % args.cam_y)
    for key, c in CAMERAS.items():
        res = min_height(c, args.cam_y, corners, args.margin)
        txt = f"{res[0]} mm, pitch {res[1][0]:.1f}-{res[1][1]:.1f} deg" if res else "not reachable"
        print(f"  {key:9s} {c.name:45s} {txt}")

    print(f"\nChosen: {cam.name}, lens at y = {args.cam_y:.0f}, z = {args.cam_z:.0f}, pitch {args.pitch:.1f} deg")
    print(f"  fits: {fits(cam, pos, args.pitch, corners, args.margin)}")
    rng = pitch_range(cam, pos, corners, args.margin)
    print(f"  allowed pitch at this position: {rng[0]:.2f}-{rng[1]:.2f} deg" if rng else "  no pitch fits")
    hs = [h for h in range(200, 800, 5) if fits(cam, (0.0, args.cam_y, float(h)), args.pitch, corners, args.margin)]
    print(f"  allowed lens height at this pitch: {min(hs)}-{max(hs)} mm" if hs else "  no height fits")
    ys = [y for y in range(-150, 100) if fits(cam, (0.0, float(y), args.cam_z), args.pitch, corners, args.margin)]
    print(f"  allowed lens y at this pitch/height: {min(ys)}..{max(ys)} mm" if ys else "  no y fits")
    axis_hit = args.cam_y + args.cam_z / math.tan(math.radians(args.pitch))
    print(f"  optical axis meets the floor at y = {axis_hit:.0f} mm")

    worst = 1.0
    for dp, dz, dy in itertools.product((-3, 0, 3), (-10, 0, 10), (-10, 0, 10)):
        p = (0.0, args.cam_y + dy, args.cam_z + dz)
        if not fits(cam, p, args.pitch + dp, corners, args.margin):
            worst = 0.0
    print(f"  robust to pitch +-3 deg, height +-10 mm, y +-10 mm at once: {bool(worst)}")

    fx = (cam.width_px / 2) / math.tan(math.radians(cam.hfov_deg / 2))
    print("\n  point (x, y, z)            pixel (u, v)     range   elevation  px/mm")
    for x, y, z in itertools.product((-args.width / 2, 0.0), (0.0, args.depth / 2, args.depth), (0.0, args.height)):
        u, v, d = project(cam, pos, args.pitch, (x, y, z))
        vec = _sub((x, y, z), pos)
        rng_mm = math.sqrt(_dot(vec, vec))
        elev = math.degrees(math.asin(-vec[2] / rng_mm))
        print(f"  ({x:6.0f},{y:4.0f},{z:4.0f})   ({u:5.0f},{v:5.0f})   {rng_mm:5.0f}   {elev:6.1f}     {fx / d:4.1f}")

    for coc in (2, 4):
        near, far = depth_of_field(cam, args.focus, coc)
        print(f"\n  focus {args.focus:.0f} mm (LensPosition {1000 / args.focus:.2f} dioptres), "
              f"blur <= {coc} px: sharp from {near:.0f} to {far:.0f} mm")


if __name__ == "__main__":
    main()
