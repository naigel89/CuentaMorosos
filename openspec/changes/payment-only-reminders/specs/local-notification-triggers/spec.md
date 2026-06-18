# Delta for local-notification-triggers

## MODIFIED Requirements

### R107: Personalized Message Context

All notification messages SHALL include contextual variables resolved at trigger time.

| Variable | Source | Example |
|---|---|---|
| inviterName / inviteeName | `ProfileItem.displayName` or email fallback | "Ana" |
| eventName | `EventInvitation.eventName` or `EventItem.name` | "Asado" |
| amountOwed | `SettlementEngine` output, formatted with `formatEuros` | "€25,50" |
| profileName | `ProfileItem.displayName` or email fallback of debtor/creditor | "Luis" |
| amountEuros | `DebtItem.amountEuros`, formatted with `formatEuros` | "15,50" |
| isOwedToYou | `true` when debt owed TO current user, `false` when BY | true |

(Previously: included `daysUntil` and `dateFormatted` for upcoming events — removed with R104. Extended with `profileName`, `amountEuros`, `isOwedToYou` for payment reminders.)

#### Scenario: Profile name available

- GIVEN the inviter has a `ProfileItem` with `displayName = "Ana García"`
- WHEN an invitation-received notification is built
- THEN the message SHALL use "Ana García" (not email)

#### Scenario: Profile name unavailable — fallback

- GIVEN the inviter has no `ProfileItem` (only Firebase email)
- WHEN an invitation-received notification is built
- THEN the message SHALL use the email local part as fallback

#### Scenario: Payment reminder "te debe" direction

- GIVEN debt of €15,50 is owed TO current user by profile "Luis"
- WHEN a payment reminder notification is built
- THEN `isOwedToYou` SHALL be true
- AND body SHALL contain "Luis te debe €15,50"

#### Scenario: Payment reminder "debés a" direction

- GIVEN debt of €8,00 is owed BY current user to profile "Ana"
- WHEN a payment reminder notification is built
- THEN `isOwedToYou` SHALL be false
- AND body SHALL contain "Debes €8,00 a Ana"

## REMOVED Requirements

### R104: Upcoming Event Trigger

Reason: Simplified to payment-only reminders. `ReminderType.UPCOMING_EVENT`, `ReminderType.INCOMPLETE_EVENT`, and `buildUpcomingEventMessages()` are removed. Reminder triggers are now limited to `PENDING_DEBT` only. Events without debts do not need reminders; events with debts are already covered by PENDING_DEBT.

### Manual Reminder Dispatch

The manual "Enviar ahora" button in `SettingsScreen`, the "Ocultar" button in `ReminderSummaryCard`, and the `NotificationScheduler` class SHALL be removed. All reminder dispatch now flows exclusively through `ReminderWorker`'s periodic `WorkManager` schedule. The `onPostReminders` callback wiring in `CuentaMorososApp` and `MainActivity` SHALL be removed.
