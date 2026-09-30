# TASK-001 — Human Product Decisions

For TASK-001, use a simplified motor insurance claim-intake process.

- The application supports motor insurance claims only.
- The reporting user is treated as an authenticated policyholder for this prototype.
- A claim must reference an existing policy.

## Required Claim Information

- Policy number
- Incident type
- Incident date
- Description

## Supported Incident Types

- COLLISION
- THEFT
- GLASS_DAMAGE
- OTHER

## Validation and Business Rules

- The incident date cannot be in the future.
- A newly created claim receives the status `REPORTED`.
- The system generates a unique claim number when the claim is created.
- Policy coverage and claim approval are not determined during claim creation.

## Scope Decisions

- Duplicate detection is outside TASK-001.
- Attachments are outside TASK-001.
- Draft claims are outside TASK-001.
- Claim assessment, approval, rejection, and payment are outside TASK-001.

## Success Behavior

After successful claim creation, the user sees a success message containing the generated claim number.