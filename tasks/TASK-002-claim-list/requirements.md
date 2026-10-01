# TASK-002 — Claim List and Detail Requirements

## Feature Summary

Show the user's own claim list directly when the application opens, with access to read-only claim details. Each list entry displays claim number, policy number, incident type, incident date, and status. Historical and processed claims remain visible. Pages contain 10 claims, ordered by incident date descending and then by creation recency for equal incident dates. Loading, empty, and error states are provided, with retry after list-loading failure.

## Sources and Decision Status

- `request.md`: claim list available after opening the application, with access to details.
- `human-decisions.md`: authoritative ownership, list and detail fields, initial display, ordering, history, pagination, loading, empty and error states, retry, read-only behavior, lifecycle, and scope decisions.
- Approved TASK-001 domain review and human approval: existing claim creation assigns `REPORTED`; TASK-001 is accepted and complete.

The newly authoritative Claim Status Lifecycle section in `human-decisions.md`, confirmed by the current human instruction, resolves the previously open status relationship and meanings. It supersedes the earlier statement in that file leaving the relationship unresolved. `REPORTED` remains the initial status established by TASK-001; it is not a display synonym for `PENDING`.

### Confirmed Status Lifecycle and Meanings

```text
REPORTED → PENDING → APPROVED
                    ↘ REJECTED
```

| Status | Authoritative meaning |
| --- | --- |
| `REPORTED` | The claim has been initially created and recorded. |
| `PENDING` | The company has started reviewing the claim, but review has not reached a final outcome. |
| `APPROVED` | Company review resulted in approval. |
| `REJECTED` | Company review resulted in rejection. |

Status transitions are performed by company-side processes. TASK-002 displays the recorded status; it does not implement those processes. These meanings establish no additional coverage, liability, payment-entitlement, payment-completion, review deadline, or rejection-reason rule.

## Scope

### In Scope

- Showing only claims created by the current user directly on application opening, with the five confirmed list fields.
- Viewing historical and processed claims alongside other claims.
- Ordering claims by incident date descending, with more recently created claims first when incident dates match.
- Pagination with a page size of 10 claims.
- An empty list state when the user has no claims.
- A loading indicator while claims load and a clear error message with retry if list loading fails.
- Selecting a claim and viewing its six specified detail fields.
- Viewing recorded status across all four confirmed lifecycle states.
- Read-only claim viewing.

### Out of Scope

- Claim editing, deletion, and new claim creation.
- User-driven status changes.
- Company-side status-change processes.
- Claim assessment and payment workflows.
- New insurance eligibility, coverage, or validation rules.

## User Stories

1. As a user, I want to see the claims I created when I open the application so that I can review my claim history.
2. As a user with more than 10 claims, I want to access additional pages so that I can view all my claims.
3. As a user, I want to select one of my claims and read its details so that I can inspect its recorded information and status.

## Functional Requirements

| ID | Requirement |
| --- | --- |
| FR-01 | When the application opens, the user's own claim list shall be shown directly. |
| FR-02 | The list shall contain only claims created by the current user. Claim details available through this feature shall also be restricted to that user's own claims. |
| FR-03 | Historical and processed claims created by the user shall remain visible in the list; age or processing alone shall not exclude them. |
| FR-04 | Claims shall be ordered by incident date descending across the complete list, including page boundaries. For equal incident dates, the more recently created claim shall appear first. |
| FR-05 | The page size shall be 10 claims. When the user has more than 10 claims, pagination shall allow access to all of them. Each full page shall show 10 claims, and the final page shall show the remaining claims. |
| FR-06 | When the user has no claims, the claim list shall show an empty state. Claims belonging to other users shall not prevent this empty state. |
| FR-07 | Selecting a claim in the list shall open a detail screen for that selected claim. |
| FR-08 | The detail screen shall display the selected claim's claim number, policy number, incident type, incident date, description, and status. |
| FR-09 | Claim details shall be read-only. The feature shall not allow the user to edit claim information, change status, or delete a claim. |
| FR-10 | The list and detail shall display the claim's recorded status as `REPORTED`, `PENDING`, `APPROVED`, or `REJECTED`, with the meanings defined above. Own claims shall remain visible in each of these states, including existing TASK-001 `REPORTED` claims. `REPORTED` shall not be relabeled or automatically converted to `PENDING` through viewing. |
| FR-11 | Status viewing shall reflect the authoritative lifecycle `REPORTED` → `PENDING` → `APPROVED` or `REJECTED`. Only company-side processes perform transitions. TASK-002 shall provide no user action to initiate a transition and shall not change status when listing or selecting a claim. Implementing company-side review and transition processes is outside scope. |
| FR-12 | Each list entry shall display claim number, policy number, incident type, incident date, and recorded status for that claim. |
| FR-13 | A loading indicator shall be displayed while claims are being loaded. |
| FR-14 | If the claim list cannot be loaded, a clear error message shall be displayed and the user shall be able to retry loading the claims. A failure shall not be presented as a confirmed no-claims empty state. |

## Acceptance Criteria

### AC-01 — Access and Ownership (FR-01, FR-02)

Given a user has created claims and other users have also created claims, when the user opens the application, then their own claim list is shown directly without requiring navigation to find it. Once loading succeeds, the list displays their own claims and no claims created by other users. A claim belonging to another user must not be viewable in this feature's detail screen, including through an attempted direct access.

### AC-02 — History Visibility (FR-03)

Given the user has historical and processed claims, including claims in `APPROVED` or `REJECTED`, when the user views all applicable list pages, then those claims remain available and selectable for details. Status shall not override incident-date ordering or exclude an own claim.

### AC-03 — Incident-Date Ordering (FR-04)

Given the user's claims have different incident dates, when the user views the list and moves between pages, then later incident dates precede earlier incident dates throughout the list. Creation time or processing state must not override this primary incident-date ordering.

Given two claims have the same incident date and different creation times, when the list is displayed, then the more recently created claim appears first, including across page boundaries.

### AC-04 — Pagination Boundaries (FR-05)

- Given 1 through 10 claims, the list shows all of them on one page.
- Given 11 claims, the first page shows 10 and the next page shows 1.
- Given 20 claims, two pages each show 10.
- Given 21 claims, successive pages show 10, 10, and 1.
- Given an unchanged set of claims, viewing every page exposes every own claim exactly once, with no omissions or duplicated entries across pages.

### AC-05 — Empty State (FR-02, FR-06)

Given the user has created no claims, even if other users have claims, when loading the user's list succeeds, then an empty state is shown with no claim entries.

### AC-06 — Selected Claim Details (FR-07, FR-08)

Given a claim appears on any list page, when the user selects it, then a detail screen opens for that claim and displays its claim number, policy number, incident type, incident date, description, and recorded status. Displayed values must correspond to the selected claim and use the confirmed status meanings.

### AC-07 — Read-Only Behavior (FR-09)

Given the user views a claim, when the user inspects the available actions, then the user cannot edit its information, change its status, or delete it through this feature. Viewing the list or details does not modify the claim.

### AC-08 — All Confirmed Statuses Are Viewable (FR-10)

Given the user has claims recorded in each of `REPORTED`, `PENDING`, `APPROVED`, and `REJECTED`, when the user views the applicable list pages and selects each claim, then every claim is available and its recorded status is displayed in both list and detail. An existing TASK-001 claim still recorded as `REPORTED` is displayed as `REPORTED`, not omitted or relabeled `PENDING`.

### AC-09 — Status Meaning (FR-10)

For each of the four recorded statuses, any explanation of that status shall be consistent with the confirmed meaning above. `PENDING` indicates review has started without a final outcome; `APPROVED` and `REJECTED` indicate the respective company-review outcome. The feature shall not represent these labels as establishing unstated coverage, liability, or payment conclusions. This criterion does not require a particular tooltip, explanatory text, or visual design.

### AC-10 — Company-Controlled Transitions (FR-09, FR-11)

Given a claim is recorded in any confirmed state, when the user opens the list or its detail screen, then its status is not changed by viewing and no user status-change action is available. Given an authorized company-side process has already moved a claim from `REPORTED` to `PENDING`, or from `PENDING` to `APPROVED` or `REJECTED`, when the feature retrieves that updated claim for viewing, then it displays the recorded new status. This criterion verifies viewing of externally updated information, not implementation of company-side transitions; refresh timing remains PQ-05.

### AC-11 — List Fields (FR-12)

Given the user's claim list loads successfully, when a claim entry is displayed on any page, then it displays that claim's claim number, policy number, incident type, incident date, and recorded status.

### AC-12 — Loading Indicator (FR-13)

Given claims are being loaded, including during retry, while loading is in progress, then a loading indicator is displayed.

### AC-13 — List Failure and Retry (FR-14)

Given the claim list cannot be loaded, when loading fails, then a clear error message is shown and the user can retry. The failure is not presented as confirmation that the user has no claims.

Given a list-loading failure, when the user retries, then loading is attempted again with a loading indicator. If it succeeds, the user's claims are shown with the confirmed fields, sorting, and pagination, or the empty state is shown if they have no claims. If retry fails, a clear error message and retry remain available.

## Functional Edge Cases

| Case | Expected behavior or unresolved decision |
| --- | --- |
| No own claims, but other users have claims | Show the empty state; expose no other user's claims. |
| Exactly 10, 11, 20, or 21 own claims | Apply AC-04 without an extra empty page or an inaccessible remainder. |
| Older incident recorded more recently than a newer incident | Order by incident date, not creation date. |
| Historical or processed claim falls on a later page | Keep it accessible through pagination and selectable for details. |
| Claims share an incident date | Show the more recently created claim first, including across page boundaries. |
| Existing claim has `REPORTED` status | Include it and display `REPORTED`; viewing does not start review or convert it to `PENDING`. |
| Claim is `PENDING` | Display the recorded state meaning company review has started without a final outcome; do not infer a deadline or result. |
| Claim is `APPROVED` or `REJECTED` | Keep it visible and selectable; display the company-review outcome without adding coverage or payment semantics. |
| Company-side process changes a claim's status | Display the updated recorded status when retrieved for viewing; no user transition action is introduced. Refresh timing remains PQ-05. |
| Attempt to access another user's claim details | Do not expose that claim; the exact user-facing response is unspecified. |
| Claims or statuses change while pages or details are being viewed | Refresh and pagination consistency behavior requires product clarification; no company-side processing workflow is defined here. |
| Claims are loading | Show a loading indicator. |
| Claim list loading fails | Show a clear error message and allow retry; do not present failure as a confirmed empty state. |
| Retry succeeds with no own claims | Show the empty state. |
| Retry succeeds with own claims | Show the list with the confirmed fields, sorting, and pagination. |
| Retry also fails | Show a clear error message and keep retry available. |
| Detail loading fails or a selected claim becomes unavailable | Presentation still requires BA / Human clarification. |
| A record lacks one of the required detail values or has an unrecognized status | Presentation requires clarification; do not invent replacement values or domain meanings. |

## Open Questions

### Previously Open Domain Questions — Human Decisions Incorporated

- **DQ-01 — Resolved by human decision:** `REPORTED` is the initial recorded state, followed by `PENDING` when company review starts, followed by `APPROVED` or `REJECTED` as the review outcome. All four recorded states are viewable; users cannot perform transitions. No conversion occurs merely through viewing.
- **DQ-02 — Resolved by human decision:** The four status meanings are stated in the confirmed status table. Additional coverage, liability, and payment semantics are not authorized or required by this viewing task.
- **DQ-03 — Visibility confirmed; explanation question retained:** Historical and processed own claims remain visible. No age threshold, retention rule, or additional business classification is needed for this inclusion requirement. Whether user-facing explanations of these terms are wanted remains a BA / Human question below; any requested domain meaning beyond the authoritative sources must return to the Domain Expert.

No additional unresolved insurance-domain rule has been identified for the confirmed viewing scope. This product/UI requirements update does not change the domain-review artifact or grant domain approval.

### Confirmed Product/UI Decisions — Resolved Questions

| Human decision | Requirements and verification | Question resolution |
| --- | --- | --- |
| List displays claim number, policy number, incident type, incident date, and status | FR-12; AC-11 | PQ-01 resolved. |
| Own claim list is shown directly when the application opens | FR-01; AC-01 | Initial-display portion of PQ-02 resolved. |
| Incident date descending; more recently created claim first for equal incident dates | FR-04; AC-03; equal-date edge case | PQ-03 resolved. |
| Loading indicator while claims load | FR-13; AC-12; loading edge case | Loading behavior confirmed. |
| Empty state when the user has no claims | FR-06; AC-05; no-own-claims and empty retry edge cases | Empty-state behavior confirmed. |
| Clear list-loading error and ability to retry | FR-14; AC-13; failure and retry edge cases | Claim-list failure portion of PQ-04 resolved. |

### Remaining Business Analyst / Human Product Clarification

- **PQ-02 (remaining portion):** How should users return from detail to the list, and should their previous page be retained?
- **PQ-04 (remaining portion):** What should users see for detail-loading failures, unavailable claims, missing display values, and denied detail access? Claim-list loading failures are resolved by the error-and-retry decision.
- **PQ-05:** When should list and detail information refresh, and what consistency is expected when claims or statuses change during viewing?
- **PQ-06:** Are user-facing explanations of “historical” or “processed” needed? If so, what wording is intended, subject to Domain Expert validation of any additional business meaning?

## Business Analyst Handoff

The authoritative list fields, direct initial display, primary and secondary sorting, loading indicator, empty state, and list error/retry behavior are incorporated into requirements, acceptance criteria, and edge cases. PQ-01 and PQ-03 are resolved; answered portions of PQ-02 and PQ-04 are removed. The confirmed lifecycle and status meanings remain intact. The remaining product/UI questions require clarification before their dependent behavior can be implemented reliably. This update does not change domain review, define technical design, or grant final human acceptance.
