from datetime import date
from app.ml.data.extractor import TemporalFeatureExtractor


def test_extractor_excludes_current_and_future_deliveries() -> None:
    extractor = TemporalFeatureExtractor(lookback_days=90, disruption_delay_threshold_days=7)
    obs_date = date(2025, 6, 1)

    historical_deliveries = [
        # In-window historical: 30 days before T, on time
        {
            "actualArrivalDate": "2025-05-02",
            "expectedArrivalDate": "2025-05-05",
            "status": "DELIVERED",
        },
        # In-window historical: 10 days before T, 2 days late
        {
            "actualArrivalDate": "2025-05-22",
            "expectedArrivalDate": "2025-05-20",
            "status": "DELIVERED",
        },
        # LEAKAGE TEST 1: Same day as observation T (must be excluded from historical lookback)
        {
            "actualArrivalDate": "2025-06-01",
            "expectedArrivalDate": "2025-05-20",
            "status": "DELIVERED",
        },
        # LEAKAGE TEST 2: Future delivery after T (must be excluded)
        {
            "actualArrivalDate": "2025-06-15",
            "expectedArrivalDate": "2025-06-10",
            "status": "DELIVERED",
        },
        # Old delivery before the 90-day window (120 days prior)
        {
            "actualArrivalDate": "2025-01-01",
            "expectedArrivalDate": "2025-01-02",
            "status": "DELIVERED",
        },
    ]

    features = extractor.extract_historical_features(
        supplier_id=1,
        observation_date=obs_date,
        historical_deliveries=historical_deliveries,
        historical_po_items=[],
        supplier_profile={"leadTimeDays": 14, "reliabilityScore": 80.0, "capacity": 1000.0, "country": "DE"},
        material_profile={"criticality": "HIGH", "dailyConsumption": 20.0, "currentStock": 200.0},
        current_po_item={"quantity": 100.0, "unitPrice": 50.0},
    )

    # Exactly 2 historical deliveries in window:
    # Delivery 1: On-time (actual 05-02 <= expected 05-05), delay 0
    # Delivery 2: Late (actual 05-22 > expected 05-20), delay 2
    # OTDR = 1/2 = 50.0%
    # Avg Delay = (0 + 2)/2 = 1.0 day
    assert features["hist_otdr_90d"] == 50.0
    assert features["hist_avg_delay_90d"] == 1.0
    assert features["supplier_country"] == "DE"
    assert features["material_criticality"] == "HIGH"
    assert features["order_volume_ratio"] == 5.0  # 100 / 20
    assert features["inventory_coverage_days"] == 10.0  # 200 / 20
    assert features["po_line_value"] == 5000.0  # 100 * 50


def test_labeling_logic() -> None:
    extractor = TemporalFeatureExtractor(disruption_delay_threshold_days=7)

    # 1. Normal on-time delivery
    y_normal = extractor.compute_disruption_label(
        delivery_outcome={"delayDays": 2, "status": "DELIVERED"},
        po_outcome={"status": "PLACED"},
    )
    assert y_normal == 0

    # 2. Critical delay >= 7 days
    y_late = extractor.compute_disruption_label(
        delivery_outcome={"delayDays": 7, "status": "DELIVERED"},
        po_outcome={"status": "PLACED"},
    )
    assert y_late == 1

    # 3. Cancelled PO after placement
    y_po_cancelled = extractor.compute_disruption_label(
        delivery_outcome=None,
        po_outcome={"status": "CANCELLED"},
    )
    assert y_po_cancelled == 1

    # 4. Cancelled in-transit delivery
    y_del_cancelled = extractor.compute_disruption_label(
        delivery_outcome={"delayDays": 0, "status": "CANCELLED"},
        po_outcome={"status": "PLACED"},
    )
    assert y_del_cancelled == 1
