import os
from unittest.mock import patch

from app.config import Settings


def test_settings_development_defaults() -> None:
    settings = Settings()
    assert settings.APP_NAME == "supply-chain-ml-service"
    assert settings.APP_VERSION == "0.1.0"
    assert settings.APP_ENV == "development"
    assert settings.APP_PORT == 8000
    assert settings.LOG_LEVEL == "INFO"


def test_settings_environment_overrides() -> None:
    with patch.dict(
        os.environ,
        {
            "APP_NAME": "custom-ml-service",
            "APP_ENV": "production",
            "APP_PORT": "9000",
            "LOG_LEVEL": "DEBUG",
        },
    ):
        settings = Settings()
        assert settings.APP_NAME == "custom-ml-service"
        assert settings.APP_ENV == "production"
        assert settings.APP_PORT == 9000
        assert settings.LOG_LEVEL == "DEBUG"
