from datetime import date
import pandas as pd
import pytest

from app.ml.data.extractor import TemporalFeatureExtractor
from app.ml.data.splitter import TemporalSplitError, TemporalSplitter
from app.ml.data.validator import DatasetValidationError, DatasetValidator


def test_leakage_guard_rejects_future_delivery_in_historical_otdr() -> None:
    extractor = TemporalFeatureExtractor(lookback_days=90)
    obs_date = date(2025, 5, 1)

    # Historical lookback should only see deliveries before 2025-05-01
    deliveries_with_future = [
        {"actualArrivalDate": "2025-04-10", "expectedArrivalDate": "2025-04-10", "status": "DELIVERED"}, # In window (on-time)
        {"actualArrivalDate": "2025-05-20", "expectedArrivalDate": "2025-05-10", "status": "DELIVERED"}, # FUTURE (10 days late)
    ]

    features = extractor.extract_historical_features(
        supplier_id=10,
        observation_date=obs_date,
        historical_deliveries=deliveries_with_future,
        historical_po_items=[],
        supplier_profile={"leadTimeDays": 14, "reliabilityScore": 90.0},
        material_profile={"criticality": "LOW", "dailyConsumption": 10.0, "currentStock": 50.0},
        current_po_item={"quantity": 50.0, "unitPrice": 10.0},
    )

    # OTDR should be 100% because the future late delivery on 05-20 MUST NOT leak into 05-01 metrics
    assert features["hist_otdr_90d"] == 100.0
    assert features["hist_avg_delay_90d"] == 0.0


def test_leakage_guard_rejects_current_delivery_in_historical_disruptions() -> None:
    extractor = TemporalFeatureExtractor(lookback_days=90)
    obs_date = date(2025, 5, 1)

    deliveries_with_current = [
        {"actualArrivalDate": "2025-04-15", "expectedArrivalDate": "2025-04-15", "status": "DELIVERED"},
        {"actualArrivalDate": "2025-05-01", "expectedArrivalDate": "2025-04-20", "status": "DELIVERED"}, # 11 days late on T
    ]

    features = extractor.extract_historical_features(
        supplier_id=10,
        observation_date=obs_date,
        historical_deliveries=deliveries_with_current,
        historical_po_items=[],
        supplier_profile={"leadTimeDays": 14, "reliabilityScore": 90.0},
        material_profile={"criticality": "LOW", "dailyConsumption": 10.0, "currentStock": 50.0},
        current_po_item={"quantity": 50.0, "unitPrice": 10.0},
    )

    # Disruption count must be 0 because delivery on T=05-01 is not strictly prior to T
    assert features["hist_disruptions_90d"] == 0


def test_leakage_guard_temporal_splitter_rejects_shuffled_time_series() -> None:
    # Construct an unordered dataframe where future rows appear before past rows
    df_shuffled = pd.DataFrame({
        "observation_id": ["A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L"],
        "observation_timestamp": [
            "2025-06-01", "2025-01-01", "2025-05-01", "2025-02-01",
            "2025-07-01", "2025-03-01", "2025-08-01", "2025-04-01",
            "2025-09-01", "2025-10-01", "2025-11-01", "2025-12-01",
        ],
    })

    splitter = TemporalSplitter()
    # Splitter must re-sort chronologically and ensure max(train) <= min(val) <= max(val) <= min(test)
    split = splitter.split(df_shuffled)
    assert pd.to_datetime(split.train_df["observation_timestamp"]).max() <= pd.to_datetime(split.val_df["observation_timestamp"]).min()
    assert pd.to_datetime(split.val_df["observation_timestamp"]).max() <= pd.to_datetime(split.test_df["observation_timestamp"]).min()
