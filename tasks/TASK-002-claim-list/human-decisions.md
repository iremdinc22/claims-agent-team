# TASK-002 — Human Decisions

## Claim List

- Users can view only claims they have created.
- Historical and processed claims remain visible in the list.
- Pagination is required when the number of claims exceeds the page size.
- Page size is 10 claims.
- If the user has no claims, the claim list is shown in an empty state.

The claim list displays:

- Claim number
- Policy number
- Incident type
- Incident date
- Status

## Sorting

- Claims are ordered by incident date descending, newest incident first.
- If two claims have the same incident date, the more recently created claim appears first.

## Initial Application Behavior

- When the application opens, the user's own claim list is shown directly.

## Loading

- A loading indicator is displayed while claims are being loaded.

## Empty State

- If the user has no claims, an empty state is displayed.

## Error Handling

- If the claim list cannot be loaded, a clear error message is displayed.
- The user can retry loading the claims.

## Claim Detail

Selecting a claim opens a detail screen.

The detail screen displays:

- Claim number
- Policy number
- Incident type
- Incident date
- Description
- Status

The detail screen is read-only.

Users cannot edit claims.

Users cannot change claim status.

Claim status changes are performed by company-side processes and are outside the user's capabilities in this task.

## Status

The intended claim statuses for the list are:

- PENDING
- APPROVED
- REJECTED

The exact lifecycle relationship between these statuses and the existing `REPORTED` status from TASK-001 must be validated by the Domain Expert.

## Scope

TASK-002 covers:

- Claim listing
- Claim history visibility
- Pagination
- Claim detail viewing

TASK-002 does not include:

- Claim editing
- User-driven status changes
- Claim deletion
- New claim creation
- Claim assessment or payment workflows

## Claim Status Lifecycle

The claim status lifecycle for TASK-002 is:

REPORTED → PENDING → APPROVED
                    ↘ REJECTED

- `REPORTED`: The claim has been initially created and recorded in the system.
- `PENDING`: The company has started reviewing the claim, but the review has not reached a final outcome.
- `APPROVED`: The company review has resulted in approval.
- `REJECTED`: The company review has resulted in rejection.

Users cannot change claim status. Status transitions are performed by company-side processes.

## Identity

For the current prototype, TASK-002 may use a trusted backend-side
demo user identity instead of implementing a full authentication
system.

The demo identity must not be supplied by the browser or accepted
from a user-controlled request parameter/header.

This is a prototype-only mechanism. A future authentication task
will replace the demo identity with a real authenticated user
context.

## Legacy claims

Existing TASK-001 demonstration claims may be treated as prototype data.

During the TASK-002 schema upgrade, existing demonstration claims may
be assigned to the trusted demo user.

Because their original creation timestamps were not persisted in
TASK-001, their migration timestamp may be used as their created_at
value.

This is explicitly a technical treatment of prototype data and must
not be represented as the original historical creation time.

## New claims

All claims created after this change must persist the trusted creator
identity and actual creation timestamp.

## Ownership

All TASK-002 list and detail queries must be restricted to the trusted
current user identity.

No browser-supplied user ID may determine claim ownership.
