from datetime import date, datetime, timedelta, timezone
from typing import Any, Dict, List, Optional
import pandas as pd

from app.core.errors import FeatureTransformationException


class TemporalFeatureExtractor:
    """Extracts historical 90-day features and future binary disruption labels for procurement events."""

    def __init__(self, lookback_days: int = 90, disruption_delay_threshold_days: int = 7) -> None:
        self.lookback_days = lookback_days
        self.disruption_delay_threshold_days = disruption_delay_threshold_days

    def extract_historical_features(
        self,
        supplier_id: int,
        observation_date: date,
        historical_deliveries: List[Dict[str, Any]],
        historical_po_items: List[Dict[str, Any]],
        supplier_profile: Dict[str, Any],
        material_profile: Dict[str, Any],
        current_po_item: Dict[str, Any],
    ) -> Dict[str, Any]:
        """Calculate features strictly using historical events in [observation_date - 90d, observation_date)."""
        window_start = observation_date - timedelta(days=self.lookback_days)

        # 1. Filter deliveries strictly before observation_date and within lookback window
        # CRITICAL LEAKAGE GUARD: actualArrivalDate < observation_date
        completed_deliveries = []
        for d in historical_deliveries:
            arrival = d.get("actualArrivalDate")
            if arrival is None:
                continue
            if isinstance(arrival, str):
                arrival = date.fromisoformat(arrival)
            elif isinstance(arrival, datetime):
                arrival = arrival.date()

            # Must fall strictly in window [window_start, observation_date)
            if window_start <= arrival < observation_date and d.get("status") == "DELIVERED":
                completed_deliveries.append((d, arrival))

        # 2. Compute OTDR and Avg Delay
        if completed_deliveries:
            on_time_count = 0
            delays = []
            for d, actual_arrival in completed_deliveries:
                exp_arrival = d.get("expectedArrivalDate")
                if isinstance(exp_arrival, str):
                    exp_arrival = date.fromisoformat(exp_arrival)
                elif isinstance(exp_arrival, datetime):
                    exp_arrival = exp_arrival.date()

                if exp_arrival is not None and actual_arrival <= exp_arrival:
                    on_time_count += 1

                if exp_arrival is not None:
                    delay = max(0, (actual_arrival - exp_arrival).days)
                else:
                    delay = max(0, d.get("delayDays", 0))
                delays.append(delay)

            hist_otdr_90d = round((on_time_count / len(completed_deliveries)) * 100.0, 2)
            hist_avg_delay_90d = round(sum(delays) / len(delays), 2)
        else:
            # Baseline when insufficient history
            hist_otdr_90d = float(supplier_profile.get("reliabilityScore", 85.0))
            hist_avg_delay_90d = 0.0

        # 3. Compute Fulfillment Rate
        window_po_items = []
        for item in historical_po_items:
            po_date = item.get("orderDate")
            if po_date is None:
                continue
            if isinstance(po_date, str):
                po_date = date.fromisoformat(po_date)
            elif isinstance(po_date, datetime):
                po_date = po_date.date()

            if window_start <= po_date < observation_date and item.get("poStatus") != "CANCELLED":
                window_po_items.append(item)

        if window_po_items:
            total_ordered = sum(float(it.get("quantity", 0)) for it in window_po_items)
            total_received = sum(float(it.get("receivedQuantity", 0)) for it in window_po_items)
            if total_ordered > 0:
                hist_fulfillment_rate_90d = round(min(100.0, (total_received / total_ordered) * 100.0), 2)
            else:
                hist_fulfillment_rate_90d = 100.0
        else:
            hist_fulfillment_rate_90d = 100.0

        # 4. Compute Historical Disruptions strictly before observation_date
        # Deduplicate PO cancellations by purchaseOrderId so multi-item POs do not inflate disruption count
        disruptions = 0
        for d, arrival in completed_deliveries:
            exp_arrival = d.get("expectedArrivalDate")
            if isinstance(exp_arrival, str):
                exp_arrival = date.fromisoformat(exp_arrival)
            elif isinstance(exp_arrival, datetime):
                exp_arrival = exp_arrival.date()

            if exp_arrival and (arrival - exp_arrival).days >= self.disruption_delay_threshold_days:
                disruptions += 1

        cancelled_po_ids = set()
        for it in historical_po_items:
            po_date = it.get("orderDate")
            if isinstance(po_date, str):
                po_date = date.fromisoformat(po_date)
            elif isinstance(po_date, datetime):
                po_date = po_date.date()

            if po_date is not None and window_start <= po_date < observation_date and it.get("poStatus") == "CANCELLED":
                po_id = it.get("purchaseOrderId", it.get("poId", id(it)))
                cancelled_po_ids.add(po_id)

        disruptions += len(cancelled_po_ids)

        # 5. Extract item, material, and supplier profile features
        supplier_lead_time_contract = int(supplier_profile.get("leadTimeDays", 14))
        material_criticality = str(material_profile.get("criticality", "MEDIUM")).upper()

        current_qty = float(current_po_item.get("quantity", 100.0))
        unit_price = float(current_po_item.get("unitPrice", 10.0))
        daily_consumption = float(material_profile.get("dailyConsumption", 10.0))
        current_stock = float(material_profile.get("currentStock", 100.0))

        # Order volume ratio = PO item quantity / Supplier capacity
        capacity = float(supplier_profile.get("capacity", 1000.0))
        if capacity <= 0:
            capacity = 1000.0
        order_volume_ratio = round(current_qty / capacity, 4)

        # Inventory coverage days = current stock / daily consumption
        if daily_consumption > 0:
            inventory_coverage_days = round(current_stock / daily_consumption, 2)
        else:
            inventory_coverage_days = 30.0

        po_line_value = round(current_qty * unit_price, 2)
        supplier_country = str(supplier_profile.get("country", "US")).upper()

        return {
            "hist_otdr_90d": hist_otdr_90d,
            "hist_avg_delay_90d": hist_avg_delay_90d,
            "hist_fulfillment_rate_90d": hist_fulfillment_rate_90d,
            "hist_disruptions_90d": disruptions,
            "supplier_lead_time_contract": supplier_lead_time_contract,
            "material_criticality": material_criticality,
            "order_volume_ratio": order_volume_ratio,
            "inventory_coverage_days": inventory_coverage_days,
            "po_line_value": po_line_value,
            "supplier_country": supplier_country,
        }

    def compute_disruption_label(
        self,
        delivery_outcome: Optional[Dict[str, Any]] = None,
        po_outcome: Optional[Dict[str, Any]] = None,
    ) -> int:
        """Evaluate deterministic binary disruption label from future realization occurring after observation date."""
        # Condition 1: Delivery delay >= 7 days
        if delivery_outcome is not None:
            delay = delivery_outcome.get("delayDays")
            if delay is None and delivery_outcome.get("actualArrivalDate") and delivery_outcome.get("expectedArrivalDate"):
                act = delivery_outcome["actualArrivalDate"]
                exp = delivery_outcome["expectedArrivalDate"]
                if isinstance(act, str):
                    act = date.fromisoformat(act)
                if isinstance(exp, str):
                    exp = date.fromisoformat(exp)
                delay = (act - exp).days

            if delay is not None and delay >= self.disruption_delay_threshold_days:
                return 1

            # Condition 2: Delivery cancelled in transit
            if delivery_outcome.get("status") == "CANCELLED":
                return 1

        # Condition 3: Purchase order cancelled after placement
        if po_outcome is not None and po_outcome.get("status") == "CANCELLED":
            return 1

        return 0
