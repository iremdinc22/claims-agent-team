# TASK-003 — Backend Implementation Notes

## Handoff status

Backend implementation complete and ready for independent QA verification. Final feature acceptance remains subject to Human Approval. No commit or push was performed.

## Implementation summary

- `GET /api/claims` accepts optional single, case-sensitive status: REPORTED, PENDING, APPROVED, or REJECTED. Omission retains the original TASK-002 controller/service entry point and count/page queries.
- Blank, whitespace, lowercase/mixed-case, All/ALL, unknown, comma-separated, and repeated status values return HTTP 400 with the existing VALIDATION_ERROR shape and `fieldErrors.status = "Invalid value"`, before service/database access. Existing page and unsupported-parameter validation remains intact.
- The service resolves the trusted current user once, then counts and retrieves only that owner's exact recorded-status matches. Bound PostgreSQL predicates apply before LIMIT/OFFSET. Browser identity cannot select another owner; unavailable identity fails closed.
- Both public list entry points retain read-only REPEATABLE_READ transactions. Filtered counts and content use the same predicates. Ordering remains incidentDate DESC, createdAt DESC, UUID ASC; page size remains 10. Totals, DTO fields, 64-bit offsets, and valid out-of-range empty-page behavior remain unchanged.
- No status transitions, dependencies, schema changes, or domain-rule changes were introduced. No design conflict or new assumption was required. The current human implementation authorization supersedes historical request/design-stage restrictions in the reviewed artifacts.

## Files changed

- `backend/src/main/java/com/claimsagentteam/claim/ClaimController.java` — list status validation and dispatch only.
- `backend/src/main/java/com/claimsagentteam/claim/ClaimService.java` — transactional list overload and optional filtered branch only.
- `backend/src/main/java/com/claimsagentteam/claim/ClaimRepository.java` — owner/status count and bound ordered page query.
- `backend/src/test/java/com/claimsagentteam/claim/ClaimReadControllerTest.java` — four-status contracts, exact invalid-status errors, repeated status, sanitized filtered failure, and unchanged detail query rejection; adapted the formerly unsupported-status assertion.
- `backend/src/test/java/com/claimsagentteam/claim/ClaimServiceTest.java` — filtered fail-closed identity, invalid page, and maximum-page coverage.
- `backend/src/test/java/com/claimsagentteam/claim/ClaimPostgresIntegrationTest.java` — each status at 0/1/10/11/20/21 matches, interleaved own nonmatches and other-owner matches, exact page traversal, totals, out-of-range/max pages, creation-recency/UUID ordering across boundaries, incident-date priority, detail/read-only checks, and creation-to-filter regression.
- `tasks/TASK-003-claim-filter/backend-notes.md` — this handoff.

## Tests executed and results

Executed on 2026-10-01 using OpenJDK 23.0.1, Java release target 21, and existing Compose PostgreSQL 17.11 on port 55432. Commands ran from `backend/`:

| Command | Result |
| --- | --- |
| `./mvnw -Dtest=ClaimReadControllerTest,ClaimServiceTest test` | BUILD SUCCESS; 41 tests, 0 failures, 0 errors, 0 skipped |
| `TASK002_POSTGRES_TESTS=true ./mvnw test` | BUILD SUCCESS; 69 tests, 0 failures, 0 errors, 0 skipped |
| `TASK002_POSTGRES_TESTS=true ./mvnw clean package` | BUILD SUCCESS; 69 tests, 0 failures, 0 errors, 0 skipped; executable backend JAR produced |
| `git diff --check` (repository root) | Passed after final documentation update |

The 17 PostgreSQL test invocations include the existing schema-upgrade/metadata regressions and added filter scenarios; each pagination invocation exercises all six boundary counts. Fixtures place 12 newer own nonmatches ahead of selected matches, proving filtering happens before pagination. Other-owner matches do not affect totals or content. Existing unfiltered, missing/non-owned detail, all-status preservation, POST validation/policy/date/error, trusted metadata, UUID, and initial REPORTED checks remain passing.

Logs: `/tmp/task003-relevant.log`, `/tmp/task003-full.log`, `/tmp/task003-build.log`. Final detailed results: `backend/target/surefire-reports/`. PostgreSQL tests use the existing opt-in switch and isolated random schemas; they clean up their schemas without changing demonstration claims.

## Compatibility with TASK-002

The omitted-status path retains the original queries, response shape, fixed pagination, ordering, ownership, history visibility, and error behavior. `createClaim`, `getClaim`, entity/enum, identity provider, DTOs, SQL initialization, configuration, and dependencies are unchanged. No TASK-001/TASK-002 artifacts, approved TASK-003 inputs, or frontend files were modified. All four recorded statuses retain their values and meanings.

## Limitations and QA handoff

No additional backend limitation was introduced. Existing prototype limits remain: all browsers share the trusted demo identity rather than real authentication; separate offset-page requests do not share a stable snapshot when records change concurrently; legacy backfilled timestamps are migration metadata rather than historical creation times.

QA should verify the merged frontend/API flow for All versus specific-status requests, filter reset to page 1, pagination/ordering, no-match presentation, loading/error/retry/cancellation, retained filter/page after detail navigation, ownership and creation regressions. Frontend/browser verification is outside this backend-only handoff. The existing PostgreSQL container remains running; no application server was started.
