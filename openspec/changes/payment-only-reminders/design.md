# Design: Payment-Only Reminders

## Technical Approach

Simplify reminders to payment-only: one `ReminderMessage` per unpaid debt with personalized direction ("te debe"/"debés a"). Remove `UpcomingEvent` and `NotificationScheduler`. Add `PaymentReminder` notification type on `ch_reminders` channel. Clean up "Enviar ahora" UI and its wiring.

## Architecture Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Profile/creditor resolution | Resolve inside `buildReminderMessages()` with `profiles` + `expenses` params | Pure function, testable; keeps service as single entry point |
| Creditor logic | Port `resolveEventCreditor()` into `ReminderService` | Single consumer; original stays in `CuentaMorososApp.kt` for dashboard |
| `ReminderType` enum | Keep `PENDING_DEBT` as only value | Minimal change; preserves type for extensibility |
| `CH_UPCOMING_EVENTS` channel | Stop creating; keep constant | Android can't un-create channels; old devices unaffected |
| Large icon | New `getAppLogoBitmap()` loading `R.mipmap.ic_launcher` | Spec requires app logo; launcher icon is canonical identity |

## Data Flow

```
ReminderWorker.doWork()
  │
  ├─ profileRepository.observeProfiles().first()          [NEW]
  ├─ eventRepository.observeEvents().first()
  ├─ debtRepository.observeAllDebts().first()
  ├─ expenseRepository.observeAllExpenses().first()
  ├─ currentUserUid ← FirebaseAuth.getInstance().uid      [NEW]
  │
  ├─ ReminderService.buildReminderMessages(                [MODIFIED]
  │     events, debts, expenses, profiles,
  │     currentUserUid, reminderDays, remindersEnabled)
  │     │
  │     └─ Per unpaid debt where eventAge ≥ thresholdMillis:
  │          ├─ debt.profileId != currentUserUid → "te debe"
  │          │    profileName = profileMap[debt.profileId].displayName
  │          │    ReminderMessage(profileName, amountEuros, isOwedToYou=true)
  │          └─ debt.profileId == currentUserUid → "debés a"
  │               creditorId = resolveEventCreditor(debt, expenses, eventMap, uid)
  │               profileName = profileMap[creditorId].displayName
  │               ReminderMessage(profileName, amountEuros, isOwedToYou=false)
  │
  └─ dispatcher.dispatch(PaymentReminder(...))             [NEW]
       ├─ fingerprintFor → "PAYMENT_REMINDER:{eventId}:{profileName}"
       ├─ ensureChannels() → creates 3 channels (no ch_upcoming_events)
       ├─ buildNotification → ch_reminders, ic_notification_calc, app logo
       ├─ title: "Recordatorio de pago"
       └─ body: from ReminderMessage.body
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `shared/.../data/ReminderService.kt` | **Modify** | Remove `INCOMPLETE_EVENT`, `UPCOMING_EVENT` from enum. Add `profileName`, `amountEuros`, `isOwedToYou` to `ReminderMessage`; drop `daysUntil`, `dateFormatted`. Add `profiles`, `currentUserUid`, `expenses` params. Per-debt iteration with direction + creditor resolution. Remove `buildUpcomingEventMessages()`. |
| `shared/.../notifications/NotificationEvent.kt` | **Modify** | Add `PaymentReminder(eventId, profileName, amountEuros, isOwedToYou)`. Remove `UpcomingEvent`. |
| `app/.../notifications/NotificationDispatcher.kt` | **Modify** | Add `TAG_PAYMENT_REMINDER`. Add `PaymentReminder` branch to: `fingerprintFor`, `notificationType`, `channelIdFor`→`CH_REMINDERS`, `smallIconResFor`→`ic_notification_calc`, `titleFor`, `bodyFor`, `notificationTag`, `addActions`. Replace `UPCOMING_EVENT` with `PAYMENT_REMINDER` in `NotificationType` enum. Remove `ch_upcoming_events` creation in `ensureChannels()`. Add `getAppLogoBitmap()` method. |
| `app/.../data/ReminderWorker.kt` | **Modify** | Fetch profiles via `profileRepository`. Get `currentUserUid` from `FirebaseAuth`. Pass both to `buildReminderMessages()`. Build `PaymentReminder` events per message. Remove `buildUpcomingEventMessages()` call and `UpcomingEvent` dispatch. |
| `app/.../data/NotificationScheduler.kt` | **Delete** | Only used by "Enviar ahora" flow. Dedup now handled by `NotificationDispatcher`. |
| `shared/.../ui/SettingsScreen.kt` | **Modify** | Remove `onPostReminders` param (L48) + forwarding (L96). Remove "Enviar ahora" `OutlinedButton` (L249–260). |
| `shared/.../ui/ScreenComponents.kt` | **Modify** | Remove "Ocultar" `TextButton` (L152–154) from `ReminderSummaryCard`. |
| `shared/.../ui/CuentaMorososApp.kt` | **Modify** | Remove `onPostReminders` param (L193) and its forwarding to `SettingsScreen` (L712). Update `reminderMessages` derivation to pass `profiles` + `currentUserUid`. |
| `app/.../MainActivity.kt` | **Modify** | Remove `NotificationScheduler` import (L33), `ensureChannel` call (L113), `onPostReminders` lambda (L314–316). |
| `app/src/test/.../NotificationSchedulerDedupTest.kt` | **Delete** | Tests removed `NotificationScheduler`. |
| `shared/src/commonTest/.../ReminderServiceUpcomingTest.kt` | **Delete** | Tests removed `buildUpcomingEventMessages()`. |
| `app/src/test/.../ReminderWorkerDedupTest.kt` | **Modify** | `PaymentReminder` instead of `UpcomingEvent` in test assertions. |
| `app/src/test/.../NotificationDispatcherTest.kt` | **Modify** | Add `PaymentReminder` dispatch test cases. |

## Interfaces / Contracts

```kotlin
// ReminderMessage (modified)
data class ReminderMessage(
    val title: String,
    val body: String,
    val type: ReminderType = ReminderType.PENDING_DEBT,
    val eventId: String? = null,
    val profileName: String? = null,    // NEW
    val amountEuros: Double? = null,    // NEW
    val isOwedToYou: Boolean = false,   // NEW
)

// ReminderType (modified)
enum class ReminderType { PENDING_DEBT }

// NotificationEvent.PaymentReminder (new)
data class PaymentReminder(
    override val eventId: String,
    val profileName: String,
    val amountEuros: Double,
    val isOwedToYou: Boolean,
) : NotificationEvent()

// ReminderService.buildReminderMessages (modified signature)
fun buildReminderMessages(
    events: List<EventItem>,
    debts: List<EventDebtItem>,
    expenses: List<EventExpenseItem>,
    profiles: List<ProfileItem>,       // NEW
    currentUserUid: String,            // NEW
    reminderDays: Int,
    remindersEnabled: Boolean,
    nowMillis: Long = currentTimeMillis(),
): List<ReminderMessage>
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit — `ReminderService` | Per-debt message generation with both directions; empty when disabled/no debts; thresholdMillis logic | Pure function tests with fake data; existing test file updated |
| Unit — `NotificationDispatcher` | `PaymentReminder` fingerprint, channel, icon, title/body, dedup | Mirror existing tests for other event types |
| Unit — `ReminderWorker` | Dispatches `PaymentReminder` (not `UpcomingEvent`); respects `remindersEnabled` | Update existing dedup test |
| Deletion | `buildUpcomingEventMessages()` tests, `NotificationScheduler` tests | Remove test files; verify no remaining references |

## Migration / Rollout

No data migration required — all changes are compile-time (enum values, model fields). Backward compat: `CH_UPCOMING_EVENTS` channel remains on existing devices but unused; `UpcomingEvent` dedup fingerprints in local store are harmless and cleaned up by periodic `cleanupOldEntries()`. Revert is a git revert.

## Open Questions

*None — all decisions resolved.* `resolveEventCreditor` SHALL be extracted to a shared utility (e.g. `internal fun resolveEventCreditor(...)` in `shared/.../data/ReminderService.kt` or a new `shared/.../model/EventCreditorResolver.kt`) and used by both `CuentaMorososApp.kt` and `ReminderService`. No duplication tolerated.
