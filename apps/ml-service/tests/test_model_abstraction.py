import numpy as np
import pytest
from fastapi.testclient import TestClient

from app.core.errors import ModelNotAvailableException
from app.ml.inference.pipeline import InferencePipeline
from app.ml.models.base import UntrainedDisruptionModel
from app.schemas.prediction import DisruptionPredictionRequest


def test_untrained_model_is_not_available() -> None:
    model = UntrainedDisruptionModel()
    assert model.is_available() is False
    assert model.model_version == "none"

    dummy_features = np.zeros((1, 9))
    with pytest.raises(ModelNotAvailableException):
        model.predict(dummy_features)

    with pytest.raises(ModelNotAvailableException):
        model.predict_proba(dummy_features)


def test_inference_pipeline_raises_model_not_available(
    valid_prediction_request: DisruptionPredictionRequest,
) -> None:
    pipeline = InferencePipeline(model=UntrainedDisruptionModel())
    assert pipeline.is_model_available() is False

    with pytest.raises(ModelNotAvailableException):
        pipeline.predict(valid_prediction_request)


def test_predict_endpoint_with_trained_model(
    client: TestClient, valid_prediction_payload: dict
) -> None:
    response = client.post("/api/predict/disruption", json=valid_prediction_payload)
    assert response.status_code == 200
    data = response.json()
    assert 0.0 <= data["disruption_probability"] <= 1.0
    assert data["predicted_label"] in (0, 1)
    assert data["risk_tier"] in ("LOW", "MEDIUM", "HIGH", "CRITICAL")
    assert data["model_version"] == "disruption-baseline-v1"
    assert "inference_timestamp" in data


def test_predict_endpoint_returns_503_when_untrained(
    valid_prediction_payload: dict,
) -> None:
    from app.api.routes.prediction import get_prediction_service
    from app.main import create_app
    from app.services.prediction_service import PredictionService

    app = create_app()
    untrained_service = PredictionService(pipeline=InferencePipeline(model=UntrainedDisruptionModel()))
    app.dependency_overrides[get_prediction_service] = lambda: untrained_service
    test_client = TestClient(app)

    response = test_client.post("/api/predict/disruption", json=valid_prediction_payload)
    assert response.status_code == 503
    data = response.json()
    assert data["status"] == 503
    assert data["error"] == "Model Not Available"
    assert "not yet trained" in data["message"]


def test_predict_endpoint_with_invalid_payload_returns_422(client: TestClient) -> None:
    response = client.post("/api/predict/disruption", json={"invalid": "payload"})
    assert response.status_code == 422
    data = response.json()
    assert data["status"] == 422
    assert data["error"] == "Validation Error"
