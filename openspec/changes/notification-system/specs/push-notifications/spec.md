# Push Notifications Specification

## Purpose

Defines the FCM service registration, runtime permission flow, notification channels, rich notification building (icons, action buttons), and deep linking for CuentaMorosos push and local notifications.

## Requirements

### R001: FCM Service Registration

The AndroidManifest.xml SHALL declare `CuentaMorososFirebaseMessagingService` with a `com.google.firebase.MESSAGING_EVENT` intent-filter so Firebase can deliver messages.

#### Scenario: Service declared and discoverable

- GIVEN the app is installed on a device with Google Play Services
- WHEN Firebase sends a data message to the user's FCM token
- THEN `CuentaMorososFirebaseMessagingService.onMessageReceived` SHALL be invoked

#### Scenario: FCM token refresh

- GIVEN the service is registered and the user is logged in
- WHEN Firebase rotates the FCM token
- THEN `onNewToken` SHALL persist the new token via `FirebaseUserSyncManager.saveTokenForCurrentUser`

---

### R002: POST_NOTIFICATIONS Runtime Permission

The app SHALL request `POST_NOTIFICATIONS` permission at runtime on Android 13+ (API 33) before attempting to post any notification.

#### Scenario: Permission granted (happy path)

- GIVEN the app runs on Android 13+ and permission has not been requested
- WHEN the user authenticates and `MainActivity` renders
- THEN the system permission dialog SHALL appear
- AND granting permission SHALL allow all notification channels to post

#### Scenario: Permission denied

- GIVEN the user denies `POST_NOTIFICATIONS`
- WHEN the system or FCM attempts to display a notification
- THEN the notification SHALL NOT be displayed
- AND the app SHALL continue functioning without crashes

#### Scenario: Pre-Android 13

- GIVEN the device runs Android 12 or below
- WHEN the app starts
- THEN no runtime permission request SHALL occur (permission is granted at install time)

---

### R003: Notification Channels

The system SHALL create exactly 4 notification channels, each with a distinct ID, name (in Spanish), and importance level.

| Channel ID | Name | Importance |
|---|---|---|
| `ch_invitations` | Invitaciones | HIGH |
| `ch_calculations` | Cálculos y liquidaciones | DEFAULT |
| `ch_reminders` | Recordatorios | DEFAULT |
| `ch_upcoming_events` | Próximos eventos | DEFAULT |

#### Scenario: Channels created on first launch

- GIVEN a fresh app install
- WHEN `NotificationDispatcher.ensureChannels()` runs
- THEN all 4 channels SHALL exist in system notification settings

#### Scenario: Channel importance matches use case

- GIVEN the user opens system notification settings
- WHEN they inspect each channel
- THEN "Invitaciones" SHALL default to HIGH (heads-up)
- AND the other 3 SHALL default to DEFAULT

---

### R004: NotificationDispatcher

A `NotificationDispatcher` class in the `app` module SHALL centralize all notification building and posting. It SHALL accept a notification type, context variables (names, amounts, dates), and produce a channel-specific notification.

#### Scenario: Dispatch invitation-received notification

- GIVEN `NotificationDispatcher` receives type `INVITATION_RECEIVED`
- AND context `{ inviterName: "Ana", eventName: "Asado" }`
- THEN it SHALL post a notification on `ch_invitations`
- WITH title "Invitación recibida"
- AND body "Ana te invitó al evento 'Asado'"
- AND small icon `ic_notification_invitation`

#### Scenario: Dispatch calculation-completed notification

- GIVEN type `CALCULATION_COMPLETED`
- AND context `{ eventName: "Cumpleaños", amountOwed: 25.50 }`
- THEN body SHALL be "Se calcularon los gastos de 'Cumpleaños'. Debes €25,50"

#### Scenario: Dispatch upcoming-event notification

- GIVEN type `UPCOMING_EVENT`
- AND context `{ eventName: "Reunión", daysUntil: 3, dateFormatted: "15/06/2026" }`
- THEN body SHALL be "El evento 'Reunión' es en 3 días (15/06/2026)"

---

### R005: Notification Icons

Each notification type SHALL use a distinct small icon resource. All icons MUST follow the NeoFintech design system for visual consistency.

#### Design System Specifications

**Common Style (applies to all 4 icons):**
- **Style**: Outline (stroke-based), NOT filled
- **Stroke width**: 1.5dp (consistent with Material Icons outlined)
- **Canvas size**: 24dp × 24dp (standard Android notification icon)
- **Padding**: 2dp internal padding (icon content area: 20dp × 20dp)
- **Corner radius**: 2dp on any rounded elements (subtle, not pill-shaped)
- **Visual weight**: Balanced — all 4 icons must appear to have similar visual density

**Color Treatment:**
- **Small icon (status bar)**: Monochrome white (Android system applies tint)
- **Large icon (notification drawer)**: Use NeoFintech accent colors as background circle with white icon overlay

| Type | Large Icon Background | Rationale |
|------|----------------------|-----------|
| INVITATION_RECEIVED | `primaryContainer` (neon green #39FF14) | Social/connection — positive action |
| INVITATION_ACCEPTED | `primaryFixedDim` (darker green #2AE500) | Confirmation — success state |
| CALCULATION_COMPLETED | `warning` (amber #FFA000) | Financial/money — attention needed |
| UPCOMING_EVENT | `tertiaryContainer` (blue-grey #D6DDED) | Time/calendar — informational |

#### Icon Concepts

**INVITATION_RECEIVED — `ic_notification_invitation`**
- **Concept**: Person silhouette with a "+" badge (top-right)
- **Visual**: Simple bust (head + shoulders) in outline, small circle with "+" in corner
- **Meaning**: Someone is inviting you to join
- **Avoid**: Envelope icon (too email-like), hand waving (too casual)

**INVITATION_ACCEPTED — `ic_notification_check`**
- **Concept**: Checkmark inside a circle
- **Visual**: Circular outline with a clean checkmark (45° angle, stroke caps rounded)
- **Meaning**: Confirmation, acceptance, success
- **Avoid**: Thumbs up (too informal), handshake (too complex at small size)

**CALCULATION_COMPLETED — `ic_notification_calc`**
- **Concept**: Calculator or coins stack with Euro symbol
- **Visual**: Simple calculator outline (rectangle with grid dots) OR 2-3 coins stacked with "€" on top
- **Meaning**: Money calculation, settlement, what you owe
- **Avoid**: Dollar sign (wrong currency), complex chart (too detailed)

**UPCOMING_EVENT — `ic_notification_calendar`**
- **Concept**: Calendar page with alert indicator
- **Visual**: Calendar outline (rectangle with top tabs) with small exclamation mark or bell in corner
- **Meaning**: Time-sensitive, upcoming date, reminder
- **Avoid**: Clock (too generic), bell alone (too alarm-like)

#### Large Icon Implementation

For notifications shown in the notification drawer, use `setLargeIcon()` with a programmatically generated bitmap:

```kotlin
fun createLargeIcon(iconRes: Int, backgroundColor: Color): Bitmap {
    // 48dp circle with backgroundColor
    // White icon (24dp) centered inside
    // Return as Bitmap
}
```

**Specifications:**
- **Size**: 48dp × 48dp (Android large icon standard)
- **Shape**: Circle (not rounded rectangle)
- **Background**: Solid color from table above
- **Foreground**: White icon (24dp) centered with 12dp margin

#### Scenario: Distinct icons per type

- GIVEN a notification is posted
- WHEN the user views it in the notification shade
- THEN the small icon SHALL match the table above for its type
- AND the large icon SHALL be a 48dp circle with the specified background color
- AND the white icon SHALL be centered inside the circle

#### Scenario: Visual consistency

- GIVEN all 4 notification types are displayed simultaneously
- WHEN the user inspects them
- THEN all icons SHALL have the same stroke width (1.5dp)
- AND all icons SHALL have similar visual weight (no icon appears heavier or lighter)
- AND all icons SHALL use the same internal padding (2dp)

#### Scenario: Dark mode adaptation

- GIVEN the device is in dark mode
- WHEN a notification is displayed
- THEN the small icon SHALL remain white (system handles tint)
- AND the large icon background color SHALL remain the same (colors are mode-independent)
- AND the icon SHALL remain visible and clear against the background

---

### R006: Action Buttons

Notifications of type `INVITATION_RECEIVED` SHALL include "Aceptar" and "Rechazar" action buttons. Notifications of type `CALCULATION_COMPLETED` and `UPCOMING_EVENT` SHALL include a "Ver detalles" action button.

#### Scenario: Invitation actions

- GIVEN an `INVITATION_RECEIVED` notification is displayed
- WHEN the user taps "Aceptar"
- THEN `InvitationsViewModel.acceptInvitation` SHALL be called for that invitation
- AND the notification SHALL be dismissed

#### Scenario: Invitation rejection

- GIVEN an `INVITATION_RECEIVED` notification is displayed
- WHEN the user taps "Rechazar"
- THEN `InvitationsViewModel.rejectInvitation` SHALL be called
- AND the notification SHALL be dismissed

#### Scenario: View details action

- GIVEN a `CALCULATION_COMPLETED` notification is displayed
- WHEN the user taps "Ver detalles"
- THEN the app SHALL navigate to the event detail screen (see R007)

---

### R007: Deep Linking

Tapping a notification body or action button SHALL navigate the app to the relevant screen via a `PendingIntent` with an explicit Intent containing the target route as extras.

| Type | Target Screen | Extras |
|---|---|---|
| INVITATION_RECEIVED | InvitationsScreen (page 3) | `eventId`, `invitationId` |
| INVITATION_ACCEPTED | EventDetailScreen (page 1) | `eventId` |
| CALCULATION_COMPLETED | EventDetailScreen (page 1) | `eventId` |
| UPCOMING_EVENT | EventDetailScreen (page 1) | `eventId` |

#### Scenario: Deep link to invitations

- GIVEN the app is in background
- WHEN the user taps an `INVITATION_RECEIVED` notification
- THEN the app SHALL open to the Invitations tab (pager page 3)

#### Scenario: Deep link to event detail

- GIVEN the app is in background
- WHEN the user taps a `CALCULATION_COMPLETED` notification
- THEN the app SHALL open to the Events tab (pager page 1) with the specific event selected

#### Scenario: App already in foreground

- GIVEN the app is in foreground on any screen
- WHEN a notification is tapped
- THEN the app SHALL navigate to the target screen without recreating the activity

---

### R008: FCM Message Dispatch

`onMessageReceived` SHALL parse `remoteMessage.data["type"]` and delegate to `NotificationDispatcher` with the appropriate context variables extracted from the data payload.

#### Scenario: Valid FCM data message in foreground

- GIVEN the app is in foreground
- WHEN an FCM message arrives with `{ type: "invitation_received", inviterName: "Ana", eventName: "Asado", eventId: "abc123" }`
- THEN `NotificationDispatcher` SHALL post a local notification with the parsed context

#### Scenario: FCM message with unknown type

- GIVEN an FCM message arrives with `type: "unknown_type"`
- WHEN `onMessageReceived` processes it
- THEN the message SHALL be logged and ignored (no notification posted)

#### Scenario: FCM message with missing required fields

- GIVEN an FCM message arrives with `type: "invitation_received"` but no `eventName`
- WHEN `onMessageReceived` processes it
- THEN the notification SHALL NOT be posted
- AND a warning SHALL be logged

#### Scenario: App backgrounded — FCM data-only message

- GIVEN the app is in background
- WHEN a data-only FCM message arrives (no `notification` key)
- THEN `onMessageReceived` SHALL still be invoked and post the local notification

---

## Affected Files

| File | Change |
|---|---|
| `app/src/main/AndroidManifest.xml` | Add `<service>` for FCM |
| `app/.../MainActivity.kt` | Add `ActivityResultContracts.RequestPermission()` for POST_NOTIFICATIONS |
| `app/.../CuentaMorososFirebaseMessagingService.kt` | Implement `onMessageReceived` with dispatch |
| `app/.../NotificationScheduler.kt` | Replace single channel with 4 channels; add multi-type post methods |
| `app/.../NotificationDispatcher.kt` | **New** — central dispatcher |
| `app/src/main/res/drawable/ic_notification_*.xml` | **New** — 4 icon resources |
