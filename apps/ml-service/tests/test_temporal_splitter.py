import pandas as pd
import pytest

from app.ml.data.generator import SyntheticDatasetGenerator
from app.ml.data.splitter import TemporalSplitError, TemporalSplitter


def test_temporal_splitter_chronological_partitions() -> None:
    generator = SyntheticDatasetGenerator(seed=42)
    df, _ = generator.generate(n_samples=1000)

    splitter = TemporalSplitter(train_ratio=0.70, val_ratio=0.15, test_ratio=0.15)
    split = splitter.split(df)

    assert split.train_size == 700
    assert split.val_size == 150
    assert split.test_size == 150

    # Ensure max(train) <= min(val) <= max(val) <= min(test)
    assert split.train_df["observation_timestamp"].max() <= split.val_df["observation_timestamp"].min()
    assert split.val_df["observation_timestamp"].max() <= split.test_df["observation_timestamp"].min()


def test_temporal_splitter_disjoint_observation_ids() -> None:
    generator = SyntheticDatasetGenerator(seed=42)
    df, _ = generator.generate(n_samples=500)

    splitter = TemporalSplitter()
    split = splitter.split(df)

    train_ids = set(split.train_df["observation_id"])
    val_ids = set(split.val_df["observation_id"])
    test_ids = set(split.test_df["observation_id"])

    assert train_ids.isdisjoint(val_ids)
    assert train_ids.isdisjoint(test_ids)
    assert val_ids.isdisjoint(test_ids)


def test_temporal_splitter_rejects_invalid_ratios() -> None:
    with pytest.raises(TemporalSplitError, match="must sum to 1.0"):
        TemporalSplitter(train_ratio=0.80, val_ratio=0.15, test_ratio=0.15)  # Sums to 1.10


def test_temporal_splitter_rejects_tiny_dataset() -> None:
    df_tiny = pd.DataFrame({
        "observation_id": ["A", "B"],
        "observation_timestamp": pd.date_range("2025-01-01", periods=2),
    })
    splitter = TemporalSplitter()
    with pytest.raises(TemporalSplitError, match="too small"):
        splitter.split(df_tiny)
