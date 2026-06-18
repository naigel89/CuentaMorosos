# payment-reminders Specification

## Purpose

Per-profile payment reminder generation with personalized "te debe"/"debés a" direction, dispatched via `PaymentReminder` notification type on `ch_reminders` channel.

## Requirements

### R001: Per-Profile Payment Message Generation

`ReminderService.buildReminderMessages()` SHALL produce one `ReminderMessage` per unpaid debt (not per event), containing debtor/creditor profile name and amount in euros. Only `PENDING_DEBT` type remains.

#### Scenario: Debt owed TO current user ("te debe")

- GIVEN profile "Luis" owes €15,50 to current user in event "Cena"
- WHEN `buildReminderMessages` runs
- THEN message body SHALL be "Luis te debe €15,50"
- AND `isOwedToYou` SHALL be true

#### Scenario: Debt owed BY current user ("debés a")

- GIVEN current user owes €8,00 to profile "Ana" in event "Cena"
- WHEN `buildReminderMessages` runs
- THEN message body SHALL be "Debes €8,00 a Ana"
- AND `isOwedToYou` SHALL be false

#### Scenario: Multiple debts produce per-debt messages

- GIVEN event "Fiesta" has 3 unpaid debts across different profiles
- WHEN `buildReminderMessages` runs
- THEN 3 separate `ReminderMessage` instances SHALL be generated

#### Scenario: No pending debts

- GIVEN all debts in all events are paid
- WHEN `buildReminderMessages` runs
- THEN an empty list SHALL be returned

#### Scenario: Reminders disabled

- GIVEN `remindersEnabled = false`
- WHEN `buildReminderMessages` is called
- THEN an empty list SHALL be returned

### R002: PaymentReminder Notification Data

`NotificationEvent.PaymentReminder(eventId, profileName, amountEuros, isOwedToYou)` SHALL carry per-debt notification data for dispatch.

#### Scenario: Direction field

- GIVEN debt is owed TO current user by profile "Luis"
- WHEN `PaymentReminder` is constructed
- THEN `isOwedToYou` SHALL be true
- AND `profileName` SHALL be "Luis"

### R003: PaymentReminder Notification Dispatch

`NotificationDispatcher` SHALL post `PaymentReminder` on `ch_reminders` channel with `ic_notification_calc` small icon and app logo as large icon.

#### Scenario: Channel and icon

- GIVEN a `PaymentReminder` notification
- WHEN `NotificationDispatcher.dispatch()` is called
- THEN channel SHALL be `ch_reminders`
- AND small icon SHALL be `ic_notification_calc`
- AND large icon SHALL be the app logo vector drawable

#### Scenario: Fingerprint dedup

- GIVEN two identical `PaymentReminder` instances for same event + profile
- WHEN both are dispatched within the dedup window
- THEN the second SHALL be suppressed via `fingerprintFor` / `hasNotificationBeenSent`

#### Scenario: Existing notification types unaffected

- GIVEN `InvitationReceived`, `InvitationAccepted`, or `CalculationCompleted` events
- WHEN `NotificationDispatcher.dispatch()` is called
- THEN behavior SHALL be unchanged from current implementation

### R004: ReminderWorker PaymentReminder Dispatch

`ReminderWorker` SHALL construct `PaymentReminder` events from `ReminderMessage` list and dispatch them. `UpcomingEvent` SHALL NOT be dispatched.

#### Scenario: Worker dispatches payment reminders

- GIVEN `ReminderService.buildReminderMessages()` returns 2 messages
- WHEN `ReminderWorker.doWork()` executes
- THEN 2 `PaymentReminder` events SHALL be dispatched
- AND no `UpcomingEvent` SHALL be dispatched

#### Scenario: Worker respects remindersEnabled

- GIVEN `remindersEnabled = false`
- WHEN `ReminderWorker.doWork()` executes
- THEN zero `PaymentReminder` events SHALL be dispatched
