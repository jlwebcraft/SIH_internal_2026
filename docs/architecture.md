# High-Level Architecture

The system will use a monorepo layout with independently organized application areas for the web frontend, backend API, ML service, infrastructure, and documentation.

## Frontend

The frontend will be a React and TypeScript dashboard application built with Vite. Tailwind CSS will provide styling, and Recharts will be used for dashboard visualizations.

## Backend API

The backend API foundation is implemented under `apps/api` as a Java 25 Spring Boot application built with Maven.

The current backend includes:

- Spring Boot application entry point in package `com.sih.supplychain`
- Spring MVC web support
- Spring Data JPA and Hibernate configured for PostgreSQL
- PostgreSQL datasource configuration using environment variables
- Flyway configured to run PostgreSQL migrations from `infra/database/migrations`
- Operational supply-chain JPA entities for suppliers, materials, products, inventory, orders, deliveries, and supplier performance
- Spring Data JPA repositories for operational persistence access
- Core service layer for supplier, material, BOM, and inventory business rules
- Minimal health endpoint at `GET /api/health`
- Automated tests for application startup and the health endpoint

Persistence dependencies, entity mappings, Flyway migrations, repository interfaces, and initial business services are present, but security, REST business APIs, and advanced domain workflows are not implemented yet.

The current service layer owns business invariants such as duplicate business identifiers, required referenced entities, non-negative operational quantities, valid BOM quantities, safe deletes, and inventory adjustment checks. REST controllers and DTO validation will be added in a later phase.

## Database

PostgreSQL is the initial relational database. Operational schema changes are managed by Flyway migrations in `infra/database/migrations`.

Inventory stock adjustment is currently protected by transactional service validation, but advanced concurrency control is not implemented yet. Locking strategy will be revisited when transactional API workflows are introduced.

## ML Service

The ML service is implemented under `apps/ml-service` as a Python 3.13+ FastAPI microservice.

### Architectural Boundary & Responsibilities:
```text
React Frontend (Future)
       │
       ▼
Spring Boot Backend API (apps/api)
       │
       ├─────────────────────────────────┐
       ▼                                 ▼
PostgreSQL Database              FastAPI ML Service (apps/ml-service)
(System of Record)                       │
                                         ▼
                               Feature Engineering
                                         │
                                         ▼
                                ML Model Inference
```

1. **Backend Ownership:**
   - Spring Boot remains the primary business application backend, orchestrating procurement workflows, entity lifecycle state transitions, and operational security.
   - Spring Boot owns the deterministic 5-dimension rolling 90-day supplier performance and risk engine (`SupplierPerformanceService` / `SupplierRiskEngineService`).
   - PostgreSQL is exclusively owned and queried by Spring Boot; the ML service does **not** directly access PostgreSQL in Phase 7C.

2. **ML Service Ownership:**
   - FastAPI owns ML feature engineering, model lifecycle management, and probabilistic inference pipelines.
   - Disruption prediction ($P(\text{Disruption}) \in [0.0, 1.0]$) represents a distinct, forward-looking probabilistic signal.
   - In Phase 7C, the service establishes the FastAPI scaffolding, configuration, candidate feature transformers, and model abstraction contracts, returning explicit `503 Model Not Available` responses rather than manufacturing fake predictions.

3. **Combined Decision Layer (Future):**
   - Downstream recommendation, alert, and simulation engines in Spring Boot will consume both the backward-looking deterministic risk score and forward-looking ML disruption probability to drive prescriptive actions.

## Authentication And Firebase

Authentication and Firebase-related services are planned for a later phase. They are intentionally not included in the current foundation phase.
