# Intelligent Supply Chain Disruption Prediction

Intelligent Supply Chain Disruption Prediction is an SIH-level software product for helping organizations monitor supply-chain operations, assess supplier risk, predict disruptions, and support mitigation planning.

## Current Architecture

The project is organized as a monorepo with separate areas for the frontend, backend API, ML service, infrastructure, documentation, and helper scripts.

- `apps/web`: React dashboard frontend
- `apps/api`: Spring Boot REST API backend
- `apps/ml-service`: Python FastAPI service for future prediction workflows
- `infra`: Docker and database-related infrastructure
- `docs`: architecture, domain, and API planning documents
- `scripts`: project automation and helper scripts

## Planned Technology Stack

- Frontend: React, TypeScript, Vite, Tailwind CSS, Recharts
- Backend: Java 25, Spring Boot, Maven, Spring Web, Spring Data JPA, Hibernate, Validation
- Database: PostgreSQL
- ML service: Python, FastAPI, pandas, NumPy, scikit-learn
- Later phases: Spring Security, Firebase authentication/services, XGBoost if appropriate

## Current Development Phase

This repository is currently in the project foundation phase. The monorepo structure and planning documents are being prepared before any application features, database schema, authentication, Firebase integration, or ML models are implemented.
