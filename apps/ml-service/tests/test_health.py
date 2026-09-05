from fastapi.testclient import TestClient


def test_health_endpoint_returns_200_and_up_status(client: TestClient) -> None:
    response = client.get("/api/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "UP"
    assert data["service"] == "supply-chain-ml-service"
    assert data["version"] == "0.1.0"
