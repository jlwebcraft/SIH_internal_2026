import numpy as np
import pytest

from app.ml.features.transformer import FeatureTransformer
from app.schemas.prediction import DisruptionPredictionRequest, MaterialCriticality


def test_feature_transformer_single_vector_ordering(valid_prediction_request: DisruptionPredictionRequest) -> None:
    transformer = FeatureTransformer()
    matrix = transformer.transform(valid_prediction_request)

    assert isinstance(matrix, np.ndarray)
    assert matrix.shape == (1, 9)
    assert matrix.dtype == np.float64

    # Check exact feature ordering
    # 0: hist_otdr_90d = 85.5
    # 1: hist_avg_delay_90d = 2.3
    # 2: hist_fulfillment_rate_90d = 96.0
    # 3: hist_disruptions_90d = 1.0
    # 4: supplier_lead_time_contract = 14.0
    # 5: material_criticality_encoded = 3.0 (HIGH)
    # 6: order_volume_ratio = 1.25
    # 7: inventory_coverage_days = 18.0
    # 8: po_line_value = 15400.0
    expected = np.array([[85.5, 2.3, 96.0, 1.0, 14.0, 3.0, 1.25, 18.0, 15400.0]], dtype=np.float64)
    np.testing.assert_allclose(matrix, expected)


@pytest.mark.parametrize(
    "criticality,expected_encoded",
    [
        (MaterialCriticality.LOW, 1.0),
        (MaterialCriticality.MEDIUM, 2.0),
        (MaterialCriticality.HIGH, 3.0),
    ],
)
def test_criticality_encoding(
    valid_prediction_payload: dict, criticality: MaterialCriticality, expected_encoded: float
) -> None:
    payload = valid_prediction_payload.copy()
    payload["material_criticality"] = criticality.value
    request = DisruptionPredictionRequest(**payload)

    transformer = FeatureTransformer()
    vector = transformer.transform_single_vector(request)
    assert vector[5] == expected_encoded


def test_feature_transformer_batch(valid_prediction_request: DisruptionPredictionRequest) -> None:
    transformer = FeatureTransformer()
    batch = [valid_prediction_request, valid_prediction_request]
    matrix = transformer.transform_batch(batch)

    assert matrix.shape == (2, 9)
    assert not np.isnan(matrix).any()
    assert not np.isinf(matrix).any()


def test_feature_transformer_empty_batch() -> None:
    transformer = FeatureTransformer()
    matrix = transformer.transform_batch([])
    assert matrix.shape == (0, 9)
