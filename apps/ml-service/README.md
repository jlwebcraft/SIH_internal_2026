# Supply Chain Disruption Prediction ML Service

FastAPI-based Machine Learning microservice for the Intelligent Supply Chain Disruption Prediction platform.

In **Phase 7C (ML Service Foundation)** and **Phase 7D (ML Dataset and Feature Engineering Pipeline)**, this service establishes:
1. The standalone FastAPI service architecture, configuration management, feature transformation layer, and model abstraction contracts.
2. The ML dataset schema, temporal feature extractor, deterministic disruption labeler, temporal dataset splitter, preprocessing pipeline, and synthetic data generation CLI.

It does **not** train a machine learning model, return fake predictions, or load unverified dummy models. The prediction endpoint continues to return `503 Model Not Available`.

---

## 1. Technical Stack

- **Language:** Python 3.13+
- **Web Framework:** FastAPI
- **ASGI Server:** Uvicorn
- **Data & Numerical Processing:** NumPy, pandas, scikit-learn
- **Validation & Serialization:** Pydantic v2
- **Testing:** pytest, HTTPX

---

## 2. Project Layout

```text
apps/ml-service/
├── app/
│   ├── api/
│   │   └── routes/
│   │       ├── health.py        # Health and readiness endpoints
│   │       └── prediction.py    # Disruption prediction endpoint (Phase 7C contract)
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
│   │   └── models/
│   │       └── base.py          # Abstract model contract (BaseDisruptionModel)
│   ├── schemas/
│   │   ├── error.py             # Standardized error responses
│   │   ├── health.py            # Health and readiness schemas
│   │   └── prediction.py        # DisruptionPredictionRequest & Response schemas
│   ├── services/
│   │   └── prediction_service.py # Application-level prediction orchestration
│   ├── config.py                # Environment-driven configuration (BaseSettings)
│   └── main.py                  # FastAPI application entry point
├── models/                      # Future trained model artifacts (.gitkeep)
├── tests/                       # Pytest test suite (57 automated tests)
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
| `hist_fulfillment_rate_90d`| Float | `[0.0, 100.0]` % | $[T - 90\text{d}, T)$ | Available | Historical delivery quantity vs ordered quantity over prior 90 days. |
| `hist_disruptions_90d` | Integer | $\ge 0$ count | $[T - 90\text{d}, T)$ | Available | Count of historical delivery disruptions over prior 90 days. |
| `supplier_lead_time_contract`| Integer | $\ge 1$ days | Baseline | Available | Contractual supplier lead time in days. |
| `material_criticality` | String | `HIGH`, `MEDIUM`, `LOW` | Current | Available | Material business criticality (encoded ordinally: HIGH=3, MED=2, LOW=1). |
| `order_volume_ratio` | Float | $> 0.0$ ratio | Current | Available | Ratio of PO item quantity to supplier operational capacity. |
| `inventory_coverage_days` | Float | $\ge 0.0$ days | Current | Available | Days of stock on hand ($\frac{\text{currentStock}}{\text{dailyConsumption}}$). |
| `po_line_value` | Float | $\ge 0.0$ USD | Current | Available | Total monetary value of the PO item line. |
| `supplier_country` | String | ISO-2 code | Current | Available | Supplier country code (one-hot encoded, unknown category handled). |

### 3.3. Deterministic Disruption Label Definition

The binary target label `is_disrupted` ($y \in \{0, 1\}$) represents whether the upcoming procurement event experienced a disruption **after** observation timestamp $T$:

$$
y = 1 \iff (\text{Delivery Delay} \ge 7\text{ days}) \lor (\text{Post-Placement Cancellation}) \lor (\text{In-Transit Shipment Cancellation})
$$

- Routine minor delays ($< 7$ days) are classified as non-disruptions ($y = 0$).
- Label evaluation strictly uses outcomes occurring on or after $T$.

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

## 4. Synthetic Dataset Generator & CLI

### 4.1. Developer Disclaimer
> **IMPORTANT:** Synthetic datasets are generated strictly for development, testing, and pipeline validation. Synthetic data does NOT represent real-world supplier behavior and must NOT be cited as evidence of production machine-learning model accuracy.

### 4.2. CLI Usage

From `apps/ml-service/`:

```powershell
# 1. Generate Synthetic Dataset (e.g., 3,000 samples with seed 42)
.\.venv\Scripts\python.exe -m app.ml.data.cli generate --samples 3000 --seed 42 --output-dir data

# 2. Validate Dataset Integrity and Schema
.\.venv\Scripts\python.exe -m app.ml.data.cli validate data/procurement_dataset_v1.csv

# 3. Create Chronological Temporal Split
.\.venv\Scripts\python.exe -m app.ml.data.cli split data/procurement_dataset_v1.csv --train 0.70 --val 0.15 --test 0.15 --output-dir data
```

PowerShell helper scripts in repository root:
```powershell
# Generate dataset
.\scripts\generate-dataset.ps1 -Samples 3000 -Seed 42

# Validate dataset
.\scripts\validate-dataset.ps1 -FilePath apps/ml-service/data/procurement_dataset_v1.csv
```

---

## 5. Setup and Local Execution

### 5.1. Prerequisites

Ensure Python 3.13+ is installed on your system:

```powershell
python --version
```

### 5.2. Create Virtual Environment

From `apps/ml-service/`:

```powershell
# Create virtual environment
python -m venv .venv

# Activate on Windows (PowerShell)
.\.venv\Scripts\Activate.ps1
```

### 5.3. Install Dependencies

```powershell
pip install -r requirements.txt
```

### 5.4. Run FastAPI Development Server

```powershell
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

Endpoints:
- Base API: `http://localhost:8000`
- OpenAPI Docs: `http://localhost:8000/docs`
- Health: `http://localhost:8000/api/health`
- Readiness: `http://localhost:8000/api/ready`

---

## 6. Running Automated Tests

Run the pytest suite (57 tests covering schema validation, leakage safeguards, temporal splitting, generator, and API contracts):

```powershell
.\.venv\Scripts\pytest.exe
```
