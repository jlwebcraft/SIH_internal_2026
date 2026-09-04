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

The ML service will be a Python FastAPI service. It will later support disruption prediction workflows using pandas, NumPy, scikit-learn, and other appropriate ML libraries.

## Authentication And Firebase

Authentication and Firebase-related services are planned for a later phase. They are intentionally not included in the current foundation phase.
