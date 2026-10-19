from __future__ import annotations

from collections.abc import Callable
from datetime import UTC, datetime
from pathlib import Path

import pytest

from shelf_agent.config import (
    PROTOCOL_DEVICE_IMAGES,
    PROTOCOL_MULTIPART,
    BackendConfig,
    DeviceIdentity,
)
from shelf_agent.models import Frame, GroundTruth

CAPTURED_AT = datetime(2026, 10, 7, 12, 30, 0, tzinfo=UTC)


@pytest.fixture
def identity() -> DeviceIdentity:
    return DeviceIdentity(device_id=7, shelf_id=3, device_key="test-key")


@pytest.fixture
def backend_config() -> BackendConfig:
    """Default protocol: ``image-events`` as published in Swagger."""
    return BackendConfig(base_url="http://backend.test", timestamp_zone="utc")


@pytest.fixture
def multipart_config() -> BackendConfig:
    """RFC-0001 multipart upload."""
    return BackendConfig(base_url="http://backend.test", protocol=PROTOCOL_MULTIPART)


@pytest.fixture
def device_images_config() -> BackendConfig:
    """Image upload to ``POST /devices/{id}/images`` (recognition + stock)."""
    return BackendConfig(
        base_url="http://backend.test",
        protocol=PROTOCOL_DEVICE_IMAGES,
        timestamp_zone="utc",
    )


@pytest.fixture
def make_frame() -> Callable[..., Frame]:
    def factory(
        name: str = "shot.jpg",
        data: bytes = b"\xff\xd8jpeg",
        truth: GroundTruth | None = None,
        source_path: Path | None = Path("/data/captures/shot.jpg"),
    ) -> Frame:
        return Frame(
            data=data,
            filename=name,
            content_type="image/jpeg",
            captured_at=CAPTURED_AT,
            ground_truth=truth,
            source_path=source_path,
        )

    return factory


@pytest.fixture
def write_images(tmp_path: Path) -> Callable[..., list[Path]]:
    """Create fake image files (content = file name) in ``tmp_path/images``."""

    def factory(*names: str) -> list[Path]:
        folder = tmp_path / "images"
        folder.mkdir(exist_ok=True)
        paths = []
        for name in names:
            path = folder / name
            path.write_bytes(name.encode())
            paths.append(path)
        return paths

    return factory
