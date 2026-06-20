# Delta for event-lifecycle

## ADDED Requirements

### Requirement: Payment checkbox visibility by event state (R007)

In `SettlementPanel`, checkboxes for payment status MUST only appear when `eventState != EventState.OPEN`. In OPEN state, participants MUST display without checkboxes regardless of whether `EventDebtItem` records exist. In CALCULATED and CLOSED states, checkboxes MUST appear as before.

#### Scenario: OPEN event shows participants without checkboxes

- GIVEN an event in OPEN state with 3 participants and existing EventDebtItem records
- WHEN SettlementPanel renders
- THEN no Checkbox MUST appear next to any participant row

#### Scenario: CALCULATED event shows checkboxes as before

- GIVEN an event in CALCULATED state with participants
- WHEN SettlementPanel renders
- THEN Checkbox MUST appear next to each participant row as before

#### Scenario: Newly added participant in OPEN event has no checkbox

- GIVEN an OPEN event after adding a new participant via batch multi-select
- WHEN SettlementPanel renders
- THEN the new participant row MUST NOT display a Checkbox

### Requirement: Batch profile addition in AddProfileToEventDialog (R008)

`AddProfileToEventDialog` MUST support multi-select via checkboxes. Multiple profiles MUST be selectable before pressing a single "Aceptar" button. The dialog MUST show a Checkbox next to each profile name and avatar, a count of selected profiles, and a single "Aceptar" button that adds all selected at once.

#### Scenario: User selects 3 profiles and adds them with one click

- GIVEN the dialog shows 5 available profiles with checkboxes
- WHEN the user checks 3 profiles and taps "Aceptar"
- THEN all 3 selected profiles MUST be added to the event in a single operation

#### Scenario: Dialog with no profiles selected — "Aceptar" disabled

- GIVEN the dialog is open with no profiles checked
- WHEN the user inspects the "Aceptar" button
- THEN "Aceptar" MUST be disabled

#### Scenario: Dialog dismissed without accepting — no profiles added

- GIVEN the dialog is open with 2 profiles checked
- WHEN the user dismisses the dialog without tapping "Aceptar"
- THEN no profiles MUST be added to the event
