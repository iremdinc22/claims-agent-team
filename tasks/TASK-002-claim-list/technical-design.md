# TASK-002 — Claim List and Detail Technical Design

## Status

**READY FOR IMPLEMENTATION — BACKEND AND FRONTEND MAY WORK IN PARALLEL**

Domain review is APPROVED. The authoritative Human decisions now resolve both previous implementation blockers: use a trusted backend-side prototype demo identity, and assign existing TASK-001 demonstration claims to that identity with migration-time creation metadata. New claims persist the actual creation timestamp and trusted creator. The shared contract below is ready for independent backend/frontend implementation. No full authentication or company-side status transitions are included.

## Sources and Existing Implementation

Reviewed `AGENTS.md`, `agents/software-architect.md`, TASK-002 request, human decisions, requirements, and approved domain review, plus TASK-001 technical design and human approval. Inspected the existing claim controller, service, entity, repository, schema, error handler, configuration, frontend shell/API/types, and dependency manifests.

The latest human product decisions and requirements resolve list columns, initial screen, equal-incident-date ordering, loading, empty state, and list error/retry. The domain review's older broad reference to PQ-01 through PQ-06 does not reopen those answered questions.

Existing stack: Java 21, Spring Boot, Spring Data JPA, PostgreSQL, React, TypeScript, and Vite. Existing creation endpoint is `POST /api/claims`. Claims use UUID claim numbers and date-only incident dates. The database currently restricts status to `REPORTED`. SQL initialization is enabled and JPA validates the schema. No creator, creation timestamp, authentication subsystem, router, or frontend test framework exists.

Retain this stack and direct controller/service/repository flow. Add no dependencies for retrieval, pagination, DTO mapping, or routing. Use the small backend-side demo identity provider described below; no authentication infrastructure or dependency is introduced.

## Technical Overview and Affected Components

1. The application opens on its own-claims list and requests page 1.
2. The backend resolves the trusted current-user identity, queries only that creator's claims, applies fixed ordering and pagination, and returns a dedicated list DTO.
3. Selecting an entry requests a dedicated detail DTO using the claim number. The database lookup includes creator identity.
4. Both endpoints are read-only. They display recorded status without initiating company review or transitions.

Backend: extend `ClaimController`, `ClaimService`, and `ClaimRepository`; add list item, list response, and detail response DTOs; extend `ClaimEntity` with creator identity and creation timestamp; extend `ClaimStatus`; extend existing error handling for GET parameter and not-found failures. Add a small `CurrentUserProvider` boundary with a `DemoCurrentUserProvider` implementation supplying the fixed backend identity described below.

Frontend: add `ClaimList` and `ClaimDetail` in the existing claims feature; extend `types.ts` and `claimsApi.ts`; use the existing `App.tsx` shell and styles to open on the list. Keep the existing creation form and POST behavior available rather than removing accepted TASK-001 functionality. The minimal navigation assumption is documented below.

## Shared API Contract

All responses are JSON. Use relative `/api` URLs through the existing Vite proxy and same-origin deployment boundary. GET requests have no body. Identity is supplied by a trusted server-side mechanism, never by a query parameter or claim field supplied by the browser. No credentials, user-ID header, login flow, or session transport is required for this prototype. Frontend code must not supply identity.

### Claim List

`GET /api/claims?page=1`

| Parameter | Contract |
| --- | --- |
| `page` | Optional positive integer, one-based; default 1. Invalid, zero, negative, repeated, or values above Java's positive integer range return 400. |
| Page size | Fixed at 10, supplied by the server. No client size or sort controls. |

Reject unrecognized query parameters with 400 rather than implying support for user, size, status, or sorting filters.

Successful response: `200 OK`.

```json
{
  "items": [
    {
      "claimNumber": "550e8400-e29b-41d4-a716-446655440000",
      "policyNumber": "MOTOR-POLICY-001",
      "incidentType": "COLLISION",
      "incidentDate": "2025-01-15",
      "status": "REPORTED"
    }
  ],
  "page": 1,
  "pageSize": 10,
  "totalItems": 1,
  "totalPages": 1
}
```

`items` contains only the five confirmed list fields. `totalItems` is a nonnegative JSON integer counting only the current user's claims; `totalPages` is the ceiling of totalItems / 10, or 0 when there are no claims. The response echoes the requested page. A valid page beyond the available range returns an empty `items` array with the correct totals, not a 404. The UI identifies no-own-claims only when `totalItems` is 0; an empty later page is not evidence of no claims. Use a 64-bit calculation for offset and count; avoid overflow before querying.

The backend ordering is `incident_date DESC, created_at DESC, claim_number ASC`. UUID ordering is solely a deterministic technical fallback when both dates/timestamps are identical; it does not represent creation recency. Ordering is applied before pagination across the whole own-claims set. All four statuses and historical/processed claims are included without age, policy, or status filters.

### Claim Detail

`GET /api/claims/{claimNumber}`

`claimNumber` must parse as a UUID. Successful response: `200 OK`.

```json
{
  "claimNumber": "550e8400-e29b-41d4-a716-446655440000",
  "policyNumber": "MOTOR-POLICY-001",
  "incidentType": "COLLISION",
  "incidentDate": "2025-01-15",
  "description": "Rear-end collision at a junction.",
  "status": "REPORTED"
}
```

A valid claim number that does not exist or belongs to another user returns the same 404 payload. No existence or owner information is leaked. Do not retrieve by claim number alone and depend on frontend filtering.

### Shared Field Types

- Claim number: UUID string, matching TASK-001.
- Policy number and description: recorded strings, without normalization or newly imposed domain length limits.
- Incident type: `COLLISION | THEFT | GLASS_DAMAGE | OTHER`.
- Incident date: `YYYY-MM-DD`; remains a calendar date, not a timezone-converted instant.
- Recorded status: `REPORTED | PENDING | APPROVED | REJECTED`.
- Creator identity and creation timestamp are internal persistence/query metadata and are not returned as new visible fields.

TypeScript contract: define `ClaimStatus`, `ClaimListItem`, `ClaimDetail` (list fields plus description), and `ClaimListResponse` (items and the four pagination values above). Preserve `CreateClaimResponse.status` as the literal `REPORTED`. Add `listClaims(page, signal?)` and `getClaim(claimNumber, signal?)` using `fetch` and the existing `ClaimsApiError` pattern.

### Errors

Reuse the TASK-001 shape:

```json
{
  "code": "CLAIM_NOT_FOUND",
  "message": "Claim not found",
  "fieldErrors": {}
}
```

| HTTP status | Code | Use |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | Invalid page/query parameters or malformed claim UUID; use `page`, parameter name, or `claimNumber` in fieldErrors where applicable. |
| 404 | `CLAIM_NOT_FOUND` | Missing or non-owned detail, with identical generic message. |
| 500 | `INTERNAL_ERROR` | Unexpected query, persistence, or application failure; generic user-safe message. |

No GET policy lookup or insurance validation occurs. Do not return owner IDs, descriptions in routine logs, stack traces, or SQL details. Extend the existing handler so parameter conversion errors do not fall into its generic 500 catch-all. Network failures and invalid success payloads become frontend errors, not empty states. No edit, deletion, assessment, payment, or status-transition endpoint is introduced.

## Ownership and Authorization Boundary

The `CurrentUserProvider.currentUserId()` boundary returns the stable backend-only identity `prototype-demo-user` from `DemoCurrentUserProvider`. This literal is a technical identifier, not a browser input or insurance rule. Resolve it inside service operations for both GET endpoints and existing POST creation. Do not read identity from request headers, query parameters, cookies, or JSON fields. Frontend controls are not authorization. No identity override or user-switch endpoint is added.

This is explicitly prototype-only: all browser clients use the same trusted demo context, so this mechanism provides owner-scoped queries but does not distinguish real human visitors. A future authentication task replaces the provider with a real authenticated user context; preserve the query boundary and creator column for that replacement. TASK-002 adds no login, credentials, tokens, or full authentication system.

List counts and page contents must use the same creator predicate. Detail lookup is equivalent to `claim_number = requestedNumber AND created_by = currentUserId`. No policy ownership inference is allowed. If the provider unexpectedly fails or returns a blank identity, fail closed with generic 500 `INTERNAL_ERROR`; never fall back to an unrestricted query. Arbitrary browser identity headers are ignored, GET identity query parameters are rejected as unsupported, and unknown POST identity fields remain ignored under the existing TASK-001 JSON contract.

TASK-001 creation must record the same trusted creator identity for new claims while preserving its request/response fields, required-field rules, policy-existence check, UUID generation, and forced `REPORTED` status. This is necessary integration metadata, not a new insurance eligibility rule. The approved backend demo provider supplies identity without any new browser authentication requirement. Preserve the existing POST request/response contract, 201 success response, and existing 400/422/500 validation behavior; no user or timestamp input fields are added.

## Persistence and Data Access

Required metadata for claims:

| Column | Direction |
| --- | --- |
| `created_by` | Immutable opaque text identifier for the creator, not a policy number. Required for all records exposed by the feature. |
| `created_at` | Immutable PostgreSQL `timestamptz`, mapped to Java `Instant`, assigned on new creation from the existing injected Clock. Required for reliable creation-time ordering. |

Capture timestamp and creator in the creation transaction. Store all statuses as strings; extend Java `ClaimStatus` and replace the existing `claims_status_reported` database check with a named check allowing the four confirmed values. Do not update existing statuses or add transition logic. Retain the read-only status mapping; test fixtures may seed non-REPORTED states for retrieval verification.

Use owner-filtered JPA queries/projections and a separate count as needed, with dedicated response DTOs rather than serializing entities. Add a composite index on `(created_by, incident_date DESC, created_at DESC, claim_number ASC)` as part of the schema upgrade. GET service operations use read-only transactions. For count/content consistency within one list response, use PostgreSQL repeatable-read for the list transaction. Separate requests do not form a stable multi-page snapshot.

### Schema Upgrade and Authorized Backfill

Keep Spring SQL initialization and `ddl-auto: validate`; do not introduce a migration dependency. Update `schema.sql` so fresh database creation and upgrading the existing prototype schema converge on the same final schema before JPA validation. `CREATE TABLE IF NOT EXISTS` alone is insufficient.

Execute the upgrade statements in an explicit PostgreSQL transaction. Upgrade during a single-instance prototype restart, with the old backend stopped, so no old-code inserts can race metadata backfill. Use this sequence:

1. Create existing policy/claim tables if absent; preserve their approved columns, foreign key, and incident-type/description checks. For fresh databases, include the new metadata columns; for existing databases, add `created_by text` and `created_at timestamptz` with `ADD COLUMN IF NOT EXISTS`, initially nullable for upgrade purposes.
2. Backfill only missing metadata in existing TASK-001 demonstration rows: `created_by = COALESCE(created_by, 'prototype-demo-user')` and `created_at = COALESCE(created_at, transaction_timestamp())`, with a predicate matching rows where either metadata value is null. All timestamp backfills in this transaction share the migration timestamp. Preserve non-null metadata, existing claim numbers, recorded fields, and status. No claim deletion or status conversion is performed.
3. Set both metadata columns NOT NULL. Do not leave permanent demo-identity or migration-time column defaults: future inserts must explicitly supply trusted creator and actual creation time. Map both metadata fields as immutable in JPA.
4. Drop `claims_status_reported` with `IF EXISTS`; create `claims_status_allowed` if absent using a PostgreSQL catalog-guarded block, checking `status IN ('REPORTED', 'PENDING', 'APPROVED', 'REJECTED')`. A fresh database uses this final constraint. Validate it; invalid existing data causes startup failure rather than silent rewriting.
5. Create the owner/date/time/UUID index with `CREATE INDEX IF NOT EXISTS`, commit, then let JPA schema validation run. Keep existing idempotent demonstration-policy initialization.

On restart, the null-only backfill changes no already-migrated or newly created rows; timestamps and creator values are preserved. This strategy applies only to the approved prototype demonstration database. It does not authorize assigning unrelated real-world records to the demo user.

**Legacy timestamp provenance:** backfilled `created_at` is migration metadata, explicitly not the original historical creation time. Document this in backend notes and setup documentation; it is not a new displayed claim field. Original recency among these legacy claims is unknown. Equal incident dates and identical migration timestamps use the existing UUID technical fallback, without claiming historical recency. No additional provenance column is needed for this bounded prototype.

**New timestamp provenance:** after the upgrade, assign `created_at = clock.instant()` once immediately before persisting a new claim in its creation transaction, with database timestamp precision accounted for in tests. This is the actual server-side creation timestamp, not client input, incident date, or a repeated migration timestamp. New claims always use the trusted demo creator and `REPORTED` status.

## Frontend Behavior and Minimal Assumptions

Confirmed behavior: list is the initial screen; show the five list fields; fetch page 1 on opening; show a loading indicator during claims loading; render an empty state only after a successful response with totalItems 0; render a clear error and Retry on list failure. Retry requests the failed page again. Pagination uses the server's totals with pages of 10, and selection fetches the selected detail. Render text safely through React and preserve date-only values without timezone shifts.

Use local component state, existing fetch helpers, and no router/state library. Abort or ignore superseded requests so a slower earlier response cannot replace the selected page or detail. Client response validation checks the documented shape/enums and does not turn malformed records into invented domain values.

The following are explicitly provisional technical/UI assumptions for unresolved PQ portions, not additional business requirements:

- **PQ-02:** A simple Back to list action retains the selected page in local state. A separate action retains access to the accepted creation form; no creation-flow redesign is included. Confirm final navigation wording/placement with BA/Human.
- **PQ-04:** While detail loads, show the loading indicator. On detail failure, show a generic unavailable/error message and Back to list, with no fallback claim data. Use a generic 404 presentation for missing/non-owned claims. Missing required payload values or unknown statuses are treated as response-contract errors, without inventing substitute values. Final wording remains BA/Human-owned.
- **PQ-05:** Fetch on list opening/page navigation/retry and detail selection. No polling or live subscriptions. Returning to the list fetches its retained page again. Offset pagination reflects each request's current dataset; concurrent additions can move entries between pages. If a retained page becomes empty with positive totalItems, offer navigation to an available page rather than label it no claims. Refresh timing and stronger cross-page snapshot expectations still require BA/Human confirmation.
- **PQ-06:** Display recorded uppercase status labels; add no new domain explanations of historical/processed terms. Additional explanatory wording remains BA/Human-owned.

These assumptions allow basic component implementation and are reversible. They do not close the corresponding product questions or alter acceptance criteria.

## Responsibilities and Parallel Contract Handoff

Backend owns GET DTOs, query validation, error mapping, trusted ownership enforcement, fixed sorting/page size, metadata integration with POST, schema upgrade, repository tests, and backend notes. It must preserve all approved intake behavior and implement no company-side transitions.

Frontend owns the initial list/detail components, typed API methods, loading/empty/error/retry states, pagination, safe text/date rendering, stale-response protection, and frontend notes. It can use contract fixtures for owned, empty, failed, and all-status responses while backend implementation proceeds. It must not invent identity headers or implement authorization by client filtering.

Developers share the exact paths, one-based page numbering, JSON field names, error model, enum values, and fixed ordering above. Contract changes return to the Architect; business changes return to BA/Domain Expert. No agents or implementation work are started by this design artifact.

## Validation and Testing Strategy

Backend automated tests using existing test dependencies:

- MVC tests: list defaults and explicit pages; invalid/repeated/unknown parameters; malformed UUID; exact DTO fields; error shapes; browser-supplied identity query parameters rejected and identity headers unable to override the demo provider.
- Service/repository tests: demo-owned and other-owner fixture records; counts and items scoped to the demo owner; missing and non-owned detail both 404; no unrestricted fallback if the provider fails. Use provider replacement only in tests, never a browser-controlled identity override.
- Ordering fixtures: incident dates primary, created_at secondary, UUID deterministic fallback when both match; equal incident dates spanning page boundaries.
- Pagination: 0, 1, 10, 11, 20, and 21 own claims, other-user records interspersed, and out-of-range page. No omissions/duplicates for an unchanged dataset.
- All four statuses remain visible and unchanged after GET; historical/processed claims remain visible. No additional policy/coverage lookup is called.
- Existing creation regression tests: required fields, policy existence, future-date restriction, UUID, `REPORTED`, and error behavior. Assert with a fixed Clock that new claims receive `prototype-demo-user` and actual server creation time. Spoofed owner, timestamp, and status inputs must not alter these values.

PostgreSQL integration verification: fresh initialization and upgrade of a populated TASK-001 schema; row preservation; approved metadata backfill; four-value status constraint; owner-scoped queries; deterministic date/time sorting and counts; restart/idempotence; schema validation. Use the existing Compose database, not an H2 substitute. Verify legacy null metadata is assigned the demo owner and migration timestamp; preexisting non-null metadata and other-owner fixtures remain untouched. Re-run initialization and prove that neither legacy nor new timestamps/owners change. Verify creation after upgrade persists actual Clock time and appears in the demo list/detail without credentials. Verify new inserts missing metadata fail constraints. Capture that the legacy timestamp is migration metadata, not original history.

Frontend verification with contract fixtures and live backend: direct initial list, all five fields, six detail fields, page boundaries and tie ordering, all statuses, no status/edit/delete controls, loading indicator, empty success, list error/retry (including repeated failure), stale responses, and detail error handling under documented assumptions. Run the existing frontend build; no new frontend test dependency is needed solely for this task.

QA verifies the requirements' acceptance criteria, ownership isolation including direct detail access, and TASK-001 regression. Company-side updates may be represented by explicit test fixtures to verify reading changed recorded status; tests must not introduce production transition endpoints. Run backend automated tests and frontend build when implementing; this artifact-only design does not claim implementation validation.

## Resolved Blockers and Remaining Questions

- **Identity blocker resolved:** Human explicitly authorized the backend-only demo identity and deferred full authentication. The provider and browser-independent contract above implement that decision.
- **Legacy-data blocker resolved:** Human explicitly authorized assigning existing demonstration claims to the demo owner and using migration timestamp as `created_at`. The idempotent null-only backfill above preserves records and documents provenance.

No material technical or insurance-domain blocker remains for this prototype scope. Remaining PQ-02/PQ-04/PQ-05/PQ-06 portions retain the explicitly documented reversible assumptions above; they are not silently declared business decisions. Return any proposed additional domain or product rule to its owner.

## Implementation Handoff

**READY FOR IMPLEMENTATION — BACKEND AND FRONTEND MAY WORK IN PARALLEL.**

Backend implements the fixed demo provider, schema upgrade/backfill, creation metadata, owner-scoped read APIs, and automated/integration verification. Frontend implements against the unchanged JSON contracts using fixtures until the live APIs are ready; it sends no identity information. Developers must preserve approved TASK-001 intake behavior, all four recorded statuses, and read-only TASK-002 behavior. QA follows implementation; final human approval remains required for feature acceptance.
