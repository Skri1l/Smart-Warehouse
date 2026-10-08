"""Wiring for a real shelf device: config -> camera, trigger, client, agent.

Hardware modules are imported only for the ``type`` that is configured, so the
same entry point runs on a PC with ``camera.type = "folder"``.
"""

from __future__ import annotations

import logging
import threading

from shelf_agent.agent import ShelfAgent, run_heartbeat
from shelf_agent.camera.base import CameraSource
from shelf_agent.camera.folder import FolderSource
from shelf_agent.config import CameraConfig, ConfigError, DeviceConfig, TriggerConfig
from shelf_agent.registration import connect
from shelf_agent.transport.client import BackendClient
from shelf_agent.transport.outbox import Outbox
from shelf_agent.trigger.base import Trigger
from shelf_agent.trigger.interval import IntervalTrigger
from shelf_agent.trigger.manual import ManualTrigger

logger = logging.getLogger(__name__)

IMAGE_SUFFIXES = {".jpg", ".jpeg", ".png"}


def build_camera(config: CameraConfig) -> CameraSource:
    """Create the configured image source."""
    if config.type == "picamera":
        from shelf_agent.camera.picamera import Picamera2Source

        if config.capture_dir is None:
            raise ConfigError("[camera] capture_dir is required for picamera")
        return Picamera2Source(config.width, config.height, config.capture_dir)
    if config.type == "folder":
        if config.folder is None or not config.folder.is_dir():
            raise ConfigError(f"[camera] folder does not exist: {config.folder}")
        images = sorted(
            p for p in config.folder.iterdir() if p.suffix.lower() in IMAGE_SUFFIXES
        )
        return FolderSource(images, loop=config.loop)
    raise ConfigError(f"unknown camera type: {config.type!r}")


def build_trigger(config: TriggerConfig) -> Trigger:
    """Create the configured capture trigger."""
    if config.type == "gpio":
        from shelf_agent.trigger.gpio import GpioTrigger

        return GpioTrigger(config.pin, settle_s=config.settle_s)
    if config.type == "interval":
        return IntervalTrigger(config.interval_s)
    if config.type == "manual":
        return ManualTrigger()
    raise ConfigError(f"unknown trigger type: {config.type!r}")


def run_device(config: DeviceConfig, stop: threading.Event) -> None:
    """Run one shelf agent until ``stop`` is set or its source is exhausted."""
    client = BackendClient(config.backend, config.identity)
    if not connect(
        client,
        register=config.register,
        stop=stop,
        retry_s=config.heartbeat_interval_s,
    ):
        client.close()
        return
    agent = ShelfAgent(
        name=f"shelf-{config.identity.shelf_id}",
        camera=build_camera(config.camera),
        client=client,
        outbox=Outbox(config.outbox_dir),
    )
    trigger = build_trigger(config.trigger)
    heartbeat = threading.Thread(
        target=run_heartbeat,
        args=([agent], config.heartbeat_interval_s, stop),
        name="heartbeat",
        daemon=True,
    )
    logger.info(
        "device %s (shelf %s) -> %s [%s]",
        client.identity.device_id,
        config.identity.shelf_id,
        config.backend.base_url,
        config.backend.protocol,
    )
    heartbeat.start()
    try:
        agent.run(trigger, stop)
    finally:
        stop.set()
        heartbeat.join(timeout=config.backend.timeout_s + 1)
        trigger.close()
        agent.close()
