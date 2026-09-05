from typing import ClassVar, List
import numpy as np

from app.core.errors import FeatureTransformationException
from app.schemas.prediction import DisruptionPredictionRequest, MaterialCriticality


class FeatureTransformer:
    """Deterministic feature transformer converting raw validated prediction requests into numerical feature matrices."""

    CRITICALITY_ENCODING: ClassVar[dict[MaterialCriticality, float]] = {
        MaterialCriticality.LOW: 1.0,
        MaterialCriticality.MEDIUM: 2.0,
        MaterialCriticality.HIGH: 3.0,
    }

    FEATURE_NAMES: ClassVar[List[str]] = [
        "hist_otdr_90d",
        "hist_avg_delay_90d",
        "hist_fulfillment_rate_90d",
        "hist_disruptions_90d",
        "supplier_lead_time_contract",
        "material_criticality_encoded",
        "order_volume_ratio",
        "inventory_coverage_days",
        "po_line_value",
    ]

    def transform_single_vector(self, request: DisruptionPredictionRequest) -> List[float]:
        """Extract and encode individual features into a standardized numerical list."""
        if not isinstance(request, DisruptionPredictionRequest):
            raise FeatureTransformationException("Input must be a valid DisruptionPredictionRequest instance.")

        criticality_val = self.CRITICALITY_ENCODING.get(request.material_criticality)
        if criticality_val is None:
            raise FeatureTransformationException(f"Unsupported material criticality: {request.material_criticality}")

        vector = [
            float(request.hist_otdr_90d),
            float(request.hist_avg_delay_90d),
            float(request.hist_fulfillment_rate_90d),
            float(request.hist_disruptions_90d),
            float(request.supplier_lead_time_contract),
            float(criticality_val),
            float(request.order_volume_ratio),
            float(request.inventory_coverage_days),
            float(request.po_line_value),
        ]
        return vector

    def transform(self, request: DisruptionPredictionRequest) -> np.ndarray:
        """Transform a single prediction request into a 2D numpy array of shape (1, num_features)."""
        vector = self.transform_single_vector(request)
        matrix = np.array([vector], dtype=np.float64)
        self._validate_matrix(matrix)
        return matrix

    def transform_batch(self, requests: List[DisruptionPredictionRequest]) -> np.ndarray:
        """Transform multiple prediction requests into a 2D numpy array of shape (N, num_features)."""
        if not requests:
            return np.empty((0, len(self.FEATURE_NAMES)), dtype=np.float64)

        matrix = np.array([self.transform_single_vector(req) for req in requests], dtype=np.float64)
        self._validate_matrix(matrix)
        return matrix

    @staticmethod
    def _validate_matrix(matrix: np.ndarray) -> None:
        """Ensure feature matrix is mathematically valid with no NaN or Inf entries."""
        if np.isnan(matrix).any() or np.isinf(matrix).any():
            raise FeatureTransformationException("Feature matrix contains invalid numerical values (NaN or Inf).")
