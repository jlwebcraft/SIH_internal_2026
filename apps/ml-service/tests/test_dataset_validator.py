import pandas as pd
import pytest

from app.ml.data.generator import SyntheticDatasetGenerator
from app.ml.data.validator import DatasetValidationError, DatasetValidator


def test_validator_accepts_valid_dataset() -> None:
    generator = SyntheticDatasetGenerator(seed=123)
    df, _ = generator.generate(n_samples=50)
    summary = DatasetValidator.validate_dataframe(df)

    assert summary["total_rows"] == 50
    assert summary["feature_count"] == 10
    assert "disruptions" in summary


def test_validator_rejects_missing_column() -> None:
    generator = SyntheticDatasetGenerator(seed=123)
    df, _ = generator.generate(n_samples=50)
    df_invalid = df.drop(columns=["hist_otdr_90d"])

    with pytest.raises(DatasetValidationError, match="missing required columns"):
        DatasetValidator.validate_dataframe(df_invalid)


def test_validator_rejects_duplicate_observation_ids() -> None:
    generator = SyntheticDatasetGenerator(seed=123)
    df, _ = generator.generate(n_samples=50)
    df.loc[1, "observation_id"] = df.loc[0, "observation_id"]

    with pytest.raises(DatasetValidationError, match="duplicate observation_id"):
        DatasetValidator.validate_dataframe(df)


def test_validator_rejects_out_of_bound_percentages() -> None:
    generator = SyntheticDatasetGenerator(seed=123)
    df, _ = generator.generate(n_samples=50)
    df.loc[0, "hist_otdr_90d"] = 125.0

    with pytest.raises(DatasetValidationError, match="hist_otdr_90d must be bounded"):
        DatasetValidator.validate_dataframe(df)


def test_validator_rejects_negative_delays() -> None:
    generator = SyntheticDatasetGenerator(seed=123)
    df, _ = generator.generate(n_samples=50)
    df.loc[0, "hist_avg_delay_90d"] = -3.5

    with pytest.raises(DatasetValidationError, match="hist_avg_delay_90d must be non-negative"):
        DatasetValidator.validate_dataframe(df)


def test_validator_rejects_nan_values() -> None:
    generator = SyntheticDatasetGenerator(seed=123)
    df, _ = generator.generate(n_samples=50)
    df.loc[5, "po_line_value"] = None

    with pytest.raises(DatasetValidationError, match="contains null values"):
        DatasetValidator.validate_dataframe(df)


def test_validator_rejects_blank_countries() -> None:
    generator = SyntheticDatasetGenerator(seed=123)
    df, _ = generator.generate(n_samples=50)
    df.loc[2, "supplier_country"] = "   "

    with pytest.raises(DatasetValidationError, match="blank supplier_country"):
        DatasetValidator.validate_dataframe(df)


def test_validator_rejects_invalid_label_values() -> None:
    generator = SyntheticDatasetGenerator(seed=123)
    df, _ = generator.generate(n_samples=50)
    df.loc[3, "is_disrupted"] = 5

    with pytest.raises(DatasetValidationError, match="Label column must strictly contain only 0 and 1"):
        DatasetValidator.validate_dataframe(df)
