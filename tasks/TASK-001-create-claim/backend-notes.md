# TASK-001 — Backend Implementation Notes

## Implementation Summary

- Added `POST /api/claims` with the approved request, success, and error payloads.
- Added authoritative required-field, incident-type, date-format, future-date, and policy-existence validation.
- Added transactional claim creation with a backend-generated random UUID and forced initial status `REPORTED`.
- Added JPA persistence for claims and the minimal policy-reference lookup required by the approved design.
- Added idempotent PostgreSQL schema initialization and the documented `MOTOR-POLICY-001` demonstration policy.
- Added safe mappings for validation, missing-policy, malformed-request, and unexpected failures.
- Disabled scalar-to-text coercion for request deserialization so incompatible JSON values are rejected instead of silently converted.

## Implementation Decisions

- The claim entity stores the policy number directly rather than adding a JPA relationship. The database foreign key provides the approved consistency safeguard without expanding the policy model.
- Unexpected errors are logged server-side and return only the generic API-contract message.
- Jackson mapping paths are used to associate incompatible enum/date values with the applicable request field.
- A system-default-zone `Clock` bean is injected into `ClaimService`, allowing future-date behavior to be tested deterministically.

## Tests Added

- `ClaimServiceTest` covers successful creation, UUID/status assignment, persistence values, future-date rejection, and missing-policy rejection.
- `ClaimControllerTest` covers `201` success plus required-field, unsupported-incident-type, invalid-date-format, future-date, policy-not-found, and unexpected-error responses.

## Validation Performed

- `./mvnw test` — passed: 14 tests, 0 failures, 0 errors, 0 skipped.
- `./mvnw clean package` — passed: 14 tests and executable JAR packaging completed successfully.
- Live PostgreSQL verification — passed against PostgreSQL 17 in an isolated temporary container:
  - `schema.sql` and `data.sql` initialized successfully before Hibernate validation.
  - JPA validated both repositories/entities and the application started successfully.
  - A live valid `POST /api/claims` returned `201 Created`, a UUID claim number, and `REPORTED`.
  - Database inspection confirmed exactly one persisted row with the submitted values, generated UUID, and `REPORTED` status.

## Deviations from Technical Design

None.

## QA Blockers

None.

## Known Limitations

- No duplicate detection, coverage decision, policy eligibility check, authentication implementation, or post-creation claim operation is included, as required by TASK-001 scope.
