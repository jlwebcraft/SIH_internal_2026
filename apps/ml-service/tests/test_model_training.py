from pathlib import Path
import numpy as np
import pandas as pd
import pytest

from app.ml.data.generator import SyntheticDatasetGenerator
from app.ml.data.splitter import TemporalSplitter
from app.ml.features.metadata import get_feature_names
from app.ml.features.pipeline import FeaturePipeline
from app.ml.models.disruption_model import TrainedDisruptionModel
from app.ml.training.evaluator import ModelEvaluator
from app.ml.training.trainer import ModelTrainer
from app.schemas.prediction import DisruptionPredictionRequest, MaterialCriticality


def test_preprocessing_fitted_only_on_training_data() -> None:
    generator = SyntheticDatasetGenerator(seed=123)
    df, _ = generator.generate(n_samples=500)

    splitter = TemporalSplitter(train_ratio=0.70, val_ratio=0.15, test_ratio=0.15)
    split = splitter.split(df)

    raw_features = get_feature_names()
    X_train_raw = split.train_df[raw_features].copy()
    X_val_raw = split.val_df[raw_features].copy()

    pipeline = FeaturePipeline(scale_numerical=True)
    pipeline.fit(X_train_raw)

    # Imputer statistics must equal training median, not full dataset median
    num_imputer = pipeline._preprocessor.named_transformers_["num"].named_steps["imputer"]
    train_medians = X_train_raw[["hist_otdr_90d", "hist_avg_delay_90d"]].median().to_numpy()

    np.testing.assert_allclose(num_imputer.statistics_[:2], train_medians[:2])


def test_validation_and_test_data_cannot_influence_preprocessing_or_model_selection() -> None:
    generator = SyntheticDatasetGenerator(seed=999)
    df_base, _ = generator.generate(n_samples=600)

    trainer = ModelTrainer(seed=42)
    results_base = trainer.train_and_evaluate(df_base)

    # Create perturbed future data in test split only (indices 510 to 600)
    df_perturbed = df_base.copy()
    df_perturbed.iloc[510:, df_perturbed.columns.get_loc("hist_otdr_90d")] = 0.0
    df_perturbed.iloc[510:, df_perturbed.columns.get_loc("hist_avg_delay_90d")] = 50.0

    results_perturbed = trainer.train_and_evaluate(df_perturbed)

    # Preprocessing on train, validation metrics, model selection, and threshold MUST be 100% identical
    assert results_base["model_selection"]["selected_model"] == results_perturbed["model_selection"]["selected_model"]
    assert results_base["model_selection"]["selected_probability_threshold"] == results_perturbed["model_selection"]["selected_probability_threshold"]
    assert results_base["validation_comparison"]["LogisticRegression"]["optimal_threshold"]["pr_auc"] == results_perturbed["validation_comparison"]["LogisticRegression"]["optimal_threshold"]["pr_auc"]


def test_threshold_selection_uses_validation_data_only() -> None:
    y_val = np.array([0, 0, 1, 1, 0, 1, 0, 0])
    probs_val = np.array([0.1, 0.3, 0.6, 0.8, 0.4, 0.7, 0.2, 0.35])

    opt_thresh, metrics = ModelEvaluator.find_optimal_threshold(y_val, probs_val, min_threshold=0.2, max_threshold=0.8, step=0.05)

    assert 0.2 <= opt_thresh <= 0.8
    assert metrics.f1 > 0.5
    assert metrics.threshold == opt_thresh


def test_trained_model_artifact_serialization_and_inference(tmp_path: Path) -> None:
    generator = SyntheticDatasetGenerator(seed=42)
    df, meta = generator.generate(n_samples=400)

    trainer = ModelTrainer(seed=42)
    results = trainer.train_and_evaluate(df=df, dataset_metadata=meta, output_dir=tmp_path)

    artifact_path = tmp_path / "disruption_model_v1.joblib"
    assert artifact_path.exists()

    model = TrainedDisruptionModel(model_path=artifact_path)
    assert model.is_available() is True
    assert model.model_version == "disruption-baseline-v1"

    request = DisruptionPredictionRequest(
        hist_otdr_90d=85.0,
        hist_avg_delay_90d=2.0,
        hist_fulfillment_rate_90d=95.0,
        hist_disruptions_90d=1,
        supplier_lead_time_contract=14,
        material_criticality=MaterialCriticality.HIGH,
        order_volume_ratio=1.2,
        inventory_coverage_days=15.0,
        po_line_value=10000.0,
        supplier_country="DE",
    )

    pred_res = model.predict_request(request)
    assert 0.0 <= pred_res["disruption_probability"] <= 1.0
    assert pred_res["predicted_label"] in (0, 1)
    assert pred_res["model_version"] == "disruption-baseline-v1"
