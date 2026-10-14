"""Capture triggers. The agent depends only on :class:`Trigger`."""

from shelf_agent.trigger.base import Trigger
from shelf_agent.trigger.interval import IntervalTrigger
from shelf_agent.trigger.manual import ManualTrigger

__all__ = ["IntervalTrigger", "ManualTrigger", "Trigger"]
