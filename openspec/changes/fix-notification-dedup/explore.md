# Exploration: fix-notification-dedup

## Current State

The notification system spans two layers:

### Layer 1 — Simple Reminders (NotificationScheduler)
- `ReminderService.buildReminderMessages()` generates `ReminderMessage` objects for: pending debts past threshold, incomplete events, and upcoming events
- Computed every composition via `derivedStateOf` in `CuentaMorososApp.kt:280`
- User can manually trigger via "Enviar ahora" button in `SettingsScreen.kt:250`
- `NotificationScheduler.postReminders()` posts all messages unconditionally using an incrementing index as notification ID
- **Zero deduplication** — messages are re-posted every time

### Layer 2 — Event-Driven Notifications (NotificationDispatcher)
- Four event types: `InvitationReceived`, `InvitationAccepted`, `CalculationCompleted`, `UpcomingEvent`
- Triggered by:
  - `InvitationsViewModel` (on new pending invitations) — uses in-memory `notifiedIds: MutableSet<String>`
  - `DashboardViewModel` (on CALCULATED state transitions) — uses in-memory `notifiedCalculatedEventIds: MutableSet<String>`
  - `ReminderWorker` (periodic background) — calls `ReminderService` + dispatches
  - FCM push (`CuentaMorososFirebaseMessagingService`)
- Notification ID = `eventId.hashCode()` — same event always gets same tag+id, so `notify()` replaces previous
- But the **dispatch CALL itself is unconditional** — the guard is only the in-memory set

### Background Worker
- `ReminderWorker` scheduled as `PeriodicWorkRequest(24h)` via `WorkManager`
- `schedule()` called every time preferences are saved with `remindersEnabled = true`
- Worker reads from SQLDelight repositories via `CuentaMorososApp.repositoryProvider`
- Retries if `RepositoryProvider` not initialized yet

### Persistence
- `CuentaMorososLocalStore` uses `SharedPreferences` — stores events, debts, expenses, profiles, and `UserPreferences` (theme, reminderDays, remindersEnabled)
- **No field exists to track sent notifications** — no `lastNotificationSent`, no `sentReminderIds`, no `notifiedEventIds` in preferences
- `EventItem` has `lastCalculation*` fields but no `lastReminderSent` or similar

---

## Affected Areas

| File | Why |
|------|-----|
| `app/src/main/java/com/cuentamorosos/data/NotificationScheduler.kt` | `postReminders()` has no dedup logic — posts every message unconditionally |
| `shared/src/commonMain/kotlin/com/cuentamorosos/data/ReminderService.kt` | Builds reminder messages without awareness of what was already sent |
| `shared/src/commonMain/kotlin/com/cuentamorosos/ui/InvitationsViewModel.kt:22` | `notifiedIds` is in-memory, lost on ViewModel destroy |
| `shared/src/commonMain/kotlin/com/cuentamorosos/ui/DashboardViewModel.kt:35` | `notifiedCalculatedEventIds` is in-memory, lost on ViewModel destroy |
| `app/src/main/java/com/cuentamorosos/notifications/NotificationDispatcher.kt:55` | `dispatch()` has no persistence check — always posts |
| `app/src/main/java/com/cuentamorosos/data/CuentaMorososLocalStore.kt` | No `saveSentNotification`/`hasNotificationBeenSent` methods |
| `shared/src/commonMain/kotlin/com/cuentamorosos/model/Models.kt:76` | `EventItem` has no `lastReminderSent` field |
| `shared/src/commonMain/kotlin/com/cuentamorosos/model/Models.kt:152` | `UserPreferences` has no sent-notifications tracking |
| `app/src/main/java/com/cuentamorosos/data/ReminderWorker.kt` | 24h period may be too long; no dedup in dispatch loop |
| `shared/src/commonMain/kotlin/com/cuentamorosos/ui/CuentaMorososApp.kt:280-288` | `reminderMessages` recomputed every composition |
| `app/src/main/java/com/cuentamorosos/MainActivity.kt:311-312` | Passes `onPostReminders` to shared UI but no pre-filtering |
| `app/src/main/AndroidManifest.xml` | No explicit WorkManager initializer (uses default) |

---

## Per-Problem Analysis

### Problem 1: Notifications fire every time the app opens

**Root cause**: All deduplication state is purely in-memory (ViewModel `mutableSetOf`), destroyed on process death or app close. When the app reopens, every ViewModel is recreated with empty sets, so every trigger re-fires.

**Exact locations**:
- `DashboardViewModel.kt:35` — `private val notifiedCalculatedEventIds = mutableSetOf<String>()`
- `InvitationsViewModel.kt:22` — `private val notifiedIds = mutableSetOf<String>()`
- `NotificationScheduler.kt:30-48` — `postReminders()` has NO filter at all
- `NotificationDispatcher.kt:55-66` — `dispatch()` checks `areNotificationsEnabled()` but nothing else

**Impact**: Every time the user opens the app:
1. If any event is CALCULATED, a calculation notification fires even if already sent
2. If any pending invitation exists, an invitation notification fires again
3. Reminder messages (pending debts, incomplete events, upcoming events) are all re-postable via Settings "Enviar ahora"

**Proposed approach**: Add a persistent "sent notifications" registry in `CuentaMorososLocalStore`. Each notification gets a stable fingerprint (type + eventId for event-driven, or type + eventId for reminders). Before dispatching, check the registry. After dispatching, record it. This must work for BOTH `NotificationScheduler.postReminders()` and `NotificationDispatcher.dispatch()`.

---

### Problem 2: Background notifications when app is closed

**Root cause**: Infrastructure EXISTS and should work. `ReminderWorker` is a `CoroutineWorker` registered with WorkManager (24h periodic). It reads from SQLDelight via `RepositoryProvider`. However:

1. Worker is scheduled in `MainActivity` (login flow), NOT at Application startup — if user kills app and never reopens, worker might not be active
2. No `WorkManager` initializer in `AndroidManifest.xml` — uses default, which is fine for most cases but can cause delays on some OEMs
3. 24-hour period means only one check per day — might miss payment windows
4. Worker has NO dedup — same as Problem 1, all notifications re-fire on each execution

**Exact locations**:
- `ReminderWorker.kt:36-47` — `schedule()` with `EXISTING_PERIODIC_WORK_POLICY.UPDATE`
- `MainActivity.kt:306` — `onScheduleReminders = { ReminderWorker.schedule(application) }`
- `AndroidManifest.xml` — no `WorkManagerInitializer` or custom configuration

**Proposed approach**:
- Enhance `ReminderWorker` to also check the persisted sent-notification registry before dispatching
- Consider adding periodic re-scheduling logic on Application startup (not just login)
- Optionally reduce period to 6h for more frequent checking (with battery optimization awareness)

---

### Problem 3: Payment reminders after X days

**Root cause**: The logic EXISTS (`ReminderService.buildReminderMessages()`, line 33-49) and correctly checks `eventAge >= thresholdMillis`. But:
1. When the condition is met (e.g., unpaid debts exist AND threshold passed), the reminder is generated EVERY time — there's no "already reminded" tracking
2. The worker is 24h, so reminders could be up to 24h late
3. No distinction between "first reminder" and "recurring reminder"

**Exact locations**:
- `ReminderService.kt:33` — `thresholdMillis = reminderDays.coerceAtLeast(1) * MILLIS_PER_DAY`
- `ReminderService.kt:39-49` — generates pending-debt reminders when `eventAge >= thresholdMillis`
- `ReminderService.kt:51-59` — generates incomplete-event reminders unconditionally

**Proposed approach**:
- Add a `lastReminderSent` timestamp to `EventItem` (persisted in SQLDelight + SharedPreferences)
- OR store per-event notification timestamps in the sent-notification registry (Problem 1's solution)
- `ReminderService` should skip events that were reminded within the current threshold window
- Allow re-reminding after the threshold advances (e.g., remind at day 7, then again at day 7+3)

---

## Approaches

### Approach A — SharedPreferences sent-notification registry (Recommended)

Add a `Set<String>` to `CuentaMorososLocalStore` storing fingerprints of sent notifications. Each fingerprint: `"{type}:{eventId}:{slice}"` where slice is date-based (e.g., day-level for reminders, event-level for calculations).

- **Pros**: Minimal new infra, uses existing storage, shared module-compatible via interface, fast lookup, no model changes to EventItem/SQLDelight
- **Cons**: SharedPreferences has size limits for StringSet; need cleanup strategy for old entries; two notification systems (Scheduler + Dispatcher) need separate integration points
- **Effort**: Medium

### Approach B — Add `lastReminderSent` to EventItem model

Add a nullable `Long` field to `EventItem` tracking when the last reminder was sent. `ReminderService` checks this timestamp. For invitations and calculations, add similar fields.

- **Pros**: Clean data model, survives migrations, clear ownership
- **Cons**: Requires SQLDelight schema migration, SharedPreferences migration, all EventItem deserializers updated, doesn't cover non-event notifications (invitations from non-owned events), tightly couples notification concern to domain model
- **Effort**: High

### Approach C — Gate at NotifiationDispatcher/NotificationScheduler level only

Add a pre-dispatch check using a persistent registry, but don't change ViewModels. ViewModels still fire callbacks, but the dispatcher filters duplicates.

- **Pros**: Centralized change — only modify `NotificationDispatcher.dispatch()` and `NotificationScheduler.postReminders()`, minimal refactor
- **Cons**: ViewModels still trigger unnecessarily (wasteful), harder to unit-test the filter, mixed concerns (dispatcher shouldn't own dedup)
- **Effort**: Low (but fragile)

---

## Recommendation

**Approach A (SharedPreferences sent-notification registry)** with layered implementation:

1. **Persistence layer**: Add methods to `CuentaMorososLocalStore`:
   - `hasNotificationBeenSent(fingerprint: String): Boolean`
   - `recordNotificationSent(fingerprint: String)`
   - `clearSentNotifications()` (called on sign-out)
   - Store as `Set<String>` in SharedPreferences

2. **Dispatcher guard**: Modify `NotificationDispatcher.dispatch()` to check the registry before posting. Generate fingerprint from event type + eventId.

3. **Scheduler guard**: Modify `NotificationScheduler.postReminders()` to filter out messages whose fingerprint already exists in the registry.

4. **ReminderWorker**: Already uses `NotificationDispatcher` — will benefit from (2) automatically, BUT also needs fingerprint tracking for `ReminderMessage` objects that don't have eventId. Add composite fingerprint using `type + title hash`.

5. **ViewModel cleanup**: Remove in-memory `notifiedIds`/`notifiedCalculatedEventIds` sets — the persistent registry replaces them. ViewModels continue firing callbacks (stateless), and the dispatcher handles dedup.

### Tradeoff callout
The `ReminderService` currently generates messages for *incomplete events* UNCONDITIONALLY (line 51-59). This should either be changed to only report once (current behavior will spam every time) or track "last reported incomplete" per event.

---

## Risks

- **SharedPreferences StringSet corruption**: If the set grows too large (>1MB), performance degrades. Mitigate with periodic cleanup of entries older than `reminderDays * 2`.
- **Race conditions**: If `NotificationDispatcher` is called from multiple threads (FCM + MainActivity + Worker), simultaneous writes to `SharedPreferences.edit()` could lose entries. Mitigate by using `synchronized` or single-threaded dispatch.
- **Fingerprint collision**: Using `eventId.hashCode()` could collide. Use full string comparison instead.
- **Backward compatibility**: Existing users will have no sent-notification registry — all current pending events will fire on first app open after update. Mitigate by seeding the registry with all existing events on first run.
- **ReminderWorker reliability**: WorkManager on some OEMs (Xiaomi, Huawei) aggressively kills periodic workers. Consider adding a note to documentation or user-facing explanation.

---

## Ready for Proposal
Yes — root causes identified, proposed approach clear, files mapped. Ready for `sdd-propose`.
