"""Scenario files: which virtual shelves exist and which images each one sends.

A scenario is a TOML file next to an ``images/`` folder and a ``labels.csv``
with the ground truth of every image (see ADR-0010 and Device/README.md).
"""

from __future__ import annotations

import csv
import tomllib
from collections.abc import Mapping, Sequence
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from shelf_agent.config import (
    BackendConfig,
    ConfigError,
    DeviceIdentity,
    parse_backend,
    resolve_path,
)
from shelf_agent.models import GroundTruth

_GLOB_CHARS = set("*?[")
LABEL_COLUMNS = ("file", "sku", "count")


@dataclass(frozen=True)
class EmulatedShelf:
    """One virtual shelf device."""

    name: str
    identity: DeviceIdentity
    frames: tuple[Path, ...]
    loop: bool = False


@dataclass(frozen=True)
class Scenario:
    """A complete emulator run."""

    backend: BackendConfig
    shelves: tuple[EmulatedShelf, ...]
    labels: Mapping[str, GroundTruth]
    outbox_dir: Path
    interval_s: float = 10.0
    heartbeat_interval_s: float = 30.0
    register_devices: bool = True

    def shelf(self, name: str) -> EmulatedShelf:
        for shelf in self.shelves:
            if shelf.name == name:
                return shelf
        raise KeyError(name)


def load_scenario(path: Path) -> Scenario:
    """Load and validate a scenario file.

    Raises:
        ConfigError: If the file is invalid, an image is missing, or two
            shelves claim the same device or shelf id.
    """
    try:
        with path.open("rb") as file:
            raw = tomllib.load(file)
    except FileNotFoundError as exc:
        raise ConfigError(f"scenario not found: {path}") from exc
    except tomllib.TOMLDecodeError as exc:
        raise ConfigError(f"{path}: {exc}") from exc

    base_dir = path.parent
    images_dir = resolve_path(base_dir, raw.get("images_dir", "images"))
    labels_file = raw.get("labels")
    labels = load_labels(resolve_path(base_dir, labels_file)) if labels_file else {}

    shelves_raw = raw.get("shelves")
    if not isinstance(shelves_raw, list) or not shelves_raw:
        raise ConfigError("scenario needs at least one [[shelves]] entry")
    shelves = tuple(_parse_shelf(item, images_dir) for item in shelves_raw)
    _check_unique(shelves)

    if "backend" not in raw:
        raise ConfigError("[backend] section is required")
    return Scenario(
        backend=parse_backend(raw["backend"]),
        shelves=shelves,
        labels=labels,
        outbox_dir=resolve_path(base_dir, raw.get("outbox_dir", ".outbox")),
        interval_s=float(raw.get("interval_s", 10.0)),
        heartbeat_interval_s=float(raw.get("heartbeat_interval_s", 30.0)),
        register_devices=bool(raw.get("register_devices", True)),
    )


def load_labels(path: Path) -> dict[str, GroundTruth]:
    """Read ``labels.csv`` (columns ``file,sku,count``) into a dict by file name.

    The same file is meant to drive the ML service's accuracy tests, so it
    describes the images, not the scenario.
    """
    try:
        with path.open(newline="", encoding="utf-8") as file:
            reader = csv.DictReader(
                row for row in file if row.strip() and not row.startswith("#")
            )
            missing = set(LABEL_COLUMNS) - set(reader.fieldnames or ())
            if missing:
                raise ConfigError(f"{path}: missing column(s) {sorted(missing)}")
            labels: dict[str, GroundTruth] = {}
            for row in reader:
                try:
                    count = int(row["count"])
                except ValueError as exc:
                    raise ConfigError(
                        f"{path}: count of '{row['file']}' must be an integer"
                    ) from exc
                labels[row["file"].strip()] = GroundTruth(row["sku"].strip(), count)
            return labels
    except FileNotFoundError as exc:
        raise ConfigError(f"labels file not found: {path}") from exc


def _parse_shelf(item: Any, images_dir: Path) -> EmulatedShelf:
    if not isinstance(item, Mapping):
        raise ConfigError("each [[shelves]] entry must be a table")
    name = str(item.get("name") or "")
    if not name:
        raise ConfigError("every shelf needs a 'name'")
    try:
        identity = DeviceIdentity(
            device_id=int(item["device_id"]) if "device_id" in item else None,
            shelf_id=int(item["shelf_id"]),
            device_key=str(item.get("device_key") or f"emulator-{name}"),
        )
    except KeyError as exc:
        raise ConfigError(f"shelf '{name}': {exc.args[0]} is required") from exc
    frames = _expand_frames(name, item.get("frames"), images_dir)
    return EmulatedShelf(name, identity, frames, loop=bool(item.get("loop", False)))


def _expand_frames(name: str, patterns: Any, images_dir: Path) -> tuple[Path, ...]:
    """Resolve frame entries; an entry with ``*``/``?``/``[`` is a sorted glob."""
    if not isinstance(patterns, Sequence) or isinstance(patterns, str) or not patterns:
        raise ConfigError(f"shelf '{name}': 'frames' must be a non-empty list")
    frames: list[Path] = []
    for pattern in map(str, patterns):
        if _GLOB_CHARS & set(pattern):
            matches = sorted(p for p in images_dir.glob(pattern) if p.is_file())
            if not matches:
                raise ConfigError(
                    f"shelf '{name}': '{pattern}' matched no files in {images_dir}"
                )
            frames.extend(matches)
        else:
            path = images_dir / pattern
            if not path.is_file():
                raise ConfigError(f"shelf '{name}': image not found: {path}")
            frames.append(path)
    return tuple(frames)


def _check_unique(shelves: Sequence[EmulatedShelf]) -> None:
    columns: dict[str, list[object]] = {
        "name": [s.name for s in shelves],
        "device_key": [s.identity.device_key for s in shelves],
        "shelf_id": [s.identity.shelf_id for s in shelves],
        "device_id": [s.identity.device_id for s in shelves],
    }
    for attribute, values in columns.items():
        seen: set[object] = set()
        for value in values:
            if value is None:
                continue
            if value in seen:
                raise ConfigError(f"duplicate shelf {attribute}: {value}")
            seen.add(value)
