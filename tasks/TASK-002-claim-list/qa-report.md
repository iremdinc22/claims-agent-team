# TASK-002 — Independent QA Report

## Decision

**PASS — Ready for Human Approval.**

No application defects were found in the executed acceptance, integration, and regression checks. This is QA approval of the bounded prototype implementation, not final acceptance. Human Approval remains required.

QA date: 2026-10-01 (Europe/Istanbul). Role: QA Engineer.

Verified merged `main` commit: `408be27819ff8452d0506c26f75813c056108210`.

## Inputs and Scope

Read AGENTS.md, agents/qa-engineer.md, TASK-002 request, requirements, human decisions, domain review, technical design, backend notes, and frontend notes, plus TASK-001 human approval and technical design. Inspected backend controller/service/repository/entity and PostgreSQL tests, and frontend shell, list/detail, read-state hook, API validation, and build configuration.

Used the latest human decisions and technical contract: all four statuses, backend-only `prototype-demo-user`, owner-scoped reads, fixed page size 10, incident-date/creation-time ordering, and authorized prototype legacy backfill. Evaluated AC-01 through AC-13. Remaining product questions are carried forward under the technical design's provisional assumptions.

Application code was not modified. No commit or push was performed.

## Environment and Execution

- Java 23.0.1, compiling for Java 21; Spring Boot 3.5.6.
- PostgreSQL 17 Docker image; existing TASK-002 PostgreSQL container on port 55432.
- React/Vite frontend from this merged checkout, started on localhost:5175; backend from the newly packaged JAR on localhost:8080.
- Created separate database `task002_qa` for browser/API fixtures. Existing demonstration claims were not changed. Opt-in integration tests used their own randomly named schemas and cleaned those schemas up.
- Browser: Codex in-app browser, real React application → Vite proxy → real Spring API → real PostgreSQL. No mocked success responses were used for this flow.

Commands executed:

```sh
cd backend
./mvnw test
TASK002_POSTGRES_TESTS=true ./mvnw clean package
# Runtime override only; no configuration/source edits:
DATABASE_URL=jdbc:postgresql://localhost:55432/task002_qa \
  java -jar target/backend-0.0.1-SNAPSHOT.jar

cd frontend
npm ci
npm run build
npm run dev -- --host 127.0.0.1 --port 5175 --strictPort
```

Initial sandboxed backend test execution failed because Mockito could not attach its mock-maker to the JVM. Re-executed outside that restriction: **45 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**. This was an execution-environment failure, not an implementation defect.

`docker compose up -d postgres` was attempted. The repository's container could not bind 55432 because the previous TASK-002 backend worktree's PostgreSQL container was already running there. Reused that running PostgreSQL instance with an isolated QA database. Did not stop unrelated containers or the pre-existing frontend on 5173.

## Backend Results

| Scenario | Actual result | Result |
| --- | --- | --- |
| Automated tests and clean package | 11 create-controller + 7 service + 18 read-controller + 9 real-PostgreSQL integration cases passed; packaged executable JAR | PASS |
| PostgreSQL startup/integration | SQL initialization, fresh schema, JPA validation, and backend HTTP startup succeeded against isolated PostgreSQL data | PASS |
| List contract | Live HTTP 200; exact five item fields and pagination metadata; no creator/time/description leakage; default page verified by controller tests | PASS |
| Detail contract | Live HTTP 200 for every seeded own item; exact six fields; values matched corresponding list entries | PASS |
| Ownership isolation | Other-owner newest record excluded from contents and totals; direct detail returned 404 even with spoofed identity | PASS |
| Browser identity spoofing | X-User-ID, Cookie userId, and Authorization header could not select another owner; userId query rejected with 400; POST owner/time/status JSON ignored | PASS |
| Pagination | PostgreSQL parameterized fixtures verified 0/1/10/11/20/21; full pages of 10, correct remainders/totals, no omissions/duplicates; live 21-record dataset yielded 10/10/1 | PASS |
| Out-of-range/overflow | Live page 4 and 2147483647 returned empty items with correct positive totals; no overflow | PASS |
| Ordering | Live incident date 2026-09-30 preceded 2025-01-01; 20 equal-date fixtures ordered by created_at descending across page boundaries. Integration case verified UUID ascending fallback and incident-date priority over creation time | PASS |
| All statuses/history | REPORTED/PENDING/APPROVED/REJECTED all present and retrievable, including old incident dates; reads did not change recorded status | PASS |
| Empty result | PostgreSQL zero-own-record fixture with other-owner data returned totalItems/totalPages 0; empty browser database returned successful empty state | PASS |
| Invalid list queries | Live 400 VALIDATION_ERROR for zero, negative, blank, text, fractional, overflow and repeated page; size, sort, status, userId unsupported parameters also rejected | PASS |
| Invalid/missing/non-owned detail | Malformed UUID 400; missing and non-owned UUIDs returned identical generic 404 payloads; no ownership/existence leak | PASS |
| Legacy upgrade/backfill | PostgreSQL integration test upgraded populated original TASK-001 structure; preserved description/status; assigned demo owner and migration timestamp; preserved populated metadata during partial backfill; repeated schema initialization preserved timestamps/owners | PASS |
| Constraints | PostgreSQL tests rejected absent metadata and unknown status; live columns were NOT NULL with no permanent defaults | PASS |
| New metadata | Browser creation persisted prototype-demo-user and server time 2026-10-01T12:21:41.502806Z. Spoofed POST persisted the same trusted owner and actual time 2026-10-01T12:23:05.371464Z, not supplied year 2000. Restart preserved both values | PASS |
| TASK-001 POST regression | Live 201 UUID/REPORTED with exact two response fields; unknown policy 422; future date, invalid type, blank description, invalid calendar date 400; failures had no claimNumber and created no additional rows | PASS |
| Unexpected failures | Real unavailable QA table produced sanitized INTERNAL_ERROR without SQL/stack details; existing unit tests verified persistence failure produces no success and unavailable identity fails closed | PASS |

The legacy timestamp test verifies migration metadata, not recovery of original historical creation time. That original time was never stored.

## Frontend and End-to-End Results

| Scenario / acceptance coverage | Actual result | Result |
| --- | --- | --- |
| Production build | npm ci and npm run build succeeded; TypeScript passed, Vite transformed 22 modules | PASS |
| Initial list (AC-01) | Opening localhost:5175 immediately showed Your claims and fetched the list | PASS |
| Loading (AC-12) | Loading claims visible on initial opening/navigation; pausing the real backend kept loading visible. Hook inspection confirms the same loading branch for retry | PASS |
| Empty (AC-05) | Fresh QA database showed You have no claims yet after successful loading | PASS |
| List failure and Retry (AC-13) | Stopped backend and unavailable database table produced clear error + Retry, never confirmed empty. Repeated Retry against failure retained error/Retry. Restoring the table then clicking Retry loaded the actual list | PASS |
| Pagination/order/history (AC-02–04) | Browser page 1 and page 2 displayed 10 records; 21-own-record fixture reached page 3 with one PENDING claim and Next disabled. Previous was disabled on page 1. Historical/processed records remained selectable | PASS |
| List fields/statuses (AC-08/09/11) | Five headers and corresponding values rendered; all four uppercase statuses visible. No unsupported insurance/payment explanations introduced | PASS |
| Selection/detail (AC-06/08) | Opened real browser-created claim and each of four status fixtures. Six matching detail fields rendered, including description and unchanged date strings | PASS |
| Read-only (AC-07/10) | Detail offered only Back to list; no editable field, delete action, or status mutation control. Source inspection found no mutation integration. SQL recheck retained REPORTED for newly created claim | PASS |
| Back navigation | Selection from page 2 followed by Back to list retained/refetched page 2 | PASS |
| Detail failure/ownership | Changed an already-listed QA fixture to another owner via test setup, then selected its stale list entry: generic unavailable/error + Back to list, no description or fallback detail exposed | PASS |
| TASK-001 access and success | Report a claim opened accepted form. Browser submission created claim 41f848b5-616f-40bc-8cbe-298c3350916b, showed generated-number success and cleared form; pending submission disabled fields/button | PASS |
| Create → list → detail | Returned to list; created claim appeared first with MOTOR-POLICY-001/COLLISION/2026-09-30/REPORTED. Opening it displayed submitted multiline description and all correct fields | PASS |
| TASK-001 failed submission | Real unknown-policy submission showed Policy does not exist, preserved policy/type/date/description, and showed no success state. Missing incident date also produced required-field feedback | PASS |

Statuses other than REPORTED were seeded directly as disposable database fixtures, representing externally recorded company-side states. QA did not introduce a status-transition endpoint or use the frontend to mutate status.

## Defects and Handoffs

**No application defects found; no developer rework handoff required.**

Remaining PQ-02/PQ-04/PQ-05/PQ-06 product choices remain BA/Human-owned: navigation wording/page retention, final detail-error wording, refresh/concurrent pagination expectations, and optional explanations. Verification follows the documented reversible technical assumptions; it does not close these questions.

## Limitations

- Trusted prototype identity scopes database access but all browser visitors share one demo context. This is not verification of real authentication or separate human accounts.
- Legacy created_at is migration time; historical creation recency cannot be reconstructed. UUID fallback is only deterministic ordering.
- Separate page requests are not a stable snapshot during concurrent changes; unchanged-fixture ordering/pagination was verified.
- Production bundle was built; interactive browser verification used the Vite development server and its approved proxy, not a production hosting deployment.
- No frontend automated test framework exists. Browser DOM/accessibility/screenshot observations and source inspection supplement backend automation and independent live HTTP assertions.
- Comprehensive accessibility, cross-browser/mobile testing, performance/load testing, and every delayed/superseded-response permutation were not undertaken. Browser automation occasionally reported stale-node/selector timeouts; successful explicit state checks and grouped navigation verified the required flows. These tooling errors were not treated as application defects.
- Retry loading is confirmed by shared hook inspection and initial/navigation delayed API observation; repeated failure and recovery were exercised in the actual browser.
- QA database and fixtures are retained in task002_qa for reproduction. Temporary fault injection was reversed. QA-started backend/frontend were stopped after verification; pre-existing PostgreSQL and other services were left running.

## Evidence

- Clean backend test/build log: `/tmp/task002-qa-build.log`.
- Surefire results: `backend/target/surefire-reports/` (45 passing cases).
- Initial/restart runtime logs: `/tmp/task002-qa-server.log`, `/tmp/task002-qa-server-restart.log`.
- Independent live HTTP assertion script: `/tmp/task002-qa-api.py` (passed before later fixture changes).
- Browser-created read-only detail screenshot: `/tmp/task002-qa-detail.jpg`.
- Final source check: only this QA report added; application source unchanged.

**Final QA status: PASS. TASK-002 is ready for Human Approval, not finally accepted.**
