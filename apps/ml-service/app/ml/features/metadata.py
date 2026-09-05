from dataclasses import dataclass
from enum import Enum
from typing import List, Optional


class FeatureStatus(str, Enum):
    AVAILABLE = "AVAILABLE"
    UNAVAILABLE = "UNAVAILABLE"
    REQUIRES_EXTENSION = "REQUIRES_EXTENSION"


class FeatureType(str, Enum):
    NUMERICAL = "NUMERICAL"
    CATEGORICAL = "CATEGORICAL"
    ORDINAL = "ORDINAL"


@dataclass(frozen=True)
class FeatureMetadata:
    name: str
    feature_type: FeatureType
    data_type: str
    unit: str
    min_val: Optional[float]
    max_val: Optional[float]
    lookback_days: Optional[int]
    status: FeatureStatus
    leakage_risk: str
    description: str


FEATURE_CATALOG: List[FeatureMetadata] = [
    FeatureMetadata(
        name="hist_otdr_90d",
        feature_type=FeatureType.NUMERICAL,
        data_type="float64",
        unit="percentage",
        min_val=0.0,
        max_val=100.0,
        lookback_days=90,
        status=FeatureStatus.AVAILABLE,
        leakage_risk="Must only aggregate completed deliveries occurring strictly before observation date T.",
        description="Supplier on-time delivery rate over the 90-day window preceding observation.",
    ),
    FeatureMetadata(
        name="hist_avg_delay_90d",
        feature_type=FeatureType.NUMERICAL,
        data_type="float64",
        unit="days",
        min_val=0.0,
        max_val=None,
        lookback_days=90,
        status=FeatureStatus.AVAILABLE,
        leakage_risk="Must only aggregate positive delays from deliveries strictly preceding observation date T.",
        description="Supplier mean positive delivery delay in calendar days over the 90-day lookback window.",
    ),
    FeatureMetadata(
        name="hist_fulfillment_rate_90d",
        feature_type=FeatureType.NUMERICAL,
        data_type="float64",
        unit="percentage",
        min_val=0.0,
        max_val=100.0,
        lookback_days=90,
        status=FeatureStatus.AVAILABLE,
        leakage_risk="Must only aggregate finalized purchase orders placed strictly before observation date T.",
        description="Supplier purchase order item quantity fulfillment percentage over the 90-day window.",
    ),
    FeatureMetadata(
        name="hist_disruptions_90d",
        feature_type=FeatureType.NUMERICAL,
        data_type="int64",
        unit="count",
        min_val=0.0,
        max_val=None,
        lookback_days=90,
        status=FeatureStatus.AVAILABLE,
        leakage_risk="Critical delays (>=7d), cancellations, and transit failures strictly before observation date T.",
        description="Count of critical historical disruption events in the 90-day lookback window.",
    ),
    FeatureMetadata(
        name="supplier_lead_time_contract",
        feature_type=FeatureType.NUMERICAL,
        data_type="int64",
        unit="days",
        min_val=0.0,
        max_val=None,
        lookback_days=None,
        status=FeatureStatus.AVAILABLE,
        leakage_risk="Static contract parameter known at purchase order placement time.",
        description="Contracted supplier or supplier-material lead time in calendar days.",
    ),
    FeatureMetadata(
        name="material_criticality",
        feature_type=FeatureType.ORDINAL,
        data_type="string",
        unit="tier",
        min_val=None,
        max_val=None,
        lookback_days=None,
        status=FeatureStatus.AVAILABLE,
        leakage_risk="Master BOM metadata known prior to order placement.",
        description="BOM material criticality tier: HIGH, MEDIUM, LOW (encoded ordinally as 3.0, 2.0, 1.0).",
    ),
    FeatureMetadata(
        name="order_volume_ratio",
        feature_type=FeatureType.NUMERICAL,
        data_type="float64",
        unit="ratio",
        min_val=0.0,
        max_val=None,
        lookback_days=None,
        status=FeatureStatus.AVAILABLE,
        leakage_risk="Derived from current PO item quantity and baseline daily consumption.",
        description="Ratio of ordered quantity to material daily consumption (or supplier capacity).",
    ),
    FeatureMetadata(
        name="inventory_coverage_days",
        feature_type=FeatureType.NUMERICAL,
        data_type="float64",
        unit="days",
        min_val=0.0,
        max_val=None,
        lookback_days=None,
        status=FeatureStatus.AVAILABLE,
        leakage_risk="Current inventory stock snapshot available at observation time T.",
        description="Available warehouse inventory stock expressed in days of daily consumption.",
    ),
    FeatureMetadata(
        name="po_line_value",
        feature_type=FeatureType.NUMERICAL,
        data_type="float64",
        unit="currency",
        min_val=0.0,
        max_val=None,
        lookback_days=None,
        status=FeatureStatus.AVAILABLE,
        leakage_risk="Committed purchase order line value (quantity * unitPrice) at placement time.",
        description="Total monetary value of the procurement line item.",
    ),
    FeatureMetadata(
        name="supplier_country",
        feature_type=FeatureType.CATEGORICAL,
        data_type="string",
        unit="country_code",
        min_val=None,
        max_val=None,
        lookback_days=None,
        status=FeatureStatus.AVAILABLE,
        leakage_risk="Static profile metadata; one-hot encoded without lookahead on unseen categories.",
        description="Supplier geographic country code or name of origin.",
    ),
]


def get_feature_names() -> List[str]:
    """Return all feature names in canonical order."""
    return [f.name for f in FEATURE_CATALOG]
