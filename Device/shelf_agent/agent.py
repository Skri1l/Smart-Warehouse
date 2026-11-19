"""The shelf agent: capture -> deliver, with ordered retries.

:class:`ShelfAgent` is shared by the Raspberry Pi and the emulator. It knows
nothing about real hardware or test folders: it receives a camera, a backend
client and an outbox, and is driven by a trigger (``run``) or step by step
(``tick``).
"""

from __future__ import annotations

import enum
import logging
import threading
from dataclasses import dataclass
from typing import Protocol

from shelf_agent.camera.base import CameraSource, SourceExhausted
from shelf_agent.models import Frame
from shelf_agent.transport.client import BackendError, UploadReceipt
from shelf_agent.transport.outbox import Outbox
from shelf_agent.trigger.base import Trigger

logger = logging.getLogger(__name__)


class Uploader(Protocol):
    """What the agent needs from the transport; :class:`BackendClient` implements it."""

    def upload_image(self, frame: Frame) -> UploadReceipt: ...

    def heartbeat(self) -> None: ...

    def close(self) -> None: ...


class Delivery(enum.Enum):
    """What happened to a captured frame."""

    SENT = "sent"
    QUEUED = "queued"
    REJECTED = "rejected"


@dataclass(frozen=True)
class TickResult:
    """Outcome of one :meth:`ShelfAgent.tick`."""

    frame: Frame
    delivery: Delivery
    receipt: UploadReceipt | None = None
    error: str | None = None


class ShelfAgent:
    """Captures shelf images and delivers them to the backend.

    Delivery is at-least-once and strictly in capture order: while anything is
    waiting in the outbox, new frames are queued behind it instead of being
    sent first, because the backend computes stock deltas from consecutive
    frames of the same shelf.

    Args:
        name: Human-readable shelf name used in logs.
        camera: Image source.
        client: Backend transport.
        outbox: Persistent queue for frames that could not be delivered yet.
    """

    def __init__(
        self, name: str, camera: CameraSource, client: Uploader, outbox: Outbox
    ) -> None:
        self.name = name
        self._camera = camera
        self._client = client
        self._outbox = outbox
        # tick() runs on the trigger thread, flush() also on the heartbeat thread.
        self._lock = threading.Lock()

    @property
    def queued(self) -> int:
        """Frames waiting in the outbox."""
        return len(self._outbox)

    def tick(self) -> TickResult:
        """Capture one frame and try to deliver it.

        Raises:
            SourceExhausted: If the camera is a finite source with no frames left.
        """
        frame = self._camera.capture()
        with self._lock:
            self._flush_locked()
            if len(self._outbox):
                self._outbox.put(frame)
                logger.warning(
                    "[%s] %s queued behind %d undelivered frame(s)",
                    self.name,
                    frame.filename,
                    len(self._outbox) - 1,
                )
                return TickResult(frame, Delivery.QUEUED)
            return self._deliver(frame)

    def flush(self) -> int:
        """Send queued frames in order until the queue is empty or a send fails.

        Returns:
            Number of frames delivered.
        """
        with self._lock:
            return self._flush_locked()

    def heartbeat(self) -> bool:
        """Send a heartbeat; failures are logged, never raised."""
        try:
            self._client.heartbeat()
        except BackendError as exc:
            logger.warning("[%s] heartbeat failed: %s", self.name, exc)
            return False
        return True

    def run(self, trigger: Trigger, stop: threading.Event) -> None:
        """Capture on every trigger until ``stop`` is set or the source runs out."""
        while trigger.wait(stop):
            try:
                self.tick()
            except SourceExhausted:
                logger.info("[%s] no more frames, stopping", self.name)
                return

    def close(self) -> None:
        self._camera.close()
        self._client.close()

    def _deliver(self, frame: Frame) -> TickResult:
        try:
            receipt = self._client.upload_image(frame)
        except BackendError as exc:
            if exc.retryable:
                self._outbox.put(frame)
                logger.warning(
                    "[%s] %s queued for retry: %s", self.name, frame.filename, exc
                )
                return TickResult(frame, Delivery.QUEUED, error=str(exc))
            self._outbox.add_rejected(frame)
            logger.error("[%s] %s rejected: %s", self.name, frame.filename, exc)
            return TickResult(frame, Delivery.REJECTED, error=str(exc))

        logger.info(
            "[%s] %s sent (imageEvent=%s)",
            self.name,
            frame.filename,
            receipt.image_event_id,
        )
        return TickResult(frame, Delivery.SENT, receipt=receipt)

    def _flush_locked(self) -> int:
        delivered = 0
        while (frame := self._outbox.peek()) is not None:
            try:
                self._client.upload_image(frame)
            except BackendError as exc:
                if exc.retryable:
                    logger.debug("[%s] outbox flush paused: %s", self.name, exc)
                    break
                logger.error("[%s] %s rejected: %s", self.name, frame.filename, exc)
                self._outbox.reject()
                continue
            self._outbox.pop()
            delivered += 1
        if delivered:
            logger.info("[%s] delivered %d queued frame(s)", self.name, delivered)
        return delivered


def run_heartbeat(
    agents: list[ShelfAgent], interval_s: float, stop: threading.Event
) -> None:
    """Send heartbeats and flush outboxes until ``stop`` is set.

    Flushing here means queued frames reach the backend as soon as it is back,
    even if no new shelf event happens.
    """
    while True:
        for agent in agents:
            if agent.heartbeat():
                agent.flush()
        if stop.wait(interval_s):
            return
