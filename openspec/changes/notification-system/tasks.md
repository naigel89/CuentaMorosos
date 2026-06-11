# Tasks: Notification System

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~1800–2200 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 (Foundation + Dispatcher) → PR 2 (Triggers + Worker + FCM) → PR 3 (Wiring + Tests) |
| Delivery strategy | ask-on-risk |
| Chain strategy | stacked-to-main |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Foundation: icons, NotificationEvent, NotificationDispatcher | PR 1 | Standalone; compiles and posts notifications |
| 2 | Triggers + FCM: ViewModel callbacks, ReminderService/Worker migration, FCM service | PR 2 | Depends on PR 1; all trigger paths wired |
| 3 | Wiring + Tests: MainActivity deep links, permission, full test suite | PR 3 | Depends on PR 2; end-to-end verifiable |

## Phase 1: Foundation

- [x] 1.1 Create 4 icon XMLs in `app/src/main/res/drawable/` — `ic_notification_invitation.xml` (person+badge), `ic_notification_check.xml` (check in circle), `ic_notification_calc.xml` (coins+€), `ic_notification_calendar.xml` (calendar+alert). All: 24dp canvas, stroke 1.5dp, white.
- [x] 1.2 Create `shared/src/commonMain/kotlin/com/cuentamorosos/notifications/NotificationEvent.kt` — sealed class with `InvitationReceived`, `InvitationAccepted`, `CalculationCompleted`, `UpcomingEvent` data classes per design.md §Interfaces.
- [x] 1.3 Create `shared/src/commonMain/kotlin/com/cuentamorosos/NotificationCallbacks.kt` — data class with 3 optional lambdas: `onInvitationReceived`, `onInvitationAccepted`, `onCalculationCompleted`.

## Phase 2: NotificationDispatcher

- [x] 2.1 Create `app/src/main/java/com/cuentamorosos/notifications/NotificationDispatcher.kt` — constructor(Context), `dispatch(event)`, `ensureChannels()` (4 channels: ch_invitations HIGH, ch_calculations/ch_reminders/ch_upcoming_events DEFAULT), `notificationType()`, `channelIdFor()`, `smallIconResFor()`, `notificationTag()`, `notificationId()`.
- [x] 2.2 Implement `titleFor()`/`bodyFor()` — Spanish titles ("Invitación recibida", etc.), bodies with format strings, `String.format("%.2f")` for amounts.
- [x] 2.3 Implement `createLargeIcon()` — 48dp Bitmap, colored circle (NeoFintech colors per type), white 24dp icon centered. Cache via `ConcurrentHashMap<NotificationType, Bitmap>`.
- [x] 2.4 Implement `addActions()` — Invitation: "Aceptar"/"Rechazar" via `PendingIntent.getBroadcast()` with `ACTION_ACCEPT_INVITATION`/`ACTION_REJECT_INVITATION`. Others: "Ver detalles" via content intent. Flags: `FLAG_IMMUTABLE | FLAG_UPDATE_CURRENT`.
- [x] 2.5 Implement `createContentIntent()` — Intent to MainActivity with extras (`EXTRA_NOTIFICATION_TYPE`, `EXTRA_EVENT_ID`, `EXTRA_INVITATION_ID`, `EXTRA_PAGER_PAGE`). Mapping: INVITATION_RECEIVED→page 3, others→page 0.

## Phase 3: Application + Manifest

- [x] 3.1 Create `app/src/main/java/com/cuentamorosos/CuentaMorososApp.kt` — Application subclass with `var repositoryProvider: RepositoryProvider?`, `@Volatile` singleton, `getInstance()`.
- [x] 3.2 Modify `app/src/main/AndroidManifest.xml` — add `android:name=".CuentaMorososApp"` to `<application>`, add `<service>` for `CuentaMorososFirebaseMessagingService` with `com.google.firebase.MESSAGING_EVENT` intent-filter.

## Phase 4: ViewModel Triggers

- [ ] 4.1 Modify `shared/src/commonMain/kotlin/com/cuentamorosos/ui/InvitationsViewModel.kt` — change `onNewInvitation` param to `((NotificationEvent.InvitationReceived) -> Unit)?`, add `notifiedIds: MutableSet<String>`, fire callback in `onEach` for PENDING invitations not yet notified.
- [ ] 4.2 Modify `shared/src/commonMain/kotlin/com/cuentamorosos/ui/DashboardViewModel.kt` — add `onCalculationCompleted` param, `notifiedCalculatedEventIds` set, detect `EventState.CALCULATED` in `computeState()`, compute `amountOwed` from unpaid debts where `profileId == currentUserUid`, fire when > 0.
- [ ] 4.3 Modify `shared/src/androidMain/kotlin/com/cuentamorosos/AppViewModelFactory.kt` — add `notificationCallbacks: NotificationCallbacks` param, pass `onInvitationReceived` to InvitationsViewModel, `onCalculationCompleted` to DashboardViewModel.

## Phase 5: ReminderService + Worker

- [ ] 5.1 Modify `shared/src/commonMain/kotlin/com/cuentamorosos/data/ReminderService.kt` — add `buildUpcomingEventMessages(events, reminderDays, nowMillis)` filtering `startDateMillis in nowMillis..windowEnd`, mapping to `ReminderMessage(type=UPCOMING_EVENT)`. Extend `ReminderMessage` with `eventId`, `daysUntil`, `dateFormatted` fields. Add `ReminderType.UPCOMING_EVENT`.
- [ ] 5.2 Modify `app/src/main/java/com/cuentamorosos/data/ReminderWorker.kt` — replace `CuentaMorososLocalStore` with `CuentaMorososApp.getInstance().repositoryProvider`, use `first()` on observe Flows, create `NotificationDispatcher` and dispatch each message. Return `Result.retry()` if provider unavailable.

## Phase 6: FCM Service

- [ ] 6.1 Create `app/src/main/java/com/cuentamorosos/data/CuentaMorososFirebaseMessagingService.kt` — `onMessageReceived()`: parse `data["type"]` → 4 parse methods (return null on missing fields), create dispatcher, dispatch. `onNewToken()`: save via FirebaseAuth uid, try/catch.

## Phase 7: MainActivity Wiring

- [ ] 7.1 Modify `app/src/main/java/com/cuentamorosos/MainActivity.kt` — create `NotificationDispatcher`, request `POST_NOTIFICATIONS` (API 33+), store `repositoryProvider` in `CuentaMorososApp`. Add `MutableSharedFlow<DeepLinkTarget>`, `handleDeepLinkIntent()` extracting extras, `onNewIntent()` override.
- [ ] 7.2 Modify `MainAppContent` — create `NotificationCallbacks` with lambdas calling `dispatcher.dispatch()`, pass to `AppViewModelFactory`. Pass `deepLinkEvent` to `CuentaMorososApp`.
- [ ] 7.3 Modify `CuentaMorososApp` composable — accept `deepLinkEvent: SharedFlow<DeepLinkTarget>?`, add `LaunchedEffect` collecting events → `pagerState.animateScrollToPage()` + `eventDetailViewModel.setEventId()`.

## Phase 8: Testing

- [ ] 8.1 Create `app/src/test/.../notifications/NotificationDispatcherTest.kt` — Robolectric: channels created (4), correct channel per type, title/body strings, large icon bitmap, notificationId deterministic, disabled state skips.
- [ ] 8.2 Create `shared/src/commonTest/.../InvitationsViewModelNotificationTest.kt` — callback fires once per invitation (dedup), no fire for non-PENDING, no crash when callback null.
- [ ] 8.3 Create `shared/src/commonTest/.../DashboardViewModelNotificationTest.kt` — fires when CALCULATED + amountOwed > 0, no fire when amountOwed = 0, no duplicate for same event.
- [ ] 8.4 Create `shared/src/commonTest/.../ReminderServiceUpcomingTest.kt` — includes events in window, excludes past, excludes beyond window.
- [ ] 8.5 Create `app/src/test/.../data/FcmParsingTest.kt` — valid parsing for all 4 types, null on missing fields, unknown type ignored.
- [ ] 8.6 Create `app/src/test/.../data/FcmIntegrationTest.kt` — end-to-end: RemoteMessage → onMessageReceived → notification posted in NotificationManager.

## Phase 9: Manual Verification

- [ ] 9.1 Execute manual checklist — POST_NOTIFICATIONS dialog on Android 13+, all 4 notification types with correct channels/titles/bodies, deep link navigation (foreground + background), icon colors match NeoFintech spec, Worker fires every 24h.
