from datetime import datetime, timezone
import json
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple
import joblib
import numpy as np
import pandas as pd
import sklearn
from sklearn.ensemble import RandomForestClassifier
from sklearn.linear_model import LogisticRegression

from app.core.errors import MLServiceException
from app.ml.data.schema import DatasetMetadata
from app.ml.data.splitter import TemporalSplitter
from app.ml.data.validator import DatasetValidator
from app.ml.features.metadata import get_feature_names
from app.ml.features.pipeline import FeaturePipeline
from app.ml.training.evaluator import EvaluationMetrics, ModelEvaluator


class ModelTrainer:
    """Trains, compares, selects, evaluates, and serializes disruption prediction models using strict temporal hygiene."""

    MODEL_VERSION = "disruption-baseline-v1"
    FEATURE_SCHEMA_VERSION = "v1"

    def __init__(
        self,
        seed: int = 42,
        train_ratio: float = 0.70,
        val_ratio: float = 0.15,
        test_ratio: float = 0.15,
    ) -> None:
        self.seed = seed
        self.splitter = TemporalSplitter(train_ratio=train_ratio, val_ratio=val_ratio, test_ratio=test_ratio)

    def train_and_evaluate(
        self,
        df: pd.DataFrame,
        dataset_metadata: Optional[DatasetMetadata] = None,
        output_dir: Optional[Path] = None,
    ) -> Dict[str, Any]:
        """Execute the end-to-end model training, validation comparison, threshold selection, and test evaluation workflow."""
        # 1. Validate input dataset schema and integrity
        validation_summary = DatasetValidator.validate_dataframe(df)

        # 2. Chronological Temporal Split
        split = self.splitter.split(df)
        raw_feature_cols = get_feature_names()
        target_col = "is_disrupted"

        X_train_raw = split.train_df[raw_feature_cols]
        y_train = split.train_df[target_col].to_numpy(dtype=int)

        X_val_raw = split.val_df[raw_feature_cols]
        y_val = split.val_df[target_col].to_numpy(dtype=int)

        X_test_raw = split.test_df[raw_feature_cols]
        y_test = split.test_df[target_col].to_numpy(dtype=int)

        # 3. Fit preprocessing pipeline STRICTLY on training data
        pipeline = FeaturePipeline(scale_numerical=True)
        X_train = pipeline.fit_transform(X_train_raw)

        # 4. Transform validation and test data using training-fitted pipeline
        X_val = pipeline.transform(X_val_raw)
        X_test = pipeline.transform(X_test_raw)

        # 5. Train Candidate 1: Logistic Regression (Baseline)
        lr_clf = LogisticRegression(
            class_weight="balanced",
            random_state=self.seed,
            max_iter=1000,
            solver="lbfgs",
        )
        lr_clf.fit(X_train, y_train)

        # 6. Train Candidate 2: Random Forest (Comparison)
        rf_clf = RandomForestClassifier(
            n_estimators=100,
            max_depth=6,
            min_samples_split=10,
            min_samples_leaf=5,
            class_weight="balanced",
            random_state=self.seed,
            n_jobs=-1,
        )
        rf_clf.fit(X_train, y_train)

        # 7. Evaluate on Validation Set (Threshold Optimization)
        lr_val_probs = lr_clf.predict_proba(X_val)[:, 1]
        rf_val_probs = rf_clf.predict_proba(X_val)[:, 1]

        lr_val_default = ModelEvaluator.evaluate_probabilities(y_val, lr_val_probs, threshold=0.50)
        rf_val_default = ModelEvaluator.evaluate_probabilities(y_val, rf_val_probs, threshold=0.50)

        lr_opt_thresh, lr_val_opt = ModelEvaluator.find_optimal_threshold(y_val, lr_val_probs)
        rf_opt_thresh, rf_val_opt = ModelEvaluator.find_optimal_threshold(y_val, rf_val_probs)

        # 8. Model Selection Rule:
        # 1. Primary criterion: PR-AUC (Average Precision score) for class imbalance
        # 2. Secondary criterion: Positive-class F1 / Recall on validation set
        # 3. Supporting criterion: ROC-AUC
        if rf_val_opt.pr_auc > lr_val_opt.pr_auc:
            selected_model_name = "RandomForestClassifier"
            selected_clf = rf_clf
            selected_threshold = rf_opt_thresh
            selected_val_metrics = rf_val_opt
            selection_reason = (
                f"RandomForestClassifier selected based on higher PR-AUC ({rf_val_opt.pr_auc:.4f} vs "
                f"{lr_val_opt.pr_auc:.4f}) and superior non-linear feature interaction modeling."
            )
        else:
            selected_model_name = "LogisticRegression"
            selected_clf = lr_clf
            selected_threshold = lr_opt_thresh
            selected_val_metrics = lr_val_opt
            selection_reason = (
                f"LogisticRegression selected based on competitive PR-AUC ({lr_val_opt.pr_auc:.4f} vs "
                f"{rf_val_opt.pr_auc:.4f}) and linear model interpretability."
            )

        # 9. Evaluate Selected Model ONCE on Untouched Chronological Test Set
        test_probs = selected_clf.predict_proba(X_test)[:, 1]
        test_metrics = ModelEvaluator.evaluate_probabilities(y_test, test_probs, threshold=selected_threshold)

        # 10. Assemble Metadata
        timestamp_str = datetime.now(timezone.utc).isoformat()
        metadata = {
            "model_version": self.MODEL_VERSION,
            "model_type": selected_model_name,
            "feature_schema_version": self.FEATURE_SCHEMA_VERSION,
            "training_timestamp": timestamp_str,
            "sklearn_version": sklearn.__version__,
            "random_seed": self.seed,
            "is_synthetic": True,
            "synthetic_disclaimer": (
                "Trained on synthetic development procurement dataset for pipeline validation and baseline benchmarking. "
                "Synthetic evaluation metrics do NOT represent verified production supplier performance."
            ),
            "dataset_info": {
                "total_observations": len(df),
                "dataset_version": dataset_metadata.dataset_version if dataset_metadata else "v1",
                "disruptions": validation_summary["disruptions"],
                "prevalence_pct": validation_summary["prevalence_pct"],
            },
            "temporal_split": {
                "train_rows": split.train_size,
                "val_rows": split.val_size,
                "test_rows": split.test_size,
                "train_ratio": 0.70,
                "val_ratio": 0.15,
                "test_ratio": 0.15,
            },
            "raw_features": raw_feature_cols,
            "transformed_feature_count": len(pipeline.get_feature_names_out()),
            "transformed_feature_names": pipeline.get_feature_names_out(),
            "model_selection": {
                "selected_model": selected_model_name,
                "selection_rule": "Primary: PR-AUC (Average Precision), Secondary: Validation F1/Recall, Supporting: ROC-AUC",
                "selection_reason": selection_reason,
                "selected_probability_threshold": selected_threshold,
                "threshold_selection_rule": "F1-optimal threshold identified via validation grid search in [0.10, 0.90]",
            },
            "validation_comparison": {
                "LogisticRegression": {
                    "default_threshold_0.50": lr_val_default.to_dict(),
                    "optimal_threshold": lr_val_opt.to_dict(),
                },
                "RandomForestClassifier": {
                    "default_threshold_0.50": rf_val_default.to_dict(),
                    "optimal_threshold": rf_val_opt.to_dict(),
                },
            },
            "final_test_metrics": test_metrics.to_dict(),
        }

        # 11. Serialize Artifact and Metadata
        if output_dir is not None:
            out_path = Path(output_dir)
            out_path.mkdir(parents=True, exist_ok=True)

            artifact_file = out_path / "disruption_model_v1.joblib"
            meta_file = out_path / "disruption_model_v1_metadata.json"

            artifact_bundle = {
                "preprocessor": pipeline,
                "model": selected_clf,
                "model_name": selected_model_name,
                "model_version": self.MODEL_VERSION,
                "threshold": selected_threshold,
                "raw_feature_names": raw_feature_cols,
                "feature_names_out": pipeline.get_feature_names_out(),
                "created_at": timestamp_str,
            }

            joblib.dump(artifact_bundle, artifact_file)
            with open(meta_file, "w", encoding="utf-8") as f:
                json.dump(metadata, f, indent=2)

            metadata["artifact_path"] = str(artifact_file)
            metadata["metadata_path"] = str(meta_file)

        return metadata
