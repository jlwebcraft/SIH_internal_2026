from datetime import datetime, timezone
import pytest
from pydantic import ValidationError

from app.ml.data.schema import ProcurementObservation, DatasetMetadata


def test_valid_procurement_observation() -> None:
    obs = ProcurementObservation(
        observation_id="OBS-000001",
        po_item_id=1001,
        supplier_id=10,
        material_id=20,
        observation_timestamp=datetime(2025, 3, 15, 10, 0, 0, tzinfo=timezone.utc),
        hist_otdr_90d=88.5,
        hist_avg_delay_90d=1.5,
        hist_fulfillment_rate_90d=98.0,
        hist_disruptions_90d=0,
        supplier_lead_time_contract=14,
        material_criticality="HIGH",
        order_volume_ratio=1.2,
        inventory_coverage_days=25.0,
        po_line_value=8500.0,
        supplier_country="IN",
        is_disrupted=0,
    )
    assert obs.observation_id == "OBS-000001"
    assert obs.is_disrupted == 0
    assert obs.material_criticality == "HIGH"
    assert obs.supplier_country == "IN"


def test_invalid_criticality_rejected() -> None:
    with pytest.raises(ValidationError):
        ProcurementObservation(
            observation_id="OBS-000002",
            po_item_id=1002,
            supplier_id=10,
            material_id=20,
            observation_timestamp=datetime.now(timezone.utc),
            hist_otdr_90d=88.5,
            hist_avg_delay_90d=1.5,
            hist_fulfillment_rate_90d=98.0,
            hist_disruptions_90d=0,
            supplier_lead_time_contract=14,
            material_criticality="URGENT",  # Invalid
            order_volume_ratio=1.2,
            inventory_coverage_days=25.0,
            po_line_value=8500.0,
            supplier_country="US",
            is_disrupted=0,
        )


def test_invalid_label_rejected() -> None:
    with pytest.raises(ValidationError):
        ProcurementObservation(
            observation_id="OBS-000003",
            po_item_id=1003,
            supplier_id=10,
            material_id=20,
            observation_timestamp=datetime.now(timezone.utc),
            hist_otdr_90d=88.5,
            hist_avg_delay_90d=1.5,
            hist_fulfillment_rate_90d=98.0,
            hist_disruptions_90d=0,
            supplier_lead_time_contract=14,
            material_criticality="LOW",
            order_volume_ratio=1.2,
            inventory_coverage_days=25.0,
            po_line_value=8500.0,
            supplier_country="US",
            is_disrupted=2,  # Invalid
        )


def test_dataset_metadata_creation() -> None:
    metadata = DatasetMetadata(
        dataset_version="v1",
        feature_schema_version="v1",
        source_type="synthetic",
        observation_count=3000,
        disruption_count=450,
        disruption_prevalence=15.0,
        features=["hist_otdr_90d", "hist_avg_delay_90d"],
        label="is_disrupted",
    )
    assert metadata.observation_count == 3000
    assert metadata.disruption_prevalence == 15.0
