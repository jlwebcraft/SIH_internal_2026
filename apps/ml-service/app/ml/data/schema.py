from datetime import datetime, timezone
from typing import Any, Dict, List, Optional
from pydantic import BaseModel, Field, field_validator


class ProcurementObservation(BaseModel):
    """Represents a single observation row in the ML training dataset."""

    observation_id: str = Field(..., description="Unique deterministic identifier for the observation")
    po_item_id: int = Field(..., ge=1, description="Primary key of the purchase order line item")
    supplier_id: int = Field(..., ge=1, description="Primary key of the supplier")
    material_id: int = Field(..., ge=1, description="Primary key of the material")
    observation_timestamp: datetime = Field(..., description="Timestamp T when PO was placed (strictly prior to outcome)")

    # 10 Phase 7A Features (Known strictly at or before T)
    hist_otdr_90d: float = Field(..., ge=0.0, le=100.0, description="Rolling 90-day OTDR (%)")
    hist_avg_delay_90d: float = Field(..., ge=0.0, description="Rolling 90-day average delay (days)")
    hist_fulfillment_rate_90d: float = Field(..., ge=0.0, le=100.0, description="Rolling 90-day fulfillment rate (%)")
    hist_disruptions_90d: int = Field(..., ge=0, description="Rolling 90-day critical disruption count")
    supplier_lead_time_contract: int = Field(..., ge=0, description="Contracted lead time (days)")
    material_criticality: str = Field(..., description="BOM material criticality tier: HIGH, MEDIUM, LOW")
    order_volume_ratio: float = Field(..., ge=0.0, description="Order quantity / daily consumption ratio")
    inventory_coverage_days: float = Field(..., ge=0.0, description="Current stock / daily consumption coverage (days)")
    po_line_value: float = Field(..., ge=0.0, description="Monetary value of the line item")
    supplier_country: str = Field(..., min_length=1, description="Supplier country code or origin")

    # Binary Disruption Label (Occurs strictly AFTER T)
    is_disrupted: int = Field(..., ge=0, le=1, description="Binary target label: 1 = Disrupted, 0 = Normal")

    # Outcome Auditing Fields (Metadata only, excluded from model features)
    outcome_delay_days: Optional[float] = Field(None, description="Actual realized delivery delay in days")
    outcome_delivery_status: Optional[str] = Field(None, description="Final delivery status")
    outcome_po_status: Optional[str] = Field(None, description="Final purchase order status")

    @field_validator("material_criticality")
    @classmethod
    def validate_criticality(cls, v: str) -> str:
        upper = v.upper().strip()
        if upper not in {"HIGH", "MEDIUM", "LOW"}:
            raise ValueError("material_criticality must be one of: HIGH, MEDIUM, LOW")
        return upper

    @field_validator("supplier_country")
    @classmethod
    def validate_country(cls, v: str) -> str:
        if not v or not v.strip():
            raise ValueError("supplier_country must not be blank")
        return v.strip().upper()


class DatasetMetadata(BaseModel):
    """Metadata summary associated with a versioned ML dataset."""

    dataset_version: str = Field(default="v1", description="Dataset schema version")
    feature_schema_version: str = Field(default="v1", description="Feature specification version")
    source_type: str = Field(..., description="Data generation source ('synthetic' or 'operational')")
    created_at: datetime = Field(
        default_factory=lambda: datetime.now(timezone.utc),
        description="Dataset creation UTC timestamp",
    )
    observation_count: int = Field(..., ge=0, description="Total number of observations in dataset")
    disruption_count: int = Field(..., ge=0, description="Total count of positive disruption observations")
    disruption_prevalence: float = Field(..., ge=0.0, le=100.0, description="Disruption rate percentage (0-100%)")
    start_date: Optional[str] = Field(None, description="Earliest observation date (YYYY-MM-DD)")
    end_date: Optional[str] = Field(None, description="Latest observation date (YYYY-MM-DD)")
    random_seed: Optional[int] = Field(None, description="Random seed used for synthetic generator")
    features: List[str] = Field(..., description="List of feature column names")
    label: str = Field(default="is_disrupted", description="Target label column name")
