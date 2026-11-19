"""Plain data types passed between the agent's ports (camera, transport, outbox)."""

from __future__ import annotations

import uuid
from dataclasses import dataclass, field
from datetime import UTC, datetime
from pathlib import Path


def utc_now() -> datetime:
    """Current time as a timezone-aware UTC datetime."""
    return datetime.now(UTC)


@dataclass(frozen=True)
class GroundTruth:
    """Expected recognition outcome of a labeled test image.

    Used only for logging and verification on the emulator side. It is never
    sent to the backend (see ADR-0010).
    """

    sku: str
    count: int


@dataclass(frozen=True)
class Frame:
    """One captured shelf image, ready to be delivered to the backend.

    Attributes:
        data: Encoded image bytes (JPEG/PNG).
        filename: File name reported in the multipart upload.
        content_type: MIME type of ``data``.
        captured_at: Moment of capture, timezone-aware. Preserved across
            retries so the backend orders events by capture, not by delivery.
        event_id: Client-generated id; lets the backend deduplicate retried
            uploads (at-least-once delivery, see ADR-0009).
        ground_truth: Label of a test image, ``None`` for real camera frames.
        source_path: Where the image is stored on the device. Sent as
            ``imagePath`` by the ``image-events`` protocol.
    """

    data: bytes
    filename: str
    content_type: str
    captured_at: datetime
    event_id: str = field(default_factory=lambda: str(uuid.uuid4()))
    ground_truth: GroundTruth | None = None
    source_path: Path | None = None
