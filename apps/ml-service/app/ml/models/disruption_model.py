from pathlib import Path
from typing import Any, Dict, List, Optional, Union
import joblib
import numpy as np
import pandas as pd

from app.core.errors import ModelNotAvailableException
from app.ml.features.pipeline import FeaturePipeline
from app.ml.models.base import BaseDisruptionModel
from app.schemas.prediction import DisruptionPredictionRequest


class TrainedDisruptionModel(BaseDisruptionModel):
    """Concrete implementation of BaseDisruptionModel wrapping a serialized scikit-learn pipeline & estimator."""

    def __init__(self, model_path: Optional[Path] = None) -> None:
        self._is_loaded = False
        self._preprocessor: Optional[FeaturePipeline] = None
        self._model: Optional[Any] = None
        self._threshold: float = 0.50
        self._version: str = "none"
        self._raw_feature_names: List[str] = []

        if model_path is not None:
            self.load(model_path)

    def is_available(self) -> bool:
        """Check if model artifact is loaded and ready for inference."""
        return self._is_loaded and self._model is not None

    def load(self, model_path: Optional[Path] = None) -> None:
        """Load the model artifact bundle from disk."""
        if model_path is None or not Path(model_path).exists():
            raise ModelNotAvailableException(f"Model artifact not found at {model_path}")

        bundle: Dict[str, Any] = joblib.load(model_path)
        self._preprocessor = bundle["preprocessor"]
        self._model = bundle["model"]
        self._threshold = float(bundle.get("threshold", 0.50))
        self._version = str(bundle.get("model_version", "disruption-baseline-v1"))
        self._raw_feature_names = list(bundle.get("raw_feature_names", []))
        self._is_loaded = True

    def predict_proba(self, features: Union[np.ndarray, pd.DataFrame]) -> np.ndarray:
        """Generate predicted disruption probabilities for given input features."""
        if not self.is_available():
            raise ModelNotAvailableException("Disruption model is not loaded.")

        if isinstance(features, pd.DataFrame):
            X = self._preprocessor.transform(features)
        else:
            X = np.asarray(features, dtype=np.float64)

        return self._model.predict_proba(X)

    def predict(self, features: Union[np.ndarray, pd.DataFrame]) -> np.ndarray:
        """Generate binary disruption class predictions using optimal probability threshold."""
        probabilities = self.predict_proba(features)
        pos_probs = probabilities[:, 1] if probabilities.ndim > 1 else probabilities
        return (pos_probs >= self._threshold).astype(int)

    def predict_request(self, request: DisruptionPredictionRequest) -> Dict[str, Any]:
        """Convenience method to execute end-to-end inference directly on a validated API request."""
        if not self.is_available():
            raise ModelNotAvailableException("Disruption model is not loaded.")

        # Convert Pydantic request to 1-row DataFrame containing all raw features
        row = {
            "hist_otdr_90d": float(request.hist_otdr_90d),
            "hist_avg_delay_90d": float(request.hist_avg_delay_90d),
            "hist_fulfillment_rate_90d": float(request.hist_fulfillment_rate_90d),
            "hist_disruptions_90d": int(request.hist_disruptions_90d),
            "supplier_lead_time_contract": int(request.supplier_lead_time_contract),
            "material_criticality": request.material_criticality.value if hasattr(request.material_criticality, "value") else str(request.material_criticality),
            "order_volume_ratio": float(request.order_volume_ratio),
            "inventory_coverage_days": float(request.inventory_coverage_days),
            "po_line_value": float(request.po_line_value),
            "supplier_country": str(request.supplier_country),
        }
        df = pd.DataFrame([row])
        X = self._preprocessor.transform(df)
        prob = float(self._model.predict_proba(X)[0, 1])
        label = int(1 if prob >= self._threshold else 0)

        return {
            "disruption_probability": prob,
            "predicted_label": label,
            "threshold": self._threshold,
            "model_version": self._version,
        }

    @property
    def threshold(self) -> float:
        return self._threshold

    @property
    def model_version(self) -> str:
        return self._version
