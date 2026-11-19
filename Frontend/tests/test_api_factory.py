import pytest

from app.api import factory
from app.api.client import ApiClient
from app.api.factory import ApiMode, create_api
from app.api.mock import MockApiClient


def test_returns_mock_when_mode_is_mock() -> None:
    assert isinstance(create_api(ApiMode.MOCK), MockApiClient)


def test_returns_http_client_when_mode_is_http() -> None:
    assert isinstance(create_api(ApiMode.HTTP, "http://unreachable.invalid"), ApiClient)


def test_auto_falls_back_to_mock_when_backend_unreachable(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    monkeypatch.setattr(factory, "is_backend_reachable", lambda url: False)
    assert isinstance(create_api(ApiMode.AUTO), MockApiClient)


def test_auto_uses_backend_when_reachable(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setattr(factory, "is_backend_reachable", lambda url: True)
    assert isinstance(create_api(ApiMode.AUTO), ApiClient)
