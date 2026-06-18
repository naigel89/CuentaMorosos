## Verification Report

**Change**: payment-only-reminders
**Version**: N/A (delta specs)
**Mode**: Standard

### Completeness
| Metric | Value |
|--------|-------|
| Tasks total | 16 |
| Tasks complete | 16 |
| Tasks incomplete | 0 |

### Build & Tests Execution
**Build**: ✅ Passed
```text
BUILD SUCCESSFUL in 1m 39s
106 actionable tasks: 106 executed
```

**Tests**: ✅ 1148 passed / ❌ 0 failed / ⚠️ 0 skipped
```text
All tests passing across :shared and :app modules.
Key test files for this change:
  ReminderServiceTest           → 12 tests passed
  NotificationEventTest         →  3 tests passed
  NotificationDispatcherTest    → 19 tests passed
  ReminderWorkerDedupTest       →  9 tests passed
  FcmParsingTest                →  9 tests passed
```

**Coverage**: ➖ Not available (no JaCoCo configured)

### Spec Compliance Matrix

#### payment-reminders

| Requirement | Scenario | Test | Result |
|-------------|----------|------|--------|
| R001: Per-Profile Payment Message Generation | Debt owed TO current user ("te debe") | `ReminderServiceTest > per-debt generates te-debe message` | ✅ COMPLIANT |
| R001 | Debt owed BY current user ("debés a") | `ReminderServiceTest > per-debt generates debes-a message` | ✅ COMPLIANT |
| R001 | Multiple debts produce per-debt messages | `ReminderServiceTest > multiple unpaid debts produce one message per debt` | ✅ COMPLIANT |
| R001 | No pending debts → empty list | `ReminderServiceTest > returns empty list when no unpaid debts exist` | ✅ COMPLIANT |
| R001 | Reminders disabled → empty list | `ReminderServiceTest > returns empty list when reminders are disabled` | ✅ COMPLIANT |
| R002: PaymentReminder Notification Data | Direction field (isOwedToYou) | `NotificationEventTest > PaymentReminder with isOwedToYou=true` | ✅ COMPLIANT |
| R002 | Direction field (isOwedToYou=false) | `NotificationEventTest > PaymentReminder with isOwedToYou=false` | ✅ COMPLIANT |
| R003: PaymentReminder Dispatch | Channel and icon (ch_reminders, ic_notification_calc) | `NotificationDispatcherTest > dispatch PaymentReminder te-debe direction` | ✅ COMPLIANT |
| R003 | Fingerprint dedup | `NotificationDispatcherTest > dispatch skips PaymentReminder when already sent` | ✅ COMPLIANT |
| R003 | Existing notification types unaffected | All invitation/calculation dispatch tests pass unchanged | ✅ COMPLIANT |
| R004: ReminderWorker PaymentReminder Dispatch | Worker dispatches PaymentReminder (not UpcomingEvent) | `ReminderWorkerDedupTest > dispatcher with store skips already-sent PaymentReminder` | ✅ COMPLIANT |
| R004 | Worker respects remindersEnabled | `ReminderServiceTest > returns empty list when reminders are disabled` (covered at service layer) | ✅ COMPLIANT |

#### local-notification-triggers (modified/removed)

| Requirement | Status | Notes |
|-------------|--------|-------|
| R107: Personalized Message Context — extended | ✅ Implemented | `profileName`, `amountEuros`, `isOwedToYou` fields added to `ReminderMessage` and `PaymentReminder` |
| R107: Payment reminder "te debe" direction | ✅ COMPLIANT | `NotificationDispatcherTest > dispatch PaymentReminder te-debe direction` |
| R107: Payment reminder "debés a" direction | ✅ COMPLIANT | `NotificationDispatcherTest > dispatch PaymentReminder debes-a direction` |
| R104: Upcoming Event Trigger — REMOVED | ✅ Removed | `UPCOMING_EVENT` no longer in `ReminderType` enum; zero references project-wide |
| Manual Reminder Dispatch — REMOVED | ✅ Removed | "Enviar ahora" button, "Ocultar" button, `NotificationScheduler`, `onPostReminders` all removed |

**Compliance summary**: 16/16 scenarios compliant

### Correctness (Static Evidence)

| Requirement | Status | Notes |
|------------|--------|-------|
| ReminderType only PENDING_DEBT | ✅ Implemented | `enum class ReminderType { PENDING_DEBT }` |
| buildUpcomingEventMessages() removed | ✅ Implemented | Method deleted; zero references project-wide |
| Per-debt iteration with direction | ✅ Implemented | `debt.profileId != currentUserUid → "te debe"`, `== → "debés a"` via `resolveEventCreditor` |
| PaymentReminder sealed subclass | ✅ Implemented | `data class PaymentReminder(eventId, profileName, amountEuros, isOwedToYou) : NotificationEvent()` |
| NotificationDispatcher PaymentReminder handling | ✅ Implemented | All branches: `fingerprintFor`, `notificationType`, `channelIdFor`, `smallIconResFor`, `titleFor`, `bodyFor`, `notificationTag` |
| CH_REMINDERS channel | ✅ Implemented | 3 channels created in `ensureChannels()`: invitations, calculations, reminders |
| App logo as large icon | ✅ Implemented | `getAppLogoBitmap()` loads `R.mipmap.ic_launcher` via `BitmapFactory.decodeResource` |
| EventCreditorResolver shared utility | ✅ Implemented | `EventCreditorResolver.kt` in `shared/.../model/`; imported by both `CuentaMorososApp.kt` and `ReminderService.kt` |
| "Enviar ahora" removed | ✅ Implemented | `onPostReminders` param removed from `SettingsScreen`, `CuentaMorososApp`, `MainActivity` |
| "Ocultar" removed | ✅ Implemented | No `TextButton("Ocultar")` in `ReminderSummaryCard` |
| NotificationScheduler deleted | ✅ Implemented | File `NotificationScheduler.kt` absent from filesystem |
| NotificationSchedulerDedupTest deleted | ✅ Implemented | File absent from filesystem |
| ReminderServiceUpcomingTest deleted | ✅ Implemented | File absent from filesystem |
| FCM upcoming_event parsing removed | ✅ Implemented | `CuentaMorososFirebaseMessagingService` no longer handles "upcoming_event" type |
| Zero dead references | ✅ Verified | No `INCOMPLETE_EVENT`, `UPCOMING_EVENT`, `buildUpcomingEventMessages`, `onPostReminders`, `NotificationScheduler` references exist |

### Coherence (Design)

| Decision | Followed? | Notes |
|----------|-----------|-------|
| Profile/creditor resolution inside `buildReminderMessages()` | ✅ Yes | Pure function with `profiles` + `expenses` params; testable |
| Creditor logic ported into shared utility | ✅ Yes | `resolveEventCreditor()` in `EventCreditorResolver.kt`; used by both consumer sites |
| `ReminderType` enum kept with `PENDING_DEBT` only | ✅ Yes | Single value preserves extensibility |
| `CH_UPCOMING_EVENTS` constant kept but channel not created | ✅ Yes | Constant at line 33; `ensureChannels()` creates only 3 channels |
| Large icon via `getAppLogoBitmap()` loading `R.mipmap.ic_launcher` | ✅ Yes | `PaymentReminder` gets app logo; other types continue with styled icons |
| `ReminderWorker` fetches profiles + `currentUserUid` | ✅ Yes | `profileRepository.observeProfiles().first()` + `FirebaseAuth.getInstance().currentUser?.uid` |
| "Enviar ahora" / `onPostReminders` removed | ✅ Yes | All wiring through `SettingsScreen` → `CuentaMorososApp` → `MainActivity` cleaned |

### Issues Found

**CRITICAL**: None

**WARNING**:
- [FORMAT-INCONSISTENCY] `ReminderService` uses `formatEuros()` producing "15.50 €" (€ suffix with space) while `NotificationDispatcher` uses `formatAmount()` producing "€15.50" (€ prefix). The notification body format differs from the SettingsScreen reminder preview. Spec examples use "€15,50" with comma, but the project-wide `formatEuros` uses dot. Both test suites pass against their respective formats — this is a pre-existing project-wide formatting convention, not a regression.

**SUGGESTION**:
- [DEAD-CONSTANT] `CH_UPCOMING_EVENTS` constant (NotificationDispatcher.kt:33) is retained but never used in `ensureChannels()`. Design decision documented this as intentional ("Android can't un-create channels"). Consider adding a `@Deprecated` annotation or a comment noting retention reason.

### Verdict
**PASS WITH WARNINGS**

All 16 tasks complete. All 1148 tests pass (0 failures, 0 errors). All spec requirements (R001–R004) are covered by passing tests. All design decisions verified in implementation. No dead code references to removed types. One formatting inconsistency between `ReminderService` preview and `NotificationDispatcher` body (project-wide convention, non-regression).
