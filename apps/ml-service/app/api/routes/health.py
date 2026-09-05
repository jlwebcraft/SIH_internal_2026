from fastapi import APIRouter, Depends

from app.config import Settings, get_settings
from app.schemas.health import HealthResponse, ReadinessResponse
from app.services.prediction_service import PredictionService

router = APIRouter(tags=["Health & Status"])


def get_prediction_service() -> PredictionService:
    return PredictionService()


@router.get(
    "/health",
    response_model=HealthResponse,
    summary="Health check",
    description="Check the liveness and running status of the ML service.",
)
def get_health(settings: Settings = Depends(get_settings)) -> HealthResponse:
    return HealthResponse(
        status="UP",
        service=settings.APP_NAME,
        version=settings.APP_VERSION,
    )


@router.get(
    "/ready",
    response_model=ReadinessResponse,
    summary="Readiness check",
    description="Check if the ML service is ready to accept traffic and whether an ML model artifact is loaded.",
)
def get_readiness(
    service: PredictionService = Depends(get_prediction_service),
) -> ReadinessResponse:
    return service.get_readiness()
