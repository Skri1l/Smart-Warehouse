from __future__ import annotations

from collections.abc import Callable
from pathlib import Path

import pytest

from shelf_agent.config import ConfigError
from shelf_agent.emulator.scenario import load_labels, load_scenario
from shelf_agent.models import GroundTruth

DEVICE_ROOT = Path(__file__).parents[1]

SCENARIO = """
labels = "labels.csv"

[backend]
base_url = "http://127.0.0.1:8080"

[[shelves]]
name = "A1"
device_id = 1
shelf_id = 1
frames = ["a_*.jpg"]

[[shelves]]
name = "B1"
device_id = 2
shelf_id = 2
device_key = "key-b1"
frames = ["b_02.jpg", "b_01.jpg"]
loop = true
"""

LABELS = """file,sku,count
# comment lines are skipped
a_01.jpg,MILK-1L,6
a_02.jpg,MILK-1L,5
"""


@pytest.fixture
def scenario_file(tmp_path: Path, write_images: Callable[..., list[Path]]) -> Path:
    write_images("a_02.jpg", "a_01.jpg", "b_01.jpg", "b_02.jpg")
    (tmp_path / "labels.csv").write_text(LABELS, encoding="utf-8")
    path = tmp_path / "scenario.toml"
    path.write_text(SCENARIO, encoding="utf-8")
    return path


def test_expands_globs_sorted_and_keeps_explicit_order(scenario_file: Path) -> None:
    scenario = load_scenario(scenario_file)

    assert [p.name for p in scenario.shelf("A1").frames] == ["a_01.jpg", "a_02.jpg"]
    assert [p.name for p in scenario.shelf("B1").frames] == ["b_02.jpg", "b_01.jpg"]


def test_reads_identity_and_defaults_device_key(scenario_file: Path) -> None:
    scenario = load_scenario(scenario_file)

    assert scenario.shelf("A1").identity.device_key == "emulator-A1"
    assert scenario.shelf("B1").identity.device_key == "key-b1"
    assert scenario.shelf("B1").loop is True


def test_loads_labels_by_file_name(scenario_file: Path) -> None:
    scenario = load_scenario(scenario_file)

    assert scenario.labels["a_01.jpg"] == GroundTruth("MILK-1L", 6)


def test_fails_when_listed_image_is_missing(scenario_file: Path) -> None:
    scenario_file.write_text(SCENARIO.replace("b_02.jpg", "nope.jpg"), encoding="utf-8")

    with pytest.raises(ConfigError, match="nope.jpg"):
        load_scenario(scenario_file)


def test_fails_when_glob_matches_nothing(scenario_file: Path) -> None:
    scenario_file.write_text(SCENARIO.replace("a_*.jpg", "z_*.jpg"), encoding="utf-8")

    with pytest.raises(ConfigError, match="matched no files"):
        load_scenario(scenario_file)


def test_fails_when_two_shelves_share_a_device(scenario_file: Path) -> None:
    text = SCENARIO.replace("device_id = 2", "device_id = 1")
    scenario_file.write_text(text, encoding="utf-8")

    with pytest.raises(ConfigError, match="duplicate shelf device_id"):
        load_scenario(scenario_file)


def test_fails_when_label_count_is_not_a_number(tmp_path: Path) -> None:
    path = tmp_path / "labels.csv"
    path.write_text("file,sku,count\na.jpg,MILK,many\n", encoding="utf-8")

    with pytest.raises(ConfigError, match="a.jpg"):
        load_labels(path)


def test_fails_when_label_column_is_missing(tmp_path: Path) -> None:
    path = tmp_path / "labels.csv"
    path.write_text("file,count\na.jpg,1\n", encoding="utf-8")

    with pytest.raises(ConfigError, match="sku"):
        load_labels(path)


def test_bundled_smoke_scenario_is_valid() -> None:
    scenario = load_scenario(DEVICE_ROOT / "scenarios" / "smoke" / "scenario.toml")

    assert [s.name for s in scenario.shelves] == ["A1", "B1"]
    assert all(
        frame.name in scenario.labels for s in scenario.shelves for frame in s.frames
    )


def test_device_id_is_optional_and_registration_on_by_default(
    scenario_file: Path,
) -> None:
    scenario_file.write_text(
        SCENARIO.replace("device_id = 1\n", "").replace("device_id = 2\n", ""),
        encoding="utf-8",
    )

    scenario = load_scenario(scenario_file)

    assert scenario.shelf("A1").identity.device_id is None
    assert scenario.register_devices is True


def test_fails_when_two_shelves_share_a_device_key(scenario_file: Path) -> None:
    text = SCENARIO.replace('device_key = "key-b1"', 'device_key = "emulator-A1"')
    scenario_file.write_text(text, encoding="utf-8")

    with pytest.raises(ConfigError, match="duplicate shelf device_key"):
        load_scenario(scenario_file)


WEBCAM_SHELVES = """
[webcam]
width = 640
preview = false

[[shelves]]
name = "W1"
shelf_id = 5
webcam = 1

[[shelves]]
name = "W2"
shelf_id = 6
webcam = true
"""


def test_webcam_shelves_have_a_camera_index_instead_of_frames(
    scenario_file: Path,
) -> None:
    scenario_file.write_text(SCENARIO + WEBCAM_SHELVES, encoding="utf-8")

    scenario = load_scenario(scenario_file)

    assert (scenario.shelf("W1").webcam, scenario.shelf("W1").frames) == (1, ())
    assert scenario.shelf("W2").webcam == 0
    assert scenario.shelf("A1").webcam is None
    assert (scenario.webcam.width, scenario.webcam.height) == (640, 720)
    assert scenario.webcam.preview is False
    assert scenario.webcam.capture_dir == scenario_file.parent / ".captures"


def test_shelf_cannot_have_both_webcam_and_frames(scenario_file: Path) -> None:
    scenario_file.write_text(
        SCENARIO.replace('frames = ["a_*.jpg"]', 'frames = ["a_*.jpg"]\nwebcam = 0'),
        encoding="utf-8",
    )

    with pytest.raises(ConfigError, match="either 'webcam' or 'frames'"):
        load_scenario(scenario_file)


def test_bundled_webcam_scenario_is_valid() -> None:
    scenario = load_scenario(DEVICE_ROOT / "scenarios" / "webcam" / "scenario.toml")

    assert [(s.name, s.webcam) for s in scenario.shelves] == [("W1", 0)]
    assert scenario.backend.protocol == "device-images"
