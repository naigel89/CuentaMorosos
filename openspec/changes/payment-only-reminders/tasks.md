# Tasks: Payment-Only Reminders

## Review Workload Forecast

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: feature-branch-chain
400-line budget risk: Medium

| Field | Value |
|-------|-------|
| Estimated changed lines | 400–550 (12 files: 8 mod, 4 del, 2 new test files) |
| Suggested split | PR1: Model+Dispatch (6 files) → PR2: Worker+UI (5 files) → PR3: Deletions (3 files) |

### Suggested Work Units

| # | Goal | Base |
|---|------|------|
| 1 | ReminderService + NotificationEvent + NotificationDispatcher + new tests | `feature/payment-only-reminders` |
| 2 | ReminderWorker + UI cleanup (SettingsScreen, ScreenComponents, CuentaMorososApp, MainActivity) + test updates | PR 1 branch |
| 3 | Deletions: NotificationScheduler.kt + 2 old test files | PR 2 branch |

## Phase 1: Model Foundation (TDD)

- [x] 1.1 RED: Test per-debt ReminderMessage generation (both directions: "te debe" / "debés a", empty debts, remindersDisabled) — `shared/src/commonTest/kotlin/com/cuentamorosos/data/ReminderServiceTest.kt` (new file)
- [x] 1.2 RED: Test PaymentReminder construction + isOwedToYou direction — `shared/src/commonTest/kotlin/com/cuentamorosos/notifications/NotificationEventTest.kt` (new file)
- [x] 1.3 GREEN: Refactor ReminderMessage (add `profileName: String?`, `amountEuros: Double?`, `isOwedToYou: Boolean`; drop `daysUntil`, `dateFormatted`); ReminderType→`PENDING_DEBT` only — `shared/src/commonMain/kotlin/com/cuentamorosos/data/ReminderService.kt`
- [x] 1.4 GREEN: Add `profiles`+`currentUserUid`+`expenses` params to `buildReminderMessages()`; per-debt iteration with direction detection; extract `resolveEventCreditor()` as internal utility; remove `buildUpcomingEventMessages()` — `shared/src/commonMain/kotlin/com/cuentamorosos/data/ReminderService.kt`
- [x] 1.5 GREEN: Add `PaymentReminder(eventId, profileName, amountEuros, isOwedToYou)` to `NotificationEvent` sealed class; remove `UpcomingEvent` — `shared/src/commonMain/kotlin/com/cuentamorosos/notifications/NotificationEvent.kt`
- [x] 1.6 REFACTOR: Delete `shared/src/commonTest/kotlin/com/cuentamorosos/data/ReminderServiceUpcomingTest.kt`; verify zero `INCOMPLETE_EVENT`/`UPCOMING_EVENT` references remain project-wide

## Phase 2: Notification Dispatch (TDD)

- [x] 2.1 RED: Test PaymentReminder fingerprint, channelIdFor→CH_REMINDERS, smallIconResFor→ic_notification_calc, titleFor/bodyFor with personalized message, dedup suppression — `app/src/test/java/com/cuentamorosos/notifications/NotificationDispatcherTest.kt`
- [x] 2.2 GREEN: Add `PaymentReminder` branches to `fingerprintFor`, `notificationType`, `channelIdFor`→`CH_REMINDERS`, `smallIconResFor`→`ic_notification_calc`, `titleFor`→"Recordatorio de pago", `bodyFor`→from `ReminderMessage.body`, `notificationTag`, `notificationId`, `addActions` — `app/src/main/java/com/cuentamorosos/notifications/NotificationDispatcher.kt`
- [x] 2.3 GREEN: Replace `UPCOMING_EVENT` with `PAYMENT_REMINDER` in internal `NotificationType` enum; remove `ch_upcoming_events` creation from `ensureChannels()`; add `getAppLogoBitmap()` loading `R.mipmap.ic_launcher` — `app/src/main/java/com/cuentamorosos/notifications/NotificationDispatcher.kt`

## Phase 3: Worker Integration (TDD)

- [x] 3.1 RED: Update assertions — `PaymentReminder` dispatch (not `UpcomingEvent`); respects `remindersEnabled` — `app/src/test/java/com/cuentamorosos/data/ReminderWorkerDedupTest.kt`
- [x] 3.2 GREEN: Fetch profiles via `profileRepository.observeProfiles().first()`; get `currentUserUid` from `FirebaseAuth.getInstance().uid`; pass profiles+currentUserUid+expenses to `buildReminderMessages()` — `app/src/main/java/com/cuentamorosos/data/ReminderWorker.kt`
- [x] 3.3 GREEN: Build `PaymentReminder(eventId, profileName, amountEuros, isOwedToYou)` per message; dispatch via `dispatcher.dispatch()`; remove `buildUpcomingEventMessages()` call and `UpcomingEvent` dispatch — `app/src/main/java/com/cuentamorosos/data/ReminderWorker.kt`

## Phase 4: UI Cleanup

- [x] 4.1 Remove `onPostReminders` param + "Enviar ahora" `OutlinedButton` (L249–260) — `shared/src/commonMain/kotlin/com/cuentamorosos/ui/SettingsScreen.kt`
- [x] 4.2 Remove "Ocultar" `TextButton` from `ReminderSummaryCard` (L152–154) — `shared/src/commonMain/kotlin/com/cuentamorosos/ui/ScreenComponents.kt`
- [x] 4.3 Remove `onPostReminders` param + forwarding; update `reminderMessages` derivation (pass `profiles`+`currentUserUid`) — `app/src/main/java/com/cuentamorosos/CuentaMorososApp.kt`
- [x] 4.4 Remove `NotificationScheduler` import, `ensureChannel(CH_UPCOMING_EVENTS)` call, `onPostReminders` lambda — `app/src/main/java/com/cuentamorosos/MainActivity.kt`

## Phase 5: Deletions

- [x] 5.1 Delete `app/src/main/java/com/cuentamorosos/data/NotificationScheduler.kt`
- [x] 5.2 Delete `app/src/test/java/com/cuentamorosos/data/NotificationSchedulerDedupTest.kt`
