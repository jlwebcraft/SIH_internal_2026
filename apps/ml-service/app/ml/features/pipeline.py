from typing import List, Optional
import numpy as np
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.impute import SimpleImputer
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler

from app.core.errors import FeatureTransformationException


class FeaturePipeline:
    """Prepares and transforms raw tabular procurement datasets into numerical ML feature matrices."""

    NUMERICAL_COLS = [
        "hist_otdr_90d",
        "hist_avg_delay_90d",
        "hist_fulfillment_rate_90d",
        "hist_disruptions_90d",
        "supplier_lead_time_contract",
        "material_criticality_encoded",
        "order_volume_ratio",
        "inventory_coverage_days",
        "po_line_value",
    ]

    CATEGORICAL_COLS = [
        "supplier_country",
    ]

    CRITICALITY_MAP = {
        "LOW": 1.0,
        "MEDIUM": 2.0,
        "HIGH": 3.0,
    }

    def __init__(self, scale_numerical: bool = True) -> None:
        self.scale_numerical = scale_numerical
        self.is_fitted = False
        self._preprocessor: Optional[ColumnTransformer] = None

    def _prepare_dataframe(self, df: pd.DataFrame) -> pd.DataFrame:
        """Encode ordinal criticality and validate required input columns."""
        if df is None or not isinstance(df, pd.DataFrame):
            raise FeatureTransformationException("Input must be a valid pandas DataFrame.")

        df_copy = df.copy()

        # Check required columns
        required = [
            "hist_otdr_90d",
            "hist_avg_delay_90d",
            "hist_fulfillment_rate_90d",
            "hist_disruptions_90d",
            "supplier_lead_time_contract",
            "material_criticality",
            "order_volume_ratio",
            "inventory_coverage_days",
            "po_line_value",
            "supplier_country",
        ]
        missing = [c for c in required if c not in df_copy.columns]
        if missing:
            raise FeatureTransformationException(f"Missing required columns for feature pipeline: {missing}")

        # Map ordinal material criticality
        crit_upper = df_copy["material_criticality"].astype(str).str.upper().str.strip()
        df_copy["material_criticality_encoded"] = crit_upper.map(self.CRITICALITY_MAP).fillna(2.0)

        # Standardize supplier country string
        df_copy["supplier_country"] = df_copy["supplier_country"].astype(str).str.upper().str.strip()

        return df_copy

    def _build_column_transformer(self) -> ColumnTransformer:
        """Construct scikit-learn ColumnTransformer with training-time imputation and encoding."""
        num_steps = [("imputer", SimpleImputer(strategy="median"))]
        if self.scale_numerical:
            num_steps.append(("scaler", StandardScaler()))

        num_pipeline = Pipeline(steps=num_steps)

        cat_pipeline = Pipeline(
            steps=[
                ("imputer", SimpleImputer(strategy="constant", fill_value="UNKNOWN")),
                ("onehot", OneHotEncoder(handle_unknown="ignore", sparse_output=False)),
            ]
        )

        transformer = ColumnTransformer(
            transformers=[
                ("num", num_pipeline, self.NUMERICAL_COLS),
                ("cat", cat_pipeline, self.CATEGORICAL_COLS),
            ],
            remainder="drop",
        )
        return transformer

    def fit(self, X_train: pd.DataFrame) -> "FeaturePipeline":
        """Fit preprocessing transformers strictly on training data."""
        prepared_df = self._prepare_dataframe(X_train)
        self._preprocessor = self._build_column_transformer()
        self._preprocessor.fit(prepared_df)
        self.is_fitted = True
        return self

    def transform(self, X: pd.DataFrame) -> np.ndarray:
        """Transform input dataset using previously fitted training parameters."""
        if not self.is_fitted or self._preprocessor is None:
            raise FeatureTransformationException("FeaturePipeline must be fitted before transform can be invoked.")

        prepared_df = self._prepare_dataframe(X)
        matrix = self._preprocessor.transform(prepared_df)
        return np.asarray(matrix, dtype=np.float64)

    def fit_transform(self, X_train: pd.DataFrame) -> np.ndarray:
        """Fit on training data and return transformed feature matrix."""
        return self.fit(X_train).transform(X_train)

    def get_feature_names_out(self) -> List[str]:
        """Return generated output feature names after one-hot encoding."""
        if not self.is_fitted or self._preprocessor is None:
            raise FeatureTransformationException("FeaturePipeline must be fitted to retrieve feature names.")
        return list(self._preprocessor.get_feature_names_out())
