from __future__ import annotations

from detect_yolo_tiled import coverage, remove_group_boxes


def box(x1, y1, x2, y2):
    return {"bbox": (x1, y1, x2, y2), "class_name": "soda can", "confidence": 0.5}


def test_coverage_of_box_inside_another_is_full():
    assert coverage((2, 2, 4, 4), (0, 0, 10, 10)) == 1.0


def test_coverage_of_disjoint_boxes_is_zero():
    assert coverage((0, 0, 1, 1), (5, 5, 6, 6)) == 0.0


def test_box_spanning_two_cans_is_removed():
    left, right = box(0, 0, 10, 20), box(10, 0, 20, 20)
    group = box(0, 0, 20, 20)

    assert remove_group_boxes([left, right, group]) == [left, right]


def test_overlapping_neighbours_are_kept():
    front, behind = box(0, 10, 10, 30), box(2, 0, 12, 20)

    assert remove_group_boxes([front, behind]) == [front, behind]
