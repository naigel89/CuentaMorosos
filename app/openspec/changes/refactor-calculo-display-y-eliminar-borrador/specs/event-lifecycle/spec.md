# Event Lifecycle Specification

## Purpose

Remove the `DRAFT` state from the event lifecycle. Events MUST be created directly in `OPEN` state. All persistence layers, state machine transitions, UI components, and validation logic MUST be updated to eliminate `DRAFT` references.

## Requirements

### Requirement: DRAFT removed from EventState enum (R001)

The `EventState` enum MUST NOT contain a `DRAFT` value. The valid states SHALL be: `OPEN`, `CALCULATED`, `CLOSED`. The `isDraft()` extension function MUST be removed. The `stateLabel()` function MUST NOT contain a `DRAFT` branch.

#### Scenario: EventState enum has three values

- GIVEN the `EventState` enum after this change
- WHEN its entries are inspected
- THEN the entries MUST be exactly `OPEN`, `CALCULATED`, `CLOSED`

#### Scenario: isDraft helper removed

- GIVEN the codebase after this change
- WHEN source files are searched
- THEN no `isDraft()` function MUST exist on `EventItem`

### Requirement: Events created directly in OPEN state (R002)

`EventItem` default `state` parameter MUST be `EventState.OPEN`. All event creation paths (UI, tests) MUST produce events with `state = OPEN` without requiring a separate "open" action.

#### Scenario: New EventItem defaults to OPEN

- GIVEN a caller creates `EventItem(name = "Viaje", dateMillis = ..., ownerId = "u1")`
- WHEN the object is constructed
- THEN `event.state` MUST be `EventState.OPEN`

#### Scenario: EventsScreen creates event in OPEN state

- GIVEN the user taps "Crear nuevo evento" in `EventsScreen`
- WHEN the editable event is constructed
- THEN its `state` MUST be `EventState.OPEN`

### Requirement: "Abrir evento" button removed from UI (R003)

`EventDetailScreen` MUST NOT render the "Abrir evento" button. The `onOpenEvent` callback parameter and its wiring in `CuentaMorososApp` MUST be removed. `EventsViewModel.openEvent()` and `openEventConfirmed()` MUST be removed.

#### Scenario: No "Abrir evento" button in EventDetailScreen

- GIVEN any event in any state
- WHEN `EventDetailScreen` renders the header
- THEN no button labelled "Abrir evento" MUST be present

#### Scenario: openEvent methods removed from EventsViewModel

- GIVEN the codebase after this change
- WHEN `EventsViewModel` is inspected
- THEN `openEvent()` and `openEventConfirmed()` methods MUST NOT exist

### Requirement: Persistence fallback changed to OPEN (R004)

All three persistence layers MUST use `EventState.OPEN` as the fallback default instead of `EventState.DRAFT`:

1. `CuentaMorososLocalStore` — `getOrDefault(EventState.OPEN)` and `optString("state", "OPEN")`
2. `OfflineFirstEventRepository` — `getOrDefault(EventState.OPEN)`
3. `FirestoreEventRepository` — `getOrDefault(EventState.OPEN)` and heuristic `else -> EventState.OPEN`

#### Scenario: CuentaMorososLocalStore falls back to OPEN

- GIVEN a stored event JSON with a missing or unparseable `state` field
- WHEN `CuentaMorososLocalStore` deserializes it
- THEN the event state MUST be `EventState.OPEN`

#### Scenario: OfflineFirstEventRepository falls back to OPEN

- GIVEN a cached event row with an invalid state string
- WHEN `toEventItem()` is called
- THEN the event state MUST be `EventState.OPEN`

#### Scenario: FirestoreEventRepository falls back to OPEN

- GIVEN a Firestore document with no `state` field and no participants or calculation data
- WHEN `computeStateFromFirestore()` is called
- THEN the returned state MUST be `EventState.OPEN`

### Requirement: State machine DRAFT transitions removed (R005)

`StateMachine.attemptTransition()` MUST NOT contain any transition involving `DRAFT`. The `guardDraftToOpen()` function MUST be removed. The `CLOSED to DRAFT` case MUST be removed from `guardFromClosed()`.

#### Scenario: No DRAFT transitions in state machine

- GIVEN the state machine after this change
- WHEN any transition involving `DRAFT` is attempted
- THEN the result MUST be `Blocked("Transición no válida")`

#### Scenario: OPEN to CALCULATED still works

- GIVEN an event in `OPEN` state with expenses
- WHEN transition to `CALCULATED` is attempted
- THEN the result MUST be `Allowed`

### Requirement: UI components updated for DRAFT removal (R006)

All UI components referencing `DRAFT` MUST be updated:

- `StateBadge` — remove `DRAFT` colour and label branches
- `EventStateColors` — remove `DRAFT` colour mapping and label
- `SettlementPanel` — change default parameter from `EventState.DRAFT` to `EventState.OPEN`
- `EventValidator` — remove `isDraft` conditional; all events receive standard validation

#### Scenario: StateBadge renders OPEN correctly

- GIVEN an event in `OPEN` state
- WHEN `StateBadge` renders
- THEN it MUST display "Abierto" with the appropriate colour

#### Scenario: EventValidator applies standard validation to all events

- GIVEN a newly created event in `OPEN` state with fewer than 2 participants
- WHEN `EventValidator.validate()` is called
- THEN it MUST return an error (not a warning)
