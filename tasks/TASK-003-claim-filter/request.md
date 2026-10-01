# TASK-003 — Claim Status Filter

## Requirement Change

This is a new business requirement added after TASK-002 was completed. It extends the existing claim list and does not replace TASK-002.

## Human Request

Users want to filter their claim list by claim status. The claim list should allow the user to view all of their claims or filter the list by a specific recorded status: `REPORTED`, `PENDING`, `APPROVED`, or `REJECTED`.

The existing TASK-002 behavior must remain unchanged when no filter is selected. Pagination, ownership isolation, incident-date ordering, read-only detail viewing, and existing claim creation behavior must continue to work.

## Current Task Boundary

This artifact records the request only. Do not modify application code or TASK-001/TASK-002 artifacts. Do not create requirements, technical design, or implementation files at this stage.
