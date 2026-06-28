# Domain: event/participant-management

## Purpose
Define who may add, edit, or remove event participants and how participant roles are surfaced in the UI.

## Requirements

### Requirement: Only OWNER may mutate participants
The system MUST restrict adding, editing, and removing participants to event OWNERs. CONTRIBUTORs and VIEWERs MUST NOT perform these mutations.

#### Scenario: OWNER adds a participant
- GIVEN an OWNER is viewing the event
- WHEN they add a new participant
- THEN the participant MUST be added and persisted

#### Scenario: OWNER removes a participant
- GIVEN an OWNER is viewing the participant list
- WHEN they remove a participant
- THEN the participant MUST be removed from the event

#### Scenario: CONTRIBUTOR attempts to add a participant
- GIVEN a CONTRIBUTOR is viewing the event
- WHEN they attempt to add a participant
- THEN the add control MUST be disabled and the operation MUST be rejected

#### Scenario: VIEWER attempts to remove a participant
- GIVEN a VIEWER is viewing the participant list
- WHEN they attempt to remove a participant
- THEN the remove control MUST be disabled and the operation MUST be rejected

### Requirement: Participant controls are enabled based on role
The system MUST enable add, edit, and remove participant controls only when the current user is an OWNER.

#### Scenario: OWNER sees enabled controls
- GIVEN the current user is an OWNER
- WHEN the participant list renders
- THEN add, edit, and remove controls MUST be enabled

#### Scenario: CONTRIBUTOR sees read-only participant list
- GIVEN the current user is a CONTRIBUTOR
- WHEN the participant list renders
- THEN participant mutation controls MUST be disabled or hidden

#### Scenario: VIEWER sees read-only participant list
- GIVEN the current user is a VIEWER
- WHEN the participant list renders
- THEN participant mutation controls MUST be disabled or hidden

### Requirement: Participant list displays role badges
The system MUST visually distinguish each participant's role as OWNER, CONTRIBUTOR, or VIEWER.

#### Scenario: OWNER badge shown
- GIVEN the participant list contains an OWNER
- WHEN it renders
- THEN an OWNER role badge or label MUST be visible next to that participant

#### Scenario: All roles distinguished
- GIVEN participants with OWNER, CONTRIBUTOR, and VIEWER roles exist
- WHEN the list renders
- THEN each role MUST have a distinct badge or label
