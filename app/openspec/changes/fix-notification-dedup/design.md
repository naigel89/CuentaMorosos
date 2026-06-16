# Design: fix-notification-dedup

## Technical Approach

Add a persistent fingerprint registry to `CuentaMorososLocalStore` backed by `SharedPreferences` `StringSet`. Every dispatch path (`NotificationDispatcher.dispatch()`, `NotificationScheduler.postReminders()`, `ReminderWorker.doWork()`) computes a deterministic fingerprint, checks the registry, and skips if already sent. In-memory dedup sets in `DashboardViewModel` (`notifiedCalculatedEventIds`) and `InvitationsViewModel` (`notifiedIds`) are removed. A first-launch migration seeds fingerprints for already-calculated events so they don't re-fire. Periodic cleanup prunes entries older than 30 days.

## Architecture Decisions

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Store as `"{epochMs}\|{fingerprint}"` in `StringSet` | No native timestamp support in StringSet, but prefix-based cleanup is O(n) which is fine for <1000 entries | **Chosen**: simplest format, no JSON overhead, direct parsing |
| JSON array of `{fingerprint, timestamp}` as single String key | More structured, heavier serialization, more complex to update atomically | Rejected: StringSet avoids JSON overhead and is more idiomatic for the existing local store pattern |
| `synchronized` block on `prefs.edit()` | Blocks calling thread; all callers are in coroutine context anyway | **Chosen**: matches existing codebase style, avoids bringing in `kotlinx.coroutines.sync.Mutex` to the non-KMP app module |

## Data Flow

```
    FCM ──→ CuentaMorososFirebaseMessagingService
                  │
    UI callback ──→ NotificationDispatcher.dispatch(event)
    DashboardVM         │
    InvitationsVM    computeFingerprint(event)
                         │
                    hasNotificationBeenSent(fingerprint)?
                         │ YES → return (skip)
                         │ NO  → build + notify + recordNotificationSent()
                         
    ReminderWorker ──→ ReminderService.build*Messages()
                           │
                      ReminderMessage list
                           │
                      filter: hasNotificationBeenSent(fingerprint) per message
                           │
                      dispatch unsent → recordNotificationSent()
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `app/…/data/CuentaMorososLocalStore.kt` | Modify | Add `KEY_SENT_FINGERPRINTS`, `hasNotificationBeenSent()`, `recordNotificationSent()`, `cleanupOldEntries()`, `seedDedupMigration()` |
| `app/…/notifications/NotificationDispatcher.kt` | Modify | Accept `CuentaMorososLocalStore` param; add dedup guard as early return in `dispatch()`; expose `fingerprintFor(event)` |
| `app/…/data/NotificationScheduler.kt` | Modify | Accept `CuentaMorososLocalStore`; filter messages via `hasNotificationBeenSent()`; record after each post |
| `app/…/data/ReminderWorker.kt` | Modify | Pass `CuentaMorososLocalStore` to `NotificationScheduler` and `NotificationDispatcher` |
| `shared/…/ui/DashboardViewModel.kt` | Modify | Remove `notifiedCalculatedEventIds` mutable set; rely on persistent registry |
| `shared/…/ui/InvitationsViewModel.kt` | Modify | Remove `notifiedIds` mutable set; rely on persistent registry |
| `app/…/data/CuentaMorososFirebaseMessagingService.kt` | Modify | Pass `CuentaMorososLocalStore` to `NotificationDispatcher` |

## Interfaces / Contracts

```kotlin
// CuentaMorososLocalStore additions
fun hasNotificationBeenSent(fingerprint: String): Boolean
fun recordNotificationSent(fingerprint: String)
fun cleanupOldEntries(maxAgeDays: Int = 30)
fun seedDedupMigration(events: List<EventItem>)

// NotificationDispatcher — new companion method
fun fingerprintFor(event: NotificationEvent): String = when (event) {
    is InvitationReceived -> "$TAG_INVITATION_RECEIVED:${event.eventId}:${event.invitationId}"
    is InvitationAccepted -> "$TAG_INVITATION_ACCEPTED:${event.eventId}:${event.inviteeName}"
    is CalculationCompleted -> "$TAG_CALCULATION_COMPLETED:${event.eventId}"
    is UpcomingEvent -> "$TAG_UPCOMING_EVENT:${event.eventId}:${event.daysUntil}:${event.dateFormatted}"
}
```

Storage format: `"{epochMillis}|{fingerprint}"` per StringSet entry. On null/blank fingerprint, `hasNotificationBeenSent()` returns `false` and `recordNotificationSent()` no-ops. Thread safety via `synchronized(prefs) { prefs.edit()… }` block.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | `CuentaMorososLocalStore`: register, dedup, null safety, cleanup prunes old but keeps recent | Robolectric + `ShadowSharedPreferences`; test file at `app/src/test/…/data/CuentaMorososLocalStoreDedupTest.kt` |
| Unit | `NotificationDispatcher.fingerprintFor()`: deterministic output per event type | Pure Kotlin unit test; no Android dependencies needed (fingerprintFor is on companion) |
| Integration | `NotificationDispatcher.dispatch()` skips when fingerprint exists, dispatches + records when absent | Extend `NotificationDispatcherTest.kt`: inject store stub, verify `ShadowNotificationManager` count |
| Unit | `NotificationScheduler.postReminders()` filters sent, records unsent | Create test file; mock `CuentaMorososLocalStore`; assert notification count |
| Integration | `ReminderWorker` idempotency: second run produces zero notifications | Create `ReminderWorkerDedupTest.kt` with controlled data |
| Unit | ViewModel removal: `notifiedCalculatedEventIds`/`notifiedIds` deleted, tests still pass | Modify existing `DashboardViewModelNotificationTest.kt`, `InvitationsViewModelNotificationTest.kt` |

## Migration & Backward Compatibility

- **First-launch seed**: `seedDedupMigration(events)` runs when `KEY_SENT_FINGERPRINTS` does not exist. Iterates `EventItem` list; for each `CALCULATED` event, computes `CALCULATION_COMPLETED:{eventId}` fingerprint and records it. Pending/uncalculated events are NOT seeded.
- **Cleanup**: `cleanupOldEntries()` called on app start in `CuentaMorososApp.onCreate()`. Parses epoch prefix from each entry; removes if older than 30 days.
- **No in-memory dedup removal change for users**: The existing in-memory sets only prevented duplicates within a single ViewModel lifecycle. After migration, persistence covers all paths. Users will receive fewer duplicate notifications immediately on first launch.

## Open Questions

- [ ] Should `ReminderWorker` also use `NotificationScheduler.postReminders()` instead of calling `NotificationDispatcher` directly for `UpcomingEvent`? (Current design: separate paths, both guarded)
- [ ] Do we want a configurable max fingerprint count threshold (e.g., 5000) before forced cleanup, or just time-based?
