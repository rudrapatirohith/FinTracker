import logging

from fastapi import APIRouter, HTTPException, status
from sqlalchemy import text
from sqlalchemy.exc import SQLAlchemyError

from app.db.session import engine

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/health", tags=["Health"])


@router.get("/live")
def liveness_check() -> dict[str, str]:
    """Confirms that the API process is running; it does not depend on the DB."""
    return {"status": "alive"}


@router.get("/ready")
def readiness_check() -> dict[str, str]:
    """Confirms that the API can reach PostgreSQL before accepting work."""
    try:
        with engine.connect() as connection:
            connection.execute(text("SELECT 1"))
    except SQLAlchemyError:
        # Keep internal connection details out of the public response.
        logger.exception("Database readiness check failed")
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Database is unavailable",
        )

    return {"status": "ready", "database": "connected"}
