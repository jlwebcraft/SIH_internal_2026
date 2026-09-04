# High-Level Architecture

The system will use a monorepo layout with independently organized application areas for the web frontend, backend API, ML service, infrastructure, and documentation.

## Frontend

The frontend will be a React and TypeScript dashboard application built with Vite. Tailwind CSS will provide styling, and Recharts will be used for dashboard visualizations.

## Backend API

The backend will be a Java 25 Spring Boot REST API. It will use Maven for builds and dependency management, Spring Web for HTTP APIs, Spring Data JPA and Hibernate for persistence, and Spring Validation for request and domain validation.

## Database

PostgreSQL will be the initial relational database. Database schema design and migrations will be handled in a later backend/database phase.

## ML Service

The ML service will be a Python FastAPI service. It will later support disruption prediction workflows using pandas, NumPy, scikit-learn, and other appropriate ML libraries.

## Authentication And Firebase

Authentication and Firebase-related services are planned for a later phase. They are intentionally not included in the current foundation phase.
