from dataclasses import dataclass
from typing import Tuple
import pandas as pd

from app.core.errors import FeatureTransformationException


class TemporalSplitError(FeatureTransformationException):
    """Raised when a dataset cannot be cleanly split chronologically."""
    pass


@dataclass
class DatasetSplit:
    train_df: pd.DataFrame
    val_df: pd.DataFrame
    test_df: pd.DataFrame

    @property
    def train_size(self) -> int:
        return len(self.train_df)

    @property
    def val_size(self) -> int:
        return len(self.val_df)

    @property
    def test_size(self) -> int:
        return len(self.test_df)


class TemporalSplitter:
    """Performs strictly chronological, leakage-free temporal dataset splits."""

    def __init__(
        self,
        train_ratio: float = 0.70,
        val_ratio: float = 0.15,
        test_ratio: float = 0.15,
        timestamp_col: str = "observation_timestamp",
    ) -> None:
        total = train_ratio + val_ratio + test_ratio
        if abs(total - 1.0) > 1e-6:
            raise TemporalSplitError(f"Split ratios must sum to 1.0 (got {total:.4f}).")

        self.train_ratio = train_ratio
        self.val_ratio = val_ratio
        self.test_ratio = test_ratio
        self.timestamp_col = timestamp_col

    def split(self, df: pd.DataFrame) -> DatasetSplit:
        """Sort dataset chronologically and partition into train, validation, and test sets."""
        if df is None or not isinstance(df, pd.DataFrame):
            raise TemporalSplitError("Input must be a valid pandas DataFrame.")

        n_rows = len(df)
        if n_rows < 10:
            raise TemporalSplitError(f"Dataset is too small for meaningful temporal splitting (got {n_rows} rows).")

        if self.timestamp_col not in df.columns:
            raise TemporalSplitError(f"Timestamp column '{self.timestamp_col}' not found in dataset.")

        # 1. Sort strictly by observation timestamp (ascending)
        sorted_df = df.sort_values(by=self.timestamp_col).reset_index(drop=True)

        # 2. Compute index cutoffs
        train_end = int(n_rows * self.train_ratio)
        val_end = train_end + int(n_rows * self.val_ratio)

        train_df = sorted_df.iloc[:train_end].copy().reset_index(drop=True)
        val_df = sorted_df.iloc[train_end:val_end].copy().reset_index(drop=True)
        test_df = sorted_df.iloc[val_end:].copy().reset_index(drop=True)

        if train_df.empty or val_df.empty or test_df.empty:
            raise TemporalSplitError("One or more split partitions resulted in 0 rows.")

        # 3. Validate Temporal Integrity (Strict ordering: max(train) <= min(val) and max(val) <= min(test))
        max_train_ts = train_df[self.timestamp_col].max()
        min_val_ts = val_df[self.timestamp_col].min()
        max_val_ts = val_df[self.timestamp_col].max()
        min_test_ts = test_df[self.timestamp_col].min()

        if max_train_ts > min_val_ts:
            raise TemporalSplitError(
                f"Temporal leakage detected: max(train_ts)={max_train_ts} is after min(val_ts)={min_val_ts}"
            )

        if max_val_ts > min_test_ts:
            raise TemporalSplitError(
                f"Temporal leakage detected: max(val_ts)={max_val_ts} is after min(test_ts)={min_test_ts}"
            )

        # 4. Validate no overlapping observation IDs
        if "observation_id" in df.columns:
            train_ids = set(train_df["observation_id"])
            val_ids = set(val_df["observation_id"])
            test_ids = set(test_df["observation_id"])

            if not train_ids.isdisjoint(val_ids):
                raise TemporalSplitError("Overlapping observation IDs found between train and validation sets.")
            if not train_ids.isdisjoint(test_ids):
                raise TemporalSplitError("Overlapping observation IDs found between train and test sets.")
            if not val_ids.isdisjoint(test_ids):
                raise TemporalSplitError("Overlapping observation IDs found between validation and test sets.")

        return DatasetSplit(train_df=train_df, val_df=val_df, test_df=test_df)
