# Event Participant Removal Specification

## Purpose

Allow event owners and contributors to remove participants from an event, with proper authorization checks and confirmation flow.

## Requirements

### Requirement: Remove Participant UI

The system MUST display a remove button (trash icon) next to each participant in the event detail screen. The button SHALL be styled as a red square with a trash icon.

#### Scenario: Remove button visible to authorized users

- GIVEN a user with OWNER or CONTRIBUTOR role views the event detail screen
- WHEN the participant list is rendered
- THEN each participant row MUST show a red trash icon button

#### Scenario: Remove button hidden from readers

- GIVEN a user with READER role views the event detail screen
- WHEN the participant list is rendered
- THEN NO remove buttons MUST be visible

### Requirement: Remove Participant Confirmation

The system MUST show a confirmation dialog before removing a participant. The dialog SHALL suggest the action and allow the user to confirm or cancel.

#### Scenario: User confirms removal

- GIVEN the user taps the trash button next to participant "Juan"
- WHEN the confirmation dialog appears
- AND the user taps "Eliminar"
- THEN "Juan" MUST be removed from the event's participants and memberIds
- AND the UI MUST update to reflect the removal

#### Scenario: User cancels removal

- GIVEN the user taps the trash button next to participant "Juan"
- WHEN the confirmation dialog appears
- AND the user taps "Cancelar"
- THEN "Juan" MUST remain in the event

### Requirement: Owner Removal Promotes Successor

When removing the event owner, the system SHALL promote the oldest CONTRIBUTOR to OWNER, following the existing `removeMember` logic.

#### Scenario: Owner leaves event with contributors

- GIVEN the current owner taps remove on their own participant entry
- WHEN there are other contributors in the event
- THEN the oldest contributor MUST be promoted to OWNER
- AND the former owner MUST be removed from the event

#### Scenario: Cannot remove last participant

- GIVEN the event has only one participant (the owner)
- WHEN the owner attempts to remove themselves
- THEN the system MUST prevent removal and show an error message

### Requirement: Authorization Check

The system MUST verify the current user has `ManageParticipants` permission before allowing removal. This check MUST use the existing `PermissionEngine`.

#### Scenario: Unauthorized removal attempt

- GIVEN a user without `ManageParticipants` permission
- WHEN they attempt to invoke the remove participant action
- THEN the action MUST be blocked and no removal occurs
