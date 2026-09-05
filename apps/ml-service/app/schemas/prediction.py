from datetime import datetime, timezone
from enum import Enum
from typing import Optional
from pydantic import BaseModel, Field, field_validator


class MaterialCriticality(str, Enum):
    """Material criticality classifications."""

    HIGH = "HIGH"
    MEDIUM = "MEDIUM"
    LOW = "LOW"


class DisruptionPredictionRequest(BaseModel):
    """Input payload representing a procurement transaction for ML disruption prediction."""

    hist_otdr_90d: float = Field(
        ...,
        ge=0.0,
        le=100.0,
        description="Rolling 90-day on-time delivery rate percentage (0.0 to 100.0)",
    )
    hist_avg_delay_90d: float = Field(
        ...,
        ge=0.0,
        description="Rolling 90-day average delivery delay in calendar days",
    )
    hist_fulfillment_rate_90d: float = Field(
        ...,
        ge=0.0,
        le=100.0,
        description="Rolling 90-day purchase order volume fulfillment percentage (0.0 to 100.0)",
    )
    hist_disruptions_90d: int = Field(
        ...,
        ge=0,
        description="Rolling 90-day count of critical disruption events",
    )
    supplier_lead_time_contract: int = Field(
        ...,
        ge=0,
        description="Supplier contracted lead time in calendar days",
    )
    material_criticality: MaterialCriticality = Field(
        ...,
        description="BOM material criticality classification: HIGH, MEDIUM, LOW",
    )
    order_volume_ratio: float = Field(
        ...,
        ge=0.0,
        description="Ratio of purchase order item quantity to estimated daily consumption",
    )
    inventory_coverage_days: float = Field(
        ...,
        ge=0.0,
        description="Warehouse inventory coverage in days of consumption",
    )
    po_line_value: float = Field(
        ...,
        ge=0.0,
        description="Monetary line item value for the ordered material (quantity * unitPrice)",
    )
    supplier_country: str = Field(
        ...,
        min_length=1,
        max_length=100,
        description="Supplier country of origin (non-blank)",
    )

    @field_validator("supplier_country")
    @classmethod
    def validate_non_blank_country(cls, v: str) -> str:
        if not v or not v.strip():
            raise ValueError("supplier_country must not be blank")
        return v.strip()


class DisruptionPredictionResponse(BaseModel):
    """Output payload produced by ML disruption prediction model inference."""

    disruption_probability: float = Field(
        ...,
        ge=0.0,
        le=1.0,
        description="Predicted probability of operational delivery disruption (0.0 to 1.0)",
    )
    predicted_label: int = Field(
        ...,
        ge=0,
        le=1,
        description="Predicted binary class label (0 = On-Time / Normal, 1 = Disrupted)",
    )
    risk_tier: Optional[str] = Field(
        None,
        description="Disruption risk tier classification (e.g., LOW, MEDIUM, HIGH, CRITICAL)",
    )
    model_version: str = Field(
        ...,
        description="Identifier of the model artifact used for inference",
    )
    inference_timestamp: datetime = Field(
        default_factory=lambda: datetime.now(timezone.utc),
        description="UTC timestamp when inference was executed",
    )
    confidence: Optional[float] = Field(
        None,
        ge=0.0,
        le=1.0,
        description="Mathematically verified prediction uncertainty or confidence score (omitted if unavailable)",
    )
