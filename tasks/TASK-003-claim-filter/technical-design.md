# TASK-003 — Claim Status Filter Technical Design

## Status and Scope

Domain review is **APPROVED**. TASK-003 extends the completed TASK-002 own-claim list with optional recorded-status filtering. It preserves the accepted unfiltered list, ownership boundary, ordering, pagination, read-only details, and TASK-001 creation behavior.

This is a design artifact only. No implementation is performed or other task artifacts changed. No new status values, transitions, authentication, insurance rules, editing/deletion, or other filters are introduced.

## Materials Reviewed and Existing Components

Read `AGENTS.md`, `agents/software-architect.md`, TASK-003 request, requirements and domain review, and TASK-002 technical design, requirements and human approval. Inspected the existing claim controller/service/repository, frontend App, ClaimList, ClaimDetail, claimsApi, types, and useClaimRead.

Retain Java 21 target, Spring Boot 3.5, Spring Data JPA, PostgreSQL 17, React/TypeScript/Vite, and the direct controller-service-repository structure. No dependencies, routing library, state library, migration framework, or new architectural layers are needed.

Current `ClaimController` allows only `page` on list GET; `ClaimService` resolves trusted ownership, counts and reads in repeatable-read; `ClaimRepository` already applies owner/date/time/UUID ordering with SQL LIMIT/OFFSET. `App.tsx` owns page/navigation, `ClaimList.tsx` loads through `useClaimRead`, and `claimsApi.ts` validates the existing DTOs. Extend these components rather than replacing them.

## API Contract

### Updated List Endpoint

`GET /api/claims?page=1&status=PENDING`

GET has no body. The only allowed list query names are `page` and `status`.

| Parameter | Contract |
| --- | --- |
| `page` | Optional one-based positive integer; defaults to 1. Preserve TASK-002 validation: blank, malformed, zero, negative, repeated, and values above 2147483647 return 400. |
| `status` | Optional, single value, exactly REPORTED, PENDING, APPROVED, or REJECTED, case-sensitive. Omission means no status filtering. |
| Page size/order | Server-fixed page size 10 and existing ordering. No size/sort/identity/other filter parameter is supported. |

The UI option All is represented by **omitting `status`**, never by sending `status=All`, `status=ALL`, an empty string, or a fifth enum value. Thus `GET /api/claims`, `GET /api/claims?page=1`, and the UI All request retain the TASK-002 unfiltered result semantics.

Invalid status includes blank/whitespace, lowercase or mixed-case names, All/ALL, unknown values, comma-separated values, and repeated parameters (even identical values). Do not trim, normalize, or ignore invalid supplied values. Validate before database access; return the existing error shape:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "fieldErrors": { "status": "Invalid value" }
}
```

HTTP status is 400. Preserve existing page error mapping and unsupported-query mapping, using the applicable field name. Multiple invalid parameters need not have a new error-precedence contract. Unexpected provider/query failures remain sanitized 500 INTERNAL_ERROR, never successful empty results.

### Unchanged List Response Shape

Successful filtered and unfiltered requests return 200 with the existing `ClaimListResponse` contract:

```json
{
  "items": [{
    "claimNumber": "550e8400-e29b-41d4-a716-446655440000",
    "policyNumber": "MOTOR-POLICY-001",
    "incidentType": "COLLISION",
    "incidentDate": "2025-01-15",
    "status": "PENDING"
  }],
  "page": 1,
  "pageSize": 10,
  "totalItems": 1,
  "totalPages": 1
}
```

`totalItems` counts only own claims matching the optional status; `totalPages` is its ceiling divided by 10. With no matches, items is empty and both totals are 0. The requested page is echoed. A valid page beyond the matching range returns empty items with accurate totals, including when page is 2147483647. Use 64-bit counts and offset arithmetic.

Keep exactly the five item fields and pagination metadata; add no filter echo, owner, timestamp, or description. Status is the recorded status, not a derived value. All field types remain as defined in TASK-002.

### Detail and Creation Remain Unchanged

`GET /api/claims/{claimNumber}` retains UUID validation, no supported query parameters, six detail fields, ownership-restricted lookup, generic identical 404 for missing/non-owned claims, and existing errors. Do not send a status parameter to detail or require its status to match the previously selected list filter. The selected claim may have changed recorded status since listing; detail reads the current record under existing behavior.

`POST /api/claims` retains its exact request/response, validation, 201, trusted creator, actual creation timestamp, UUID generation, and initial REPORTED. List selection is not a creation input.

## Query / Data Flow

After validating query parameters, process the list in this logical order:

1. **Ownership scope:** resolve `CurrentUserProvider.currentUserId()` inside the service and restrict records to `created_by = currentUserId`.
2. **Optional status filter:** if supplied, further restrict to `status = selectedStatus` within that own-claim set.
3. **Existing ordering:** `incident_date DESC, created_at DESC, claim_number ASC` over the complete matching set.
4. **Pagination:** LIMIT 10 and OFFSET `((long) page - 1) * 10`.
5. **Response mapping:** map the selected rows to existing list DTOs and return matching totals.

Count uses the same owner and optional status predicates as content. Resolve owner once per list operation. Count/content remain inside one read-only PostgreSQL repeatable-read transaction. Database query planning may combine predicates; it must preserve these semantics. Filtering a fetched page or loading all claims into the browser to filter is incorrect.

Separate requests retain TASK-002 offset-pagination consistency: concurrent additions/status changes may move entries between pages. No cross-page snapshot, polling, or live subscription is added. Empty items with positive totalItems remain an unavailable-page condition, not a no-match state.

## Backend Responsibilities

### Controller and Validation

Extend list parameter validation to accept `status` in addition to `page`. Check single-value cardinality before parsing with the existing `ClaimStatus` enum. Map parse/cardinality failures to `InvalidClaimQueryException("status")` and the existing advice. Unknown parameters remain rejected, including user identity. Detail and POST controller methods remain behaviorally unchanged.

### Service

Keep `listClaims(int page)` as an unfiltered entry point for existing callers/tests, delegating to the extended operation `listClaims(int page, ClaimStatus status)` with null meaning omitted. Place the existing read-only repeatable-read transaction annotation on both public entry points so external calls retain transaction semantics. Validate page as before; do not interpret null as any stored status.

Branch within the list operation: omitted status uses existing count/page queries; a specific status uses owner-and-status queries. Use identical owner, offset, ordering, mapping, and total-pages calculations. Preserve the optimization returning empty items when offset is outside the matching total. Never fall back to unrestricted queries on provider failure/blank identity. Do not change createClaim, getClaim, policy checks, or metadata assignment.

### Repository and Data Model

Retain `countByCreatedBy`, `findOwnedPage`, and `findByClaimNumberAndCreatedBy`. Add `countByCreatedByAndStatus(String createdBy, ClaimStatus status)` and a bound native page query such as `findOwnedStatusPage(String owner, String status, long offset)`:

```sql
SELECT * FROM claims
WHERE created_by = :owner AND status = :status
ORDER BY incident_date DESC, created_at DESC, claim_number ASC
LIMIT 10 OFFSET :offset
```

Pass `status.name()` as the native text parameter; never build SQL from browser strings. The derived count uses the existing string-mapped enum. Retaining the unfiltered query avoids changing the accepted retrieval path or optional-null SQL typing concerns.

No entity, enum, table, constraint, index, or migration change is required. The existing index supports owner/date ordering; apply the status predicate to that scoped dataset. Do not introduce a new index without a separately justified need. Legacy backfill and actual new timestamps retain TASK-002 provenance and behavior.

## Frontend Responsibilities

### Control and State

Add an accessible select labeled Status with exactly All, REPORTED, PENDING, APPROVED, and REJECTED; use existing enum constants. Define a UI-only `ClaimStatusFilter = 'All' | ClaimStatus` type, keeping All out of `ClaimStatus` and response validators.

Replace the App's page-only state with one list-selection state `{ page: number, status: ClaimStatusFilter }`, initially `{ page: 1, status: 'All' }`. On a different selection, update both values in one state operation to reset page to 1; page navigation changes only page. Selecting the same value does not introduce a reset requirement.

Keep selection in App so existing Back to list navigation retains page **and** filter. Returning from detail or the existing creation screen refetches that retained selection, extending the accepted local page-retention approach without altering either screen. Do not add URL persistence, storage, routing, or automatic filter changes after creation.

Pass the selection and callbacks to ClaimList. Extend its key from page alone to the page/filter pair so a new selection remounts read state and immediately shows loading rather than stale entries. Keep the Status control available in loading, empty, and error states so the user can change selections.

### API Integration and Read States

Extend the helper to `listClaims(page: number, signal?: AbortSignal, status?: ClaimStatus)`; the appended optional argument preserves existing `listClaims(page, signal)` calls. ClaimList maps All to undefined. Build the URL using URLSearchParams with page and, only when defined, status. Send no identity data.

Preserve existing payload validation and `ClaimsApiError` handling. For a filtered success, also reject items whose recorded status does not equal the requested status as a response-contract error; this check supplements backend filtering and is not an ownership mechanism.

The load callback depends on page, filter, and retry attempt. Retry reloads the current selected page/filter and displays the existing loading branch. Reuse `useClaimRead` cancellation: abort superseded requests and prevent stale successes/errors from replacing the newest selection. Invalid payload/network/API failures remain errors with Retry, not empty success.

For successful `totalItems === 0`, show “No claims found” only for a specific status. For All, retain “You have no claims yet.” and existing empty behavior. For empty items with positive totals, retain “This page is no longer available” and Go to first page, keeping the selected status. Pagination labels/buttons use the matching server totals.

Keep all five list fields and existing selection callback. ClaimDetail, getClaim, detail error/loading messages, six fields, and read-only rendering remain unchanged. Add only necessary styling for the filter; preserve existing creation controls and navigation.

## Compatibility / Rework Analysis

| Area | Unchanged | Extension/rework |
| --- | --- | --- |
| List API | Omitted-status requests, JSON DTOs, page rules, errors, fixed size/order | `status` becomes an allowed optional query name; valid values narrow both content and totals |
| Existing unsupported-status test | Other unsupported parameters remain 400 | TASK-002's test expecting `status=REPORTED` to fail must become a success test; invalid-status rejection cases replace it |
| Ownership/history | Backend-only demo identity, fail-closed behavior, all own history under All | Specific status narrows the already authorized set |
| Transactions/storage | Read-only repeatable-read, entity/enum, metadata, schema/backfill/index | Additional owner-and-status repository methods only |
| Detail/creation | Paths, contracts, validation, access, read-only semantics, initial REPORTED | No behavioral extension |
| Frontend reads | Existing hook, DTO validation, list/detail navigation, All states | Status state/control, page reset, URL parameter, matching empty-state message |

Expected source changes during implementation:

- `backend/src/main/java/com/claimsagentteam/claim/ClaimController.java`: list query validation only.
- `backend/src/main/java/com/claimsagentteam/claim/ClaimService.java`: list overload and filtered branch only.
- `backend/src/main/java/com/claimsagentteam/claim/ClaimRepository.java`: filtered count/page methods.
- `frontend/src/App.tsx`: combined page/filter state and callbacks.
- `frontend/src/features/claims/ClaimList.tsx`: filter UI, reads, and filtered empty message.
- `frontend/src/features/claims/claimsApi.ts`: list helper only.
- `frontend/src/features/claims/types.ts`: UI filter type only; existing API/domain types unchanged.
- `frontend/src/styles.css`: only if needed for filter layout.
- Backend read/service/PostgreSQL test files: add filter cases and adapt the formerly unsupported-status assertion. Preserve existing regression assertions.

Components/files to leave unchanged: ClaimEntity, ClaimStatus, IncidentType, CurrentUserProvider/DemoCurrentUserProvider, response DTOs, request/create-response models, policy components, SQL schema/data, backend configuration/dependencies, ApiExceptionHandler/InvalidClaimQueryException (existing mapping suffices), CreateClaimForm, ClaimDetail, useClaimRead, frontend dependency manifests and Vite configuration. Shared files above may change only their listed responsibilities; createClaim/getClaim implementations and validators remain intact. TASK-001/TASK-002 artifacts and TASK-003 request/requirements/domain review remain unchanged.

## Testing Strategy

Use existing backend test dependencies and real Compose PostgreSQL; no H2 or new test framework is required. Fixtures can seed company-recorded statuses without introducing production transition actions.

| Coverage | Required verification |
| --- | --- |
| Four statuses | Each valid status returns only exact matching records and matching totals; list/detail reads leave status unchanged |
| All/unfiltered | Omitted status/default page and explicit pages match TASK-002 results across all statuses, including historical/processed records |
| Ownership | Interleave other-owner matches and own nonmatches; neither affects totals/pages. Spoofed browser identity cannot select another owner; provider failure/blank still fails closed |
| Invalid status/query | Blank, whitespace, lowercase, unknown, All/ALL, comma list, repeated identical/different status yield 400 with status error; existing page/unknown parameter errors persist |
| Pagination | For each status, 0/1/10/11/20/21 matching own records; interspersed nonmatches; exact page counts/remainders and no duplicates/omissions for unchanged data; out-of-range and max page |
| Filtering before pagination | Matches outside the old first unfiltered page are included and fill filtered pages correctly |
| Ordering | Incident date primary, createdAt secondary across boundaries, UUID exact-tie fallback; status and creation time do not override incident date |
| Contracts/errors | Existing exact DTO fields and sanitized failures; no extra metadata or new response fields |
| Page reset | Browser changes filter from page 2/3 to another status and to All; first request/render uses page 1 of the new selection, never the old page |
| Empty | Specific status with no own matches displays “No claims found”, including other-user matches/own other statuses; All empty text stays unchanged |
| Loading/error/retry | Delayed filtered reads show loading; failures/repeated retry remain errors; recovery reloads selected page/status; rapid filter changes cannot commit stale responses |
| Detail regression | Open a result from a filtered later page; verify six fields/read-only view, Back retains selection, and missing/non-owned detail stays generic; no status parameter sent |
| TASK-001 regression | Existing required-field/type/future-date/policy checks, 201 UUID/REPORTED, trusted creator/actual timestamp, and failure behavior; creation remains accessible while list is filtered |
| TASK-002 regression | Existing unfiltered ownership, pagination, history, ordering, detail, states, SQL initialization/metadata preservation, and unsupported parameters other than newly valid status |

Frontend developers can use contract fixtures before live integration; QA must verify the actual UI/API/PostgreSQL flow. No frontend automated framework currently exists: document browser checks for page reset, state handling, navigation, and regressions, and run the existing production build.

Implementation validation commands from the respective directories:

```sh
# backend; Compose PostgreSQL must be available for the second command
./mvnw test
TASK002_POSTGRES_TESTS=true ./mvnw clean package

# frontend
npm ci
npm run build
```

Reuse the existing PostgreSQL opt-in test harness and its TASK002_POSTGRES_TESTS switch for added regression/filter tests; no new configuration is necessary. Record actual results in subsequent implementation notes and QA report. This design does not claim tests were executed or the feature implemented.

## Implementation Boundaries and Handoff

Backend owns list parameter validation, owner-and-status count/content queries, service behavior, and backend automated/integration verification. Frontend owns control/state, list helper, loading/error/empty presentation, and page reset. Both share the exact query and DTO contracts above and may implement independently. API/design ambiguity returns to the Architect; product changes return to BA/Human; domain changes return to Domain Expert/Human.

No material requirement, domain, or technical blocker remains. TASK-002 remains the authority for behavior not explicitly extended here. Preserve its existing refresh/concurrency and detail-error treatment rather than adding new rules. The current human instruction authorizes this design stage; the request's earlier request-only boundary does not block it.

Only this technical design is created now. No backend/frontend notes, QA report, human-approval artifact, code, schema, or dependency changes are made in this review. Implementation, independent QA, and final Human Approval are subsequent stages.

**READY FOR IMPLEMENTATION — BACKEND AND FRONTEND MAY WORK IN PARALLEL.**
