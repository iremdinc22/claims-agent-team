# Claims Agent Team

Claims Agent Team is a deliberately small full-stack sandbox for experimenting
with an AI agent-based software development workflow.

This initial skeleton contains no Claims domain model or business logic.

## Prerequisites

- Java 21
- Docker with Docker Compose
- Node.js 20 or newer
- npm

The backend includes the Maven Wrapper, so a separate Maven installation is not
required.

## Start PostgreSQL

```bash
docker compose up -d postgres
```

The default local database settings are:

- Database: `claims_agent_team`
- Username: `claims_agent`
- Password: `claims_agent`
- Host port: `55432` (mapped to PostgreSQL port `5432` inside the container)

These values can be overridden through the environment variables documented in
[`backend/src/main/resources/application.yml`](backend/src/main/resources/application.yml).

## Run the backend

```bash
cd backend
./mvnw spring-boot:run
```

The backend starts on <http://localhost:8080>.

## Run the frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend development server starts on <http://localhost:5173>.

## Build

```bash
cd backend && ./mvnw clean package
cd frontend && npm ci && npm run build
```
