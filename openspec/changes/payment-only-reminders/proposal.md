# Proposal: Payment-Only Reminders

## Intent

The reminder system currently produces 3 types of messages (PENDING_DEBT, INCOMPLETE_EVENT, UPCOMING_EVENT) but dispatches ALL through `NotificationEvent.UpcomingEvent`, losing semantic differentiation. Messages are generic ("N pagos siguen abiertos") without naming profiles or amounts, and no distinction between "someone owes you" vs "you owe someone." This change simplifies reminders to payment-only with personalized per-profile messages.

## Scope

### In Scope
- Remove `INCOMPLETE_EVENT` and `UPCOMING_EVENT` from `ReminderType` enum and `ReminderService.buildReminderMessages()`
- Remove `ReminderService.buildUpcomingEventMessages()` entirely
- Add `PaymentReminder` to `NotificationEvent` sealed class (profile name, amount, direction: owes-you / you-owe)
- Generate per-profile PENDING_DEBT messages: one per debtor/creditor, not one per event
- Personalized messages: "[Profile] te debe €X.XX" / "Debés €X.XX a [Profile]"
- `NotificationDispatcher`: handle `PaymentReminder` with `ch_reminders` channel, money icon, app logo as large icon
- `ReminderWorker`: dispatch `PaymentReminder` instead of `UpcomingEvent`
- Remove "Enviar ahora" OutlinedButton from SettingsScreen (lines 249–260) and its `onPostReminders` wiring
- Remove "Ocultar" TextButton from ReminderSummaryCard in ScreenComponents.kt (lines 152–154)
- Clean up `NotificationScheduler` (becomes unused after removing "Enviar ahora")

### Out of Scope
- Invitation/accepted/calculation notifications (unchanged)
- FCM push notification changes
- UI for notification preferences beyond toggle
- iOS notifications

## Capabilities

### New Capabilities
- `payment-reminders`: per-profile payment reminder generation, personalized messages with direction, PaymentReminder notification type dispatch

### Modified Capabilities
- `local-notification-triggers`: R104 (Upcoming Event Trigger) → REMOVED; R107 (Personalized Message Context) → extended with profile name and amount direction; reminder trigger limited to PENDING_DEBT only

## Approach

1. **ReminderService**: Remove `ReminderType.INCOMPLETE_EVENT` and `.UPCOMING_EVENT`. Remove `buildUpcomingEventMessages()`. Refactor PENDING_DEBT block in `buildReminderMessages()` to emit one `ReminderMessage` per unpaid debt (including debtor/creditor profile IDs and amounts).
2. **NotificationEvent**: Add `PaymentReminder(eventId, profileName, amountEuros, isOwedToYou)` data class.
3. **NotificationDispatcher**: Add `PaymentReminder` branch in `fingerprintFor`, `notificationType`, `channelIdFor`, `titleFor`, `bodyFor`, `notificationTag`, `notificationId`, and `addActions`. Map to `ch_reminders` channel. Use `ic_notification_calc` icon. Add app logo as large icon.
4. **ReminderWorker**: Build `PaymentReminder` events per message instead of `UpcomingEvent`.
5. **UI cleanup**: Remove "Enviar ahora" button + `onPostReminders` callback from Settings/CuentaMorososApp/MainActivity. Remove "Ocultar" from ReminderSummaryCard.
6. **NotificationScheduler**: Delete file (only used by "Enviar ahora" flow).

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `shared/.../data/ReminderService.kt` | Modified | Remove 2 types + upcoming method; per-profile messages |
| `shared/.../notifications/NotificationEvent.kt` | Modified | Add `PaymentReminder` data class |
| `app/.../notifications/NotificationDispatcher.kt` | Modified | Handle `PaymentReminder`, app logo large icon |
| `app/.../data/ReminderWorker.kt` | Modified | Dispatch `PaymentReminder` instead of `UpcomingEvent` |
| `shared/.../ui/SettingsScreen.kt` | Modified | Remove "Enviar ahora" button (L249–260) |
| `shared/.../ui/ScreenComponents.kt` | Modified | Remove "Ocultar" button (L152–154) |
| `shared/.../ui/CuentaMorososApp.kt` | Modified | Remove `onPostReminders` param and wiring; simplify reminderMessages |
| `app/.../MainActivity.kt` | Modified | Remove `onPostReminders` lambda |
| `app/.../data/NotificationScheduler.kt` | Removed | Only used by "Enviar ahora" |
| `app/src/test/.../NotificationSchedulerDedupTest.kt` | Removed | Tests removed code |
| `app/src/test/.../ReminderWorkerDedupTest.kt` | Modified | `PaymentReminder` instead of `UpcomingEvent` |
| `app/src/test/.../NotificationDispatcherTest.kt` | Modified | Add `PaymentReminder` test cases |
| `shared/src/commonTest/.../ReminderServiceUpcomingTest.kt` | Removed | Tests removed `buildUpcomingEventMessages()` |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Removing upcoming events loses value for users with future events | Low | Upcoming events were redundant with PENDING_DEBT if debts exist; future events without debts don't need reminders |
| Personalized messages require profile lookup in ReminderWorker | Medium | Pass profile name from `ReminderMessage` (resolved during message building from event debts) |
| Breaking existing notification dedup | Low | `PaymentReminder` gets its own fingerprint; existing dedup for other types untouched |
| Removing "Enviar ahora" removes debug/testing capability | Low | Keep `onTestNotification` in MainActivity for dev testing |

## Rollback Plan

Revert the change commit. `NotificationScheduler` file can be restored from git. No database migrations — the `ReminderType` enum change is compile-time only.

## Dependencies

- `fix-notification-dedup` (archived): dedup infrastructure already in place; `PaymentReminder` plugs into it
- SQLDelight repositories already wired in `ReminderWorker` via `RepositoryProvider`

## Success Criteria

- [ ] `ReminderType` enum contains only `PENDING_DEBT`
- [ ] `buildUpcomingEventMessages()` method is removed
- [ ] `PaymentReminder` dispatched from `ReminderWorker` with correct channel, icon, and personalized message
- [ ] "Enviar ahora" button and `onPostReminders` wiring removed
- [ ] "Ocultar" button removed from ReminderSummaryCard
- [ ] `NotificationScheduler` removed or confirmed unused
- [ ] All existing tests pass; new tests for `PaymentReminder` dispatch
- [ ] No `UpcomingEvent` references remain in ReminderWorker or ReminderService
