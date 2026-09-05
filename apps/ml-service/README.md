# Supply Chain Disruption Prediction ML Service

FastAPI-based Machine Learning microservice for the Intelligent Supply Chain Disruption Prediction platform.

In **Phase 7C (ML Service Foundation)**, **Phase 7D (ML Dataset & Feature Engineering Pipeline)**, and **Phase 7E (Model Training & Evaluation)**, this service establishes:
1. The standalone FastAPI service architecture, configuration management, feature transformation layer, and model abstraction contracts.
2. The ML dataset schema, temporal feature extractor, deterministic disruption labeler, temporal dataset splitter, preprocessing pipeline, and synthetic data generation CLI.
3. The reproducible model training and validation pipeline, comparing `LogisticRegression` and `RandomForestClassifier` on chronological splits, selecting the F1-optimal probability threshold on validation data, and serializing `disruption-baseline-v1` for low-latency FastAPI inference.

---

## 1. Technical Stack

- **Language:** Python 3.13+
- **Web Framework:** FastAPI
- **ASGI Server:** Uvicorn
- **Data & Numerical Processing:** NumPy, pandas, scikit-learn
- **Validation & Serialization:** Pydantic v2
- **Model Serialization:** Joblib
- **Testing:** pytest, HTTPX

---

## 2. Project Layout

```text
apps/ml-service/
├── app/
│   ├── api/
│   │   └── routes/
│   │       ├── health.py        # Health and readiness endpoints
│   │       └── prediction.py    # Disruption prediction endpoint (Phase 7E inference)
│   ├── core/
│   │   ├── errors.py            # Custom exceptions and HTTP mappings
│   │   └── logging.py           # Structured logging setup
│   ├── ml/
│   │   ├── data/
│   │   │   ├── cli.py           # Developer CLI for synthetic generation, validation, splitting
│   │   │   ├── extractor.py     # Deterministic temporal feature extractor & label generator
│   │   │   ├── generator.py     # Synthetic dataset generator with plausible distributions
│   │   │   ├── schema.py        # Procurement observation schema & dataset metadata
│   │   │   ├── splitter.py      # Chronological temporal dataset splitter (train/val/test)
│   │   │   └── validator.py     # Dataset schema, bounds, type, and integrity validator
│   │   ├── features/
│   │   │   ├── metadata.py      # Single source of truth for feature definitions & catalog
│   │   │   ├── pipeline.py      # Scikit-learn ColumnTransformer preprocessing pipeline
│   │   │   └── transformer.py   # Deterministic numerical feature transformation
│   │   ├── inference/
│   │   │   └── pipeline.py      # Inference pipeline coordinator
│   │   ├── models/
│   │   │   ├── base.py          # Abstract model contract (BaseDisruptionModel)
│   │   │   └── disruption_model.py # TrainedDisruptionModel implementing Joblib artifact inference
│   │   └── training/
│   │       ├── evaluator.py     # Standardized evaluation metrics (ROC-AUC, PR-AUC, F1, CM)
│   │       ├── trainer.py       # Chronological training & validation comparison pipeline
│   │       └── train.py         # Model training CLI entry point
│   ├── schemas/
│   │   ├── error.py             # Standardized error responses
│   │   ├── health.py            # Health and readiness schemas
│   │   └── prediction.py        # DisruptionPredictionRequest & Response schemas
│   ├── services/
│   │   └── prediction_service.py # Application-level prediction orchestration
│   ├── config.py                # Environment-driven configuration (BaseSettings)
│   └── main.py                  # FastAPI application entry point
├── docs/
│   └── model-training.md        # Comprehensive Phase 7E model training & evaluation documentation
├── models/                      # Serialized model artifacts & metadata
│   ├── disruption_model_v1.joblib
│   └── disruption_model_v1_metadata.json
├── tests/                       # Pytest test suite (66 automated tests)
├── .env.example                 # Example environment variables
├── pytest.ini                   # Pytest discovery configuration
├── requirements.txt             # Pinned project dependencies
└── README.md                    # Service documentation
```

---

## 3. ML Dataset & Feature Engineering Pipeline (Phase 7D)

### 3.1. Observation Granularity

The prediction observation unit is defined as:
> **One PO item / material procurement event at observation timestamp $T$ (Purchase Order creation / placement time).**

Each observation represents an upcoming material procurement event using **only** information available strictly prior to time $T$, eliminating temporal data leakage.

### 3.2. Feature Catalog (Single Source of Truth)

All 10 features derived from the operational domain and Phase 7A design:

| Feature Name | Type | Allowed Range | Lookback | Availability Status | Description / Formula |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `hist_otdr_90d` | Float | `[0.0, 100.0]` % | $[T - 90\text{d}, T)$ | Available | Historical on-time delivery rate over prior 90 days. |
| `hist_avg_delay_90d` | Float | $\ge 0.0$ days | $[T - 90\text{d}, T)$ | Available | Historical average delay in days for late deliveries over prior 90 days. |
| `hist_fulfillment_rate_90d`| Float | `[0.0, 100.0]` % | $[T - 90\text{d}, T)$ | Available | Historical quantity fulfillment: $\min\left(100.0, \frac{\sum \text{receivedQuantity}}{\sum \text{orderedQuantity}} \times 100\right)$ over prior 90 days. |
| `hist_disruptions_90d` | Integer | $\ge 0$ count | $[T - 90\text{d}, T)$ | Available | Count of historical delivery disruptions (delay $\ge 7$d or cancelled POs deduplicated by `purchaseOrderId`) over prior 90 days. |
| `supplier_lead_time_contract`| Integer | $\ge 1$ days | Baseline | Available (Static) | Contractual supplier lead time in days (static contract master data). |
| `material_criticality` | String | `HIGH`, `MEDIUM`, `LOW` | Static BOM | Available (Static) | Material business criticality (encoded ordinally: HIGH=3, MED=2, LOW=1). |
| `order_volume_ratio` | Float | $> 0.0$ ratio | Current ($T$) | Available | Ratio of PO item quantity to supplier operational capacity ($\frac{\text{quantity}}{\text{capacity}}$). |
| `inventory_coverage_days` | Float | $\ge 0.0$ days | At $T$ | **Requires Extension** | Days of stock on hand ($\frac{\text{historicalStockAtT}}{\text{dailyConsumption}}$). Requires historical inventory snapshot extension for operational DB extraction; simulated in synthetic datasets. |
| `po_line_value` | Float | $\ge 0.0$ USD | Current ($T$) | Available | Total monetary value of the PO item line ($\text{quantity} \times \text{unitPrice}$). |
| `supplier_country` | String | ISO-2 code | Profile | Available (Static) | Supplier country code (static profile master data; one-hot encoded, unknown category handled). |

#### Raw vs. Transformed Feature Counts:
* **Raw Tabular Features (10):** 8 numerical features + 2 categorical features (`material_criticality`, `supplier_country`).
* **Transformed Model Feature Matrix (19):** 9 scaled numeric columns (8 continuous + 1 ordinal criticality) + 10 binary One-Hot columns for countries present in training data.
* **Master Data Baseline Assumptions:** `Supplier.capacity`, `Supplier.country`, `Supplier.leadTimeDays`, and `Material.criticality` are treated as static master data attributes known at order placement time, not reconstructed time-series snapshots.

### 3.3. Deterministic Disruption Label Definition & Multi-Item Semantics

The binary target label `is_disrupted` ($y \in \{0, 1\}$) represents whether the upcoming procurement event experienced a disruption **after** observation timestamp $T$:

$$
y = 1 \iff (\text{Delivery Delay} \ge 7\text{ days}) \lor (\text{Post-Placement Cancellation}) \lor (\text{In-Transit Shipment Cancellation})
$$

- Routine minor delays ($< 7$ days) are classified as non-disruptions ($y = 0$).
- **Multi-Item PO Observation Semantics:** Each PO line item is evaluated for disruption based on its specific shipment/cancellation outcome. In historical aggregation, supplier-level disruption counts (`hist_disruptions_90d`) are strictly deduplicated by `purchaseOrderId` so that a cancelled multi-item PO is counted as a single business disruption event rather than artificially inflated.

### 3.4. Temporal Leakage Rules & Protections

1. **Historical Window Boundary:** All supplier historical performance metrics must be computed strictly in $[T - 90\text{d}, T)$.
2. **Current Outcome Exclusion:** The actual arrival date, received quantity, or cancellation state of the current purchase order must NEVER enter historical features.
3. **Future Snapshot Exclusion:** No future supplier performance snapshots or inventory changes may be backdated into historical features.
4. **Training-Only Preprocessor Fitting:** Feature scalers, imputers, and one-hot encoders must be fitted **exclusively on the training split** $\mathcal{D}_{\text{train}}$ and never on validation or test sets.

### 3.5. Missing Data & Categorical Encoding Strategy

- **Missing Numerical Values:** Numerical metrics with insufficient supplier history are imputed using the **median of the training set**, preserving true zeros vs unobserved history.
- **Material Criticality:** Ordinally encoded as `HIGH: 3.0, MEDIUM: 2.0, LOW: 1.0` reflecting the natural business hierarchy.
- **Supplier Country:** Encoded via Scikit-Learn `OneHotEncoder(handle_unknown="ignore")` fitted only on training data to gracefully handle unseen countries during inference.

### 3.6. Temporal Train / Validation / Test Splitting

Data is partitioned strictly chronologically:
- **Training Set (70%):** Oldest observations $[T_0, T_1)$
- **Validation Set (15%):** Middle observations $[T_1, T_2)$
- **Test Set (15%):** Newest observations $[T_2, T_3]$
Enforcing: $\max(T_{\text{train}}) \le \min(T_{\text{val}}) \le \max(T_{\text{val}}) \le \min(T_{\text{test}})$. Random splitting is strictly prohibited.

---

## 4. Model Training & Evaluation (Phase 7E)

### 4.1. Developer Disclaimer
> **IMPORTANT:** Synthetic datasets are generated strictly for development, testing, and pipeline validation. Synthetic data does NOT represent real-world supplier behavior and must NOT be cited as evidence of production machine-learning model accuracy.

### 4.2. Model Training Workflow

```powershell
# 1. Train Model via Python Module
python -m app.ml.training.train --samples 3000 --seed 42 --output-dir models

# 2. Or Execute via PowerShell Script
.\scripts\train-model.ps1 -Samples 3000 -Seed 42
```

### 4.3. Empirical Baseline Results (`disruption-baseline-v1`):

- **Selected Model:** `LogisticRegression` (`class_weight='balanced'`)
- **Optimal Probability Threshold:** `0.46` (selected via validation F1 grid search)
- **Validation PR-AUC:** `0.3768` (vs `0.3726` for Random Forest)
- **Final Test Performance (Untouched Test Set):**
  - **ROC-AUC:** `0.6222`
  - **PR-AUC:** `0.4096`
  - **Precision:** `0.3284`
  - **Recall:** `0.5789`
  - **F1-Score:** `0.4190`
  - **Confusion Matrix:** `[[201, 135], [48, 66]]` (Support: Positives=114, Negatives=336)

---

## 5. REST Endpoints & Inference

### 5.1. Health Check
- **Endpoint:** `GET /api/health`
- **Response:**
  ```json
  {
    "status": "UP",
    "service": "supply-chain-ml-service",
    "version": "0.1.0"
  }
  ```

### 5.2. Readiness Probe
- **Endpoint:** `GET /api/ready`
- **Response (Phase 7E Loaded Model):**
  ```json
  {
    "status": "READY",
    "service": "supply-chain-ml-service",
    "version": "0.1.0",
    "model_available": true,
    "details": "ML model artifact loaded and ready for inference (disruption-baseline-v1)"
  }
  ```

### 5.3. Disruption Prediction
- **Endpoint:** `POST /api/predict/disruption`
- **Request Payload:**
  ```json
  {
    "hist_otdr_90d": 85.5,
    "hist_avg_delay_90d": 2.3,
    "hist_fulfillment_rate_90d": 96.0,
    "hist_disruptions_90d": 1,
    "supplier_lead_time_contract": 14,
    "material_criticality": "HIGH",
    "order_volume_ratio": 1.25,
    "inventory_coverage_days": 18.0,
    "po_line_value": 15400.0,
    "supplier_country": "DE"
  }
  ```
- **Response (Phase 7E Inference):**
  ```json
  {
    "disruption_probability": 0.3842,
    "predicted_label": 0,
    "risk_tier": "MEDIUM",
    "model_version": "disruption-baseline-v1",
    "inference_timestamp": "2026-09-05T13:25:00.000000Z",
    "confidence": null
  }
  ```

---

## 6. Running Automated Tests

Run the full pytest suite (66 tests covering schema validation, leakage safeguards, temporal splitting, generator, model training, and API contracts):

```powershell
.\.venv\Scripts\pytest.exe -v
```
