# Proposal: Persistent Notification Deduplication

## Intent

Notification deduplication is currently **in-memory only** — `NotificationDispatcher.dispatch()` and `NotificationScheduler.postReminders()` fire every message unconditionally with no persistent "already sent" check. The `ReminderWorker` (24h WorkManager) regenerates identical reminder messages each cycle. Result: users get duplicate notifications every app launch and every background worker run. This change adds a **persistent sent-notification registry** via `SharedPreferences` to prevent re-delivery.

## Scope

### In Scope
- Add `Set<String>` `sentNotificationFingerprints` to `CuentaMorososLocalStore` (persisted via `SharedPreferences`)
- Add `hasNotificationBeenSent(fingerprint: String): Boolean` and `recordNotificationSent(fingerprint: String)` methods
- Guard `NotificationDispatcher.dispatch()` to skip already-sent notifications
- Guard `NotificationScheduler.postReminders()` to skip already-sent reminders
- Guard `ReminderWorker.doWork()` to skip events whose fingerprint was already dispatched
- Remove any in-memory dedup sets from ViewModels (once they exist)

### Out of Scope
- FCM push notification deduplication (server-side concern)
- Notification UI or channel redesign
- Reminder timing logic changes
- Cross-device sync of sent-notification state

## Capabilities

### New Capabilities
- `notification-dedup`: Persistent fingerprint-based deduplication for all local notification types (invitations, calculations, reminders, upcoming events)

### Modified Capabilities
None — no existing spec covers notification behavior.

## Approach

1. Add `KEY_SENT_FINGERPRINTS` to `CuentaMorososLocalStore` companion, storing a `Set<String>` in `SharedPreferences`
2. Add `hasNotificationBeenSent(fingerprint)` and `recordNotificationSent(fingerprint)` — read/write the set
3. Fingerprint format: `"{tag}|{eventId}|{content_hash}"` — unique per notification instance
4. Inject dedup guard as first check in `NotificationDispatcher.dispatch()` — return early if `hasNotificationBeenSent(fingerprint)` is true
5. Add same guard at top of `NotificationScheduler.postReminders()` loop — skip individual messages already sent
6. In `ReminderWorker.doWork()`, filter messages through `hasNotificationBeenSent()` before dispatching
7. Add periodic cleanup: on app start, prune fingerprints older than 30 days (store `fingerprint|timestamp` format or separate `Set<String>` with epoch prefix)

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `data/CuentaMorososLocalStore.kt` | Modified | Add fingerprint registry + accessors |
| `notifications/NotificationDispatcher.kt` | Modified | Add dedup guard in `dispatch()` |
| `data/NotificationScheduler.kt` | Modified | Add dedup guard in `postReminders()` |
| `data/ReminderWorker.kt` | Modified | Filter messages before dispatch |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| SharedPreferences `StringSet` grows unbounded | Low | Periodic cleanup on app start (prune >30 days) |
| Race condition (FCM + Worker + UI concurrent writes) | Low | `synchronized` block on `prefs.edit()` for fingerprint writes |
| First launch after update triggers all existing events | Med | Seed registry with fingerprints of currently-visible events on first migration |
| Fingerprint collision (different events hash to same) | Low | Fingerprint includes eventId + type tag + content hash, making collision near-impossible |

## Rollback Plan

1. Revert to in-memory dedup by removing `hasNotificationBeenSent()` calls and restoring volatile `mutableSetOf` per ViewModel
2. Delete `KEY_SENT_FINGERPRINTS` from SharedPreferences
3. No data migration needed — the registry is additive-only

## Dependencies

None — isolated change to notification local layer (`data/` + `notifications/`).

## Success Criteria

- [ ] `NotificationDispatcher.dispatch()` skips notification when fingerprint exists in registry
- [ ] `NotificationScheduler.postReminders()` skips individual messages already sent
- [ ] `ReminderWorker.doWork()` does not re-dispatch reminders from previous cycles
- [ ] Registry is persisted across app restarts (survives process death)
- [ ] Cleanup prunes entries older than 30 days without deleting recent ones
- [ ] No duplicate notifications in manual testing (launch app → close → relaunch)
