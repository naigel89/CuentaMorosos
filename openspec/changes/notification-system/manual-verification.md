# Manual Verification Checklist

## Pre-requisites
- [ ] Build successful: `./gradlew :app:assembleDebug`
- [ ] Install APK on device/emulator (Android 13+ recommended)
- [ ] User logged in with valid Firebase account

## Phase 1: Permission Request
- [ ] On first launch after install, POST_NOTIFICATIONS permission dialog appears (Android 13+)
- [ ] Granting permission allows notifications to be shown
- [ ] Denying permission does not crash the app
- [ ] Check: Settings → Apps → CuentaMorosos → Notifications shows 4 channels

## Phase 2: Notification Channels
- [ ] Settings → Apps → CuentaMorosos → Notifications displays:
  - [ ] "Invitaciones" channel (HIGH importance)
  - [ ] "Cálculos y liquidaciones" channel (DEFAULT importance)
  - [ ] "Recordatorios" channel (DEFAULT importance)
  - [ ] "Próximos eventos" channel (DEFAULT importance)

## Phase 3: Invitation Received (Foreground)
- [ ] Have another user invite current user to an event
- [ ] While app is in foreground, notification appears immediately
- [ ] Notification shows:
  - [ ] Title: "Invitación recibida"
  - [ ] Body: "[InviterName] te invitó al evento '[EventName]'"
  - [ ] Icon: Green circle with person+badge icon
  - [ ] Actions: "Aceptar" and "Rechazar" buttons
- [ ] Tapping "Aceptar" accepts invitation and navigates to event detail
- [ ] Tapping "Rechazar" rejects invitation
- [ ] Tapping notification body navigates to Invitations tab

## Phase 4: Invitation Received (Background)
- [ ] Close app (swipe from recent apps)
- [ ] Have another user invite current user
- [ ] Notification appears in system tray
- [ ] Tapping notification opens app and navigates to Invitations tab
- [ ] Actions "Aceptar"/"Rechazar" work from background notification

## Phase 5: Calculation Completed
- [ ] Join an event as contributor
- [ ] Owner adds expenses and calculates totals
- [ ] If current user owes money (amountOwed > 0):
  - [ ] Notification appears with title: "Cálculos completados"
  - [ ] Body: "Se calcularon los gastos de '[EventName]'. Debes €[Amount]"
  - [ ] Icon: Orange circle with calculator/coins icon
  - [ ] Action: "Ver detalles" button
- [ ] If current user owes nothing (amountOwed = 0):
  - [ ] No notification appears
- [ ] Tapping "Ver detalles" navigates to event detail screen

## Phase 6: Upcoming Event (ReminderWorker)
- [ ] Create an event with startDateMillis within next 3 days (default reminderDays)
- [ ] Wait for ReminderWorker to execute (or trigger manually via WorkManager inspector)
- [ ] Notification appears with:
  - [ ] Title: "Próximo evento: [EventName]"
  - [ ] Body: "El evento '[EventName]' es en [X] días ([formatted date])"
  - [ ] Icon: Blue-grey circle with calendar+alert icon
  - [ ] Action: "Ver detalles" button
- [ ] Tapping notification navigates to event detail

## Phase 7: Deep Linking
- [ ] From notification tap:
  - [ ] App opens to correct screen (Invitations tab or Event detail)
  - [ ] If eventId present, correct event is selected
  - [ ] Navigation works whether app was in foreground or background
- [ ] Multiple rapid taps do not cause navigation errors

## Phase 8: Icon Visual Consistency
- [ ] All 4 notification types show distinct icons:
  - [ ] Invitation: person+badge
  - [ ] Calculation: calculator/coins
  - [ ] Reminder: calendar+alert
  - [ ] (Invitation Accepted uses same as Invitation Received)
- [ ] Large icons are 48dp circles with NeoFintech colors:
  - [ ] Invitations: neon green (#39FF14)
  - [ ] Calculations: amber (#FFA000)
  - [ ] Upcoming events: blue-grey (#D6DDED)
- [ ] Icons are clear and recognizable in notification shade

## Phase 9: Edge Cases
- [ ] Receive invitation with missing inviterName → notification still shows (fallback to email)
- [ ] Calculation with amountOwed = 0.00 → no notification
- [ ] Event with startDateMillis in past → no upcoming event notification
- [ ] Multiple invitations received rapidly → all notifications shown (no dedup across different invitations)
- [ ] Same invitation re-emitted by Firestore → no duplicate notification (dedup by invitationId)

## Phase 10: FCM Token Management
- [ ] Check Firestore: users/{uid}/fcmTokens contains current device token
- [ ] Uninstall and reinstall app → new token is saved
- [ ] Token updates are logged in Firebase Console

## Phase 11: Worker Migration
- [ ] ReminderWorker reads from SQLDelight (check logs for "RepositoryProvider" usage)
- [ ] No SharedPreferences reads in ReminderWorker logs
- [ ] Worker executes successfully even if SharedPreferences is empty

## Phase 12: Performance
- [ ] Notification dispatch is fast (<100ms from trigger to posted)
- [ ] Large icon cache works (second notification of same type is faster)
- [ ] No memory leaks after multiple notifications

## Known Limitations
- iOS notifications not implemented (out of scope)
- Rich notifications (images, custom layouts) not implemented
- Per-channel preference toggles not implemented (all-or-nothing)
- Cloud Functions for server-side FCM sending not implemented (manual testing only)

## Troubleshooting
- If notifications don't appear:
  - Check POST_NOTIFICATIONS permission granted
  - Check notification channel is enabled in system settings
  - Check FCM token exists in Firestore
  - Check logs for "NotificationDispatcher" messages
- If deep links don't work:
  - Check Intent extras are present (log in MainActivity.onNewIntent)
  - Check pagerState and selectedEventId are updated
- If Worker doesn't execute:
  - Check WorkManager is initialized
  - Trigger manually via WorkManager Inspector in Android Studio
