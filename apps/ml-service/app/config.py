from functools import lru_cache
from pathlib import Path
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Centralized configuration for the ML service."""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    APP_NAME: str = "supply-chain-ml-service"
    APP_VERSION: str = "0.1.0"
    APP_ENV: str = "development"
    APP_HOST: str = "0.0.0.0"
    APP_PORT: int = 8000
    LOG_LEVEL: str = "INFO"
    MODEL_DIR: Path = Path("models")


@lru_cache
def get_settings() -> Settings:
    """Return a cached instance of the service settings."""
    return Settings()
