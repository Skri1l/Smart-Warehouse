from __future__ import annotations

from pathlib import Path

import pytest

from shelf_agent.config import (
    DEFAULT_HEARTBEAT_PATH,
    DEFAULT_UPLOAD_PATH,
    ConfigError,
    load_device_config,
)
from shelf_agent.device import build_camera, build_trigger
from shelf_agent.trigger.interval import IntervalTrigger

MINIMAL = """
[backend]
base_url = "http://pi-backend:8080/"

[device]
device_id = 1
shelf_id = 2
"""


def write(tmp_path: Path, text: str) -> Path:
    path = tmp_path / "device.toml"
    path.write_text(text, encoding="utf-8")
    return path


def test_reads_device_key_from_environment(tmp_path: Path) -> None:
    config = load_device_config(write(tmp_path, MINIMAL), env={"SW_DEVICE_KEY": "k1"})

    assert config.identity.device_key == "k1"
    assert config.identity.device_id == 1
    assert config.identity.shelf_id == 2


def test_applies_defaults_when_optional_sections_missing(tmp_path: Path) -> None:
    config = load_device_config(write(tmp_path, MINIMAL), env={"SW_DEVICE_KEY": "k"})

    assert config.backend.base_url == "http://pi-backend:8080"
    assert config.backend.upload_path == DEFAULT_UPLOAD_PATH
    assert config.backend.heartbeat_path == DEFAULT_HEARTBEAT_PATH
    assert config.camera.type == "picamera"
    assert config.trigger.type == "interval"
    assert config.outbox_dir == (tmp_path / ".outbox").resolve()


def test_fails_when_device_key_is_missing(tmp_path: Path) -> None:
    with pytest.raises(ConfigError, match="SW_DEVICE_KEY"):
        load_device_config(write(tmp_path, MINIMAL), env={})


def test_fails_when_device_id_is_not_an_integer(tmp_path: Path) -> None:
    text = MINIMAL.replace("device_id = 1", 'device_id = "one"')

    with pytest.raises(ConfigError, match="device_id"):
        load_device_config(write(tmp_path, text), env={"SW_DEVICE_KEY": "k"})


def test_fails_when_file_does_not_exist(tmp_path: Path) -> None:
    with pytest.raises(ConfigError, match="not found"):
        load_device_config(tmp_path / "nope.toml", env={})


def test_example_config_parses() -> None:
    example = Path(__file__).parents[1] / "config" / "device.example.toml"

    config = load_device_config(example, env={"SW_DEVICE_KEY": "k"})

    assert config.trigger.type == "gpio"
    assert config.camera.type == "picamera"


def test_folder_camera_runs_device_entry_point_on_a_pc(tmp_path: Path) -> None:
    images = tmp_path / "imgs"
    images.mkdir()
    (images / "b.jpg").write_bytes(b"b")
    (images / "a.png").write_bytes(b"a")
    (images / "notes.txt").write_text("not an image")
    text = MINIMAL + '\n[camera]\ntype = "folder"\nfolder = "imgs"\n'
    config = load_device_config(write(tmp_path, text), env={"SW_DEVICE_KEY": "k"})

    camera = build_camera(config.camera)

    assert [camera.capture().filename for _ in range(2)] == ["a.png", "b.jpg"]


def test_unknown_hardware_type_is_a_config_error(tmp_path: Path) -> None:
    text = MINIMAL + '\n[trigger]\ntype = "laser"\n'
    config = load_device_config(write(tmp_path, text), env={"SW_DEVICE_KEY": "k"})

    with pytest.raises(ConfigError, match="laser"):
        build_trigger(config.trigger)


def test_builds_interval_trigger_without_hardware(tmp_path: Path) -> None:
    config = load_device_config(write(tmp_path, MINIMAL), env={"SW_DEVICE_KEY": "k"})

    assert isinstance(build_trigger(config.trigger), IntervalTrigger)


def test_uses_swagger_image_events_protocol_by_default(tmp_path: Path) -> None:
    config = load_device_config(write(tmp_path, MINIMAL), env={"SW_DEVICE_KEY": "k"})

    assert config.backend.protocol == "image-events"
    assert config.backend.image_events_path == "/image-events"
    assert config.backend.timestamp_zone == "local"
    assert config.register is False
    assert config.camera.capture_dir == (tmp_path / ".captures").resolve()


def test_device_id_is_optional_because_backend_generates_it(tmp_path: Path) -> None:
    text = MINIMAL.replace("device_id = 1\n", "")

    config = load_device_config(write(tmp_path, text), env={"SW_DEVICE_KEY": "k"})

    assert config.identity.device_id is None


def test_fails_when_protocol_is_unknown(tmp_path: Path) -> None:
    text = MINIMAL.replace("[device]", 'protocol = "mqtt"\n\n[device]')

    with pytest.raises(ConfigError, match="protocol"):
        load_device_config(write(tmp_path, text), env={"SW_DEVICE_KEY": "k"})
