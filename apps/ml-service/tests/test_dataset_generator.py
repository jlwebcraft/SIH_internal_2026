import pandas as pd
from app.ml.data.generator import SyntheticDatasetGenerator


def test_synthetic_generator_reproducibility() -> None:
    gen1 = SyntheticDatasetGenerator(seed=42)
    df1, meta1 = gen1.generate(n_samples=100)

    gen2 = SyntheticDatasetGenerator(seed=42)
    df2, meta2 = gen2.generate(n_samples=100)

    pd.testing.assert_frame_equal(df1, df2)
    assert meta1.disruption_count == meta2.disruption_count
    assert meta1.disruption_prevalence == meta2.disruption_prevalence


def test_synthetic_generator_size_and_schema() -> None:
    generator = SyntheticDatasetGenerator(seed=99)
    df, meta = generator.generate(n_samples=500)

    assert len(df) == 500
    assert meta.observation_count == 500
    assert "is_disrupted" in df.columns
    assert set(df["is_disrupted"].unique()).issubset({0, 1})
    assert 5.0 <= meta.disruption_prevalence <= 40.0  # Plausible minority rate


def test_synthetic_generator_chronological_ordering() -> None:
    generator = SyntheticDatasetGenerator(seed=100)
    df, _ = generator.generate(n_samples=200)

    ts_diffs = df["observation_timestamp"].diff().dropna()
    assert (ts_diffs >= pd.Timedelta(0)).all(), "Observation timestamps must be monotonically non-decreasing"
