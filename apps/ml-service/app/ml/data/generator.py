from datetime import datetime, timedelta, timezone
from typing import Optional, Tuple
import numpy as np
import pandas as pd

from app.ml.data.schema import DatasetMetadata
from app.ml.data.validator import DatasetValidator
from app.ml.features.metadata import get_feature_names


class SyntheticDatasetGenerator:
    """Generates realistic synthetic procurement datasets with stochastic disruption labels for ML development and testing."""

    COUNTRIES = ["IN", "US", "DE", "CN", "JP", "VN", "MX", "TW", "KR", "MY"]
    CRITICALITY_TIERS = ["HIGH", "MEDIUM", "LOW"]

    def __init__(self, seed: int = 42) -> None:
        self.seed = seed
        self.rng = np.random.default_rng(seed)

    def generate(
        self,
        n_samples: int = 3000,
        start_date: str = "2025-01-01",
        end_date: str = "2026-06-01",
    ) -> Tuple[pd.DataFrame, DatasetMetadata]:
        """Generate a reproducible synthetic procurement DataFrame and corresponding DatasetMetadata."""
        start_dt = datetime.fromisoformat(start_date)
        end_dt = datetime.fromisoformat(end_date)
        total_days = max(1, (end_dt - start_dt).days)

        # 1. Generate Timestamps chronologically (strictly sorted)
        total_hours = total_days * 24
        hour_offsets = np.sort(self.rng.integers(0, total_hours, size=n_samples))
        timestamps = [start_dt + timedelta(hours=int(h)) for h in hour_offsets]

        # 2. Generate Base Identifiers
        n_suppliers = 40
        n_materials = 80
        supplier_ids = self.rng.integers(1, n_suppliers + 1, size=n_samples)
        material_ids = self.rng.integers(1, n_materials + 1, size=n_samples)

        # Supplier static country mapping
        supplier_country_map = {
            s_id: self.rng.choice(self.COUNTRIES, p=[0.20, 0.18, 0.15, 0.12, 0.08, 0.08, 0.06, 0.05, 0.04, 0.04])
            for s_id in range(1, n_suppliers + 1)
        }
        countries = [supplier_country_map[s] for s in supplier_ids]

        # Material criticality mapping
        material_crit_map = {
            m_id: self.rng.choice(self.CRITICALITY_TIERS, p=[0.25, 0.45, 0.30])
            for m_id in range(1, n_materials + 1)
        }
        criticalities = [material_crit_map[m] for m in material_ids]

        # 3. Generate Historical Features (Plumbing realistic distributions)
        # OTDR (0 - 100%, mean around 85%)
        otdr_raw = self.rng.beta(8, 2, size=n_samples) * 100.0
        otdr = np.clip(np.round(otdr_raw, 2), 0.0, 100.0)

        # Avg Delay (0 - 15 days, skewed)
        delay_raw = self.rng.exponential(scale=2.2, size=n_samples)
        avg_delay = np.clip(np.round(delay_raw, 2), 0.0, 30.0)

        # Fulfillment Rate (70 - 100%, mean around 95%)
        fulfillment_raw = 100.0 - self.rng.exponential(scale=4.5, size=n_samples)
        fulfillment = np.clip(np.round(fulfillment_raw, 2), 0.0, 100.0)

        # Historical Disruptions (0, 1, 2, 3...)
        disruptions = self.rng.poisson(lam=0.65, size=n_samples)

        # Contracted Lead Time (5 to 60 days)
        lead_times = self.rng.choice([7, 14, 21, 28, 35, 45, 60], size=n_samples)

        # Order Volume Ratio (0.2 to 5.0)
        volume_ratios = np.round(self.rng.lognormal(mean=0.2, sigma=0.5, size=n_samples), 4)

        # Inventory Coverage (2 to 60 days)
        inventory_coverage = np.round(self.rng.gamma(shape=4.0, scale=4.5, size=n_samples), 2)

        # PO Line Value ($500 to $50,000)
        po_line_values = np.round(self.rng.uniform(500.0, 50000.0, size=n_samples), 2)

        # 4. Generate Stochastic Disruption Label
        # Using a logistic probability function combining risk indicators with realistic noise
        crit_weights = {"HIGH": 3.0, "MEDIUM": 2.0, "LOW": 1.0}
        crit_numeric = np.array([crit_weights[c] for c in criticalities])

        # Latent disruption score z
        z = (
            -2.4
            + 0.030 * (100.0 - otdr)
            + 0.085 * avg_delay
            + 0.350 * disruptions
            + 0.015 * (100.0 - fulfillment)
            + 0.150 * (crit_numeric - 1.0)
            + 0.100 * np.clip(volume_ratios - 1.0, 0, 5)
            - 0.015 * np.clip(inventory_coverage, 0, 30)
            + self.rng.normal(0.0, 0.45, size=n_samples)  # Stochastic noise
        )

        # Logistic probability P(Disruption)
        probs = 1.0 / (1.0 + np.exp(-z))
        labels = (self.rng.uniform(0.0, 1.0, size=n_samples) < probs).astype(int)

        # 5. Build DataFrame
        df = pd.DataFrame({
            "observation_id": [f"OBS-{i+1:06d}" for i in range(n_samples)],
            "po_item_id": np.arange(1001, 1001 + n_samples, dtype=int),
            "supplier_id": supplier_ids,
            "material_id": material_ids,
            "observation_timestamp": timestamps,
            "hist_otdr_90d": otdr,
            "hist_avg_delay_90d": avg_delay,
            "hist_fulfillment_rate_90d": fulfillment,
            "hist_disruptions_90d": disruptions,
            "supplier_lead_time_contract": lead_times,
            "material_criticality": criticalities,
            "order_volume_ratio": volume_ratios,
            "inventory_coverage_days": inventory_coverage,
            "po_line_value": po_line_values,
            "supplier_country": countries,
            "is_disrupted": labels,
        })

        # 6. Validate generated dataset
        summary = DatasetValidator.validate_dataframe(df)

        metadata = DatasetMetadata(
            dataset_version="v1",
            feature_schema_version="v1",
            source_type="synthetic",
            created_at=datetime.now(timezone.utc),
            observation_count=n_samples,
            disruption_count=summary["disruptions"],
            disruption_prevalence=summary["prevalence_pct"],
            start_date=start_date,
            end_date=end_date,
            random_seed=self.seed,
            features=get_feature_names(),
            label="is_disrupted",
        )

        return df, metadata
