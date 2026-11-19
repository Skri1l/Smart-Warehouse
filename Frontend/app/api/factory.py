"""Chooses which :class:`WarehouseApi` implementation the UI talks to.

Views and viewmodels depend only on the ``WarehouseApi`` protocol; this module
is the single place that decides whether requests go to the real backend or
to the in-memory mock.
"""

import logging
from enum import StrEnum

import httpx

from app.api.client import DEFAULT_BASE_URL, ApiClient, WarehouseApi
from app.api.mock import MockApiClient

log = logging.getLogger(__name__)


class ApiMode(StrEnum):
    """How the API implementation is selected."""

    HTTP = "http"
    """Always use the real backend."""
    MOCK = "mock"
    """Always use in-memory mock data."""
    AUTO = "auto"
    """Use the backend if it responds, otherwise fall back to the mock."""


def is_backend_reachable(base_url: str, timeout: float = 2.0) -> bool:
    """Return True if anything answers HTTP at ``base_url``."""
    try:
        httpx.get(base_url, timeout=timeout)
    except httpx.HTTPError:
        return False
    return True


def create_api(
    mode: ApiMode = ApiMode.AUTO, base_url: str = DEFAULT_BASE_URL
) -> WarehouseApi:
    """Build the API implementation for the given mode.

    Args:
        mode: Selection strategy, see :class:`ApiMode`.
        base_url: Backend address used by the HTTP implementation.

    Returns:
        An object implementing :class:`WarehouseApi`.
    """
    if mode is ApiMode.MOCK:
        return MockApiClient()
    if mode is ApiMode.AUTO and not is_backend_reachable(base_url):
        log.warning("Backend at %s is unreachable, using mock API", base_url)
        return MockApiClient()
    return ApiClient(base_url)
