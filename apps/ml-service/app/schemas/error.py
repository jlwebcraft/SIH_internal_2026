from datetime import datetime, timezone
from typing import Any, Optional
from pydantic import BaseModel, Field


class ErrorResponse(BaseModel):
    """Standardized API error response schema."""

    timestamp: datetime = Field(
        default_factory=lambda: datetime.now(timezone.utc),
        description="UTC timestamp when error occurred",
    )
    status: int = Field(..., description="HTTP status code")
    error: str = Field(..., description="High-level error classification")
    message: str = Field(..., description="Human-readable error description")
    path: Optional[str] = Field(None, description="Request path that triggered the error")
    details: Optional[Any] = Field(None, description="Detailed validation or constraint violation information")
