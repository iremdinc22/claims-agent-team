# TASK-002 — Claim List and Detail Domain Review

## Review Status

**APPROVED**

The updated requirements reflect the authoritative human lifecycle and status meanings while preserving the approved TASK-001 initial status. All material insurance-domain blockers for the confirmed viewing scope are resolved. Approval is limited to claim listing and read-only detail/status viewing; it does not authorize company-side processing implementation or final human acceptance.

## Materials Reviewed

- `AGENTS.md`
- `agents/domain-expert.md`
- `tasks/TASK-002-claim-list/request.md`
- `tasks/TASK-002-claim-list/human-decisions.md`
- `tasks/TASK-002-claim-list/requirements.md`
- `tasks/TASK-001-create-claim/domain-review.md`
- `tasks/TASK-001-create-claim/human-approval.md`
- The previous TASK-002 blocked domain review.

No additional domain documentation was found under `/docs`. The new Claim Status Lifecycle section in TASK-002 human decisions, explicitly confirmed by the human instructions, resolves the earlier pending-validation statement in that file. The earlier three-label list must not be used to exclude `REPORTED` claims. The approved TASK-001 review and human acceptance remain authoritative for existing intake rules.

## Reviewed Domain Concepts and Verification

| Check | Finding and requirements evidence |
| --- | --- |
| 1. Initial status | Confirmed: `REPORTED` is assigned when a claim is initially created and recorded. The requirements' status table and FR-10 preserve TASK-001 behavior. |
| 2. Start of company review | Confirmed: company-side review moves `REPORTED` to `PENDING` when review starts. The lifecycle, FR-11, and AC-10 reflect this decision without implementing the process. |
| 3. Review outcome | Confirmed: company review results in `APPROVED` or `REJECTED` after `PENDING`. The status table, FR-11, and AC-09/AC-10 preserve those meanings. |
| 4. User authority | Confirmed: users cannot change status. FR-09/FR-11 and AC-07/AC-10 prohibit user transitions. |
| 5. Viewing-only scope | Confirmed: FR-10/FR-11 and AC-08/AC-10 display recorded states; viewing does not start review or change status. Company-side transition implementation remains outside scope. |
| 6. Insurance semantics | Confirmed: status meanings describe recording, review in progress, and company-review outcomes only. The requirements and AC-09 add no coverage, liability, payment, deadline, or rejection-reason rule. |
| 7. Claim history | Confirmed: FR-03/FR-10, AC-02/AC-08, and edge cases retain historical and processed own claims, including `REPORTED`, `APPROVED`, and `REJECTED`. |
| 8. Unsupported rules | None identified in the updated requirements. Existing intake constraints are preserved without adding viewing eligibility, coverage reassessment, retention, or new claim classifications. |

## Confirmed Business Rules

1. The approved context remains simplified motor-claim intake. TASK-002 introduces no new insurance product or claim category.
2. TASK-001 treats the reporting user as an authenticated policyholder for the prototype. TASK-002 permits viewing only claims the current user created; policy association alone does not expand that permission.
3. Existing intake requires policy number, incident type, incident date, and description; incident types are `COLLISION`, `THEFT`, `GLASS_DAMAGE`, and `OTHER`. Policy existence and a non-future incident date are creation-time rules, not new checks for hiding recorded claims.
4. TASK-001 generates a unique claim number and assigns `REPORTED` at creation. Creation does not decide coverage or claim approval.
5. The authoritative lifecycle is:

```text
REPORTED → PENDING → APPROVED
                    ↘ REJECTED
```

6. Company-side processes perform these transitions. Users cannot change claim status; listing or selecting a claim does not perform a transition.
7. TASK-002 displays each claim's recorded status in list and detail. Existing `REPORTED` claims remain visible as `REPORTED`, without relabeling them as `PENDING`.
8. Historical and processed own claims remain visible and selectable. Incident date determines ordering, most recent first. Pagination does not establish an insurance eligibility filter.
9. Detail viewing includes claim number, policy number, incident type, incident date, description, and status. Viewing is read-only; editing, deletion, new creation, assessment, payment, and company-side transition implementation are outside TASK-002.

## Terminology Clarifications

| Term | Authoritative meaning and limits |
| --- | --- |
| Motor claim | A claim reported in connection with motor insurance within the approved simplified prototype. |
| Claim number | The unique business-facing reference generated at creation; no further format rule is established. |
| Incident date | The recorded date of the reported motor incident, used for list ordering. |
| `REPORTED` | The claim has been initially created and recorded. It does not establish coverage, liability, approval, or payment entitlement. |
| `PENDING` | The company has started reviewing the claim, but review has not reached a final outcome. It is distinct from initial recording. |
| `APPROVED` | Company review resulted in approval. No additional coverage, liability, settlement, or payment conclusion is established by this label. |
| `REJECTED` | Company review resulted in rejection. No rejection reason, coverage-denial meaning, appeal rule, or irreversibility rule is established by this label. |
| Historical / processed | Terms requiring continued visibility, without establishing age thresholds, retention rules, payment completion, or an exhaustive status classification. |

## Domain Constraints

- Preserve the confirmed lifecycle and meanings without adding transitions, shortcuts, reopening rules, or status equivalences.
- Display recorded status; do not automatically convert `REPORTED` to `PENDING` when a user views a claim.
- All four confirmed states remain viewable for own claims. Status or age alone must not exclude a claim.
- Displaying an externally updated status does not authorize TASK-002 to execute company-side review or assessment.
- Do not infer coverage, liability, payment entitlement, or payment completion from intake or review-status labels.
- Do not introduce deadlines, rejection reasons, historical reporting limits, renewed active-policy checks, or retrospective intake validations for viewing.
- Missing or unrecognized values must not be assigned invented domain meanings. Product presentation of such cases remains with the BA / Human.

## Previous Domain Questions

- **DQ-01 — Resolved:** The human lifecycle decision retains `REPORTED` as the initial state, moves it to `PENDING` on start of company review, and permits the stated review outcomes. FR-10/FR-11 and AC-08/AC-10 correctly incorporate this relationship and existing-claim visibility.
- **DQ-02 — Resolved:** The authoritative human definitions provide sufficient status meanings for this viewing task. AC-09 correctly avoids unsupported insurance or payment implications. Detailed assessment criteria are not required to display recorded status and remain outside scope.
- **DQ-03 — Resolved for visibility scope:** Historical and processed own claims remain visible. No further domain classification is necessary for inclusion. PQ-06 concerns whether explanations are desired; any later additional business meaning must receive domain validation.

## Rejected Assumptions

- `REPORTED` and `PENDING` are synonyms, or viewing initiates review.
- Users may initiate or perform company-side status transitions.
- TASK-002 must implement assessment or transition processes to display their recorded results.
- The approved lifecycle authorizes additional transitions or changes TASK-001's initial status.
- `APPROVED` means covered, payable, settled, or paid.
- `REJECTED` supplies a particular rejection reason, coverage denial, or irreversible outcome.
- `PENDING` establishes a service deadline or additional review stages.
- Historical or processed claims may be excluded by an unstated retention, policy, coverage, or eligibility rule.

## Remaining Questions and Ownership Boundaries

No material insurance-domain blocker remains for the confirmed scope.

PQ-01 through PQ-06 remain with the Business Analyst / Human. This review does not resolve list fields, navigation, equal-date ordering, error presentation, refresh consistency, or explanatory UI wording. Domain approval does not imply those product decisions are resolved. No architecture, API, persistence design, implementation technology, or application code is defined or changed by this review.

## Handoff and Final Status

Domain re-review is complete. TASK-002 may be handed to the Software Architect within the approved viewing scope, with unresolved BA / Human product questions carried forward to their owners. This is domain approval, not final human acceptance of the feature.

**APPROVED**
