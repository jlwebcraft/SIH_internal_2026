# High-Level Architecture

The system will use a monorepo layout with independently organized application areas for the web frontend, backend API, ML service, infrastructure, and documentation.

## Frontend

The frontend will be a React and TypeScript dashboard application built with Vite. Tailwind CSS will provide styling, and Recharts will be used for dashboard visualizations.

## Backend API

The backend API foundation is implemented under `apps/api` as a Java 25 Spring Boot application built with Maven.

The current backend includes:

- Spring Boot application entry point in package `com.sih.supplychain`
- Spring MVC web support
- Minimal health endpoint at `GET /api/health`
- Automated tests for application startup and the health endpoint

Persistence, validation-heavy domain logic, security, and business APIs are not implemented yet.

## Database

PostgreSQL will be the initial relational database. Database schema design and migrations will be handled in a later backend/database phase.

## ML Service

The ML service will be a Python FastAPI service. It will later support disruption prediction workflows using pandas, NumPy, scikit-learn, and other appropriate ML libraries.

## Authentication And Firebase

Authentication and Firebase-related services are planned for a later phase. They are intentionally not included in the current foundation phase.
