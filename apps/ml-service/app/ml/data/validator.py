from typing import Any, Dict, List
import numpy as np
import pandas as pd

from app.core.errors import FeatureTransformationException
from app.ml.features.metadata import get_feature_names


class DatasetValidationError(FeatureTransformationException):
    """Raised when a dataset fails structural, semantic, or leakage validation checks."""
    pass


class DatasetValidator:
    """Validates raw and extracted dataframes against the ML dataset contract."""

    REQUIRED_METADATA_COLUMNS = [
        "observation_id",
        "po_item_id",
        "supplier_id",
        "material_id",
        "observation_timestamp",
    ]

    REQUIRED_FEATURE_COLUMNS = get_feature_names()
    REQUIRED_LABEL_COLUMN = "is_disrupted"

    @classmethod
    def validate_dataframe(cls, df: pd.DataFrame) -> Dict[str, Any]:
        """Run all validation rules on a pandas DataFrame. Returns summary metrics if valid, raises error otherwise."""
        if df is None or not isinstance(df, pd.DataFrame):
            raise DatasetValidationError("Input must be a valid pandas DataFrame.")

        if df.empty:
            raise DatasetValidationError("Dataset is empty; cannot validate zero rows.")

        # 1. Check Required Columns
        all_required = cls.REQUIRED_METADATA_COLUMNS + cls.REQUIRED_FEATURE_COLUMNS + [cls.REQUIRED_LABEL_COLUMN]
        missing_cols = [col for col in all_required if col not in df.columns]
        if missing_cols:
            raise DatasetValidationError(f"Dataset is missing required columns: {missing_cols}")

        # 2. Check for Duplicate Observation IDs
        duplicate_obs = df["observation_id"].duplicated().sum()
        if duplicate_obs > 0:
            raise DatasetValidationError(f"Dataset contains {duplicate_obs} duplicate observation_id values.")

        # 3. Check for Nulls / NaNs in Feature & Label Columns
        feature_and_label_cols = cls.REQUIRED_FEATURE_COLUMNS + [cls.REQUIRED_LABEL_COLUMN]
        null_counts = df[feature_and_label_cols].isnull().sum()
        cols_with_nulls = null_counts[null_counts > 0]
        if not cols_with_nulls.empty:
            raise DatasetValidationError(f"Dataset contains null values in feature/label columns: {cols_with_nulls.to_dict()}")

        # 4. Check for Infinite Numerical Values
        num_cols = [
            "hist_otdr_90d",
            "hist_avg_delay_90d",
            "hist_fulfillment_rate_90d",
            "hist_disruptions_90d",
            "supplier_lead_time_contract",
            "order_volume_ratio",
            "inventory_coverage_days",
            "po_line_value",
        ]
        for col in num_cols:
            if np.isinf(df[col]).any():
                raise DatasetValidationError(f"Column '{col}' contains infinite values.")

        # 5. Check Numeric Value Ranges
        if (df["hist_otdr_90d"] < 0.0).any() or (df["hist_otdr_90d"] > 100.0).any():
            raise DatasetValidationError("hist_otdr_90d must be bounded within [0.0, 100.0].")

        if (df["hist_fulfillment_rate_90d"] < 0.0).any() or (df["hist_fulfillment_rate_90d"] > 100.0).any():
            raise DatasetValidationError("hist_fulfillment_rate_90d must be bounded within [0.0, 100.0].")

        if (df["hist_avg_delay_90d"] < 0.0).any():
            raise DatasetValidationError("hist_avg_delay_90d must be non-negative.")

        if (df["hist_disruptions_90d"] < 0).any():
            raise DatasetValidationError("hist_disruptions_90d must be non-negative integer.")

        if (df["supplier_lead_time_contract"] < 0).any():
            raise DatasetValidationError("supplier_lead_time_contract must be non-negative.")

        if (df["order_volume_ratio"] < 0.0).any():
            raise DatasetValidationError("order_volume_ratio must be non-negative.")

        if (df["inventory_coverage_days"] < 0.0).any():
            raise DatasetValidationError("inventory_coverage_days must be non-negative.")

        if (df["po_line_value"] < 0.0).any():
            raise DatasetValidationError("po_line_value must be non-negative.")

        # 6. Check Categorical Values
        valid_criticality = {"HIGH", "MEDIUM", "LOW"}
        invalid_crit = set(df["material_criticality"].dropna().unique()) - valid_criticality
        if invalid_crit:
            raise DatasetValidationError(f"Invalid material_criticality categories found: {invalid_crit}")

        blank_countries = (df["supplier_country"].astype(str).str.strip() == "").sum()
        if blank_countries > 0:
            raise DatasetValidationError(f"Dataset contains {blank_countries} blank supplier_country entries.")

        # 7. Check Label Values
        unique_labels = set(df[cls.REQUIRED_LABEL_COLUMN].unique())
        if not unique_labels.issubset({0, 1}):
            raise DatasetValidationError(f"Label column must strictly contain only 0 and 1. Found: {unique_labels}")

        # 8. Check Timestamps
        if not pd.api.types.is_datetime64_any_dtype(df["observation_timestamp"]):
            try:
                pd.to_datetime(df["observation_timestamp"])
            except Exception as e:
                raise DatasetValidationError(f"Failed to parse observation_timestamp as datetime: {e}")

        return {
            "total_rows": len(df),
            "feature_count": len(cls.REQUIRED_FEATURE_COLUMNS),
            "disruptions": int((df[cls.REQUIRED_LABEL_COLUMN] == 1).sum()),
            "prevalence_pct": round(float((df[cls.REQUIRED_LABEL_COLUMN] == 1).mean() * 100), 2),
        }
