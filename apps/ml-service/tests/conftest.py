import pytest
from fastapi.testclient import TestClient

from app.main import create_app
from app.schemas.prediction import DisruptionPredictionRequest, MaterialCriticality


@pytest.fixture
def client() -> TestClient:
    """FastAPI TestClient fixture."""
    app = create_app()
    return TestClient(app)


@pytest.fixture
def valid_prediction_payload() -> dict:
    """Sample valid prediction request payload conforming to Phase 7A candidate features."""
    return {
        "hist_otdr_90d": 85.5,
        "hist_avg_delay_90d": 2.3,
        "hist_fulfillment_rate_90d": 96.0,
        "hist_disruptions_90d": 1,
        "supplier_lead_time_contract": 14,
        "material_criticality": "HIGH",
        "order_volume_ratio": 1.25,
        "inventory_coverage_days": 18.0,
        "po_line_value": 15400.0,
        "supplier_country": "DE",
    }


@pytest.fixture
def valid_prediction_request(valid_prediction_payload: dict) -> DisruptionPredictionRequest:
    """DisruptionPredictionRequest instance from valid payload."""
    return DisruptionPredictionRequest(**valid_prediction_payload)
