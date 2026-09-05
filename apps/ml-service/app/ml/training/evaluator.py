from dataclasses import asdict, dataclass
from typing import Any, Dict, List, Tuple
import numpy as np
from sklearn.metrics import (
    average_precision_score,
    confusion_matrix,
    f1_score,
    precision_score,
    recall_score,
    roc_auc_score,
)


@dataclass(frozen=True)
class EvaluationMetrics:
    """Standardized evaluation metrics for binary disruption classification."""

    roc_auc: float
    pr_auc: float
    threshold: float
    precision: float
    recall: float
    f1: float
    confusion_matrix: List[List[int]]
    positive_support: int
    negative_support: int
    total_support: int

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


class ModelEvaluator:
    """Evaluates binary probabilistic disruption classifiers across standard and minority-class metrics."""

    @staticmethod
    def evaluate_probabilities(
        y_true: np.ndarray,
        y_prob: np.ndarray,
        threshold: float = 0.50,
    ) -> EvaluationMetrics:
        """Compute comprehensive evaluation metrics for given ground truth and predicted positive probabilities."""
        y_true_arr = np.asarray(y_true, dtype=int)
        y_prob_arr = np.asarray(y_prob, dtype=float)

        # ROC-AUC and PR-AUC (Average Precision)
        roc_auc = float(roc_auc_score(y_true_arr, y_prob_arr))
        pr_auc = float(average_precision_score(y_true_arr, y_prob_arr))

        # Binary predictions at specified threshold
        y_pred = (y_prob_arr >= threshold).astype(int)

        precision = float(precision_score(y_true_arr, y_pred, zero_division=0))
        recall = float(recall_score(y_true_arr, y_pred, zero_division=0))
        f1 = float(f1_score(y_true_arr, y_pred, zero_division=0))

        cm = confusion_matrix(y_true_arr, y_pred, labels=[0, 1]).tolist()

        pos_support = int((y_true_arr == 1).sum())
        neg_support = int((y_true_arr == 0).sum())
        total_support = int(len(y_true_arr))

        return EvaluationMetrics(
            roc_auc=round(roc_auc, 4),
            pr_auc=round(pr_auc, 4),
            threshold=round(threshold, 4),
            precision=round(precision, 4),
            recall=round(recall, 4),
            f1=round(f1, 4),
            confusion_matrix=cm,
            positive_support=pos_support,
            negative_support=neg_support,
            total_support=total_support,
        )

    @classmethod
    def find_optimal_threshold(
        cls,
        y_true: np.ndarray,
        y_prob: np.ndarray,
        min_threshold: float = 0.10,
        max_threshold: float = 0.90,
        step: float = 0.02,
    ) -> Tuple[float, EvaluationMetrics]:
        """Find the optimal probability threshold maximizing F1-score on validation data."""
        best_threshold = 0.50
        best_f1 = -1.0
        best_metrics: EvaluationMetrics = cls.evaluate_probabilities(y_true, y_prob, threshold=0.50)

        thresholds = np.arange(min_threshold, max_threshold + step / 2, step)
        for t in thresholds:
            t_val = round(float(t), 4)
            metrics = cls.evaluate_probabilities(y_true, y_prob, threshold=t_val)
            if metrics.f1 > best_f1:
                best_f1 = metrics.f1
                best_threshold = t_val
                best_metrics = metrics

        return best_threshold, best_metrics
