# Local Notification Triggers Specification

## Purpose

Defines the ViewModel-level and service-level trigger points that fire the 4 notification types. These triggers are platform-agnostic callbacks in the `shared` module that delegate to `NotificationDispatcher` (in `app`) for actual posting.

## Requirements

### R101: Invitation Received Trigger

`InvitationsViewModel` SHALL invoke `onNewInvitation` when a new `EventInvitation` with status `PENDING` appears in the Firestore listener stream and has not been previously notified in this session.

#### Scenario: New invitation arrives (happy path)

- GIVEN the user is logged in and `InvitationsViewModel` is active
- WHEN Firestore emits a new invitation with `status = PENDING` for the current user
- THEN `onNewInvitation` SHALL be called with `(eventName, inviterName, eventId, invitationId)`
- AND the invitation ID SHALL be recorded in `notifiedIds` to prevent duplicates

#### Scenario: Duplicate suppression within session

- GIVEN an invitation with ID "inv-001" was already notified
- WHEN the Firestore listener re-emits the same invitation (e.g., after a sync refresh)
- THEN `onNewInvitation` SHALL NOT be called again for "inv-001"

#### Scenario: Callback not wired (graceful degradation)

- GIVEN `AppViewModelFactory` creates `InvitationsViewModel` without `onNewInvitation` callback
- WHEN a new invitation arrives
- THEN the invitation SHALL appear in `pendingInvitations` StateFlow
- AND no notification SHALL be posted (no crash)

---

### R102: Invitation Accepted Trigger

The system SHALL notify the inviter when an invitation they sent changes status to `ACCEPTED`.

#### Scenario: Invitation accepted by invitee

- GIVEN user "Ana" sent an invitation to "Luis" for event "Asado"
- WHEN the Firestore listener detects the invitation status changed to `ACCEPTED`
- THEN a notification SHALL be posted with type `INVITATION_ACCEPTED`
- AND body SHALL be "Luis aceptó tu invitación a 'Asado'"
- AND deep link SHALL target `EventDetailScreen` with the event's ID

#### Scenario: Invitation rejected — no notification

- GIVEN user "Ana" sent an invitation to "Luis"
- WHEN the invitation status changes to `REJECTED`
- THEN NO notification SHALL be posted (rejection is shown in-app only)

#### Scenario: Multiple invitations accepted in quick succession

- GIVEN 3 invitations are accepted within 5 seconds
- THEN 3 separate notifications SHALL be posted (no batching, no suppression)

---

### R103: Calculation Completed Trigger

When event calculations complete and the current user has a non-zero share, a notification SHALL be posted.

#### Scenario: Calculation completes with amount owed

- GIVEN the user is a participant in event "Cumpleaños"
- WHEN `DashboardViewModel` detects the event state transitions to `CALCULATED`
- AND the settlement engine computes the user owes €25.50
- THEN a notification SHALL be posted with type `CALCULATION_COMPLETED`
- AND body SHALL include "Debes €25,50"

#### Scenario: Calculation completes but user owes nothing

- GIVEN the user is a participant in event "Reunión"
- WHEN calculations complete and the user's balance is €0.00
- THEN NO notification SHALL be posted

#### Scenario: Calculation re-run (same event)

- GIVEN a notification was already posted for event "Cumpleaños" calculation
- WHEN the owner recalculates and the amount changes to €30.00
- THEN a new notification SHALL be posted with the updated amount
- AND the previous notification for the same event SHALL be cancelled (same notification ID)

---

### R104: Upcoming Event Trigger

`ReminderService` SHALL generate upcoming-event messages for events whose `dateMillis` falls within `reminderDays` in the future from the current time.

#### Scenario: Event within reminder window

- GIVEN `reminderDays = 3` and current time is 12/Jun/2026
- WHEN an event exists with `dateMillis = 15/Jun/2026` (3 days away)
- THEN a `ReminderMessage` SHALL be generated
- WITH title "Próximo evento: [Nombre]"
- AND body "El evento '[Nombre]' es en 3 días (15/06/2026)"

#### Scenario: Event outside reminder window

- GIVEN `reminderDays = 3`
- WHEN an event exists with `dateMillis = 20/Jun/2026` (8 days away)
- THEN NO upcoming-event message SHALL be generated for this event

#### Scenario: Event date already passed

- GIVEN an event with `dateMillis` in the past
- WHEN `ReminderService.buildReminderMessages` runs
- THEN NO upcoming-event message SHALL be generated (only existing pending-debt logic applies)

#### Scenario: Reminders disabled

- GIVEN `remindersEnabled = false`
- WHEN `buildReminderMessages` is called
- THEN an empty list SHALL be returned (no messages of any type)

---

### R105: ReminderWorker SQLDelight Migration

`ReminderWorker` SHALL read events, debts, and expenses from SQLDelight repositories via `RepositoryProvider` instead of `CuentaMorososLocalStore` (SharedPreferences).

#### Scenario: Worker reads fresh data

- GIVEN the SQLDelight database has been synced with Firestore
- WHEN `ReminderWorker.doWork()` executes
- THEN it SHALL query events from `EventRepository` (SQLDelight-backed)
- AND the data SHALL reflect the latest synced state

#### Scenario: RepositoryProvider unavailable

- GIVEN `ReminderWorker` cannot obtain `RepositoryProvider` (e.g., DI not initialized)
- WHEN `doWork()` executes
- THEN the worker SHALL return `Result.retry()` to reschedule

---

### R106: ViewModel Callback Wiring

`AppViewModelFactory` SHALL accept an optional `NotificationCallback` parameter and pass the relevant callbacks to each ViewModel that needs them.

#### Scenario: Factory creates InvitationsViewModel with callback

- GIVEN `AppViewModelFactory` is constructed with a `NotificationCallback` instance
- WHEN it creates `InvitationsViewModel`
- THEN the `onNewInvitation` parameter SHALL be wired to `NotificationCallback.onInvitationReceived`

#### Scenario: Factory creates InvitationsViewModel without callback

- GIVEN `AppViewModelFactory` is constructed with `NotificationCallback = null`
- WHEN it creates `InvitationsViewModel`
- THEN `onNewInvitation` SHALL be `null` (graceful degradation, no crash)

---

### R107: Personalized Message Context

All notification messages SHALL include contextual variables resolved at trigger time.

| Variable | Source | Example |
|---|---|---|
| inviterName / inviteeName | `ProfileItem.displayName` or email fallback | "Ana" |
| eventName | `EventInvitation.eventName` or `EventItem.name` | "Asado" |
| amountOwed | `SettlementEngine` output, formatted with `formatEuros` | "€25,50" |
| daysUntil | Computed from `(event.dateMillis - nowMillis) / MILLIS_PER_DAY` | "3" |
| dateFormatted | `event.formattedDate()` | "15/06/2026" |

#### Scenario: Profile name available

- GIVEN the inviter has a `ProfileItem` with `displayName = "Ana García"`
- WHEN an invitation-received notification is built
- THEN the message SHALL use "Ana García" (not email)

#### Scenario: Profile name unavailable — fallback

- GIVEN the inviter has no `ProfileItem` (only Firebase email)
- WHEN an invitation-received notification is built
- THEN the message SHALL use the email local part as fallback (e.g., "ana@gmail.com")

---

## Affected Files

| File | Change |
|---|---|
| `shared/.../ui/InvitationsViewModel.kt` | Extend `onNewInvitation` signature with eventId/invitationId |
| `shared/.../ui/DashboardViewModel.kt` | Add calculation-completed trigger callback |
| `shared/.../data/ReminderService.kt` | Add `buildUpcomingEventMessages()` method |
| `app/.../ReminderWorker.kt` | Migrate from `CuentaMorososLocalStore` to `RepositoryProvider` |
| `shared/.../AppViewModelFactory.kt` | Add `NotificationCallback` parameter, wire to ViewModels |
| `app/.../MainActivity.kt` | Create `NotificationDispatcher`, pass callbacks to factory |
| `shared/.../data/repository/FirestoreInvitationRepository.kt` | Add listener for invitation status changes (accepted) |
