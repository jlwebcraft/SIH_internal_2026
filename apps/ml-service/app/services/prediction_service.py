from typing import Optional
from pathlib import Path

from app.config import Settings, get_settings
from app.ml.inference.pipeline import InferencePipeline
from app.ml.models.base import UntrainedDisruptionModel
from app.ml.models.disruption_model import TrainedDisruptionModel
from app.schemas.health import ReadinessResponse
from app.schemas.prediction import DisruptionPredictionRequest, DisruptionPredictionResponse


class PredictionService:
    """Application service for disruption predictions and ML model readiness checks."""

    def __init__(
        self,
        pipeline: Optional[InferencePipeline] = None,
        settings: Optional[Settings] = None,
    ) -> None:
        self.settings = settings or get_settings()

        if pipeline is not None:
            self.pipeline = pipeline
        else:
            model_artifact = self.settings.MODEL_DIR / "disruption_model_v1.joblib"
            if model_artifact.exists():
                model = TrainedDisruptionModel(model_path=model_artifact)
                self.pipeline = InferencePipeline(model=model)
            else:
                self.pipeline = InferencePipeline(model=UntrainedDisruptionModel())

    def get_readiness(self) -> ReadinessResponse:
        """Evaluate readiness status distinguishing service initialization and model availability."""
        model_ready = self.pipeline.is_model_available()
        details = (
            f"ML model artifact loaded and ready for inference ({self.pipeline.get_model_version()})"
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
