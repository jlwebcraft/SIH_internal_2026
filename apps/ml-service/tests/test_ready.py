from fastapi.testclient import TestClient
from app.api.routes.prediction import get_prediction_service
from app.main import create_app
from app.ml.inference.pipeline import InferencePipeline
from app.ml.models.base import UntrainedDisruptionModel
from app.services.prediction_service import PredictionService


def test_ready_endpoint_with_trained_model(client: TestClient) -> None:
    response = client.get("/api/ready")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "READY"
    assert data["service"] == "supply-chain-ml-service"
    assert data["version"] == "0.1.0"
    assert data["model_available"] is True
    assert "disruption-baseline-v1" in data["details"]


def test_ready_endpoint_with_untrained_model() -> None:
    from app.api.routes.health import get_prediction_service as get_health_prediction_service
    app = create_app()
    untrained_service = PredictionService(pipeline=InferencePipeline(model=UntrainedDisruptionModel()))
    app.dependency_overrides[get_health_prediction_service] = lambda: untrained_service
    test_client = TestClient(app)

    response = test_client.get("/api/ready")
    assert response.status_code == 200
    data = response.json()
    assert data["model_available"] is False
    assert "Phase 7C foundation" in data["details"]
