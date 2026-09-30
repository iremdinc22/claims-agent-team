# TASK-001 — Create Claim Technical Design

## Status

**READY FOR IMPLEMENTATION**

The Domain Expert review is `APPROVED`. This design translates the approved functional and domain decisions into a minimal backend/frontend contract without adding insurance rules.

## Technical Overview

TASK-001 will add one synchronous claim-creation flow:

1. The React application presents a motor-claim creation form.
2. The frontend performs immediate required-field, supported-incident-type, and future-date checks.
3. The frontend sends a JSON request to `POST /api/claims`.
4. The Spring Boot backend performs authoritative request validation.
5. The backend verifies that the referenced policy number exists in a minimal PostgreSQL policy-reference table.
6. The backend generates a UUID claim number, creates the claim with status `REPORTED`, and persists it in one transaction.
7. The backend returns the claim number and status.
8. The frontend displays a success message containing the returned claim number.

The backend remains the authority for validation and persistence. The frontend validation exists only to provide timely user feedback.

## Existing Technology and Dependency Direction

Use the existing stack:

- Java 21 and Spring Boot
- Spring Web
- Spring Data JPA
- Spring Validation
- PostgreSQL
- React, TypeScript, and Vite

No production dependency is required for this task.

The backend may add `spring-boot-starter-test` with test scope because the Backend Developer role requires automated tests and the project currently has no test dependency. No additional persistence, mapping, UUID, validation, frontend state-management, routing, form, or API-client library should be added.

## Affected Components

### Backend

Add a small feature package under `com.claimsagentteam.claim` containing:

- `ClaimController` — HTTP boundary for claim creation.
- `CreateClaimRequest` — incoming API model.
- `CreateClaimResponse` — successful API model.
- `ClaimService` — transaction and creation workflow.
- `ClaimEntity` — persisted claim representation.
- `ClaimRepository` — Spring Data JPA repository.
- `IncidentType` — the four approved incident values.
- `ClaimStatus` — contains only `REPORTED` for this task.

Add a minimal package under `com.claimsagentteam.policy` containing:

- `PolicyEntity` — policy-number reference only.
- `PolicyRepository` — existence lookup by policy number.

Add a small shared HTTP error component under `com.claimsagentteam.common`:

- `ApiErrorResponse` — consistent error payload.
- `ApiExceptionHandler` — maps validation, policy-not-found, malformed-request, and unexpected failures to the API contract.

Do not introduce ports/adapters, command buses, mapping frameworks, base entities, generic service layers, or a policy-management API.

### Database Initialization

Keep `spring.jpa.hibernate.ddl-auto: validate`.

Use Spring Boot's existing SQL initialization support rather than adding a migration dependency for this first prototype:

- `schema.sql` creates the `policies` and `claims` tables idempotently.
- `data.sql` inserts one documented demonstration policy idempotently: `MOTOR-POLICY-001`.
- Configure `spring.sql.init.mode: always` so initialization also runs for PostgreSQL before JPA schema validation.

The demonstration policy is test/setup data, not an insurance-domain rule. No endpoint for creating or changing policies is part of TASK-001.

### Frontend

Keep the application as a single-page shell without adding a router. Add:

- `src/features/claims/CreateClaimForm.tsx` — form, submission state, field feedback, and success/error presentation.
- `src/features/claims/types.ts` — API request, success, incident-type, and error types.
- `src/features/claims/claimsApi.ts` — a small `fetch` wrapper for `POST /api/claims`.

Update the existing `App.tsx` and styles to host the form. No additional screens or unrelated shell redesign are required.

Configure the Vite development server to proxy `/api` to `http://localhost:8080`. The frontend should call the relative path `/api/claims`; this avoids backend CORS configuration and preserves a same-origin production boundary.

## API Contract

### Create Claim

`POST /api/claims`

Request content type: `application/json`

#### Request Model

```json
{
  "policyNumber": "MOTOR-POLICY-001",
  "incidentType": "COLLISION",
  "incidentDate": "2025-01-15",
  "description": "Rear-end collision at a junction."
}
```

| Field | JSON type | Required | Contract |
|---|---|---:|---|
| `policyNumber` | string | yes | Non-blank; must exactly reference an existing policy. No case conversion or inferred normalization. |
| `incidentType` | string | yes | One of `COLLISION`, `THEFT`, `GLASS_DAMAGE`, `OTHER`. |
| `incidentDate` | string | yes | ISO-8601 calendar date (`YYYY-MM-DD`); today or earlier. |
| `description` | string | yes | Non-blank. No unapproved domain length or content rule. |

Unknown JSON properties should be ignored using Spring Boot's existing JSON behavior; they are not persisted and do not expand the accepted domain model.

#### Successful Response

Status: `201 Created`

```json
{
  "claimNumber": "550e8400-e29b-41d4-a716-446655440000",
  "status": "REPORTED"
}
```

| Field | JSON type | Contract |
|---|---|---|
| `claimNumber` | string | UUID textual representation generated by the backend and unique in the claims table. |
| `status` | string | Always `REPORTED` for TASK-001. |

Do not return a `Location` header because TASK-001 does not define a claim-retrieval endpoint.

#### Error Response Model

All task-defined errors use:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "fieldErrors": {
    "incidentDate": "Incident date cannot be in the future"
  }
}
```

`fieldErrors` is a JSON object from request field name to a user-safe error message. It may be empty for non-field-specific failures. Internal exception details and stack traces must never be returned.

| HTTP status | Error code | When used |
|---:|---|---|
| `400 Bad Request` | `VALIDATION_ERROR` | Required/blank field, future incident date, unsupported incident type, invalid date format, malformed JSON, or incompatible JSON value. |
| `422 Unprocessable Entity` | `POLICY_NOT_FOUND` | Request is structurally valid but `policyNumber` does not reference an existing policy. Include a `policyNumber` field error. |
| `500 Internal Server Error` | `INTERNAL_ERROR` | Unexpected persistence or application failure. Return a generic message only. |

No duplicate-related `409` response or idempotency contract is defined because duplicate detection is explicitly outside TASK-001. Each accepted HTTP request creates one claim; the frontend prevents repeat clicks while one submission is in progress, but the backend does not infer duplicate business meaning.

## Data Model Direction

### `policies`

| Column | PostgreSQL type | Constraints |
|---|---|---|
| `policy_number` | `text` | Primary key, non-blank check |

This is a reference table solely for policy-existence validation. It deliberately contains no coverage, active-status, effective-date, insured-party, or approval information.

### `claims`

| Column | PostgreSQL type | Constraints |
|---|---|---|
| `claim_number` | `uuid` | Primary key |
| `policy_number` | `text` | Not null, foreign key to `policies.policy_number` |
| `incident_type` | `text` | Not null, check against the four approved values |
| `incident_date` | `date` | Not null |
| `description` | `text` | Not null, non-blank check |
| `status` | `text` | Not null, check equal to `REPORTED` |

Use the generated claim number as the entity primary key. A separate internal identifier adds no value to this prototype.

Persist enums as strings, never ordinals. Do not add timestamps, monetary data, vehicle details, claimant details, attachments, or other unapproved fields.

## Policy Existence Lookup

`ClaimService` calls `PolicyRepository.existsById(policyNumber)` before creating a claim.

- If the policy does not exist, stop and return `422 POLICY_NOT_FOUND`.
- Do not check active status, policy dates, ownership, coverage, or eligibility because those rules are not approved for TASK-001.
- Retain the database foreign key as a final consistency safeguard.
- Do not expose policy lookup or policy management through a new API.

The seeded `MOTOR-POLICY-001` record allows the prototype flow and integration tests to exercise a successful creation without introducing a separate policy feature.

## Claim Number Generation

Generate a random Java `UUID` in `ClaimService` immediately before persistence. Store it as PostgreSQL `uuid`, use it as the claim primary key, and serialize it as the response `claimNumber` string.

The primary-key constraint is the uniqueness guarantee. UUID format and random generation are technical decisions only; they must not be described as insurer-specific numbering rules.

No sequence service, prefix logic, distributed ID infrastructure, or retry/idempotency feature is required.

## Validation Responsibilities

### Frontend

- Require all four fields before submission.
- Present incident type as a fixed select using the four contract values.
- Accept incident date as a calendar date and reject a locally future date for immediate feedback.
- Treat whitespace-only policy number and description as missing.
- Show field-specific backend errors even if equivalent client checks exist.
- Prevent a second form submission while the first request is pending.

Frontend validation is advisory. It must not be treated as the authoritative business validation.

### Backend

- Use Spring Validation on `CreateClaimRequest` for non-null/non-blank fields.
- Deserialize `incidentType` to the approved enum so unsupported values fail validation/request parsing.
- Represent `incidentDate` as `LocalDate`, not a timestamp.
- In `ClaimService`, reject an incident date after `LocalDate.now(clock)` and map that failure to the `incidentDate` field. Inject a `Clock` so “today” is testable. Use the server's configured system zone; no insurer-specific business timezone has been approved.
- Verify policy existence through `PolicyRepository`.
- Assign `REPORTED` on the server; never accept status or claim number from the request.
- Enforce database constraints as defense in depth.
- Do not add length limits, historical-date limits, case normalization, coverage checks, or incident-type-specific rules.

## Backend Responsibilities

- Own the API contract and authoritative validation.
- Verify policy existence.
- Generate the claim number.
- Force initial status to `REPORTED`.
- Persist exactly one claim per accepted request in a single transaction.
- Return the success response only after persistence succeeds.
- Map expected failures to the documented error response.
- Log unexpected failures without exposing their internals to the client.
- Add backend automated tests and document implementation/validation in `backend-notes.md`.

Authentication implementation is not part of TASK-001. The prototype operates under the authoritative decision that the reporting user is treated as an authenticated policyholder; no user identity or authentication subsystem is added or persisted.

## Frontend Responsibilities

- Render the four-field motor-claim form in the existing application shell.
- Label required fields and use the fixed incident-type options.
- Manage idle, submitting, success, validation-error, policy-not-found, and unexpected-error states locally in the form component.
- Call only `POST /api/claims` through the small API module.
- Disable submission while a request is pending.
- On success, clear the submitted field values and show a success message containing the returned claim number.
- On failure, do not show success; preserve entered values so the user can correct or retry.
- Display field errors near their fields and a general error message when appropriate.
- Run the frontend build and document implementation/validation in `frontend-notes.md`.

The frontend must not generate claim numbers, choose claim status, infer policy validity, perform coverage decisions, or work around a backend contract mismatch.

## Error Handling and State Behavior

- Validation failure: remain on the form, preserve values, and show field errors.
- Missing policy: remain on the form, preserve values, and show the `policyNumber` error.
- Unexpected backend/network failure: remain on the form, preserve values, and show a general retry-safe failure message.
- Success: show the generated claim number and clear the form for a possible new entry.
- A response that cannot be parsed as the documented success model is treated as an unexpected failure, not as success.
- Backend logging may include the claim number and technical exception context, but should avoid logging the free-text description as routine request data.

## Testing Strategy

### Backend Automated Tests

Using `spring-boot-starter-test`:

1. `ClaimService` tests with mocked repositories and a fixed `Clock`:
   - valid request creates one claim with a generated UUID and `REPORTED` status
   - future incident date is rejected before policy lookup or persistence
   - missing policy stops creation
   - repository failure does not return success
2. MVC/controller tests with the service mocked:
   - valid JSON returns `201` with `claimNumber` and `REPORTED`
   - each missing/blank required field returns `400`
   - unsupported incident type returns `400`
   - malformed incident date returns `400`
   - missing policy maps to `422` and a `policyNumber` field error
   - unexpected failure maps to sanitized `500`
3. Verify the backend compiles and all automated tests pass.

Do not add H2 merely for tests. PostgreSQL-specific schema behavior should be checked through the Compose-backed integration verification below.

### Frontend Verification

No frontend test framework exists, and adding one is disproportionate to this initial form. Verify:

- all fields render and are marked required
- incident options exactly match the contract
- client validation prevents incomplete and locally future-dated submission
- submit is disabled while pending
- backend field errors render at the correct field
- missing-policy and general failures preserve input and do not show success
- success displays the returned claim number and clears the form
- `npm run build` succeeds

### Integration Verification

With PostgreSQL running through Docker Compose:

1. Start the backend and confirm SQL initialization and JPA validation succeed.
2. Create a claim using seeded policy `MOTOR-POLICY-001`; verify `201`, UUID claim number, `REPORTED`, and one persisted row.
3. Submit an unknown policy; verify `422` and no claim row.
4. Submit a future date, unsupported type, and missing field; verify `400` and no claim row.
5. Exercise the React form against the live backend and verify success and error states.
6. Re-run backend tests and both backend/frontend builds as regression checks.

## Important Technical Decisions

- One feature package and a direct controller-service-repository flow are sufficient.
- PostgreSQL remains the sole runtime database.
- Spring SQL initialization is used instead of adding a migration framework for the initial prototype.
- Policy existence is backed by a minimal table and repository, not a hardcoded application set or external service.
- The claim number is a UUID and also the primary key.
- API enums are uppercase strings and database enums are stored as strings.
- Date-only input uses `LocalDate`.
- The backend owns all authoritative decisions; the frontend duplicates only safe usability checks.
- Vite proxies relative `/api` requests in development; no CORS policy is added.
- Authentication, policy management, duplicate detection, idempotency, attachments, drafts, claim retrieval, and adjudication remain outside scope.

## Remaining Blockers

None.

The approved requirements, human decisions, domain review, and this API contract provide enough information for Backend and Frontend Developers to implement independently. Any proposed change to required claim information, insurance validation, policy eligibility, initial status, supported incident types, or scope exclusions must be returned to the owning role rather than decided during implementation.

## Handoff

TASK-001 is ready for parallel handoff to:

- Backend Developer
- Frontend Developer
