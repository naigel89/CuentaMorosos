# Tasks: fix-notification-dedup

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 490–540 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 → PR 2 → PR 3 |
| Delivery strategy | ask-on-risk |
| Chain strategy | feature-branch-chain |

Decision needed before apply: Resolved
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain (Slice 1 → feature/notification-system-slice1-dedup targeting feature/notification-system)
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Persistent registry + dispatcher guard | PR 1 | base=main; ~220 lines; standalone deliverable |
| 2 | Scheduler guard + worker + wiring | PR 2 | base=PR 1; ~180 lines; depends on PR 1 |
| 3 | ViewModel cleanup + migration + tests | PR 3 | base=PR 2; ~140 lines; depends on PR 1 |

---

## Phase 1: Persistent Registry (Group 1)

- [x] **T-01** Write `CuentaMorososLocalStoreDedupTest.kt` — tests for `hasNotificationBeenSent()`, `recordNotificationSent()`, null/blank safety, persistence across restarts, thread safety, cleanup (old pruned, recent kept), migration seed (only CALCULATED events). **RED**
  - **Files**: `app/src/test/…/data/CuentaMorososLocalStoreDedupTest.kt` (new)
  - **Deps**: none
  - **Est. lines**: +80
- [x] **T-02** Add dedup registry to `CuentaMorososLocalStore.kt` — `KEY_SENT_FINGERPRINTS`, `hasNotificationBeenSent()`, `recordNotificationSent()`, `cleanupOldEntries(maxAgeDays=30)`, `seedDedupMigration(events)`. Storage: `"{epochMs}|{fingerprint}"` in `StringSet`. Guard: `synchronized(prefs)` block. Null/blank fingerpint → no-op. **GREEN**
  - **Files**: `app/src/main/…/data/CuentaMorososLocalStore.kt`
  - **Deps**: T-01
  - **Est. lines**: +55
- [x] **T-03** Run `CuentaMorososLocalStoreDedupTest` → all pass. **REFACTOR** if needed.
  - **Files**: `app/src/test/…/data/CuentaMorososLocalStoreDedupTest.kt`
  - **Deps**: T-02
  - **Est. lines**: +10

## Phase 2: Dispatcher Guard (Group 2)

- [x] **T-04** Extend `NotificationDispatcherTest.kt` — test `fingerprintFor()` deterministic output per event type; test dispatch skips when fingerprint registered; test dispatch records fingerprint after posting; test notifications-disabled still returns early before dedup check. **RED**
  - **Files**: `app/src/test/…/notifications/NotificationDispatcherTest.kt`
  - **Deps**: T-02
  - **Est. lines**: +55
- [x] **T-05** Add dedup guard to `NotificationDispatcher` — accept `CuentaMorososLocalStore` param (default null for bwd compat); add `fingerprintFor(event)` companion method per design table; in `dispatch()`: after enabled check, compute fingerprint, check registry, return if sent, record after notify. **GREEN**
  - **Files**: `app/src/main/…/notifications/NotificationDispatcher.kt`
  - **Deps**: T-04
  - **Est. lines**: +30
- [x] **T-06** Verify existing `NotificationDispatcherTest` tests still pass + new dedup tests pass.
  - **Files**: `app/src/test/…/notifications/NotificationDispatcherTest.kt`
  - **Deps**: T-05
  - **Est. lines**: +5

## Phase 3: Scheduler Guard (Group 3)

- [x] **T-07** Write `NotificationSchedulerDedupTest.kt` — test filtering: 3 reminders, 1 already sent → 2 posted; test all sent → 0 posted; test record after post; fingerprint format `"reminder:{eventId}:{dateFormatted}:{type}"` (adapted from spec — `ReminderMessage` has no `profileId`). **RED**
  - **Files**: `app/src/test/…/data/NotificationSchedulerDedupTest.kt` (new)
  - **Deps**: T-02
  - **Est. lines**: +65
- [x] **T-08** Add dedup guard to `NotificationScheduler.postReminders()` — accept `CuentaMorososLocalStore` (nullable for bwd compat); `fingerprintFor()` companion; filter via `hasNotificationBeenSent()` per message; record after each successful post. **GREEN**
  - **Files**: `app/src/main/…/data/NotificationScheduler.kt`
  - **Deps**: T-07
  - **Est. lines**: +33
- [x] **T-09** Run `NotificationSchedulerDedupTest` → all pass (13/13). Full suite: 51/51 new tests pass.
  - **Files**: `app/src/test/…/data/NotificationSchedulerDedupTest.kt`
  - **Deps**: T-08
  - **Est. lines**: +5

## Phase 4: Worker + Wiring (Groups 4+6)

- [x] **T-10** Write `ReminderWorkerDedupTest.kt` — test idempotency via dispatcher dedup: skip already-sent UpcomingEvent; persistence across dispatcher instances; fingerprint format stability. **RED** (note: true RED not possible without full worker infrastructure; tests validate the integration point: dispatcher+store dedup covers worker idempotency)
  - **Files**: `app/src/test/…/data/ReminderWorkerDedupTest.kt` (new)
  - **Deps**: T-05, T-08
  - **Est. lines**: +55
- [x] **T-11** Wire `localStore` through dispatch paths — `ReminderWorker`: pass store to `NotificationDispatcher` + call `seedDedupMigration()`; `CuentaMorososFirebaseMessagingService`: pass store to dispatcher; `MainActivity`: pass store to `NotificationDispatcher()` and `NotificationScheduler.postReminders()`; `CuentaMorososApp.onCreate()`: call `cleanupOldEntries()`. **GREEN**
  - **Files**: `ReminderWorker.kt`, `CuentaMorososFirebaseMessagingService.kt`, `MainActivity.kt`, `CuentaMorososApp.kt`
  - **Deps**: T-05, T-08
  - **Est. lines**: +45
- [x] **T-12** Run `ReminderWorkerDedupTest` → all pass (9/9).
  - **Files**: `app/src/test/…/data/ReminderWorkerDedupTest.kt`
  - **Deps**: T-11
  - **Est. lines**: +5

## Phase 5: ViewModel Cleanup (Group 5)

- [ ] **T-13** Remove in-memory dedup from ViewModels — `DashboardViewModel`: delete `notifiedCalculatedEventIds` set, remove guard condition (callback fires unconditionally, dispatcher handles dedup); `InvitationsViewModel`: delete `notifiedIds` set, remove guard condition. **GREEN**
  - **Files**: `shared/…/ui/DashboardViewModel.kt`, `shared/…/ui/InvitationsViewModel.kt`
  - **Deps**: T-05
  - **Est. lines**: -30
- [ ] **T-14** Update ViewModel notification tests — Dashboard: change "does NOT fire twice" → callback fires on every emission (dispatcher dedups); Invitations: change "fires only once" → fires per emission. Verify null-callback safety, non-CALCULATED/PENDING skip unchanged. **GREEN**
  - **Files**: `shared/src/commonTest/…/ui/DashboardViewModelNotificationTest.kt`, `shared/src/commonTest/…/ui/InvitationsViewModelNotificationTest.kt`
  - **Deps**: T-13
  - **Est. lines**: +20
- [ ] **T-15** Run `./gradlew test` — all existing + new tests pass. Final regression check.
  - **Deps**: T-14
  - **Est. lines**: 0
