import numpy as np
import pandas as pd
import pytest

from app.core.errors import FeatureTransformationException
from app.ml.data.generator import SyntheticDatasetGenerator
from app.ml.data.splitter import TemporalSplitter
from app.ml.features.pipeline import FeaturePipeline


def test_pipeline_fit_transform_on_train_and_transform_on_test() -> None:
    generator = SyntheticDatasetGenerator(seed=77)
    df, _ = generator.generate(n_samples=600)

    splitter = TemporalSplitter(train_ratio=0.70, val_ratio=0.15, test_ratio=0.15)
    split = splitter.split(df)

    pipeline = FeaturePipeline(scale_numerical=True)
    X_train_transformed = pipeline.fit_transform(split.train_df)
    X_test_transformed = pipeline.transform(split.test_df)

    assert pipeline.is_fitted is True
    assert isinstance(X_train_transformed, np.ndarray)
    assert isinstance(X_test_transformed, np.ndarray)

    assert X_train_transformed.shape[0] == 420
    assert X_test_transformed.shape[0] == 90
    assert X_train_transformed.shape[1] == X_test_transformed.shape[1]

    # No NaN or Inf
    assert not np.isnan(X_train_transformed).any()
    assert not np.isnan(X_test_transformed).any()


def test_pipeline_handles_unseen_country_without_error() -> None:
    generator = SyntheticDatasetGenerator(seed=77)
    df, _ = generator.generate(n_samples=200)

    # Train without country 'ZZ'
    df_train = df[df["supplier_country"] != "ZZ"].copy()
    pipeline = FeaturePipeline()
    pipeline.fit(df_train)

    # Test with novel/unseen country 'ZZ'
    df_test = df.iloc[:10].copy()
    df_test["supplier_country"] = "ZZ"

    # Should transform safely without raising exception (one-hot encodes to all zeros for unknown)
    X_test = pipeline.transform(df_test)
    assert X_test.shape[0] == 10
    assert not np.isnan(X_test).any()


def test_pipeline_handles_missing_numerical_values_via_median_imputer() -> None:
    generator = SyntheticDatasetGenerator(seed=77)
    df, _ = generator.generate(n_samples=100)

    pipeline = FeaturePipeline()
    pipeline.fit(df)

    # Introduce missing values in test set
    df_missing = df.iloc[:5].copy()
    df_missing.loc[0, "hist_otdr_90d"] = np.nan
    df_missing.loc[1, "hist_avg_delay_90d"] = np.nan

    X_missing = pipeline.transform(df_missing)
    assert not np.isnan(X_missing).any()


def test_pipeline_rejects_transform_before_fit() -> None:
    pipeline = FeaturePipeline()
    dummy_df = pd.DataFrame({"dummy": [1, 2]})

    with pytest.raises(FeatureTransformationException, match="must be fitted"):
        pipeline.transform(dummy_df)
