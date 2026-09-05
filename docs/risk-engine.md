# Supplier Performance and Risk Engine Design Specification

## 1. Architectural Overview

The Supplier Performance and Risk Engine provides a deterministic, explainable assessment of supplier operational risk. It operates on historical procurement and delivery records to calculate standardized performance metrics, compute a composite risk score (0–100), and assign actionable risk tiers.

The design maintains a strict decoupling between:
1. **Deterministic Risk Engine (Rule-based & Historical Analytics)**
2. **Predictive ML Engine (Statistical & Machine Learning Disruption Probability)**

```text
Operational Procurement Data
(Purchase Orders, PO Items, Deliveries, Supplier Profiles)
                │
                ▼
  Performance Calculator Engine
(Time-windowed aggregations & metrics)
                │
                ▼
  Supplier Performance Snapshot
(Persisted in supplier_performances table)
                │
                ▼
   Deterministic Risk Engine ────────┐
 (Multi-dimensional weighted score)   │
                │                     │
                ▼                     ▼
      Supplier Risk Score      ML Feature Pipeline
       (Explainable 0–100)     (Feature extraction & training)
                │                     │
                │                     ▼
                │           ML Disruption Probability
                │                     │
                └──────────┬──────────┘
                           ▼
               Combined Decision Layer
       (Downstream impact analysis & simulations)
```

---

## 2. Existing Data & Field Inspection

An inspection of the current database schema (`V1`, `V2`) and JPA domain model reveals the available data sources:

| Source Entity / Table | Available Operational Fields | Utility for Risk Engine |
| --- | --- | --- |
| **`Supplier`** (`suppliers`) | `id`, `code`, `name`, `leadTimeDays`, `capacity`, `reliabilityScore`, `status` | Baseline supplier configuration, profile reliability baseline, master lead time. |
| **`SupplierMaterial`** (`supplier_materials`) | `unitPrice`, `leadTimeDays`, `minimumOrderQuantity`, `maximumCapacity`, `reliabilityScore`, `status` | Material-specific lead times, capacity limits, and relationship reliability scores. |
| **`PurchaseOrder`** (`purchase_orders`) | `id`, `supplier_id`, `poNumber`, `orderDate`, `expectedDeliveryDate`, `actualDeliveryDate`, `status`, `totalAmount` | Order volume, promised delivery schedules, actual completion dates, order cancellation tracking. |
| **`PurchaseOrderItem`** (`purchase_order_items`) | `purchase_order_id`, `material_id`, `quantity`, `unitPrice`, `receivedQuantity`, `status` | Ordered quantity vs received quantity, item-level fulfillment, line values. |
| **`Delivery`** (`deliveries`) | `purchase_order_id`, `trackingNumber`, `dispatchDate`, `expectedArrivalDate`, `actualArrivalDate`, `status`, `delayDays`, `notes` | Dispatch and arrival dates, delay tracking, transit disruptions, delivery status (`PENDING`, `DISPATCHED`, `IN_TRANSIT`, `DELIVERED`, `DELAYED`, `CANCELLED`). |
| **`SupplierPerformance`** (`supplier_performances`) | `supplier_id`, `evaluation_date`, `on_time_delivery_rate`, `average_delay_days`, `lead_time_variance`, `fulfillment_rate`, `rejection_rate`, `capacity_utilization`, `disruption_count`, `overall_score` | Target table for periodic historical snapshots (unique on `[supplier_id, evaluation_date]`). |

---

## 3. Performance Metrics Calculation Methodology

All metrics are evaluated across an explicit time window $W$ (recommended: rolling 90 days prior to `evaluation_date`).

### 3.1. On-Time Delivery Rate (`on_time_delivery_rate`)
- **Definition:** The percentage of completed deliveries that arrived on or before the promised expected arrival date.
- **Data Scope:** Deliveries associated with the supplier's purchase orders where `status = 'DELIVERED'` and `actualArrivalDate` is non-null.
- **On-Time Condition:** `actualArrivalDate <= expectedArrivalDate` (or `delayDays == 0` when actual arrival date is absent).
- **Formula:**
  $$\text{OTDR} = \left( \frac{\text{Count of Completed Deliveries where } \text{actualArrivalDate} \le \text{expectedArrivalDate}}{\text{Total Completed Deliveries}} \right) \times 100$$
- **Edge Cases:**
  - Undelivered shipments (`PENDING`, `DISPATCHED`, `IN_TRANSIT`) are excluded from completed delivery counts.
  - If total completed deliveries = 0, metric evaluates to `NULL` (or baseline configured score), avoiding artificial 0% penalties.

### 3.2. Average Delay Days (`average_delay_days`)
- **Definition:** Mean positive delay in calendar days across all completed deliveries.
- **Formula:**
  $$\text{Delay}_i = \begin{cases} 
  \max(0, \text{actualArrivalDate}_i - \text{expectedArrivalDate}_i), & \text{if } \text{actualArrivalDate}_i \text{ exists} \\
  \max(0, \text{delayDays}_i), & \text{otherwise}
  \end{cases}$$
  $$\text{AvgDelay} = \frac{1}{N} \sum_{i=1}^{N} \text{Delay}_i$$
- **Handling Early Deliveries:** Early arrivals ($\text{actual} < \text{expected}$) yield $\text{Delay}_i = 0$. Negative delay values are not permitted to offset or cancel out late shipments.

### 3.3. Lead-Time Variance (`lead_time_variance`)
- **Definition:** Statistical variance (or mean absolute deviation) between the actual realized delivery cycle and the contracted lead time.
- **Realized Lead Time:** $\text{ActualLeadTime}_i = \text{actualArrivalDate}_i - \text{orderDate}_{\text{PO}}$ (in days).
- **Expected Lead Time:** Contracted `Supplier.leadTimeDays` or `SupplierMaterial.leadTimeDays`.
- **Formula (Mean Absolute Deviation):**
  $$\text{LTV} = \frac{1}{N} \sum_{i=1}^{N} |\text{ActualLeadTime}_i - \text{ExpectedLeadTime}_i|$$
- **Limitation & Data Requirement:** When `orderDate` or `actualArrivalDate` is missing, the delivery is omitted from variance calculation.

### 3.4. Fulfillment Rate (`fulfillment_rate`)
- **Definition:** The proportion of ordered material volume that was successfully delivered/received.
- **Data Scope:** Purchase order items under purchase orders finalized or partially received in window $W$.
- **Formula:**
  $$\text{FulfillmentRate} = \left( \frac{\sum \text{receivedQuantity}_j}{\sum \text{quantity}_j} \right) \times 100$$
- **Constraints:** Clamped between $0\%$ and $100\%$. If `receivedQuantity` is null on an unfulfilled item, it is treated as $0$.

### 3.5. Rejection Rate (`rejection_rate`)
- **Current Data Limitation:** The current schema does not include a `quality_inspections` table or `rejected_quantity` column on `purchase_order_items`.
- **Methodology in Phase 7A/7B:** Must remain `NULL` in `supplier_performances` rather than fabricating fake quality data.
- **Future Schema Extension:**
  ```sql
  ALTER TABLE purchase_order_items ADD COLUMN rejected_quantity NUMERIC(19, 3) DEFAULT 0;
  ALTER TABLE purchase_order_items ADD COLUMN rejection_reason VARCHAR(255);
  ```

### 3.6. Capacity Utilization (`capacity_utilization`)
- **Current Data Limitation:** The schema contains configured static capacity (`Supplier.capacity` and `SupplierMaterial.maximumCapacity`), but does not record total operational workload across external buyers.
- **Defensible Metric:** Active committed order volume across our system as a fraction of configured capacity within window $W$:
  $$\text{InternalUtilization} = \min\left(100.0, \left( \frac{\text{Total Active Ordered Quantity in Window}}{\text{Supplier.capacity}} \right) \times 100\right)$$
- If `Supplier.capacity` is `NULL` or 0, this metric evaluates to `NULL`.

### 3.7. Historical Disruption Count (`disruption_count`)
- **Definition:** Frequency of severe operational disruptions attributed to the supplier within the evaluation window.
- **Disruption Criteria:**
  1. A delivery delayed by $\ge 7$ calendar days (critical delay threshold).
  2. A Purchase Order cancelled after being placed (`status = 'CANCELLED'` where prior status was `PLACED`).
  3. A Delivery marked `CANCELLED` while in transit or dispatched.
- **Formula:**
  $$\text{DisruptionCount} = \text{Count}(\text{Critical Delayed Deliveries}) + \text{Count}(\text{Cancelled Active Orders}) + \text{Count}(\text{Failed In-Transit Deliveries})$$

---

## 4. Data Quality, Completeness & Edge Case Handling

| Edge Case Scenario | Risk Engine Behavior | Rationale |
| --- | --- | --- |
| **New supplier with 0 completed orders** (Cold Start) | Mark historical metrics as `NULL`. Set overall score based on configured profile `reliabilityScore`. Flag status as `INSUFFICIENT_HISTORY`. | Prevents giving an unproven supplier an automatic 0 (Critical Risk) or unearned 100. |
| **Missing `actualArrivalDate` on delivered item** | Fallback to `Delivery.delayDays` if present; otherwise exclude from delay calculations. | Preserves metric integrity without throwing runtime calculation errors. |
| **Orders with `status = 'CANCELLED'`** | Excluded from on-time delivery percentages; included in disruption counters if cancelled after placement. | Cancellation before placement is a draft discard; cancellation after placement indicates failure. |
| **Future planned deliveries** | Excluded from past performance aggregations. | Only realized historical events measure performance. |
| **Duplicate evaluations on same day** | Enforced by PostgreSQL unique constraint `uk_supplier_performances_supplier_date`. Engine updates snapshot idempotently. | Maintains clean time-series history without duplicate records. |

---

## 5. Evaluation Window & Snapshot Strategy

### 5.1. Recommended Window: Rolling 90 Days
- **Why 90 Days?**
  - **Reactivity vs Stability:** A 30-day window is too sparse for low-frequency industrial procurement (e.g. 1–2 POs/month), causing massive score volatility. A 365-day window reacts too slowly to recent supplier degradation.
  - **SIH Alignment:** 90 days represents a standard quarterly evaluation window that captures recent trends while maintaining statistical stability.

### 5.2. Snapshot Lifecycle
- Snapshots are generated:
  1. On a scheduled background cycle (e.g. nightly or monthly cron).
  2. On-demand when a procurement manager requests recalculation.
  3. Pre-event before major purchase order commitment.

---

## 6. Deterministic Supplier Risk Engine Design

The deterministic risk score translates operational performance into a risk index from **0 (Lowest Risk / Best Performance)** to **100 (Highest Risk / Severe Hazard)**.

### 6.1. Risk Dimensions and Normalization

Each dimension produces a normalized sub-score $R_d \in [0, 100]$:

#### Dimension 1: Delivery Risk ($R_{\text{delivery}}$)
- **Inputs:** On-Time Delivery Rate ($\text{OTDR}$) and Average Delay ($\text{AvgDelay}$).
- **Formula:**
  $$R_{\text{otdr}} = 100 - \text{OTDR}$$
  $$R_{\text{delay}} = \min(100.0, \text{AvgDelay} \times 10)$$  *(10 days delay = 100 risk)*
  $$R_{\text{delivery}} = 0.60 \times R_{\text{otdr}} + 0.40 \times R_{\text{delay}}$$

#### Dimension 2: Fulfillment / Volume Risk ($R_{\text{fulfillment}}$)
- **Input:** Fulfillment Rate ($\text{FR}$).
- **Formula:**
  $$R_{\text{fulfillment}} = 100 - \text{FR}$$

#### Dimension 3: Lead-Time Stability Risk ($R_{\text{leadtime}}$)
- **Input:** Lead-Time Variance ($\text{LTV}$).
- **Formula:**
  $$R_{\text{leadtime}} = \min(100.0, \text{LTV} \times 12.5)$$  *(8 days variance = 100 risk)*

#### Dimension 4: Baseline Profile Reliability Risk ($R_{\text{profile}}$)
- **Input:** Configured `Supplier.reliabilityScore` (scale 0–100, where 100 is most reliable).
- **Formula:**
  $$R_{\text{profile}} = 100 - \text{reliabilityScore}$$

#### Dimension 5: Disruption Event Risk ($R_{\text{disruption}}$)
- **Input:** Disruption Count ($\text{DC}$) in 90-day window.
- **Formula:**
  $$R_{\text{disruption}} = \min(100.0, \text{DC} \times 25.0)$$  *(4 disruptions = 100 risk)*

---

### 6.2. Dimension Weights and Dynamic Rebalancing

When all metrics are present, the default weights are:

| Dimension | Default Weight ($w_d$) | Core Driver / Rationale |
| --- | :---: | --- |
| **Delivery Risk** | **35%** ($0.35$) | Direct indicator of schedule reliability and shipment punctuality. |
| **Disruption Risk** | **25%** ($0.25$) | Severe negative events (major delays, cancellations) carry high severity. |
| **Fulfillment Risk** | **20%** ($0.20$) | Short shipments and incomplete orders jeopardize downstream manufacturing. |
| **Lead-Time Stability Risk** | **10%** ($0.10$) | High lead-time variance disrupts MRP planning schedules. |
| **Profile Reliability Risk** | **10%** ($0.10$) | Supplier credit, compliance, and relationship baseline score. |
| **Total** | **100%** ($1.00$) | |

#### Dynamic Missing-Data Weight Rebalancing
If a metric is unavailable (e.g. cold-start supplier with no deliveries):
1. Unavailable dimensions are omitted: $D_{\text{avail}} \subset D$.
2. Active weights are normalized to sum to 1.0:
   $$w'_d = \frac{w_d}{\sum_{k \in D_{\text{avail}}} w_k}$$
   $$\text{FinalRiskScore} = \sum_{d \in D_{\text{avail}}} w'_d \times R_d$$

---

## 7. Deterministic Risk Bands

The composite score maps monotonically to 4 standardized risk tiers:

```text
  0                    25                   50                   75                  100
  ┌────────────────────┬────────────────────┬────────────────────┬────────────────────┐
  │      LOW RISK      │    MEDIUM RISK     │     HIGH RISK      │   CRITICAL RISK    │
  │    (0.00 - 24.99)  │   (25.00 - 49.99)  │   (50.00 - 74.99)  │  (75.00 - 100.00)  │
  └────────────────────┴────────────────────┴────────────────────┴────────────────────┘
```

- **LOW (0–24.99):** Consistent on-time delivery, high fulfillment ($\ge 95\%$), zero critical disruptions. Preferred supplier for critical BOM items.
- **MEDIUM (25–49.99):** Minor occasional delays ($< 3$ days), acceptable fulfillment ($\ge 90\%$). Standard monitoring.
- **HIGH (50–74.99):** Frequent late shipments, lead-time instability, or sub-90% fulfillment. Requires active mitigation / dual-sourcing.
- **CRITICAL (75–100):** Multiple critical disruptions, extensive delays, or high cancellation rates. Triggers automated procurement alerts.

---

## 8. Explainability Framework

Every score evaluation produces an explainability payload that breaks down the calculation into human-readable drivers:

```json
{
  "supplierId": 5,
  "supplierCode": "SUP-001",
  "supplierName": "Precision Dynamics",
  "evaluationDate": "2026-09-05",
  "dataWindow": "90_DAYS",
  "overallScore": 76.50,
  "riskLevel": "CRITICAL",
  "dimensionScores": {
    "deliveryRisk": 82.00,
    "disruptionRisk": 75.00,
    "fulfillmentRisk": 65.00,
    "leadTimeRisk": 70.00,
    "profileRisk": 20.00
  },
  "underlyingMetrics": {
    "onTimeDeliveryRate": 45.00,
    "averageDelayDays": 5.80,
    "leadTimeVarianceDays": 4.20,
    "fulfillmentRate": 87.00,
    "disruptionCount": 3,
    "totalOrdersInWindow": 12,
    "completedDeliveriesInWindow": 10
  },
  "topRiskDrivers": [
    "On-time delivery rate is 45.00% (below 80% benchmark)",
    "Average delivery delay is 5.80 days across 10 completed shipments",
    "3 critical disruption events recorded in the last 90 days"
  ],
  "recommendations": [
    "Identify alternate supplier for high-criticality BOM materials",
    "Increase safety stock buffer by 5.8 days of average consumption"
  ]
}
```

---

## 9. Machine Learning (ML) Boundary & Hybrid Architecture

### 9.1. Architectural Principle: Complementary, Not Substitutive
- **Deterministic Risk Score:** Backward-looking audit and realized performance index. Explains *what has happened* and *supplier baseline health*.
- **ML Disruption Model:** Forward-looking probabilistic classifier ($P(\text{Disruption} \mid \mathbf{x}) \in [0.0, 1.0]$). Predicts *the likelihood of a specific upcoming PO/delivery suffering a disruption*.

```text
Deterministic Score (0-100) ──► Macro Supplier Risk Level
                                           │
                                           ▼
                                 Combined Decision Layer ◄── ML Disruption Probability (0.0 - 1.0)
                                           │
                                           ▼
                               Prescriptive Action & Alerts
```

---

## 10. Future ML Features Categorization

| Feature Name | Category | Source / Derivation |
| --- | :---: | --- |
| `hist_otdr_90d` | **AVAILABLE NOW** | Rolling 90d on-time delivery rate |
| `hist_avg_delay_90d` | **AVAILABLE NOW** | Rolling 90d average delay days |
| `hist_fulfillment_rate_90d` | **AVAILABLE NOW** | Rolling 90d fulfillment percentage |
| `hist_disruptions_90d` | **AVAILABLE NOW** | Rolling 90d severe disruption count |
| `supplier_lead_time_contract` | **AVAILABLE NOW** | `Supplier.leadTimeDays` or `SupplierMaterial` |
| `material_criticality` | **AVAILABLE NOW** | `Material.criticality` (`HIGH`, `MEDIUM`, `LOW`) |
| `order_volume_ratio` | **AVAILABLE NOW** | `PO_item.quantity` / `Material.dailyConsumption` |
| `inventory_coverage_days` | **AVAILABLE NOW** | `Inventory.quantityOnHand` / `Material.dailyConsumption` |
| `po_line_value` | **AVAILABLE NOW** | `PO_item.quantity` $\times$ `PO_item.unitPrice` |
| `supplier_country` | **AVAILABLE NOW** | `Supplier.country` |
| `supplier_rejection_rate` | **REQUIRES NEW DATA** | Quality inspection outcomes / rejected item counts |
| `external_weather_risk_index`| **FUTURE / OPTIONAL** | Logistics route geographic weather risk APIs |
| `supplier_geopolitical_risk` | **FUTURE / OPTIONAL** | Country-level macro risk indicators |

---

## 11. ML Data Sufficiency & Modeling Strategy Assessment

1. **Volume & Sample Size Considerations:**
   - A supervised ML model (e.g. Gradient Boosted Trees / XGBoost / Random Forest) typically requires at least **500–1,000 completed procurement/delivery transactions** with documented outcomes to generalize effectively.
   - Initial enterprise/pilot deployments start with sparse datasets; thus, the **deterministic risk engine must carry full operational load** until transaction history matures.
2. **Disruption Target Definition ($y \in \{0, 1\}$):**
   - $y = 1$ if `delay_days >= 7` OR `status = 'CANCELLED'` (after placement) OR `fulfillment_rate < 80%`.
   - $y = 0$ otherwise.
3. **Class Imbalance Management:**
   - In standard supply chains, disruptions occur in 5–15% of transactions. Class weighting (`scale_pos_weight` in XGBoost), SMOTE, or Precision-Recall AUC (PR-AUC) optimization must be used rather than raw accuracy.
4. **Temporal Integrity (No Data Leakage):**
   - Training and validation must use time-series walk-forward splits (e.g., Train on Month 1–6, Test on Month 7–8) rather than random $k$-fold cross-validation to prevent future-data leakage.

---

## 12. Phase 7B Implemented Endpoints & Verification

The Phase 7B implementation exposes the following deterministic REST endpoints:

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/api/suppliers/{id}/performance` | Computes rolling 90-day performance metrics (OTDR, Avg Delay, LTV, Fulfillment, Capacity Commitment, Disruptions). |
| `GET` | `/api/suppliers/{id}/performance/history` | Returns historical snapshots persisted in `supplier_performances` ordered by `evaluationDate DESC`. |
| `POST` | `/api/suppliers/{id}/performance/snapshot` | Evaluates current performance/risk and creates an idempotent daily snapshot in `supplier_performances`. |
| `GET` | `/api/suppliers/{id}/risk` | Computes deterministic 0–100 risk score, risk level band, dimension breakdown, effective weights, and explainable risk drivers. |

### 12.1. Date Selection and Evaluation Window Rules
- **Window:** Evaluates events where `businessEventDate >= evaluationDate - 90 days` and `businessEventDate <= evaluationDate`.
- **Deliveries:** Evaluated based on `actualArrivalDate` (or `dispatchDate` for in-transit disruptions). Future deliveries (`actualArrivalDate > evaluationDate`) are excluded.
- **Purchase Orders:** Evaluated based on `orderDate` within the 90-day window.

### 12.2. Minimum History and Missing Data Handling
- **Insufficient History Threshold:** Minimum 1 completed delivery or active purchase order in the 90-day window.
- **Dynamic Weight Redistribution:** Unavailable dimensions (e.g. delivery metrics for cold-start suppliers) have their weights proportionally redistributed across available dimensions.
- **Rejection Rate:** Explicitly preserved as `null` without fabricating fictitious quality inspections.
