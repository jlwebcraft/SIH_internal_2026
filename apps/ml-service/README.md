# Supply Chain Disruption Prediction ML Service

FastAPI-based Machine Learning microservice for the Intelligent Supply Chain Disruption Prediction platform.

In **Phase 7C (ML Service Foundation)**, this service establishes the standalone Python service architecture, configuration management, feature transformation layer, and model abstraction contracts. It does **not** return fake predictions or load unverified dummy models.

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
│   │   ├── features/
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
├── tests/                       # Pytest test suite
├── .env.example                 # Example environment variables
├── requirements.txt             # Pinned project dependencies
└── README.md                    # Service documentation
```

---

## 3. Setup and Local Execution

### 3.1. Prerequisites

Ensure Python 3.13+ is installed on your system:

```powershell
python --version
```

### 3.2. Create Virtual Environment

From the root of the repository or within `apps/ml-service/`:

```powershell
# Navigate to ML service directory
cd apps/ml-service

# Create virtual environment
python -m venv .venv

# Activate on Windows (PowerShell)
.\.venv\Scripts\Activate.ps1

# (Or on Linux/macOS)
# source .venv/bin/activate
```

### 3.3. Install Dependencies

```powershell
pip install -r requirements.txt
```

### 3.4. Run FastAPI Development Server

```powershell
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

The service will be accessible at:
- Base API: `http://localhost:8000`
- Interactive OpenAPI Docs (Swagger): `http://localhost:8000/docs`
- ReDoc Docs: `http://localhost:8000/redoc`

---

## 4. REST Endpoints

### 4.1. Health Check
- **Endpoint:** `GET /api/health`
- **Response:**
  ```json
  {
    "status": "UP",
    "service": "supply-chain-ml-service",
    "version": "0.1.0"
  }
  ```

### 4.2. Readiness Probe
- **Endpoint:** `GET /api/ready`
- **Response (Phase 7C Foundation):**
  ```json
  {
    "status": "READY",
    "service": "supply-chain-ml-service",
    "version": "0.1.0",
    "model_available": false,
    "details": "Service initialized; ML model artifact not loaded (Phase 7C foundation)"
  }
  ```

### 4.3. Disruption Prediction (Contract)
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
- **Phase 7C Response:**
  ```json
  {
    "timestamp": "2026-09-05T10:30:00Z",
    "status": 503,
    "error": "Model Not Available",
    "message": "Disruption prediction ML model is not yet trained or loaded in this phase (Phase 7C foundation).",
    "path": "/api/predict/disruption"
  }
  ```

---

## 5. Running Automated Tests

Run the full pytest suite:

```powershell
pytest
```
