# TASK-001 — Create Claim Domain Review

## Review Status

**APPROVED**

The authoritative product decisions in `human-decisions.md` resolve the material domain questions identified by the previous reviews. TASK-001 now defines a sufficiently bounded motor-claim intake capability, including its required information, permitted incident types, creation-time rules, initial status, identifier requirement, and exclusions.

The task may proceed to Software Architecture. Approval applies only to the scope and rules explicitly documented here; it does not authorize additional insurance rules.

## Materials Reviewed

- `tasks/TASK-001-create-claim/request.md`
- `tasks/TASK-001-create-claim/requirements.md`
- `tasks/TASK-001-create-claim/human-decisions.md`
- The previous blocked domain review

## Previous Blockers Resolved

1. **Supported claim context — resolved.** TASK-001 supports a simplified motor insurance claim-intake process and motor claims only.
2. **Mandatory and optional information — resolved.** Policy number, incident type, incident date, and description are required. No optional claim information has been authorized for this task, so additional fields must not be inferred.
3. **Validation rules — resolved for the approved scope.** All four required items must be supplied; incident type must be one of `COLLISION`, `THEFT`, `GLASS_DAMAGE`, or `OTHER`; incident date must not be in the future; and the referenced policy must exist. No additional field constraints are established.
4. **Party and policy relationships — resolved for the prototype.** The reporting user is treated as an authenticated policyholder, and the claim must reference an existing policy. No separate claimant, insured-party, or policyholder data capture is required by TASK-001.
5. **Verification at intake — resolved.** Policy existence is required. Policy coverage and claim approval are explicitly not determined during claim creation. No additional coverage decision may be introduced at intake.
6. **Creation eligibility — resolved for the approved scope.** Creation depends on the required information and stated validation rules. Coverage assessment, claim assessment, approval, rejection, and payment occur outside TASK-001.
7. **Initial lifecycle state — resolved.** Every newly created claim receives the status `REPORTED`. In this task, `REPORTED` means the claim has been recorded for subsequent handling; it does not mean accepted, covered, approved, or payable.
8. **Duplicate handling — resolved by scope exclusion.** Duplicate detection is outside TASK-001. No duplicate-rejection, matching, flagging, or linking rule may be invented for this feature.
9. **Claim identification — resolved.** The system generates a unique claim number when the claim is created. No business format, sequence, or presentation rule has been specified beyond uniqueness.

## Confirmed Business Rules

1. The feature supports motor insurance claims only.
2. The process is simplified claim intake rather than claim adjudication.
3. The reporting user is treated as an authenticated policyholder for this prototype.
4. A claim must reference an existing policy.
5. A new claim requires all of the following:
   - policy number
   - incident type
   - incident date
   - description
6. Incident type must be one of:
   - `COLLISION`
   - `THEFT`
   - `GLASS_DAMAGE`
   - `OTHER`
7. The incident date cannot be in the future.
8. A successfully created claim receives the status `REPORTED`.
9. The system generates a unique claim number at creation.
10. Policy coverage and claim approval are not determined during creation.
11. After successful creation, the user is shown a success message containing the generated claim number.
12. Duplicate detection, attachments, drafts, assessment, approval, rejection, and payment are outside TASK-001.

## Domain Constraints

- Missing required claim information makes the submission invalid for creation.
- An incident type outside the supported set makes the submission invalid for creation.
- A future incident date makes the submission invalid for creation.
- A policy number that does not reference an existing policy makes the submission invalid for creation.
- Creation records a reported claim only; it must not communicate or imply a coverage, liability, approval, rejection, or payment decision.
- Each successfully created claim must receive a unique claim number and the initial status `REPORTED`.
- No validation rule beyond the authoritative decisions may be added as an insurance-domain rule for TASK-001.

## Terminology Clarifications

- **Motor claim:** A claim reported in connection with motor insurance. TASK-001 does not subdivide the supported motor products or coverage types.
- **Reporting user:** The application user submitting the claim. For this prototype, the reporting user is treated as an authenticated policyholder.
- **Existing policy:** A policy reference that is present in the application's applicable policy source. Existence is required, but TASK-001 does not perform a coverage or claim-approval decision.
- **Incident type:** The product-defined category selected from the four supported values.
- **Incident date:** The date of the reported motor incident. It may be today or in the past, but not in the future. No maximum historical age is specified.
- **`REPORTED`:** The initial intake status showing that the claim has been recorded. It carries no determination of coverage, liability, approval, or payment.
- **Claim number:** The unique business-facing reference generated when creation succeeds. Its format is not a domain requirement in TASK-001.
- **Claim creation:** Recording the claim and assigning its claim number and `REPORTED` status. It is not claim assessment or adjudication.

## Rejected Assumptions

The following must not be inferred or introduced:

1. Successful creation means the claim is covered, accepted, approved, payable, or valid for indemnification.
2. The referenced policy must satisfy an unstated active-status, effective-date, ownership-verification, or coverage rule beyond the explicit requirement that it exists.
3. Incident dates are subject to a historical reporting limit; only future dates are explicitly prohibited.
4. The description has an insurer-defined length, content, or formatting rule not stated in the authoritative decisions.
5. `OTHER` requires additional category-specific information.
6. Different incident types require different fields or validation in TASK-001.
7. Duplicate-looking submissions must be detected, rejected, linked, or flagged.
8. A specific claim-number format, prefix, sequence, or generation method is required.
9. Attachments, monetary amounts, currency, vehicle details, claimant details, injury details, third-party details, or loss estimates are part of this task.
10. `REPORTED` represents any decision beyond successful intake.

## Remaining Domain Blockers

None.

The unspecified implementation details—such as claim-number generation technique, data representation, policy lookup mechanism, and incident-date handling at technical boundaries—belong to Software Architecture and must preserve the confirmed domain rules without creating new ones.

## Domain Conflict Assessment

No authoritative human decision conflicts with the insurance domain. In particular, separating claim intake from coverage and approval decisions is domain-consistent, and the simplified field set is acceptable for the explicitly bounded prototype as long as it is not represented as a universal motor-claim standard.

## Handoff

Domain review is complete and **APPROVED**. TASK-001 may be handed to the Software Architect for technical design.
