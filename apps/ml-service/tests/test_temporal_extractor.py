from datetime import date
import pytest
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
    assert features["order_volume_ratio"] == 0.10  # 100 / 1000
    assert features["inventory_coverage_days"] == 10.0  # 200 / 20
    assert features["po_line_value"] == 5000.0  # 100 * 50


def test_fulfillment_rate_cases_a_through_e() -> None:
    extractor = TemporalFeatureExtractor(lookback_days=90)
    obs_date = date(2025, 6, 1)

    # Case A: 100 ordered, 100 received -> 100%
    feat_a = extractor.extract_historical_features(
        supplier_id=1,
        observation_date=obs_date,
        historical_deliveries=[],
        historical_po_items=[{"orderDate": "2025-05-01", "quantity": 100, "receivedQuantity": 100, "poStatus": "DELIVERED"}],
        supplier_profile={"capacity": 1000},
        material_profile={"dailyConsumption": 10, "currentStock": 100},
        current_po_item={"quantity": 50, "unitPrice": 10},
    )
    assert feat_a["hist_fulfillment_rate_90d"] == 100.0

    # Case B: 100 ordered, 50 received -> 50%
    feat_b = extractor.extract_historical_features(
        supplier_id=1,
        observation_date=obs_date,
        historical_deliveries=[],
        historical_po_items=[{"orderDate": "2025-05-01", "quantity": 100, "receivedQuantity": 50, "poStatus": "DELIVERED"}],
        supplier_profile={"capacity": 1000},
        material_profile={"dailyConsumption": 10, "currentStock": 100},
        current_po_item={"quantity": 50, "unitPrice": 10},
    )
    assert feat_b["hist_fulfillment_rate_90d"] == 50.0

    # Case C: 100 ordered, 0 received -> 0%
    feat_c = extractor.extract_historical_features(
        supplier_id=1,
        observation_date=obs_date,
        historical_deliveries=[],
        historical_po_items=[{"orderDate": "2025-05-01", "quantity": 100, "receivedQuantity": 0, "poStatus": "PARTIAL"}],
        supplier_profile={"capacity": 1000},
        material_profile={"dailyConsumption": 10, "currentStock": 100},
        current_po_item={"quantity": 50, "unitPrice": 10},
    )
    assert feat_c["hist_fulfillment_rate_90d"] == 0.0

    # Case D: 100 ordered, 120 received -> capped at 100%
    feat_d = extractor.extract_historical_features(
        supplier_id=1,
        observation_date=obs_date,
        historical_deliveries=[],
        historical_po_items=[{"orderDate": "2025-05-01", "quantity": 100, "receivedQuantity": 120, "poStatus": "DELIVERED"}],
        supplier_profile={"capacity": 1000},
        material_profile={"dailyConsumption": 10, "currentStock": 100},
        current_po_item={"quantity": 50, "unitPrice": 10},
    )
    assert feat_d["hist_fulfillment_rate_90d"] == 100.0

    # Case E: Future PO/receipt after T must not affect historical fulfillment
    feat_e = extractor.extract_historical_features(
        supplier_id=1,
        observation_date=obs_date,
        historical_deliveries=[],
        historical_po_items=[
            # Historical PO: 100 ordered, 80 received
            {"orderDate": "2025-05-01", "quantity": 100, "receivedQuantity": 80, "poStatus": "DELIVERED"},
            # Future PO after T: 500 ordered, 0 received (must be excluded)
            {"orderDate": "2025-06-15", "quantity": 500, "receivedQuantity": 0, "poStatus": "PLACED"},
        ],
        supplier_profile={"capacity": 1000},
        material_profile={"dailyConsumption": 10, "currentStock": 100},
        current_po_item={"quantity": 50, "unitPrice": 10},
    )
    assert feat_e["hist_fulfillment_rate_90d"] == 80.0


def test_multi_item_po_cancellation_deduplication() -> None:
    extractor = TemporalFeatureExtractor(lookback_days=90)
    obs_date = date(2025, 6, 1)

    # A single cancelled purchase order containing 3 items (Item A, Item B, Item C)
    multi_item_cancelled_po = [
        {"purchaseOrderId": 501, "orderDate": "2025-05-10", "poStatus": "CANCELLED", "materialId": 1},
        {"purchaseOrderId": 501, "orderDate": "2025-05-10", "poStatus": "CANCELLED", "materialId": 2},
        {"purchaseOrderId": 501, "orderDate": "2025-05-10", "poStatus": "CANCELLED", "materialId": 3},
    ]

    features = extractor.extract_historical_features(
        supplier_id=1,
        observation_date=obs_date,
        historical_deliveries=[],
        historical_po_items=multi_item_cancelled_po,
        supplier_profile={"capacity": 1000},
        material_profile={"dailyConsumption": 10, "currentStock": 100},
        current_po_item={"quantity": 50, "unitPrice": 10},
    )

    # Disruption count must be 1 (one cancelled PO event), NOT 3
    assert features["hist_disruptions_90d"] == 1


def test_labeling_logic_and_delay_boundaries() -> None:
    extractor = TemporalFeatureExtractor(disruption_delay_threshold_days=7)

    # 1. Exactly 6 days late -> no disruption (0)
    y_6days = extractor.compute_disruption_label(
        delivery_outcome={"delayDays": 6, "status": "DELIVERED"},
        po_outcome={"status": "PLACED"},
    )
    assert y_6days == 0

    # 2. Exactly 7 days late -> disruption (1)
    y_7days = extractor.compute_disruption_label(
        delivery_outcome={"delayDays": 7, "status": "DELIVERED"},
        po_outcome={"status": "PLACED"},
    )
    assert y_7days == 1

    # 3. 10 days late -> disruption (1)
    y_10days = extractor.compute_disruption_label(
        delivery_outcome={"delayDays": 10, "status": "DELIVERED"},
        po_outcome={"status": "PLACED"},
    )
    assert y_10days == 1

    # 4. Cancelled PO after placement -> disruption (1)
    y_po_cancelled = extractor.compute_disruption_label(
        delivery_outcome=None,
        po_outcome={"status": "CANCELLED"},
    )
    assert y_po_cancelled == 1

    # 5. Cancelled in-transit delivery -> disruption (1)
    y_del_cancelled = extractor.compute_disruption_label(
        delivery_outcome={"delayDays": 0, "status": "CANCELLED"},
        po_outcome={"status": "PLACED"},
    )
    assert y_del_cancelled == 1

