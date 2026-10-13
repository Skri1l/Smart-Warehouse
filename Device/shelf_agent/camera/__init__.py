"""Image sources. The agent depends only on :class:`CameraSource`."""

from shelf_agent.camera.base import CameraSource, SourceExhausted
from shelf_agent.camera.folder import FolderSource

__all__ = ["CameraSource", "FolderSource", "SourceExhausted"]
