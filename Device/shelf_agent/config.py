"""Configuration of a real shelf device, loaded from a TOML file.

Secrets (the device key) are read from an environment variable rather than
the file, so a config can be committed as a template (NFR-03).
"""

from __future__ import annotations

import os
import tomllib
from collections.abc import Mapping
from dataclasses import dataclass
from pathlib import Path
from typing import Any

DEFAULT_UPLOAD_PATH = "/api/v1/device-events/images"
DEFAULT_HEARTBEAT_PATH = "/devices/{device_id}/heartbeat"
DEFAULT_DEVICE_KEY_ENV = "SW_DEVICE_KEY"


class ConfigError(ValueError):
    """Raised when a config or scenario file is missing data or is invalid."""


@dataclass(frozen=True)
class BackendConfig:
    """Where and how the agent reaches the backend.

    Attributes:
        base_url: Backend origin, e.g. ``http://192.168.1.10:8080``.
        upload_path: Image upload endpoint (RFC-0001).
        heartbeat_path: Heartbeat endpoint; ``{device_id}`` is substituted.
        timeout_s: Per-request timeout in seconds.
    """

    base_url: str
    upload_path: str = DEFAULT_UPLOAD_PATH
    heartbeat_path: str = DEFAULT_HEARTBEAT_PATH
    timeout_s: float = 10.0


@dataclass(frozen=True)
class DeviceIdentity:
    """How a device identifies itself to the backend (``Device`` entity)."""

    device_id: int
    shelf_id: int
    device_key: str


@dataclass(frozen=True)
class CameraConfig:
    """Image source. ``type`` is ``picamera`` on the device, ``folder`` on a PC."""

    type: str = "picamera"
    width: int = 2304
    height: int = 1296
    folder: Path | None = None
    loop: bool = False


@dataclass(frozen=True)
class TriggerConfig:
    """What starts a capture: ``gpio`` sensor, ``interval`` timer or ``manual``."""

    type: str = "interval"
    interval_s: float = 60.0
    pin: int = 17
    settle_s: float = 2.0


@dataclass(frozen=True)
class DeviceConfig:
    """Complete configuration of one shelf agent process."""

    backend: BackendConfig
    identity: DeviceIdentity
    camera: CameraConfig
    trigger: TriggerConfig
    outbox_dir: Path
    heartbeat_interval_s: float = 30.0


def load_device_config(
    path: Path, env: Mapping[str, str] | None = None
) -> DeviceConfig:
    """Load a device config file.

    Relative paths inside the file are resolved against the file's directory.

    Args:
        path: TOML config file.
        env: Environment to read the device key from; defaults to ``os.environ``.

    Returns:
        The parsed configuration.

    Raises:
        ConfigError: If a required value is missing or has the wrong type.
    """
    env = os.environ if env is None else env
    raw = _read_toml(path)
    base_dir = path.parent

    device = _table(raw, "device")
    camera = raw.get("camera", {})
    trigger = raw.get("trigger", {})
    folder = camera.get("folder")

    return DeviceConfig(
        backend=parse_backend(_table(raw, "backend")),
        identity=DeviceIdentity(
            device_id=_int(device, "device_id"),
            shelf_id=_int(device, "shelf_id"),
            device_key=_device_key(device, env),
        ),
        camera=CameraConfig(
            type=str(camera.get("type", "picamera")),
            width=int(camera.get("width", 2304)),
            height=int(camera.get("height", 1296)),
            folder=resolve_path(base_dir, folder) if folder else None,
            loop=bool(camera.get("loop", False)),
        ),
        trigger=TriggerConfig(
            type=str(trigger.get("type", "interval")),
            interval_s=float(trigger.get("interval_s", 60.0)),
            pin=int(trigger.get("pin", 17)),
            settle_s=float(trigger.get("settle_s", 2.0)),
        ),
        outbox_dir=resolve_path(base_dir, device.get("outbox_dir", ".outbox")),
        heartbeat_interval_s=float(device.get("heartbeat_interval_s", 30.0)),
    )


def parse_backend(table: Mapping[str, Any]) -> BackendConfig:
    """Build a :class:`BackendConfig` from a ``[backend]`` TOML table."""
    if "base_url" not in table:
        raise ConfigError("[backend] base_url is required")
    return BackendConfig(
        base_url=str(table["base_url"]).rstrip("/"),
        upload_path=str(table.get("upload_path", DEFAULT_UPLOAD_PATH)),
        heartbeat_path=str(table.get("heartbeat_path", DEFAULT_HEARTBEAT_PATH)),
        timeout_s=float(table.get("timeout_s", 10.0)),
    )


def resolve_path(base_dir: Path, value: str | Path) -> Path:
    """Resolve ``value`` against ``base_dir`` unless it is already absolute."""
    path = Path(value)
    return path if path.is_absolute() else (base_dir / path).resolve()


def _read_toml(path: Path) -> dict[str, Any]:
    try:
        with path.open("rb") as file:
            return tomllib.load(file)
    except FileNotFoundError as exc:
        raise ConfigError(f"config file not found: {path}") from exc
    except tomllib.TOMLDecodeError as exc:
        raise ConfigError(f"{path}: {exc}") from exc


def _table(raw: Mapping[str, Any], name: str) -> Mapping[str, Any]:
    table = raw.get(name)
    if not isinstance(table, Mapping):
        raise ConfigError(f"[{name}] section is required")
    return table


def _int(table: Mapping[str, Any], key: str) -> int:
    value = table.get(key)
    if not isinstance(value, int) or isinstance(value, bool):
        raise ConfigError(f"'{key}' must be an integer, got {value!r}")
    return value


def _device_key(device: Mapping[str, Any], env: Mapping[str, str]) -> str:
    env_name = str(device.get("device_key_env", DEFAULT_DEVICE_KEY_ENV))
    key = env.get(env_name) or device.get("device_key")
    if not key:
        raise ConfigError(
            f"device key is missing: set the {env_name} environment variable"
        )
    return str(key)
