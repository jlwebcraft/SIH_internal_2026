from typing import Optional

from app.config import Settings, get_settings
from app.ml.inference.pipeline import InferencePipeline
from app.schemas.health import ReadinessResponse
from app.schemas.prediction import DisruptionPredictionRequest, DisruptionPredictionResponse


class PredictionService:
    """Application service for disruption predictions and ML model readiness checks."""

    def __init__(
        self,
        pipeline: Optional[InferencePipeline] = None,
        settings: Optional[Settings] = None,
    ) -> None:
        self.pipeline = pipeline or InferencePipeline()
        self.settings = settings or get_settings()

    def get_readiness(self) -> ReadinessResponse:
        """Evaluate readiness status distinguishing service initialization and model availability."""
        model_ready = self.pipeline.is_model_available()
        details = (
            "ML model loaded and ready for inference"
            if model_ready
            else "Service initialized; ML model artifact not loaded (Phase 7C foundation)"
        )
        return ReadinessResponse(
            status="READY",
            service=self.settings.APP_NAME,
            version=self.settings.APP_VERSION,
            model_available=model_ready,
            details=details,
        )

    def predict(self, request: DisruptionPredictionRequest) -> DisruptionPredictionResponse:
        """Run disruption inference on a validated procurement prediction request."""
        return self.pipeline.predict(request)
