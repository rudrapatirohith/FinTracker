from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    # The .env file lives at the repository root. We run Uvicorn from /backend,
    # so ../.env points back to that root file.
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    app_name: str = "FinTracker API"
    app_env: str = "development"
    database_url: str
    frontend_origin: str = "http://localhost:3000"


@lru_cache
def get_settings() -> Settings:
    # Cache configuration so the application reads settings once per process.
    return Settings()


settings = get_settings()
