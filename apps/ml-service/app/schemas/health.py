from typing import Optional
from pydantic import BaseModel, Field


class HealthResponse(BaseModel):
    """Health check response schema."""

    status: str = Field(default="UP", description="Current liveness status of the service")
    service: str = Field(..., description="Service identifier")
    version: str = Field(..., description="Application version")


class ReadinessResponse(BaseModel):
    """Readiness probe response schema."""

    status: str = Field(default="READY", description="Current readiness status")
    service: str = Field(..., description="Service identifier")
    version: str = Field(..., description="Application version")
    model_available: bool = Field(default=False, description="Flag indicating if a trained ML model artifact is loaded")
    details: Optional[str] = Field(None, description="Diagnostic details on model and component readiness")
