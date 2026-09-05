from datetime import datetime, timezone
from typing import Optional

from app.core.errors import ModelNotAvailableException
from app.ml.features.transformer import FeatureTransformer
from app.ml.models.base import BaseDisruptionModel, UntrainedDisruptionModel
from app.schemas.prediction import DisruptionPredictionRequest, DisruptionPredictionResponse


class InferencePipeline:
    """Coordinates feature extraction, model inference, and output response creation."""

    def __init__(
        self,
        transformer: Optional[FeatureTransformer] = None,
        model: Optional[BaseDisruptionModel] = None,
    ) -> None:
        self.transformer = transformer or FeatureTransformer()
        self.model = model or UntrainedDisruptionModel()

    def is_model_available(self) -> bool:
        """Check whether the underlying ML model is loaded and ready for predictions."""
        return self.model.is_available()

    def get_model_version(self) -> str:
        """Get current model version identifier."""
        return self.model.model_version

    def predict(self, request: DisruptionPredictionRequest) -> DisruptionPredictionResponse:
        """Execute end-to-end inference on a single procurement request."""
        if not self.model.is_available():
            raise ModelNotAvailableException(
                "Disruption prediction ML model is not yet trained or loaded in this phase (Phase 7C foundation)."
            )

        if hasattr(self.model, "predict_request"):
            result = self.model.predict_request(request)
            prob = float(result["disruption_probability"])
            label = int(result["predicted_label"])
        else:
            # Fallback for generic BaseDisruptionModel implementations
            feature_matrix = self.transformer.transform(request)
            probabilities = self.model.predict_proba(feature_matrix)
            labels = self.model.predict(feature_matrix)
            prob = float(probabilities[0][1]) if probabilities.ndim > 1 else float(probabilities[0])
            label = int(labels[0])

        return DisruptionPredictionResponse(
            disruption_probability=round(prob, 4),
            predicted_label=label,
            risk_tier=self._derive_tier(prob),
            model_version=self.model.model_version,
            inference_timestamp=datetime.now(timezone.utc),
            confidence=None,  # Not fabricated without validated calibration
        )

    @staticmethod
    def _derive_tier(probability: float) -> str:
        if probability < 0.25:
            return "LOW"
        elif probability < 0.50:
            return "MEDIUM"
        elif probability < 0.75:
            return "HIGH"
        return "CRITICAL"
