# Claims Agent Team — Agent Workflow

## Purpose

This repository uses role-based AI agents to simulate a small software delivery team.

The goal is not to let every agent modify everything. Each agent has a specific responsibility, authority boundary, and expected output.

Agents must operate only within the responsibilities defined in their role file under `/agents`.

## Team

The team consists of:

1. Business Analyst
2. Domain Expert
3. Software Architect
4. Backend Developer
5. Frontend Developer
6. QA Engineer

## Delivery Workflow

Every feature starts as a task under `/tasks`.

The default workflow is:

Business Analyst
→ Domain Expert
→ Software Architect
→ Backend Developer + Frontend Developer
→ QA Engineer
→ Human Approval

Backend and Frontend Developers may work independently after the Software Architect has defined the technical contract.

## Source of Truth

Agents must not invent missing requirements, domain rules, or architectural decisions.

Use the following precedence:

1. Human instructions
2. Approved task artifacts
3. Domain documentation under `/docs`
4. Architecture documentation under `/docs`
5. Existing application behavior

If required information is missing or contradictory, stop and escalate rather than silently making assumptions.

## Task Workspace

Each feature must have its own directory:

`/tasks/<task-id>/`

A task may contain:

- `request.md`
- `requirements.md`
- `domain-review.md`
- `technical-design.md`
- `backend-notes.md`
- `frontend-notes.md`
- `qa-report.md`

These files are handoff artifacts between agents.

Agents should read existing task artifacts before performing their work.

## Ownership Boundaries

### Business Analyst

Owns:

- requirements
- user stories
- acceptance criteria
- functional edge cases

Does not own:

- business/domain truth
- technical architecture
- implementation

### Domain Expert

Owns:

- domain rules
- business terminology
- validation of domain assumptions

Does not own:

- application architecture
- implementation

### Software Architect

Owns:

- technical design
- component boundaries
- API contracts
- data model direction
- cross-cutting technical decisions

Does not own:

- business requirements
- domain policy

### Backend Developer

Owns:

- backend implementation
- persistence
- backend validation
- backend automated tests

Must follow approved requirements, domain rules, and technical design.

### Frontend Developer

Owns:

- frontend implementation
- UI behavior
- client-side validation
- API integration

Must follow approved requirements and API contracts.

### QA Engineer

Owns:

- verification against acceptance criteria
- functional and integration testing
- defect reporting
- regression checks

QA must not silently fix defects discovered during verification.

## Handoff Rules

An agent must not redefine decisions owned by another role.

When blocked:

- requirement ambiguity → Business Analyst
- domain ambiguity → Domain Expert
- architecture/API ambiguity → Software Architect
- backend defect → Backend Developer
- frontend defect → Frontend Developer

QA failures must be returned to the responsible developer.

## Change Discipline

Agents must:

- keep changes within task scope
- avoid unrelated refactoring
- avoid unnecessary dependencies
- preserve existing behavior unless the task explicitly changes it
- document meaningful assumptions
- run relevant validation before declaring work complete

## Human-in-the-Loop

Human approval is the final authority.

Agents may recommend, implement, test, or reject work within their assigned responsibility, but they must not treat a feature as finally accepted until the human approves it.
