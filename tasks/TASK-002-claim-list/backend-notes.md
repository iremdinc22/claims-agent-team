# TASK-002 — Backend Implementation Notes

## Handoff status

Backend implementation complete; ready for QA verification and subsequent human review. This is not final feature acceptance. Changes remain uncommitted in the TASK-002 backend worktree.

## Implemented scope

- Added `GET /api/claims` and `GET /api/claims/{claimNumber}` with the approved dedicated DTOs. Internal creator/time metadata never appears in response contracts.
- List defaults to one-based page 1, fixes page size at 10, rejects invalid/repeated/unknown query parameters, and returns correct owner-scoped totals and empty content for valid out-of-range pages. Offset/count/page calculations use long arithmetic.
- Owner-scoped database ordering is incident date descending, creation timestamp descending, then UUID ascending. No age, policy, or status filters are applied. Count/content run within one read-only repeatable-read transaction; detail uses a read-only transaction.
- `CurrentUserProvider` is resolved inside service operations. `DemoCurrentUserProvider` supplies only the backend constant `prototype-demo-user`. No request headers, cookies, query identity, or browser JSON identity determine ownership. Empty/null/failing providers fail closed. Missing and non-owned detail return identical generic 404 payloads.
- New POST claims persist immutable `createdBy` and `createdAt` using the trusted provider and injected `Clock.instant()`. TASK-001 request/response fields, 201 without Location, validation, policy existence, UUID generation, and forced REPORTED are preserved. Unknown JSON metadata/status fields remain ignored.
- Added all four recorded status values without any transition endpoint or company processing logic. GET does not modify records.
- No dependencies, frontend files, requirements, human decisions, domain review, or technical design artifacts were changed. No commit or push was performed.

## Schema upgrade and timestamp provenance

`schema.sql` explicitly begins/commits a PostgreSQL transaction. Fresh schema creation and TASK-001 upgrade converge through idempotent column additions, null-only COALESCE backfill, NOT NULL constraints, removal of the old REPORTED-only check, a catalog-guarded four-status check, and the approved composite owner/date/time/UUID index. No permanent metadata defaults are introduced. Existing populated metadata and recorded claim fields are preserved.

Backfilled legacy `created_at` is the migration timestamp, **not the original historical creation time**. Original recency among TASK-001 demonstration rows is unknown. Equal incident dates and migration timestamps use UUID ascending only for deterministic technical ordering. New claim timestamps represent actual server-side creation time and are preserved on reinitialization. This backfill is approved only for prototype demonstration data.

Setup/restart instructions and reproducible test commands are in `backend/README.md`. Stop the old backend before upgrading; the design assumes a single-instance prototype restart. The DO body uses PostgreSQL single-quoted syntax so Spring SQL initialization keeps its internal semicolons intact.

## Automated validation

Executed on 2026-10-01 using Java 23.0.1 with Java release target 21 and Compose PostgreSQL 17.11:

- `./backend/mvnw -f backend/pom.xml test`: passed the initial 36 service/MVC tests before PostgreSQL tests were added.
- `TASK002_POSTGRES_TESTS=true ./backend/mvnw -f backend/pom.xml clean package`: **BUILD SUCCESS; 45 tests, 0 failures, 0 errors, 0 skipped**, including 9 real-PostgreSQL integration cases. Repeated after final schema/test edits with the same successful result.
- `git diff --check`: passed.

Coverage includes exact list/detail/error response contracts; default/explicit/invalid/repeated/unknown query parameters; malformed UUID; sanitized failures; unavailable identity; owner-only counts and content; non-owned/missing detail; page boundaries for 0, 1, 10, 11, 20, 21; maximum page; no duplicate/omitted records for unchanged fixtures; creation-time sorting across pages; incident-date priority; UUID fallback; all statuses remaining unchanged after reads; browser header/JSON identity spoofing; trusted creator and microsecond-precision actual timestamp persistence; TASK-001 creation/validation regressions; persistence failure; fresh schema/JPA validation; original TASK-001 populated schema upgrade; partial metadata preservation; null-only migration backfill; repeated migration; preservation of new-claim timestamps/owners; rejection of missing metadata and unknown database status.

Integration tests use randomly named schemas within the Compose database and clean up only their own schemas. They do not alter demonstration claims. PostgreSQL tests are opt-in with `TASK002_POSTGRES_TESTS=true`; without it they are explicitly skipped. No H2 or test dependency was added. Detailed final results remain in `backend/target/surefire-reports/`; the final build log is `/tmp/task002-build.log`.

## Limitations and QA handoff

All browser clients share the trusted prototype demo context; this is owner-scoped retrieval, not real visitor authentication. Future authentication replaces the provider. Separate page requests are not a stable dataset snapshot; concurrent inserts may shift page boundaries, as documented in the approved design.

Frontend build/browser verification was not performed in this backend-only worktree; frontend dependencies are absent. Frontend integration, independent acceptance verification, and final human approval remain with their respective owners. QA should run the opt-in PostgreSQL command and verify the merged UI against the shared API contracts. The Compose PostgreSQL service started for verification remains available on port 55432; no application server was left running.
