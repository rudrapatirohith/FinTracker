from functools import lru_cache
from pathlib import Path
from pydantic_settings import BaseSettings, SettingsConfigDict

# Get the directory where config.py lives, then go 4 levels up to the root
ROOT_DIR = Path(__file__).resolve().parent.parent.parent.parent.parent
ENV_PATH = ROOT_DIR / ".env"


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=str(ENV_PATH),
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