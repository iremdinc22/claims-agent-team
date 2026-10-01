# TASK-003 — Claim Status Filter Requirements

## Feature Summary and Sources

TASK-003 is a requirement change extending the completed TASK-002 claim list, not replacing it. Users can view all their claims or select one recorded status to narrow the list.

Sources: the approved [request](request.md), the human's ten clarified product decisions supplied in this conversation, and the accepted TASK-002 [requirements](../TASK-002-claim-list/requirements.md) and [human approval](../TASK-002-claim-list/human-approval.md). The current human instruction authorizes this requirements artifact, superseding the request's earlier request-only stage boundary.

## Scope

In scope: a Status filter with All and the four existing recorded statuses; filtering only own claims; pagination of filtered results; resetting to page 1 on selection changes; the confirmed no-match message; and preserving existing list, detail, and creation behavior.

Out of scope: new status values or meanings, status transitions, claim editing/deletion, company-side processing, real authentication, other filters, and changes to claim creation. This artifact specifies functional behavior only; it does not define APIs, persistence, or implementation.

## User Story

As a user, I want to filter my own claim list by recorded status so that I can find relevant claims while retaining access to all my claims and their read-only details.

## Preserved and Extended TASK-002 Behavior

| Area | Preserved behavior | TASK-003 extension |
| --- | --- | --- |
| Initial list and All | Own-claim list opens directly; no status exclusion when unfiltered | Status control exposes All as the unfiltered option |
| Ownership | Only current-user claims are listed or accessible in detail | Selected status narrows only the own-claim set |
| Pagination | Page size 10; all applicable claims accessible | Pages reflect matching claims; selection changes reset to page 1 |
| Ordering/history | Incident date descending, then creation recency descending for equal incident dates; historical/processed claims remain available | Same ordering within matching results; status alone is the new selection criterion |
| List fields | Claim number, policy number, incident type, incident date, status | No field changes |
| Empty/loading/error | Existing loading, empty-state presentation, and error/Retry behavior; failure is not an empty success | Successful status selection with no matches displays “No claims found” |
| Details | Same six fields, ownership isolation, and read-only behavior | Filtered entries open the existing detail view |
| Creation/status | TASK-001 creation remains accessible and unchanged; viewing never changes recorded status | Filtering only changes which claims are shown |

All preserves existing TASK-002 behavior, including its unfiltered empty state. The approved page-reset rule applies when changing a selection to All as well as to a specific status.

## Functional Requirements

| ID | Requirement |
| --- | --- |
| FR-01 | The claim list shall provide a Status filter control with exactly All, REPORTED, PENDING, APPROVED, and REJECTED. |
| FR-02 | All shall mean no status filtering. With no filter selected, including initial unfiltered viewing, the existing TASK-002 own-claim list behavior shall remain unchanged. |
| FR-03 | Selecting a specific status shall show only the current user's claims whose recorded status equals that selection. Other users' claims shall not appear or affect available pages. |
| FR-04 | Pagination shall apply to the matching own-claim set with page size 10. Full pages contain 10 entries; the final page contains the remainder. Page navigation shall continue to apply the selected status. |
| FR-05 | Changing the selected status, including changing to or from All, shall reset pagination to page 1. |
| FR-06 | When successful retrieval for a specific selected status finds no matching own claims, the existing empty-state behavior shall be shown with the message “No claims found”. |
| FR-07 | Matching results shall retain TASK-002 ordering across pages: incident date descending, then more recently created first for equal incident dates. Historical and processed own claims remain eligible when their recorded status matches. |
| FR-08 | List entries shall retain the existing five fields. Selecting a filtered entry shall open the existing ownership-restricted, read-only detail view with its six fields and unchanged behavior. |
| FR-09 | Filtering shall not change any claim's recorded status or other values, relabel statuses, or expose a status-change action. |
| FR-10 | Existing loading and list error/Retry behavior shall apply while retrieving filtered claims. A retrieval failure shall not be presented as “No claims found”; retry shall retrieve the selected result set. |
| FR-11 | TASK-001 creation access, validation, persistence, and initial REPORTED behavior shall remain unchanged. Existing ownership isolation and read-only detail behavior shall remain intact. |

## Acceptance Criteria

### AC-01 — Control and Unfiltered Regression (FR-01, FR-02)

Given the claim list is opened, the Status control offers All and the four specified statuses. With no status filter applied or All selected, the user's list retains TASK-002 contents, fields, history visibility, ordering, page size, loading/empty/error behavior, and access to details.

### AC-02 — Exact Status and Ownership (FR-03, FR-09)

For each of REPORTED, PENDING, APPROVED, and REJECTED: given own and other-user claims in that status and own claims in other statuses, selecting it shows only own claims with that exact recorded status. Other-user claims are excluded from entries and pagination. Recorded status values remain unchanged.

### AC-03 — Filtered Pagination (FR-04)

Given a selected status has 1–10 matching own claims, all fit on one page. For 11, 20, and 21 matches, page sizes are respectively 10/1, 10/10, and 10/10/1. Nonmatching and other-user claims do not consume page slots. For an unchanged dataset, traversing the filtered pages exposes each matching own claim exactly once.

### AC-04 — Selection Resets Page (FR-05)

Given the user is on page 2 or later, changing to another status displays page 1 of that status's results. Changing to All also resets to page 1 of the unfiltered own-claim list. Changing to a status with fewer pages does not leave the user on the previous page.

### AC-05 — No Matches (FR-06)

Given there are no own claims matching the selected specific status, successful retrieval displays the existing empty-state presentation with “No claims found” and no claim entries. This applies even when own claims in other statuses or other users' matching claims exist.

### AC-06 — Ordering and History (FR-07)

Given matching claims have different incident dates, later incident dates precede earlier dates across all filtered pages. For equal incident dates and different creation times, more recently created claims precede older ones, including across page boundaries. Matching historical or processed claims remain accessible.

### AC-07 — Detail and Read-Only Regression (FR-08, FR-09)

Given a matching claim appears on any filtered page, selecting it opens its existing detail view showing claim number, policy number, incident type, incident date, description, and recorded status. No edit, delete, or status-change action is introduced. Direct access to another user's detail remains denied, and filtering or viewing does not modify a claim.

### AC-08 — Loading, Failure, and Retry (FR-10)

While filtered claims load, the existing loading indicator is shown. A failure displays the existing error and Retry rather than a no-match state. Retry retrieves results for the selected status; success shows matching claims or the confirmed no-match state, and repeated failure retains error/Retry.

### AC-09 — Creation Regression (FR-11)

Existing claim creation remains accessible and follows accepted TASK-001 behavior. A valid creation still records a claim with initial REPORTED status; existing validation and success/failure behavior remain unchanged. TASK-003 filtering introduces no creation or status-transition rule.

## Relevant Edge Cases

| Case | Expected behavior |
| --- | --- |
| No own claims, but other users have matching claims | Specific-status selection shows “No claims found”; no other-user entries |
| Own claims exist only in other statuses | Selected status shows no-match state; All shows existing unfiltered results |
| 10, 11, 20, or 21 matching own claims | Apply filtered pagination boundaries from AC-03 |
| Matches are dispersed across the unfiltered pages | Filtered pagination includes all matches, not just matches from the previously visible page |
| Change status from a later page to a smaller result set | Reset to page 1; show its results or no-match state |
| Change from specific status to All | Reset to page 1; restore TASK-002 unfiltered behavior |
| Equal incident dates across filtered page boundaries | Preserve creation-recency ordering |
| Historical APPROVED or REJECTED claims match | Keep them visible and selectable |
| REPORTED selected | Show recorded REPORTED claims without conversion to PENDING |
| Filtered retrieval or retry fails | Show error/Retry, not a successful empty state |
| Filtered claim selected for detail | Preserve existing detail fields, ownership, and read-only behavior |
| Claims/statuses change during viewing | Preserve existing TASK-002 refresh and concurrency behavior; no new refresh or snapshot rule is introduced |

## Open Questions and Handoff

No additional product clarification is required for the confirmed filter scope. Existing TASK-002 matters outside this change are not redefined. Status meanings and company-controlled transitions remain as previously approved; no new insurance rule is proposed.

Ready for Domain Expert review. This requirements artifact does not grant domain approval, define a technical design, authorize implementation, or establish final feature acceptance.
