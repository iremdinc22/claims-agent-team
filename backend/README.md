# TASK-002 backend setup and verification

Start the existing Compose PostgreSQL service from the repository root with `docker compose up -d postgres`.
Stop old backend instances before starting the upgraded backend; SQL initialization upgrades the prototype schema in one transaction before JPA validation.

Existing TASK-001 demonstration rows receive `prototype-demo-user` and migration-time `created_at` only where metadata is null. This timestamp is migration metadata, **not original historical creation time**. Restart preserves populated metadata and all recorded claim values/statuses. Equal legacy incident dates/timestamps use UUID ascending as a deterministic fallback; historical creation recency is unknown.

New claims explicitly persist the trusted backend creator and actual `Clock.instant()` creation time. Database columns have no permanent defaults. All browser clients share the prototype demo identity; this does not distinguish real visitors. No identity header, token, login, or browser user ID is needed.

From `backend/`:

```bash
./mvnw test
TASK002_POSTGRES_TESTS=true ./mvnw clean package
```

PostgreSQL integration tests use random schemas and clean up only those test schemas. Demonstration rows are not changed. Set `TASK002_TEST_DATABASE_URL`, `TASK002_TEST_DATABASE_USER`, and `TASK002_TEST_DATABASE_PASSWORD` to target another test PostgreSQL instance; defaults match Compose. Without `TASK002_POSTGRES_TESTS=true`, PostgreSQL integration tests are explicitly skipped.

Read APIs: `GET /api/claims?page=1` and `GET /api/claims/{claimNumber}`. Page size is 10. Unknown query parameters are rejected. Both APIs scope access to the backend identity, with identical 404 responses for missing/non-owned detail. Existing POST request and response contracts remain unchanged.
