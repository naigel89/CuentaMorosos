# Proposal: Notification System

## Intent

The app has partial notification infrastructure (FCM service, ReminderWorker, NotificationScheduler) but none of it works end-to-end. The FCM service is not declared in the manifest, `onMessageReceived` is an empty stub, the invitation callback is never wired, and ReminderWorker reads from stale SharedPreferences instead of SQLDelight. Users never receive push or local notifications for the 4 required scenarios.

## Scope

### In Scope
- Register FCM service in AndroidManifest.xml with correct intent-filter
- Request POST_NOTIFICATIONS runtime permission (Android 13+)
- Implement `onMessageReceived` to parse FCM data and dispatch local notifications
- Wire `InvitationsViewModel.onNewInvitation` callback through AppViewModelFactory
- Refactor ReminderWorker to read from SQLDelight repositories instead of SharedPreferences
- Add "upcoming event" logic to ReminderService (future events within N days)
- Add notification for "calculation completed" (triggered from DashboardViewModel)
- Add notification for "invitation accepted" (triggered from Firestore listener)
- Create 4 notification channels with appropriate priorities

### Out of Scope
- iOS push notifications (conditional target, deferred)
- Rich notifications with actions/buttons
- Notification preferences per-channel (global toggle only)
- Firebase Cloud Functions for server-side FCM sending

## Capabilities

### New Capabilities
- `push-notifications`: FCM service registration, runtime permission, onMessageReceived dispatch, notification channels
- `local-notification-triggers`: ViewModel-level notification triggers for invitation-received, invitation-accepted, calculation-completed, upcoming-event

### Modified Capabilities
None (no existing specs cover notifications)

## Approach

**Layered notification dispatcher** in `app` module (platform-specific), with **trigger points** in `shared` module (platform-agnostic callbacks).

1. **AndroidManifest**: Add `<service>` declaration for `CuentaMorososFirebaseMessagingService` with `com.google.firebase.MESSAGING_EVENT` intent-filter
2. **Permission**: Add `ActivityResultContracts.RequestPermission()` in MainActivity for `POST_NOTIFICATIONS`
3. **FCM dispatch**: Parse `remoteMessage.data["type"]` → route to `NotificationDispatcher` (new class) that builds channel-specific notifications
4. **ViewModel triggers**: Pass `onNewInvitation` lambda from MainActivity through AppViewModelFactory → InvitationsViewModel. Same pattern for calculation-completed (DashboardViewModel) and invitation-accepted (Firestore snapshot listener)
5. **ReminderWorker migration**: Inject `RepositoryProvider` instead of `CuentaMorososLocalStore`; query events from SQLDelight
6. **ReminderService extension**: Add `buildUpcomingEventMessages()` for events where `dateMillis` is within `reminderDays` in the future

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `app/src/main/AndroidManifest.xml` | Modified | Add FCM service declaration |
| `app/.../MainActivity.kt` | Modified | Add runtime permission request |
| `app/.../CuentaMorososFirebaseMessagingService.kt` | Modified | Implement onMessageReceived with dispatch |
| `app/.../NotificationScheduler.kt` | Modified | Add 4 channels + multi-type post methods |
| `app/.../ReminderWorker.kt` | Modified | Migrate from SharedPreferences to SQLDelight |
| `shared/.../ReminderService.kt` | Modified | Add upcoming event logic |
| `shared/.../InvitationsViewModel.kt` | Modified | No changes needed (callback already exists) |
| `shared/.../AppViewModelFactory.kt` | Modified | Wire onNewInvitation callback |
| `app/.../NotificationDispatcher.kt` | New | Central dispatcher for FCM + local triggers |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| FCM data messages silently dropped when app is backgrounded | Medium | Use both `notification` and `data` payload; test in both foreground/background |
| Runtime permission denied by user | High | Graceful degradation: local reminders still work via NotificationScheduler, FCM only needs permission for foreground display |
| ReminderWorker reads stale SQLDelight cache | Low | Worker runs after sync stagger completes; add freshness check |
| Multiple notification channels confuse users | Low | Clear channel names in Spanish; default importance levels match use case |

## Rollback Plan

Revert the change commit. The existing broken infrastructure (stub service, empty onMessageReceived) is non-functional, so rollback restores the same non-working state with no regression.

## Dependencies

- Firebase Messaging SDK already in `app/build.gradle.kts` (added in fix-critical-bugs)
- SQLDelight repositories already wired through RepositoryProvider
- WorkManager already configured for ReminderWorker

## Success Criteria

- [ ] FCM service receives messages (verified via Firebase Console test message)
- [ ] Runtime permission dialog shown on Android 13+ devices
- [ ] Invitation-received notification appears when Firestore invitation is created
- [ ] Invitation-accepted notification reaches the inviter
- [ ] Calculation-completed notification shows amount owed
- [ ] Upcoming-event reminder fires for events within configured days
- [ ] All 4 notification types have distinct channels in system settings
- [ ] Existing unit tests pass; new tests for NotificationDispatcher and ReminderService extension
