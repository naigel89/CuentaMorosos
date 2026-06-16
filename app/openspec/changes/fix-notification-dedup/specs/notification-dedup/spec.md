# Notification Dedup Specification

## Purpose

Persistent fingerprint-based deduplication for all local notification types to prevent duplicate delivery across app restarts and background worker cycles.

## Requirements

### Requirement: Persistent Sent-Notification Registry

`CuentaMorososLocalStore` MUST expose `hasNotificationBeenSent(fingerprint: String): Boolean` and `recordNotificationSent(fingerprint: String)`. The registry MUST persist via `SharedPreferences`, survive app restarts, and be thread-safe for concurrent Worker/UI/FCM access. Null/blank fingerprints MUST return `false`/no-op without crashing.

#### Scenario: Fingerprint persists across restarts

- GIVEN `"CALCULATION_COMPLETED:evt-1"` was recorded
- WHEN the app is killed and relaunched
- THEN `hasNotificationBeenSent("CALCULATION_COMPLETED:evt-1")` returns `true`

#### Scenario: Null/empty and concurrent-write safety

- GIVEN null or blank fingerprint → MUST no-op safely without exception
- GIVEN two threads call `recordNotificationSent` simultaneously with different fingerprints → both MUST persist without data loss

### Requirement: Periodic Cleanup

The registry MUST prune entries older than 30 days on app startup. Each entry SHALL store an epoch-millis timestamp. Entries under 30 days MUST NOT be deleted. Empty-registry cleanup MUST no-op.

#### Scenario: Old pruned, recent kept

- GIVEN entries aged 31 days and 1 day
- WHEN cleanup runs
- THEN the 31-day entry is removed AND the 1-day entry remains

### Requirement: NotificationDispatcher Dedup Guard

`NotificationDispatcher.dispatch()` MUST compute a deterministic fingerprint per event, check the registry, and skip silently if already sent. Unsent events MUST dispatch and record.

| Event Type | Fingerprint |
|---|---|
| `InvitationReceived` | `INVITATION_RECEIVED:{eventId}:{invitationId}` |
| `InvitationAccepted` | `INVITATION_ACCEPTED:{eventId}:{inviteeName}` |
| `CalculationCompleted` | `CALCULATION_COMPLETED:{eventId}` |
| `UpcomingEvent` | `UPCOMING_EVENT:{eventId}:{daysUntil}:{dateFormatted}` |

#### Scenario: Send, record, skip duplicate

- GIVEN `CalculationCompleted` for `evt-1` was never sent
- WHEN `dispatch()` is called → notification posts AND registry records it
- WHEN `dispatch()` is called again with same event → no notification posts
- GIVEN system notifications disabled → `dispatch()` returns early before dedup check

### Requirement: NotificationScheduler Dedup Guard

`NotificationScheduler.postReminders()` MUST filter reminders via `hasNotificationBeenSent()`. Fingerprint format: `"reminder:{eventId}:{profileId}:{date}"`. Only unsent reminders SHALL post; each posted reminder MUST be recorded.

#### Scenario: Mixed-list filtering

- GIVEN 3 reminders, one already registered
- WHEN `postReminders()` is called
- THEN 2 unsent post and are recorded; the sent one is skipped

### Requirement: ReminderWorker Background Dedup

`ReminderWorker.doWork()` MUST reuse the registry through `NotificationDispatcher`. Payment reminders SHALL use `"payment-reminder:{eventId}:{profileId}:{daysLate}"`. Worker re-runs with unchanged state MUST produce zero notifications.

#### Scenario: Idempotent worker and new debt

- GIVEN worker dispatched all reminders in a prior run → re-run with no changes → zero notifications
- GIVEN new unpaid debt after prior run → worker dispatches only the new notification and records it

### Requirement: Volatile Dedup Set Removal

All in-memory dedup state in ViewModels MUST be removed. If `notifiedCalculatedEventIds` or `notifiedIds` exist, they SHALL be deleted. Dedup MUST rely solely on the persistent registry.

#### Scenario: Dedup survives ViewModel lifecycle

- GIVEN a notification was dispatched
- WHEN the hosting ViewModel is destroyed and recreated
- THEN the same notification is NOT re-dispatched

### Requirement: First-Launch Migration Seeding

On first launch after deployment, the registry SHALL seed fingerprints for visible events with completed calculations. Uncalculated events MUST NOT be pre-registered.

#### Scenario: Seeds only calculated events

- GIVEN 3 events: 2 calculated, 1 pending
- WHEN app updates and first-launches with dedup
- THEN fingerprints for the 2 calculated events are seeded; the pending event is not
