import pytest
from pydantic import ValidationError

from app.schemas.prediction import DisruptionPredictionRequest, MaterialCriticality


def test_valid_disruption_prediction_request(valid_prediction_payload: dict) -> None:
    request = DisruptionPredictionRequest(**valid_prediction_payload)
    assert request.hist_otdr_90d == 85.5
    assert request.hist_avg_delay_90d == 2.3
    assert request.hist_fulfillment_rate_90d == 96.0
    assert request.hist_disruptions_90d == 1
    assert request.supplier_lead_time_contract == 14
    assert request.material_criticality == MaterialCriticality.HIGH
    assert request.order_volume_ratio == 1.25
    assert request.inventory_coverage_days == 18.0
    assert request.po_line_value == 15400.0
    assert request.supplier_country == "DE"


@pytest.mark.parametrize(
    "field,invalid_value",
    [
        ("hist_otdr_90d", -0.1),
        ("hist_otdr_90d", 100.1),
        ("hist_fulfillment_rate_90d", -1.0),
        ("hist_fulfillment_rate_90d", 101.0),
        ("hist_avg_delay_90d", -0.5),
        ("hist_disruptions_90d", -1),
        ("supplier_lead_time_contract", -5),
        ("order_volume_ratio", -0.1),
        ("inventory_coverage_days", -1.0),
        ("po_line_value", -50.0),
        ("supplier_country", ""),
        ("supplier_country", "   "),
        ("material_criticality", "EXTREME"),
    ],
)
def test_invalid_field_values_raise_validation_error(
    valid_prediction_payload: dict, field: str, invalid_value: object
) -> None:
    payload = valid_prediction_payload.copy()
    payload[field] = invalid_value
    with pytest.raises(ValidationError):
        DisruptionPredictionRequest(**payload)


def test_missing_required_field_raises_validation_error(valid_prediction_payload: dict) -> None:
    payload = valid_prediction_payload.copy()
    del payload["hist_otdr_90d"]
    with pytest.raises(ValidationError):
        DisruptionPredictionRequest(**payload)
