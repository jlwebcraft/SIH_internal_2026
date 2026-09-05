import argparse
import json
from pathlib import Path
import sys
import pandas as pd

from app.ml.data.generator import SyntheticDatasetGenerator
from app.ml.training.trainer import ModelTrainer


def train_cli(args: argparse.Namespace) -> None:
    """CLI handler for training disruption prediction model."""
    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    if args.dataset and Path(args.dataset).exists():
        print(f"Loading existing dataset from {args.dataset}...")
        df = pd.read_csv(args.dataset)
        metadata = None
    else:
        print(f"Generating synthetic dataset with {args.samples} samples (seed={args.seed})...")
        generator = SyntheticDatasetGenerator(seed=args.seed)
        df, metadata = generator.generate(n_samples=args.samples)

    trainer = ModelTrainer(seed=args.seed, train_ratio=args.train, val_ratio=args.val, test_ratio=args.test)
    print("Training candidate models and evaluating on validation split...")
    results = trainer.train_and_evaluate(df=df, dataset_metadata=metadata, output_dir=output_dir)

    print("\n" + "=" * 60)
    print("MODEL TRAINING & EVALUATION REPORT (Phase 7E)")
    print("=" * 60)
    print(f"Model Version:         {results['model_version']}")
    print(f"Selected Model:        {results['model_type']}")
    print(f"Optimal Threshold:     {results['model_selection']['selected_probability_threshold']}")
    print(f"Selection Reason:      {results['model_selection']['selection_reason']}")
    print("-" * 60)
    print("VALIDATION PERFORMANCE:")
    val_lr = results["validation_comparison"]["LogisticRegression"]["optimal_threshold"]
    val_rf = results["validation_comparison"]["RandomForestClassifier"]["optimal_threshold"]
    print(f" LogisticRegression:     ROC-AUC={val_lr['roc_auc']:.4f}, PR-AUC={val_lr['pr_auc']:.4f}, F1={val_lr['f1']:.4f}, Recall={val_lr['recall']:.4f}")
    print(f" RandomForestClassifier: ROC-AUC={val_rf['roc_auc']:.4f}, PR-AUC={val_rf['pr_auc']:.4f}, F1={val_rf['f1']:.4f}, Recall={val_rf['recall']:.4f}")
    print("-" * 60)
    print("FINAL TEST PERFORMANCE (Untouched Test Set):")
    test_m = results["final_test_metrics"]
    print(f" ROC-AUC:   {test_m['roc_auc']:.4f}")
    print(f" PR-AUC:    {test_m['pr_auc']:.4f}")
    print(f" Precision: {test_m['precision']:.4f}")
    print(f" Recall:    {test_m['recall']:.4f}")
    print(f" F1-Score:  {test_m['f1']:.4f}")
    print(f" Confusion Matrix (TN, FP / FN, TP): {test_m['confusion_matrix']}")
    print(f" Support:   Positives={test_m['positive_support']}, Negatives={test_m['negative_support']}, Total={test_m['total_support']}")
    print("-" * 60)
    print(f"Saved Artifact: {results.get('artifact_path')}")
    print(f"Saved Metadata: {results.get('metadata_path')}")
    print("=" * 60 + "\n")


def main() -> None:
    parser = argparse.ArgumentParser(description="Supply Chain Disruption Model Training CLI")
    parser.add_argument("--dataset", type=str, default=None, help="Path to existing CSV dataset (optional)")
    parser.add_argument("--samples", type=int, default=3000, help="Number of synthetic observations if generating")
    parser.add_argument("--seed", type=int, default=42, help="Random seed")
    parser.add_argument("--train", type=float, default=0.70, help="Train ratio (default: 0.70)")
    parser.add_argument("--val", type=float, default=0.15, help="Validation ratio (default: 0.15)")
    parser.add_argument("--test", type=float, default=0.15, help="Test ratio (default: 0.15)")
    parser.add_argument("--output-dir", type=str, default="models", help="Directory to save model artifacts")

    args = parser.parse_args()
    train_cli(args)


if __name__ == "__main__":
    main()
