import argparse
import json
import sys
from pathlib import Path
import pandas as pd

from app.ml.data.generator import SyntheticDatasetGenerator
from app.ml.data.splitter import TemporalSplitter
from app.ml.data.validator import DatasetValidator


def generate_dataset_cmd(args: argparse.Namespace) -> None:
    generator = SyntheticDatasetGenerator(seed=args.seed)
    df, metadata = generator.generate(
        n_samples=args.samples,
        start_date=args.start_date,
        end_date=args.end_date,
    )

    out_dir = Path(args.output_dir)
    out_dir.mkdir(parents=True, exist_ok=True)

    csv_path = out_dir / f"procurement_dataset_{metadata.dataset_version}.csv"
    meta_path = out_dir / f"procurement_dataset_{metadata.dataset_version}_metadata.json"

    df.to_csv(csv_path, index=False)
    with open(meta_path, "w", encoding="utf-8") as f:
        json.dump(metadata.model_dump(mode="json"), f, indent=2)

    print(f"Generated {len(df)} observations to {csv_path}")
    print(f"Saved dataset metadata to {meta_path}")
    print(f"Disruption count: {metadata.disruption_count} ({metadata.disruption_prevalence}%)")


def validate_dataset_cmd(args: argparse.Namespace) -> None:
    csv_path = Path(args.file)
    if not csv_path.exists():
        print(f"Error: file not found: {csv_path}", file=sys.stderr)
        sys.exit(1)

    df = pd.read_csv(csv_path)
    summary = DatasetValidator.validate_dataframe(df)
    print(f"Dataset '{csv_path.name}' is VALID.")
    print(json.dumps(summary, indent=2))


def split_dataset_cmd(args: argparse.Namespace) -> None:
    csv_path = Path(args.file)
    if not csv_path.exists():
        print(f"Error: file not found: {csv_path}", file=sys.stderr)
        sys.exit(1)

    df = pd.read_csv(csv_path)
    splitter = TemporalSplitter(train_ratio=args.train, val_ratio=args.val, test_ratio=args.test)
    split = splitter.split(df)

    out_dir = Path(args.output_dir) if args.output_dir else csv_path.parent
    out_dir.mkdir(parents=True, exist_ok=True)

    train_path = out_dir / "train.csv"
    val_path = out_dir / "val.csv"
    test_path = out_dir / "test.csv"

    split.train_df.to_csv(train_path, index=False)
    split.val_df.to_csv(val_path, index=False)
    split.test_df.to_csv(test_path, index=False)

    print(f"Temporal split complete:")
    print(f" - Train: {split.train_size} rows -> {train_path}")
    print(f" - Val:   {split.val_size} rows -> {val_path}")
    print(f" - Test:  {split.test_size} rows -> {test_path}")


def main() -> None:
    parser = argparse.ArgumentParser(description="Supply Chain ML Dataset CLI")
    subparsers = parser.add_subparsers(dest="command", required=True)

    # Generate
    gen_parser = subparsers.add_parser("generate", help="Generate synthetic training dataset")
    gen_parser.add_argument("--samples", type=int, default=3000, help="Number of observations")
    gen_parser.add_argument("--seed", type=int, default=42, help="Random seed")
    gen_parser.add_argument("--start-date", type=str, default="2025-01-01", help="Start date (YYYY-MM-DD)")
    gen_parser.add_argument("--end-date", type=str, default="2026-06-01", help="End date (YYYY-MM-DD)")
    gen_parser.add_argument("--output-dir", type=str, default="data", help="Output directory")
    gen_parser.set_defaults(func=generate_dataset_cmd)

    # Validate
    val_parser = subparsers.add_parser("validate", help="Validate a dataset CSV file")
    val_parser.add_argument("file", type=str, help="Path to CSV dataset")
    val_parser.set_defaults(func=validate_dataset_cmd)

    # Split
    split_parser = subparsers.add_parser("split", help="Split dataset chronologically into train/val/test")
    split_parser.add_argument("file", type=str, help="Path to CSV dataset")
    split_parser.add_argument("--train", type=float, default=0.70, help="Train ratio")
    split_parser.add_argument("--val", type=float, default=0.15, help="Val ratio")
    split_parser.add_argument("--test", type=float, default=0.15, help="Test ratio")
    split_parser.add_argument("--output-dir", type=str, default=None, help="Output directory")
    split_parser.set_defaults(func=split_dataset_cmd)

    args = parser.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
