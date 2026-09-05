from fastapi.testclient import TestClient


def test_ready_endpoint_distinguishes_service_ready_and_model_unavailable(client: TestClient) -> None:
    response = client.get("/api/ready")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "READY"
    assert data["service"] == "supply-chain-ml-service"
    assert data["version"] == "0.1.0"
    assert data["model_available"] is False
    assert "Phase 7C foundation" in data["details"]
