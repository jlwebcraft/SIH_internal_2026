# Phase 7E — Disruption Model Training & Evaluation Pipeline

## 1. Overview & Objective

Phase 7E establishes a reproducible, leakage-free machine learning training, validation comparison, optimal threshold selection, and test evaluation pipeline for forward-looking purchase order disruption prediction.

> **DEVELOPER DISCLAIMER (Synthetic Dataset):**
> The model trained in this phase utilizes a synthetic procurement dataset generated strictly for development, pipeline validation, and architecture benchmarking. Synthetic performance metrics do **not** represent verified real-world supplier accuracy. Production training will require historical PostgreSQL operational ledgers.

---

## 2. Dataset & Temporal Partitioning

- **Dataset Identifier:** `procurement_dataset_v1.csv` (3,000 observations, 629 disruptions, 20.97% prevalence)
- **Chronological Split Rule:**
  - **Training Split (70%, 2,100 rows):** Oldest observations $[T_0, T_1)$ — used exclusively to fit preprocessing imputers, scalers, one-hot encoders, and train candidate estimators.
  - **Validation Split (15%, 450 rows):** Middle observations $[T_1, T_2)$ — used exclusively for model comparison and optimal probability threshold search.
  - **Test Split (15%, 450 rows):** Newest observations $[T_2, T_3]$ — evaluated **exactly once** on the finalized model and threshold.
- **Leakage Invariant:** $\max(T_{\text{train}}) \le \min(T_{\text{val}}) \le \max(T_{\text{val}}) \le \min(T_{\text{test}})$ without observation ID overlap.

---

## 3. Feature Set & Preprocessing

### 3.1. Raw Feature Input (10 Features):
1. `hist_otdr_90d` (float, %)
2. `hist_avg_delay_90d` (float, days)
3. `hist_fulfillment_rate_90d` (float, %)
4. `hist_disruptions_90d` (int, count)
5. `supplier_lead_time_contract` (int, days)
6. `material_criticality` (ordinal str: HIGH, MEDIUM, LOW $\to$ 3.0, 2.0, 1.0)
7. `order_volume_ratio` (float, ratio)
8. `inventory_coverage_days` (float, days)
9. `po_line_value` (float, USD)
10. `supplier_country` (categorical str, ISO-2)

### 3.2. Preprocessing Strategy (`FeaturePipeline`):
- **Numerical Features:** Imputed using **training-set median** (`SimpleImputer(strategy='median')`) and standardized (`StandardScaler()`).
- **Country Feature:** Encoded using Scikit-Learn `OneHotEncoder(handle_unknown='ignore', sparse_output=False)` fitted strictly on training data.
- **Unseen Categories:** Novel countries at inference map cleanly to all-zero one-hot vectors without lookahead bias.

---

## 4. Candidate Models & Training Configurations

| Model | Estimator | Hyperparameters | Purpose |
| :--- | :--- | :--- | :--- |
| **Baseline** | `LogisticRegression` | `class_weight='balanced', solver='lbfgs', max_iter=1000, random_state=42` | Linear interpretable baseline with balanced class weights. |
| **Comparison** | `RandomForestClassifier` | `n_estimators=100, max_depth=6, min_samples_split=10, min_samples_leaf=5, class_weight='balanced', random_state=42` | Tree ensemble capturing non-linear interactions with conservative depth. |

---

## 5. Model Selection & Threshold Optimization

### 5.1. Model Selection Criteria:
1. **Primary Metric:** **PR-AUC (Average Precision score)** due to disruption minority class (~21%).
2. **Secondary Metrics:** Positive-class F1-score and positive-class Recall on validation set.
3. **Supporting Metric:** ROC-AUC.

### 5.2. Threshold Selection:
- Sweep thresholds $\tau \in [0.10, 0.90]$ with step $0.02$ on the **validation split**.
- Select $\tau^*$ maximizing validation F1-score.

---

## 6. Empirical Validation & Final Test Results

### 6.1. Validation Comparison:
- **Logistic Regression:**
  - ROC-AUC: `0.6723`
  - PR-AUC: `0.3768`
  - Optimal Threshold: `0.46` (Precision: `0.3133`, Recall: `0.7157`, F1: `0.4358`)
- **Random Forest Classifier:**
  - ROC-AUC: `0.6664`
  - PR-AUC: `0.3726`
  - Optimal Threshold: `0.50` (Precision: `0.3631`, Recall: `0.5588`, F1: `0.4402`)
- **Selection Decision:** **LogisticRegression** selected based on highest PR-AUC (`0.3768`), superior positive recall (`71.57%`), and strong linear interpretability.

### 6.2. Final Test Performance (Untouched Test Set @ $\tau = 0.46$):
- **ROC-AUC:** `0.6222`
- **PR-AUC:** `0.4096`
- **Precision:** `0.3284` (32.84%)
- **Recall:** `0.5789` (57.89%)
- **F1-Score:** `0.4190`
- **Confusion Matrix:**
  - True Negatives (TN): `201`
  - False Positives (FP): `135`
  - False Negatives (FN): `48`
  - True Positives (TP): `66`
- **Support:** Positives=`114`, Negatives=`336`, Total=`450`

---

## 7. Model Artifact & Reproducibility

### 7.1. Artifact Storage:
- Serialized Model Bundle: `apps/ml-service/models/disruption_model_v1.joblib`
- Version Metadata: `apps/ml-service/models/disruption_model_v1_metadata.json`
- Model Version: `disruption-baseline-v1`

### 7.2. Execution Commands:
```powershell
# Run training via Python module
python -m app.ml.training.train --samples 3000 --seed 42 --output-dir models

# Or run via PowerShell script
.\scripts\train-model.ps1 -Samples 3000 -Seed 42
```
