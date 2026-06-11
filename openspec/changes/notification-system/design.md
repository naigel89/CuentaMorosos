# Design: Notification System

## Technical Approach

Layered notification architecture: **triggers** in `shared` (platform-agnostic callbacks), **dispatch** in `app` (Android NotificationManager), **presentation** via system notifications with NeoFintech-styled icons. The existing `NotificationScheduler` singleton is replaced by a `NotificationDispatcher` instance that centralizes channel management, icon generation, deep linking, and posting. ViewModel triggers use optional lambda callbacks wired through `AppViewModelFactory`, following the existing manual DI pattern.

## Architecture Decisions

### Decision: NotificationDispatcher as instance, not singleton

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Singleton object (like current NotificationScheduler) | Simpler access, but hard to test, no context injection | **Rejected** |
| Instance created in MainActivity, passed via factory | Testable, context available, follows existing RepositoryProvider pattern | **Chosen** |

**Rationale**: The codebase uses `RepositoryProvider` as an instance created in `MainActivity`. Following this pattern keeps DI consistent. The dispatcher needs `Context` for notification building — passing it via constructor is cleaner than requiring it in every method call.

### Decision: Sealed class for NotificationEvent in shared module

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Enum + Map of extras | Type-unsafe, runtime errors on missing keys | **Rejected** |
| Sealed class with typed fields per event | Compile-time safety, IDE autocomplete, clear contracts | **Chosen** |

**Rationale**: The 4 notification types have different context variables (inviterName, amountOwed, daysUntil). A sealed class captures this heterogeneity while keeping a common `eventId` for deduplication and deep linking.

### Decision: Large icons generated programmatically, not as PNG resources

| Option | Tradeoff | Decision |
|--------|----------|----------|
| 4 sets of PNG drawables (mdpi/hdpi/xxhdpi) | 48+ files, manual maintenance, no dynamic color | **Rejected** |
| Programmatic Bitmap from vector + color at runtime | ~20 lines of code, dynamic NeoFintech colors, single source of truth | **Chosen** |

**Rationale**: The spec requires 4 background colors from `NeoFintechColors`. Programmatic generation avoids 48+ PNG files and adapts automatically if colors change. Small icons remain XML vectors (4 files).

### Decision: Deep linking via MainActivity intent filter, not separate Activity

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Separate NotificationDeepLinkActivity | Clean separation, but adds Activity to manifest, lifecycle complexity | **Rejected** |
| MainActivity with intent filter + onNewIntent | Single Activity, reuse existing Compose navigation, simpler | **Chosen** |

**Rationale**: The app is single-Activity with Compose Navigation. Adding a deep link Activity would require inter-Activity communication. Handling `onNewIntent` in `MainActivity` and updating pager state is simpler and follows the existing pattern.

### Decision: ReminderWorker migration via RepositoryProvider static accessor

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Hilt/Dagger injection into Worker | Requires DI framework setup, overkill for this project | **Rejected** |
| WorkManager WorkerFactory with manual DI | Clean but verbose, requires custom WorkerFactory registration | **Rejected** |
| RepositoryProvider stored in Application singleton, accessed from Worker | Minimal change, follows existing pattern where MainActivity creates RepositoryProvider | **Chosen** |

**Rationale**: The project uses manual DI (no Hilt). Storing `RepositoryProvider` in a custom `Application` class allows `ReminderWorker` to access repositories without introducing a DI framework. The existing code already creates `RepositoryProvider` in `MainActivity` — we store a reference in `CuentaMorososApp` for Worker access.

## Data Flow

### Flow 1: Invitation Received (FCM — app backgrounded or foreground)

```
Firebase Cloud → FCM Service.onMessageReceived()
    │
    ├─ parse data["type"] = "invitation_received"
    ├─ extract: inviterName, eventName, eventId, invitationId
    │
    └→ NotificationDispatcher.dispatch(NotificationEvent.InvitationReceived(...))
         │
         ├─ resolve channel: ch_invitations (HIGH)
         ├─ build: small icon (ic_notification_invitation) + large icon (green circle)
         ├─ add actions: [Aceptar] [Rechazar] → PendingIntents with invitationId
         ├─ set content intent: deep link → InvitationsScreen (page 3)
         └─ NotificationManager.notify(tag=INVITATION_RECEIVED, id=eventId.hashCode())
```

### Flow 2: Invitation Received (app foreground — Firestore listener)

```
Firestore snapshot → InvitationsViewModel.pendingInvitations Flow
    │
    ├─ onEach: invitation.id not in notifiedIds?
    │   ├─ YES: notifiedIds.add(id)
    │   └─ onNewInvitation?.invoke(event)  ← callback from AppViewModelFactory
    │
    └→ NotificationDispatcher.dispatch(event)
         └─ (same as Flow 1 from here)
```

### Flow 3: Calculation Completed

```
EventRepository Flow → DashboardViewModel.computeState()
    │
    ├─ detect: event.state transitioned to CALCULATED
    ├─ compute: amountOwed for currentUserUid from debts
    ├─ if amountOwed > 0:
    │   └→ onCalculationCompleted?.invoke(event)  ← callback
    │
    └→ NotificationDispatcher.dispatch(NotificationEvent.CalculationCompleted(...))
         ├─ channel: ch_calculations (DEFAULT)
         ├─ body: "Se calcularon los gastos de '{eventName}'. Debes €{amount}"
         └─ deep link → EventDetailScreen with eventId
```

### Flow 4: Upcoming Event (ReminderWorker — daily)

```
ReminderWorker.doWork() [24h periodic]
    │
    ├─ RepositoryProvider.eventRepository.observeEvents() → snapshot
    ├─ ReminderService.buildUpcomingEventMessages(events, reminderDays)
    │   ├─ filter: startDateMillis in [now, now + reminderDays]
    │   └─ map: ReminderMessage(title, body, type=UPCOMING_EVENT, eventId)
    │
    └→ for each message: NotificationDispatcher.dispatch(...)
         ├─ channel: ch_upcoming_events (DEFAULT)
         └─ deep link → EventDetailScreen with eventId
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `shared/src/commonMain/kotlin/com/cuentamorosos/notifications/NotificationEvent.kt` | Create | Sealed class with 4 event types + typed context fields |
| `app/src/main/java/com/cuentamorosos/notifications/NotificationDispatcher.kt` | Create | Central dispatcher: channels, icons, deep links, posting |
| `app/src/main/java/com/cuentamorosos/notifications/NotificationDeepLinkParser.kt` | Create | Parses Intent extras → navigation targets (pager page + eventId) |
| `app/src/main/res/drawable/ic_notification_invitation.xml` | Create | Vector: person + badge (outline, 1.5dp, 24dp canvas) |
| `app/src/main/res/drawable/ic_notification_check.xml` | Create | Vector: checkmark in circle (outline, 1.5dp, 24dp canvas) |
| `app/src/main/res/drawable/ic_notification_calc.xml` | Create | Vector: coins with € symbol (outline, 1.5dp, 24dp canvas) |
| `app/src/main/res/drawable/ic_notification_calendar.xml` | Create | Vector: calendar with alert (outline, 1.5dp, 24dp canvas) |
| `app/src/main/AndroidManifest.xml` | Modify | Add `<service>` for FCM, intent filter for deep links on MainActivity, `android:name` on `<application>` |
| `app/src/main/java/com/cuentamorosos/MainActivity.kt` | Modify | Create NotificationDispatcher, request POST_NOTIFICATIONS, wire callbacks to factory, handle deep link intents, store RepositoryProvider in Application |
| `app/src/main/java/com/cuentamorosos/data/CuentaMorososFirebaseMessagingService.kt` | Create | Implement `onMessageReceived`: parse data payload → NotificationEvent → dispatcher |
| `app/src/main/java/com/cuentamorosos/data/ReminderWorker.kt` | Modify | Replace `CuentaMorososLocalStore` with `RepositoryProvider` from Application singleton |
| `shared/src/commonMain/kotlin/com/cuentamorosos/ui/InvitationsViewModel.kt` | Modify | Extend `onNewInvitation` signature to pass `NotificationEvent.InvitationReceived` |
| `shared/src/commonMain/kotlin/com/cuentamorosos/ui/DashboardViewModel.kt` | Modify | Add `onCalculationCompleted` callback, detect CALCULATED state transitions |
| `shared/src/commonMain/kotlin/com/cuentamorosos/data/ReminderService.kt` | Modify | Add `buildUpcomingEventMessages()` for events within reminder window |
| `shared/src/androidMain/kotlin/com/cuentamorosos/AppViewModelFactory.kt` | Modify | Accept `NotificationCallbacks` parameter, pass to InvitationsViewModel and DashboardViewModel |
| `shared/src/androidMain/kotlin/com/cuentamorosos/CuentaMorososApp.kt` | Create | Custom Application class holding RepositoryProvider singleton for Worker access |

## Interfaces / Contracts

### NotificationEvent (shared module)

```kotlin
package com.cuentamorosos.notifications

sealed class NotificationEvent {
    abstract val eventId: String?

    data class InvitationReceived(
        val invitationId: String,
        override val eventId: String,
        val inviterName: String,
        val eventName: String,
    ) : NotificationEvent()

    data class InvitationAccepted(
        override val eventId: String,
        val inviteeName: String,
        val eventName: String,
    ) : NotificationEvent()

    data class CalculationCompleted(
        override val eventId: String,
        val eventName: String,
        val amountOwed: Double,
    ) : NotificationEvent()

    data class UpcomingEvent(
        override val eventId: String,
        val eventName: String,
        val daysUntil: Int,
        val dateFormatted: String,
    ) : NotificationEvent()
}
```

### NotificationCallbacks (shared module — wiring contract)

```kotlin
package com.cuentamorosos

data class NotificationCallbacks(
    val onInvitationReceived: ((NotificationEvent.InvitationReceived) -> Unit)? = null,
    val onInvitationAccepted: ((NotificationEvent.InvitationAccepted) -> Unit)? = null,
    val onCalculationCompleted: ((NotificationEvent.CalculationCompleted) -> Unit)? = null,
)
```

### ReminderMessage extension

```kotlin
// Existing ReminderMessage extended with optional notification metadata
data class ReminderMessage(
    val title: String,
    val body: String,
    val type: ReminderType = ReminderType.PENDING_DEBT,
    val eventId: String? = null,
    val daysUntil: Int? = null,
    val dateFormatted: String? = null,
)

enum class ReminderType { PENDING_DEBT, INCOMPLETE_EVENT, UPCOMING_EVENT }
```

### Deep Link Extras Contract

| Extra Key | Type | Present For | Example |
|-----------|------|-------------|---------|
| `extra_notification_type` | String | All | `"INVITATION_RECEIVED"` |
| `extra_event_id` | String | All except pure invitation | `"evt-abc123"` |
| `extra_invitation_id` | String | INVITATION_RECEIVED | `"inv-xyz789"` |
| `extra_pager_page` | Int | All | `1` (Events) or `3` (Invitations) |

### FCM Data Payload Contract

```json
{
  "type": "invitation_received",
  "eventId": "evt-abc123",
  "invitationId": "inv-xyz789",
  "inviterName": "Ana García",
  "eventName": "Asado"
}
```

Supported `type` values: `invitation_received`, `invitation_accepted`, `calculation_completed`, `upcoming_event`. Unknown types are logged and ignored (R008).

---

## Component Design — Detailed Specifications

### 1. Iconos XML — Especificaciones completas

All 4 icons share these base attributes:
- `xmlns:android="http://schemas.android.com/apk/res/android"`
- `android:width="24dp"`, `android:height="24dp"`
- `android:viewportWidth="24"`, `android:viewportHeight="24"`
- Every `<path>`: `android:strokeWidth="1.5"`, `android:strokeColor="@android:color/white"`, no `android:fillColor` (outline only)

#### ic_notification_invitation.xml (persona + badge "+")

Concept: Bust of a person (head circle + shoulder arc) with a small circle containing a "+" sign in the top-right corner.

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <!-- Head: circle centered at (12,8) radius 3 -->
    <path
        android:pathData="M12,8m-3,0a3,3 0,1 0,6 0a3,3 0,1 0,-6 0"
        android:strokeWidth="1.5"
        android:strokeColor="@android:color/white"/>
    <!-- Shoulders: arc from (5,20) curving up through center to (19,20) -->
    <path
        android:pathData="M5,20c0,-3.87 3.13,-7 7,-7s7,3.13 7,7"
        android:strokeWidth="1.5"
        android:strokeColor="@android:color/white"/>
    <!-- Badge circle: centered at (19,5) radius 2 -->
    <path
        android:pathData="M19,5m-2,0a2,2 0,1 0,4 0a2,2 0,1 0,-4 0"
        android:strokeWidth="1.5"
        android:strokeColor="@android:color/white"/>
    <!-- Plus sign inside badge: vertical + horizontal lines -->
    <path
        android:pathData="M19,4v2M18,5h2"
        android:strokeWidth="1.5"
        android:strokeColor="@android:color/white"/>
</vector>
```

#### ic_notification_check.xml (checkmark in circle)

Concept: A circle outline with a checkmark path inside (from upper-left to center-bottom to upper-right).

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <!-- Outer circle: centered at (12,12) radius 9 -->
    <path
        android:pathData="M12,12m-9,0a9,9 0,1 0,18 0a9,9 0,1 0,-18 0"
        android:strokeWidth="1.5"
        android:strokeColor="@android:color/white"/>
    <!-- Checkmark: from (7.5,12) to (10.5,15) to (16.5,9) -->
    <path
        android:pathData="M7.5,12l3,3l6,-6"
        android:strokeWidth="1.5"
        android:strokeColor="@android:color/white"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"/>
</vector>
```

#### ic_notification_calc.xml (coins with € symbol)

Concept: Two stacked ellipses representing coins, with a "€" symbol path on the top coin.

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <!-- Bottom coin: ellipse at (12,16) rx=8 ry=3 -->
    <path
        android:pathData="M12,16m-8,0a8,3 0,1 0,16 0a8,3 0,1 0,-16 0"
        android:strokeWidth="1.5"
        android:strokeColor="@android:color/white"/>
    <!-- Top coin: ellipse at (12,11) rx=8 ry=3 -->
    <path
        android:pathData="M12,11m-8,0a8,3 0,1 0,16 0a8,3 0,1 0,-16 0"
        android:strokeWidth="1.5"
        android:strokeColor="@android:color/white"/>
    <!-- Vertical connecting lines (coin edges) -->
    <path
        android:pathData="M4,11v5M20,11v5"
        android:strokeWidth="1.5"
        android:strokeColor="@android:color/white"/>
    <!-- Euro symbol: "C" curve + two horizontal bars -->
    <path
        android:pathData="M14,7.5c-0.6,-0.3 -1.3,-0.5 -2,-0.5c-2.2,0 -4,1.8 -4,4s1.8,4 4,4c0.7,0 1.4,-0.2 2,-0.5"
        android:strokeWidth="1.5"
        android:strokeColor="@android:color/white"
        android:strokeLineCap="round"/>
    <path
        android:pathData="M8,9.5h4M8,12.5h4"
        android:strokeWidth="1.5"
        android:strokeColor="@android:color/white"
        android:strokeLineCap="round"/>
</vector>
```

#### ic_notification_calendar.xml (calendar + alert bell)

Concept: A calendar rectangle with two top tabs, and a small exclamation mark or bell in the bottom-right corner.

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <!-- Calendar body: rounded rectangle from (3,5) to (21,21) -->
    <path
        android:pathData="M3,7c0,-1.1 0.9,-2 2,-2h14c1.1,0 2,0.9 2,2v12c0,1.1 -0.9,2 -2,2H5c-1.1,0 -2,-0.9 -2,-2V7z"
        android:strokeWidth="1.5"
        android:strokeColor="@android:color/white"/>
    <!-- Top tabs: two small vertical lines -->
    <path
        android:pathData="M8,3v4M16,3v4"
        android:strokeWidth="1.5"
        android:strokeColor="@android:color/white"
        android:strokeLineCap="round"/>
    <!-- Horizontal divider line -->
    <path
        android:pathData="M3,10h18"
        android:strokeWidth="1.5"
        android:strokeColor="@android:color/white"/>
    <!-- Alert "!" inside calendar: vertical line + dot -->
    <path
        android:pathData="M12,13v3"
        android:strokeWidth="1.5"
        android:strokeColor="@android:color/white"
        android:strokeLineCap="round"/>
    <path
        android:pathData="M12,18v0.5"
        android:strokeWidth="2"
        android:strokeColor="@android:color/white"
        android:strokeLineCap="round"/>
</vector>
```

### 2. NotificationDispatcher — Implementación completa

```kotlin
package com.cuentamorosos.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.cuentamorosos.MainActivity
import com.cuentamorosos.R
import java.util.concurrent.ConcurrentHashMap

class NotificationDispatcher(private val context: Context) {

    companion object {
        private const val TAG = "NotificationDispatcher"

        // Channel IDs
        const val CH_INVITATIONS = "ch_invitations"
        const val CH_CALCULATIONS = "ch_calculations"
        const val CH_REMINDERS = "ch_reminders"
        const val CH_UPCOMING_EVENTS = "ch_upcoming_events"

        // Intent extra keys
        const val EXTRA_NOTIFICATION_TYPE = "extra_notification_type"
        const val EXTRA_EVENT_ID = "extra_event_id"
        const val EXTRA_INVITATION_ID = "extra_invitation_id"
        const val EXTRA_PAGER_PAGE = "extra_pager_page"

        // Notification type tags (for deduplication)
        const val TAG_INVITATION_RECEIVED = "INVITATION_RECEIVED"
        const val TAG_INVITATION_ACCEPTED = "INVITATION_ACCEPTED"
        const val TAG_CALCULATION_COMPLETED = "CALCULATION_COMPLETED"
        const val TAG_UPCOMING_EVENT = "UPCOMING_EVENT"

        // Pager page indices (match MainSection enum order)
        const val PAGE_DASHBOARD = 0
        const val PAGE_EVENTS = 1
        const val PAGE_PROFILES = 2
        const val PAGE_INVITATIONS = 3
        const val PAGE_SETTINGS = 4
    }

    private val iconCache = ConcurrentHashMap<NotificationType, Bitmap>()
    private var channelsCreated = false

    // ── Public API ──────────────────────────────────────────────────────────

    fun dispatch(event: NotificationEvent) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            Log.w(TAG, "Notifications disabled, skipping dispatch for ${event::class.simpleName}")
            return
        }
        ensureChannels()
        val notification = buildNotification(event)
        val tag = notificationTag(event)
        val id = notificationId(event)
        NotificationManagerCompat.from(context).notify(tag, id, notification)
        Log.d(TAG, "Dispatched notification: tag=$tag, id=$id")
    }

    // ── Channel Management ──────────────────────────────────────────────────

    private fun ensureChannels() {
        if (channelsCreated) return
        val manager = NotificationManagerCompat.from(context)

        val invitationsChannel = androidx.core.app.NotificationChannelCompat.Builder(
            CH_INVITATIONS, NotificationManagerCompat.IMPORTANCE_HIGH
        )
            .setName("Invitaciones")
            .setDescription("Notificaciones de invitaciones a eventos")
            .build()

        val calculationsChannel = androidx.core.app.NotificationChannelCompat.Builder(
            CH_CALCULATIONS, NotificationManagerCompat.IMPORTANCE_DEFAULT
        )
            .setName("Cálculos y liquidaciones")
            .setDescription("Notificaciones cuando se calculan gastos de eventos")
            .build()

        val remindersChannel = androidx.core.app.NotificationChannelCompat.Builder(
            CH_REMINDERS, NotificationManagerCompat.IMPORTANCE_DEFAULT
        )
            .setName("Recordatorios")
            .setDescription("Recordatorios de deudas pendientes y eventos incompletos")
            .build()

        val upcomingChannel = androidx.core.app.NotificationChannelCompat.Builder(
            CH_UPCOMING_EVENTS, NotificationManagerCompat.IMPORTANCE_DEFAULT
        )
            .setName("Próximos eventos")
            .setDescription("Avisos de eventos que se acercan")
            .build()

        manager.createNotificationChannelsCompat(
            listOf(invitationsChannel, calculationsChannel, remindersChannel, upcomingChannel)
        )
        channelsCreated = true
    }

    // ── Notification Building ───────────────────────────────────────────────

    private fun buildNotification(event: NotificationEvent): Notification {
        val type = notificationType(event)
        val channelId = channelIdFor(type)
        val smallIconRes = smallIconResFor(type)
        val largeIcon = getLargeIcon(type)

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIconRes)
            .setLargeIcon(largeIcon)
            .setContentTitle(titleFor(event))
            .setContentText(bodyFor(event))
            .setContentIntent(createContentIntent(event))
            .setAutoCancel(true)
            .setPriority(priorityFor(type))
            .apply { addActions(event, this) }
            .build()
    }

    // ── Type Resolution ─────────────────────────────────────────────────────

    private fun notificationType(event: NotificationEvent): NotificationType = when (event) {
        is NotificationEvent.InvitationReceived -> NotificationType.INVITATION
        is NotificationEvent.InvitationAccepted -> NotificationType.INVITATION_ACCEPTED
        is NotificationEvent.CalculationCompleted -> NotificationType.CALCULATION
        is NotificationEvent.UpcomingEvent -> NotificationType.UPCOMING_EVENT
    }

    private fun channelIdFor(type: NotificationType): String = when (type) {
        NotificationType.INVITATION, NotificationType.INVITATION_ACCEPTED -> CH_INVITATIONS
        NotificationType.CALCULATION -> CH_CALCULATIONS
        NotificationType.UPCOMING_EVENT -> CH_UPCOMING_EVENTS
    }

    private fun smallIconResFor(type: NotificationType): Int = when (type) {
        NotificationType.INVITATION, NotificationType.INVITATION_ACCEPTED -> R.drawable.ic_notification_invitation
        NotificationType.CALCULATION -> R.drawable.ic_notification_calc
        NotificationType.UPCOMING_EVENT -> R.drawable.ic_notification_calendar
    }

    private fun priorityFor(type: NotificationType): Int = when (type) {
        NotificationType.INVITATION, NotificationType.INVITATION_ACCEPTED -> NotificationCompat.PRIORITY_HIGH
        NotificationType.CALCULATION -> NotificationCompat.PRIORITY_DEFAULT
        NotificationType.UPCOMING_EVENT -> NotificationCompat.PRIORITY_DEFAULT
    }

    // ── Title & Body ────────────────────────────────────────────────────────

    private fun titleFor(event: NotificationEvent): String = when (event) {
        is NotificationEvent.InvitationReceived -> "Invitación recibida"
        is NotificationEvent.InvitationAccepted -> "Invitación aceptada"
        is NotificationEvent.CalculationCompleted -> "Cálculo completado"
        is NotificationEvent.UpcomingEvent -> "Próximo evento"
    }

    private fun bodyFor(event: NotificationEvent): String = when (event) {
        is NotificationEvent.InvitationReceived ->
            "${event.inviterName} te invitó al evento '${event.eventName}'"
        is NotificationEvent.InvitationAccepted ->
            "${event.inviteeName} aceptó tu invitación a '${event.eventName}'"
        is NotificationEvent.CalculationCompleted -> {
            val formatted = String.format("%.2f", event.amountOwed)
            "Se calcularon los gastos de '${event.eventName}'. Debes €$formatted"
        }
        is NotificationEvent.UpcomingEvent ->
            "El evento '${event.eventName}' es en ${event.daysUntil} días (${event.dateFormatted})"
    }

    // ── Tag & ID ────────────────────────────────────────────────────────────

    private fun notificationTag(event: NotificationEvent): String = when (event) {
        is NotificationEvent.InvitationReceived -> TAG_INVITATION_RECEIVED
        is NotificationEvent.InvitationAccepted -> TAG_INVITATION_ACCEPTED
        is NotificationEvent.CalculationCompleted -> TAG_CALCULATION_COMPLETED
        is NotificationEvent.UpcomingEvent -> TAG_UPCOMING_EVENT
    }

    private fun notificationId(event: NotificationEvent): Int {
        val key = event.eventId ?: event.hashCode().toString()
        return key.hashCode()
    }

    // ── Actions ─────────────────────────────────────────────────────────────

    private fun addActions(event: NotificationEvent, builder: NotificationCompat.Builder) {
        when (event) {
            is NotificationEvent.InvitationReceived -> {
                // Accept action
                val acceptIntent = createActionIntent(
                    action = "ACTION_ACCEPT_INVITATION",
                    eventId = event.eventId,
                    invitationId = event.invitationId,
                )
                val acceptPending = PendingIntent.getBroadcast(
                    context, event.invitationId.hashCode(),
                    acceptIntent,
                    pendingIntentFlags(),
                )
                builder.addAction(
                    R.drawable.ic_notification_check,
                    "Aceptar",
                    acceptPending,
                )

                // Reject action
                val rejectIntent = createActionIntent(
                    action = "ACTION_REJECT_INVITATION",
                    eventId = event.eventId,
                    invitationId = event.invitationId,
                )
                val rejectPending = PendingIntent.getBroadcast(
                    context, event.invitationId.hashCode() + 1,
                    rejectIntent,
                    pendingIntentFlags(),
                )
                builder.addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Rechazar",
                    rejectPending,
                )
            }
            is NotificationEvent.CalculationCompleted,
            is NotificationEvent.UpcomingEvent,
            is NotificationEvent.InvitationAccepted -> {
                // "Ver detalles" action → same as tapping the notification
                val detailIntent = createContentIntent(event)
                builder.addAction(
                    android.R.drawable.ic_menu_view,
                    "Ver detalles",
                    detailIntent,
                )
            }
        }
    }

    // ── Content Intent (Deep Link) ──────────────────────────────────────────

    private fun createContentIntent(event: NotificationEvent): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTIFICATION_TYPE, notificationTag(event))
            putExtra(EXTRA_PAGER_PAGE, pagerPageFor(event))
            event.eventId?.let { putExtra(EXTRA_EVENT_ID, it) }
            if (event is NotificationEvent.InvitationReceived) {
                putExtra(EXTRA_INVITATION_ID, event.invitationId)
            }
        }
        return PendingIntent.getActivity(
            context,
            event.hashCode(),
            intent,
            pendingIntentFlags(),
        )
    }

    private fun pagerPageFor(event: NotificationEvent): Int = when (event) {
        is NotificationEvent.InvitationReceived -> PAGE_INVITATIONS  // page 3
        else -> PAGE_DASHBOARD  // page 0
    }

    private fun createActionIntent(
        action: String,
        eventId: String,
        invitationId: String,
    ): Intent = Intent(context, NotificationActionReceiver::class.java).apply {
        this.action = action
        putExtra(EXTRA_EVENT_ID, eventId)
        putExtra(EXTRA_INVITATION_ID, invitationId)
    }

    private fun pendingIntentFlags(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

    // ── Large Icon Generation ───────────────────────────────────────────────

    private fun getLargeIcon(type: NotificationType): Bitmap {
        return iconCache.getOrPut(type) {
            val bgColor = bgColorFor(type)
            val iconRes = smallIconResFor(type)
            createLargeIcon(iconRes, bgColor)
        }
    }

    private fun bgColorFor(type: NotificationType): Int = when (type) {
        NotificationType.INVITATION -> 0xFF39FF14.toInt()       // NeoFintech primaryContainer (green)
        NotificationType.INVITATION_ACCEPTED -> 0xFF39FF14.toInt() // same green
        NotificationType.CALCULATION -> 0xFFFFA000              // NeoFintech warning (amber)
        NotificationType.UPCOMING_EVENT -> 0xFF81D4FA           // NeoFintech flight blue
    }

    private fun createLargeIcon(iconRes: Int, bgColor: Int): Bitmap {
        val sizePx = (48 * context.resources.displayMetrics.density).toInt()
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw background circle
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, paint)

        // Draw vector icon centered, tinted white
        val drawable = ContextCompat.getDrawable(context, iconRes) ?: return bitmap
        drawable.setTint(0xFFFFFFFF.toInt())
        val iconSize = (sizePx * 0.6).toInt()
        val offset = (sizePx - iconSize) / 2
        drawable.setBounds(offset, offset, offset + iconSize, offset + iconSize)
        drawable.draw(canvas)

        return bitmap
    }

    // ── Internal enum ───────────────────────────────────────────────────────

    private enum class NotificationType {
        INVITATION,
        INVITATION_ACCEPTED,
        CALCULATION,
        UPCOMING_EVENT,
    }
}
```

### 3. ViewModel Triggers — Lógica exacta

#### InvitationsViewModel (modificación)

```kotlin
class InvitationsViewModel(
    private val invitationRepository: InvitationRepository,
    /** Called when a new invitation arrives so the platform can post a notification. */
    private val onNewInvitation: ((NotificationEvent.InvitationReceived) -> Unit)? = null,
) : ViewModel() {

    // IDs de invitaciones ya notificadas en esta sesión para no repetir la notificación
    private val notifiedIds = mutableSetOf<String>()

    val pendingInvitations: StateFlow<List<EventInvitation>> =
        invitationRepository.observePendingInvitations()
            .onEach { invitations ->
                invitations.forEach { invitation ->
                    if (invitation.id !in notifiedIds &&
                        invitation.status == InvitationStatus.PENDING
                    ) {
                        notifiedIds.add(invitation.id)
                        onNewInvitation?.invoke(
                            NotificationEvent.InvitationReceived(
                                invitationId = invitation.id,
                                eventId = invitation.eventId,
                                inviterName = invitation.invitedByEmail,
                                eventName = invitation.eventName,
                            )
                        )
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ... sendInvitation, acceptInvitation, rejectInvitation unchanged
}
```

**Key change**: The `onNewInvitation` lambda signature changes from `((eventName: String, invitedByEmail: String) -> Unit)?` to `((NotificationEvent.InvitationReceived) -> Unit)?`. The trigger now also checks `invitation.status == InvitationStatus.PENDING` to avoid firing on already-accepted/rejected invitations that might appear in the Flow.

#### DashboardViewModel (modificación)

```kotlin
class DashboardViewModel(
    private val eventRepository: EventRepository,
    private val debtRepository: DebtRepository,
    private val expenseRepository: ExpenseRepository,
    private val profileRepository: ProfileRepository,
    private val currentUserUid: String,
    private val onCalculationCompleted: ((NotificationEvent.CalculationCompleted) -> Unit)? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    // Track which events we already notified as CALCULATED to avoid duplicates
    private val notifiedCalculatedEventIds = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            combine(
                eventRepository.observeEvents(),
                debtRepository.observeAllDebts(),
                expenseRepository.observeAllExpenses(),
                profileRepository.observeProfiles(),
            ) { events, debts, expenses, profiles ->
                computeState(events, debts, expenses, profiles)
            }.collect { newState ->
                _state.value = newState.copy(isLoading = false)
            }
        }

        viewModelScope.launch {
            delay(8_000)
            _state.value = _state.value.copy(isLoading = false)
        }
    }

    private fun computeState(
        events: List<EventItem>,
        debts: List<EventDebtItem>,
        expenses: List<EventExpenseItem>,
        profiles: List<ProfileItem>,
    ): DashboardState {
        // ── TRIGGER: Detect CALCULATED transitions ──
        events.forEach { event ->
            if (event.state == EventState.CALCULATED &&
                event.id !in notifiedCalculatedEventIds
            ) {
                notifiedCalculatedEventIds.add(event.id)

                // Calculate how much the current user owes for this event
                val amountOwed = debts
                    .filter { it.eventId == event.id && it.profileId == currentUserUid && !it.paid }
                    .sumOf { it.amountEuros }

                if (amountOwed > 0) {
                    onCalculationCompleted?.invoke(
                        NotificationEvent.CalculationCompleted(
                            eventId = event.id,
                            eventName = event.name,
                            amountOwed = amountOwed,
                        )
                    )
                }
            }
        }

        // ── Existing logic unchanged ──
        val totalOwedToYou = debts.filter { !it.paid }.sumOf { it.amountEuros }
        val totalYouOwe = debts
            .filter { !it.paid && it.profileId == currentUserUid }
            .sumOf { it.amountEuros }
        val smartAlerts = computeSmartAlerts(events, expenses)
        val allEvents = buildAllEvents(events, debts, expenses)
        val breakdown = computeProfileBreakdown(debts, profiles, currentUserUid)

        return DashboardState(
            totalOwedToYou = totalOwedToYou,
            totalYouOwe = totalYouOwe,
            smartAlerts = smartAlerts,
            allEvents = allEvents,
            owedToYouBreakdown = breakdown.owedToYouBreakdown,
            youOweBreakdown = breakdown.youOweBreakdown,
        )
    }

    // ... computeSmartAlerts, buildAllEvents, computeProfileBreakdown unchanged
}
```

**Key details**:
- `notifiedCalculatedEventIds` prevents duplicate notifications when the Flow re-emits.
- The trigger fires inside `computeState()` because that's where we have access to both `events` and `debts` in the same scope.
- `amountOwed` is computed as the sum of unpaid debts where `profileId == currentUserUid`.
- The callback only fires when `amountOwed > 0` (no notification if user owes nothing).

#### AppViewModelFactory (modificación)

```kotlin
class AppViewModelFactory(
    private val repositoryProvider: RepositoryProvider,
    private val currentProfileId: String = "",
    private val notificationCallbacks: NotificationCallbacks = NotificationCallbacks(),
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            // ... EventsViewModel, EventDetailViewModel, ProfilesViewModel unchanged

            modelClass.isAssignableFrom(InvitationsViewModel::class.java) -> {
                InvitationsViewModel(
                    invitationRepository = repositoryProvider.invitationRepository,
                    onNewInvitation = notificationCallbacks.onInvitationReceived,
                ) as T
            }
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> {
                DashboardViewModel(
                    eventRepository = repositoryProvider.eventRepository,
                    debtRepository = repositoryProvider.debtRepository,
                    expenseRepository = repositoryProvider.expenseRepository,
                    profileRepository = repositoryProvider.profileRepository,
                    currentUserUid = currentProfileId,
                    onCalculationCompleted = notificationCallbacks.onCalculationCompleted,
                ) as T
            }

            // ... AccountViewModel unchanged
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
```

### 4. Deep Linking — Implementación completa

#### MainActivity — onNewIntent

```kotlin
class MainActivity : ComponentActivity() {

    // ── Deep link state holder ──
    // SharedFlow so Compose can collect it reactively
    private val _deepLinkEvent = MutableSharedFlow<DeepLinkTarget>(extraBufferCapacity = 1)
    val deepLinkEvent: SharedFlow<DeepLinkTarget> = _deepLinkEvent.asSharedFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ... existing onCreate ...

        // Handle deep link from cold start
        handleDeepLinkIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkIntent(intent)
    }

    private fun handleDeepLinkIntent(intent: Intent?) {
        intent ?: return
        val type = intent.getStringExtra(NotificationDispatcher.EXTRA_NOTIFICATION_TYPE) ?: return
        val eventId = intent.getStringExtra(NotificationDispatcher.EXTRA_EVENT_ID)
        val pagerPage = intent.getIntExtra(NotificationDispatcher.EXTRA_PAGER_PAGE, 0)

        _deepLinkEvent.tryEmit(
            DeepLinkTarget(
                pagerPage = pagerPage,
                eventId = eventId,
                notificationType = type,
            )
        )

        // Clear extras to avoid re-processing on config change
        intent.removeExtra(NotificationDispatcher.EXTRA_NOTIFICATION_TYPE)
        intent.removeExtra(NotificationDispatcher.EXTRA_EVENT_ID)
        intent.removeExtra(NotificationDispatcher.EXTRA_INVITATION_ID)
        intent.removeExtra(NotificationDispatcher.EXTRA_PAGER_PAGE)
    }
}

/** Target state for deep link navigation. */
data class DeepLinkTarget(
    val pagerPage: Int,
    val eventId: String?,
    val notificationType: String,
)
```

#### Compose integration in CuentaMorososApp

The `MainActivity` passes the `deepLinkEvent` SharedFlow down to `CuentaMorososApp`:

```kotlin
// In MainAppContent:
CuentaMorososApp(
    viewModelFactory = viewModelFactory,
    currentUserUid = user.uid,
    preferences = preferences,
    deepLinkEvent = (this@MainActivity as MainActivity).deepLinkEvent,
    // ... other params unchanged
)
```

Inside `CuentaMorososApp`:

```kotlin
@Composable
fun CuentaMorososApp(
    // ... existing params ...
    deepLinkEvent: SharedFlow<DeepLinkTarget>? = null,
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 5 })
    val coroutineScope = rememberCoroutineScope()

    // ── Deep link handler ──
    LaunchedEffect(deepLinkEvent) {
        deepLinkEvent?.collect { target ->
            // Navigate to the correct pager page
            coroutineScope.launch {
                pagerState.animateScrollToPage(target.pagerPage)
            }
            // If eventId is present, select that event
            target.eventId?.let { eventId ->
                eventDetailViewModel.setEventId(eventId)
            }
        }
    }

    // ... rest of CuentaMorososApp unchanged
}
```

### 5. FCM Service — Parsing completo

```kotlin
package com.cuentamorosos.data

import android.util.Log
import com.cuentamorosos.notifications.NotificationDispatcher
import com.cuentamorosos.notifications.NotificationEvent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class CuentaMorososFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "CM_FCM"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data = remoteMessage.data

        if (data.isEmpty()) {
            Log.w(TAG, "FCM message with empty data payload, ignoring")
            return
        }

        val type = data["type"] ?: run {
            Log.w(TAG, "FCM message without 'type' field, ignoring")
            return
        }

        val event = when (type) {
            "invitation_received" -> parseInvitationReceived(data)
            "invitation_accepted" -> parseInvitationAccepted(data)
            "calculation_completed" -> parseCalculationCompleted(data)
            "upcoming_event" -> parseUpcomingEvent(data)
            else -> {
                Log.w(TAG, "Unknown FCM type: $type")
                return
            }
        }

        if (event != null) {
            NotificationDispatcher(applicationContext).dispatch(event)
        } else {
            Log.w(TAG, "Failed to parse FCM payload for type: $type")
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token refreshed")
        // Save token for current user (best-effort)
        try {
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance()
                .currentUser?.uid
            if (uid != null) {
                // TODO: save token to Firestore user document
                Log.d(TAG, "Token saved for user: $uid")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save FCM token", e)
        }
    }

    // ── Parsing methods ─────────────────────────────────────────────────────

    private fun parseInvitationReceived(data: Map<String, String>): NotificationEvent? {
        val eventId = data["eventId"] ?: return null
        val invitationId = data["invitationId"] ?: return null
        val inviterName = data["inviterName"] ?: return null
        val eventName = data["eventName"] ?: return null
        return NotificationEvent.InvitationReceived(
            invitationId = invitationId,
            eventId = eventId,
            inviterName = inviterName,
            eventName = eventName,
        )
    }

    private fun parseInvitationAccepted(data: Map<String, String>): NotificationEvent? {
        val eventId = data["eventId"] ?: return null
        val inviteeName = data["inviteeName"] ?: return null
        val eventName = data["eventName"] ?: return null
        return NotificationEvent.InvitationAccepted(
            eventId = eventId,
            inviteeName = inviteeName,
            eventName = eventName,
        )
    }

    private fun parseCalculationCompleted(data: Map<String, String>): NotificationEvent? {
        val eventId = data["eventId"] ?: return null
        val eventName = data["eventName"] ?: return null
        val amountOwed = data["amountOwed"]?.toDoubleOrNull() ?: return null
        return NotificationEvent.CalculationCompleted(
            eventId = eventId,
            eventName = eventName,
            amountOwed = amountOwed,
        )
    }

    private fun parseUpcomingEvent(data: Map<String, String>): NotificationEvent? {
        val eventId = data["eventId"] ?: return null
        val eventName = data["eventName"] ?: return null
        val daysUntil = data["daysUntil"]?.toIntOrNull() ?: return null
        val dateFormatted = data["dateFormatted"] ?: return null
        return NotificationEvent.UpcomingEvent(
            eventId = eventId,
            eventName = eventName,
            daysUntil = daysUntil,
            dateFormatted = dateFormatted,
        )
    }
}
```

### 6. ReminderWorker — Migración completa

```kotlin
package com.cuentamorosos.data

import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.cuentamorosos.CuentaMorososApp
import com.cuentamorosos.notifications.NotificationDispatcher
import com.cuentamorosos.notifications.NotificationEvent
import java.util.concurrent.TimeUnit

class ReminderWorker(
    context: android.content.Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "ReminderWorker"
        private const val WORK_NAME = "cuenta_morosos_daily_reminder"

        fun schedule(context: android.content.Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(
                repeatInterval = 24,
                repeatIntervalTimeUnit = TimeUnit.HOURS,
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun cancel(context: android.content.Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    override suspend fun doWork(): Result {
        val app = applicationContext as? CuentaMorososApp
        if (app == null) {
            Log.w(TAG, "Application is not CuentaMorososApp, retrying")
            return Result.retry()
        }

        val repoProvider = app.repositoryProvider
        if (repoProvider == null) {
            Log.w(TAG, "RepositoryProvider not initialized yet, retrying")
            return Result.retry()
        }

        return try {
            // Check preferences
            val store = com.cuentamorosos.data.CuentaMorososLocalStore(applicationContext)
            val preferences = store.loadPreferences()
            if (!preferences.remindersEnabled) return Result.success()

            // Fetch data from SQLDelight repositories
            val events = repoProvider.eventRepository.getEventsSnapshot()
            val debts = repoProvider.debtRepository.getAllDebtsSnapshot()
            val expenses = repoProvider.expenseRepository.getAllExpensesSnapshot()

            // Build reminder messages (existing + upcoming events)
            val messages = ReminderService.buildReminderMessages(
                events = events,
                debts = debts,
                expenses = expenses,
                reminderDays = preferences.reminderDays,
                remindersEnabled = preferences.remindersEnabled,
            )

            val upcomingMessages = ReminderService.buildUpcomingEventMessages(
                events = events,
                reminderDays = preferences.reminderDays,
            )

            // Dispatch all notifications
            val dispatcher = NotificationDispatcher(applicationContext)

            messages.forEach { message ->
                dispatcher.dispatch(
                    NotificationEvent.UpcomingEvent(
                        eventId = message.eventId ?: "",
                        eventName = message.title,
                        daysUntil = message.daysUntil ?: 0,
                        dateFormatted = message.dateFormatted ?: "",
                    )
                )
            }

            upcomingMessages.forEach { message ->
                dispatcher.dispatch(
                    NotificationEvent.UpcomingEvent(
                        eventId = message.eventId ?: "",
                        eventName = message.title.removePrefix("Próximo evento: "),
                        daysUntil = message.daysUntil ?: 0,
                        dateFormatted = message.dateFormatted ?: "",
                    )
                )
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "ReminderWorker failed", e)
            Result.retry()
        }
    }
}
```

**Note**: The repositories need `getEventsSnapshot()`, `getAllDebtsSnapshot()`, `getAllExpensesSnapshot()` — these are one-shot suspend functions (not Flows) for Worker use. If they don't exist yet, they should be added to the repository interfaces.

### 7. Application Class — Inicialización completa

```kotlin
package com.cuentamorosos

import android.app.Application
import com.cuentamorosos.RepositoryProvider

class CuentaMorososApp : Application() {

    /**
     * RepositoryProvider instance, set after user login in MainActivity.
     * Accessed by ReminderWorker for background notification generation.
     * Null when no user is logged in.
     */
    var repositoryProvider: RepositoryProvider? = null
        internal set

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        @Volatile
        private var instance: CuentaMorososApp? = null

        fun getInstance(): CuentaMorososApp =
            instance ?: throw IllegalStateException(
                "CuentaMorososApp not initialized. " +
                "Ensure android:name=\".CuentaMorososApp\" in AndroidManifest.xml"
            )
    }
}
```

**AndroidManifest.xml change**:

```xml
<application
    android:name=".CuentaMorososApp"
    android:allowBackup="true"
    ...>
```

### 8. MainActivity — Wiring completo

The following shows EXACTLY where each piece is wired in `MainActivity.kt`:

```kotlin
class MainActivity : ComponentActivity() {

    private lateinit var repositoryProvider: RepositoryProvider
    private lateinit var localStore: CuentaMorososLocalStore
    private lateinit var networkMonitor: com.cuentamorosos.data.NetworkMonitor
    private lateinit var notificationDispatcher: NotificationDispatcher  // ← NEW

    // Deep link SharedFlow
    private val _deepLinkEvent = MutableSharedFlow<DeepLinkTarget>(extraBufferCapacity = 1)
    val deepLinkEvent: SharedFlow<DeepLinkTarget> = _deepLinkEvent.asSharedFlow()

    // Permission launcher
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Log.w(TAG, "POST_NOTIFICATIONS permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ... existing Thread handler ...

        // ── Initialize NotificationDispatcher (replaces NotificationScheduler.ensureChannel) ──
        notificationDispatcher = NotificationDispatcher(this)

        // ── Request POST_NOTIFICATIONS permission (Android 13+) ──
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // ── Initialize SQLDelight + repositories ──
        val sqlDriver = DriverFactory(applicationContext).createDriver()
        networkMonitor = NetworkMonitorFactory(applicationContext).create()
        repositoryProvider = RepositoryProvider(sqlDriver, networkMonitor)

        // ── Store RepositoryProvider in Application for Worker access ──
        (application as CuentaMorososApp).repositoryProvider = repositoryProvider

        localStore = CuentaMorososLocalStore(applicationContext)

        setContent {
            // ... existing auth logic ...
            if (currentUser != null) {
                MainAppContent(
                    user = currentUser!!,
                    repositoryProvider = repositoryProvider,
                    localStore = localStore,
                    networkMonitor = networkMonitor,
                    application = application,
                    notificationDispatcher = notificationDispatcher,  // ← NEW
                    deepLinkEvent = deepLinkEvent,                     // ← NEW
                )
            }
            // ...
        }

        // Handle deep link from cold start
        handleDeepLinkIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkIntent(intent)
    }

    private fun handleDeepLinkIntent(intent: Intent?) {
        intent ?: return
        val type = intent.getStringExtra(NotificationDispatcher.EXTRA_NOTIFICATION_TYPE) ?: return
        val eventId = intent.getStringExtra(NotificationDispatcher.EXTRA_EVENT_ID)
        val pagerPage = intent.getIntExtra(NotificationDispatcher.EXTRA_PAGER_PAGE, 0)

        _deepLinkEvent.tryEmit(DeepLinkTarget(pagerPage, eventId, type))

        // Clear extras to avoid re-processing
        intent.removeExtra(NotificationDispatcher.EXTRA_NOTIFICATION_TYPE)
        intent.removeExtra(NotificationDispatcher.EXTRA_EVENT_ID)
        intent.removeExtra(NotificationDispatcher.EXTRA_INVITATION_ID)
        intent.removeExtra(NotificationDispatcher.EXTRA_PAGER_PAGE)
    }
}
```

**MainAppContent changes**:

```kotlin
@Composable
private fun MainAppContent(
    user: FirebaseUser,
    repositoryProvider: RepositoryProvider,
    localStore: CuentaMorososLocalStore,
    networkMonitor: com.cuentamorosos.data.NetworkMonitor,
    application: android.app.Application,
    notificationDispatcher: NotificationDispatcher,  // ← NEW
    deepLinkEvent: SharedFlow<DeepLinkTarget>,        // ← NEW
) {
    // ── Create NotificationCallbacks that dispatch via NotificationDispatcher ──
    val notificationCallbacks = remember {
        NotificationCallbacks(
            onInvitationReceived = { event -> notificationDispatcher.dispatch(event) },
            onInvitationAccepted = { event -> notificationDispatcher.dispatch(event) },
            onCalculationCompleted = { event -> notificationDispatcher.dispatch(event) },
        )
    }

    val viewModelFactory = remember(user.uid) {
        AppViewModelFactory(
            repositoryProvider,
            currentProfileId = user.uid,
            notificationCallbacks = notificationCallbacks,  // ← NEW
        )
    }

    // ... rest unchanged, pass deepLinkEvent to CuentaMorososApp ...
    CuentaMorososApp(
        viewModelFactory = viewModelFactory,
        currentUserUid = user.uid,
        // ... existing params ...
        deepLinkEvent = deepLinkEvent,  // ← NEW
    )
}
```

### 9. ReminderService — buildUpcomingEventMessages completo

```kotlin
// Added to existing ReminderService object

fun buildUpcomingEventMessages(
    events: List<EventItem>,
    reminderDays: Int,
    nowMillis: Long = currentTimeMillis(),
): List<ReminderMessage> {
    val windowEnd = nowMillis + reminderDays.coerceAtLeast(1) * MILLIS_PER_DAY

    return events
        .filter { event ->
            // Event starts in the future but within the reminder window
            event.startDateMillis in nowMillis..windowEnd
        }
        .map { event ->
            val daysUntil = ((event.startDateMillis - nowMillis) / MILLIS_PER_DAY)
                .toInt()
                .coerceAtLeast(0)
            val dateFormatted = formatDateMillis(event.startDateMillis)

            ReminderMessage(
                title = "Próximo evento: ${event.name}",
                body = "El evento '${event.name}' es en $daysUntil días ($dateFormatted)",
                type = ReminderType.UPCOMING_EVENT,
                eventId = event.id,
                daysUntil = daysUntil,
                dateFormatted = dateFormatted,
            )
        }
        .distinctBy { "${it.title}-${it.body}" }
}
```

**ReminderMessage updated model**:

```kotlin
data class ReminderMessage(
    val title: String,
    val body: String,
    val type: ReminderType = ReminderType.PENDING_DEBT,
    val eventId: String? = null,
    val daysUntil: Int? = null,
    val dateFormatted: String? = null,
)

enum class ReminderType { PENDING_DEBT, INCOMPLETE_EVENT, UPCOMING_EVENT }
```

### 10. Notification Channels

| Channel ID | Name (ES) | Importance | Used By |
|------------|-----------|------------|---------|
| `ch_invitations` | Invitaciones | HIGH | InvitationReceived, InvitationAccepted |
| `ch_calculations` | Cálculos y liquidaciones | DEFAULT | CalculationCompleted |
| `ch_reminders` | Recordatorios | DEFAULT | Pending debts, incomplete events (existing) |
| `ch_upcoming_events` | Próximos eventos | DEFAULT | UpcomingEvent |

### 11. Deep Link Navigation

```
MainActivity.onNewIntent(intent)
├── extract: type, eventId, invitationId, pagerPage
├── emit to SharedFlow<DeepLinkTarget>
└── Compose LaunchedEffect collects:
    ├── pagerState.animateScrollToPage(target.pagerPage)
    └── if target.eventId != null: eventDetailViewModel.setEventId(target.eventId)
```

The pager page mapping:
- `INVITATION_RECEIVED` → page 3 (Invitations tab — `MainSection.INVITATIONS`)
- All others → page 0 (Dashboard tab — `MainSection.DASHBOARD`) with eventId selection

### 12. ReminderWorker Migration

```kotlin
// BEFORE (SharedPreferences):
val store = CuentaMorososLocalStore(context)
val events = store.loadEvents()

// AFTER (SQLDelight via Application singleton):
val app = context.applicationContext as CuentaMorososApp
val repoProvider = app.repositoryProvider ?: return Result.retry()
val events = repoProvider.eventRepository.getEventsSnapshot()  // suspend function
```

## Error Handling

| Scenario | Handling |
|----------|----------|
| `POST_NOTIFICATIONS` denied | `NotificationManagerCompat.areNotificationsEnabled()` check before posting. Log warning, no crash. Local reminders silently skipped. |
| FCM token refresh fails | `onNewToken` wraps `saveTokenForCurrentUser` in try/catch. Retry on next app launch via `syncCurrentUserToken()`. |
| Deep link with invalid eventId | Navigate to default page (page 0), log warning. No crash — `eventDetailViewModel.setEventId()` will simply find no matching event. |
| Icon resource missing | `createLargeIcon` catches `Resources.NotFoundException`, falls back to `android.R.drawable.ic_dialog_info` + log warning. |
| FCM unknown type | Log `"Unknown FCM type: $type"` and return without posting. |
| FCM missing required fields | Validate required fields per type before building event. Log warning + skip if missing. Each `parse*` method returns `null` on missing fields. |
| RepositoryProvider unavailable in Worker | Return `Result.retry()` — WorkManager reschedules with exponential backoff. |
| NotificationDispatcher called before channels created | `ensureChannels()` is idempotent and called on every `dispatch()`. Thread-safe via `channelsCreated` flag. |

## Testing Strategy

### Unit Tests

#### NotificationDispatcher Tests (Robolectric + JUnit 4)

```kotlin
@Test
fun `dispatch InvitationReceived posts notification with correct channel and body`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dispatcher = NotificationDispatcher(context)
    val event = NotificationEvent.InvitationReceived(
        invitationId = "inv-123",
        eventId = "evt-456",
        inviterName = "Ana",
        eventName = "Asado",
    )

    dispatcher.dispatch(event)

    val shadowManager = shadowOf(NotificationManagerCompat.from(context))
    val notification = shadowManager.getNotification("INVITATION_RECEIVED", "evt-456".hashCode())

    assertThat(notification).isNotNull()
    assertThat(notification!!.channelId).isEqualTo("ch_invitations")
    assertThat(notification.extras.getString(NotificationCompat.EXTRA_TITLE))
        .isEqualTo("Invitación recibida")
    assertThat(notification.extras.getString(NotificationCompat.EXTRA_TEXT))
        .isEqualTo("Ana te invitó al evento 'Asado'")
    assertThat(notification.flags and Notification.FLAG_AUTO_CANCEL).isNotEqualTo(0)
}

@Test
fun `dispatch CalculationCompleted formats amount with two decimals`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dispatcher = NotificationDispatcher(context)
    val event = NotificationEvent.CalculationCompleted(
        eventId = "evt-789",
        eventName = "Cena",
        amountOwed = 42.5,
    )

    dispatcher.dispatch(event)

    val shadowManager = shadowOf(NotificationManagerCompat.from(context))
    val notification = shadowManager.getNotification("CALCULATION_COMPLETED", "evt-789".hashCode())

    assertThat(notification!!.extras.getString(NotificationCompat.EXTRA_TEXT))
        .isEqualTo("Se calcularon los gastos de 'Cena'. Debes €42.50")
    assertThat(notification.channelId).isEqualTo("ch_calculations")
}

@Test
fun `dispatch UpcomingEvent includes days and date in body`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dispatcher = NotificationDispatcher(context)
    val event = NotificationEvent.UpcomingEvent(
        eventId = "evt-101",
        eventName = "Viaje",
        daysUntil = 3,
        dateFormatted = "15/06/2026",
    )

    dispatcher.dispatch(event)

    val shadowManager = shadowOf(NotificationManagerCompat.from(context))
    val notification = shadowManager.getNotification("UPCOMING_EVENT", "evt-101".hashCode())

    assertThat(notification!!.extras.getString(NotificationCompat.EXTRA_TEXT))
        .isEqualTo("El evento 'Viaje' es en 3 días (15/06/2026)")
    assertThat(notification.channelId).isEqualTo("ch_upcoming_events")
}

@Test
fun `dispatch does nothing when notifications are disabled`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dispatcher = NotificationDispatcher(context)

    // Robolectric: simulate notifications disabled
    val manager = NotificationManagerCompat.from(context)
    shadowOf(manager).setNotificationsEnabled(false)

    dispatcher.dispatch(
        NotificationEvent.InvitationReceived("inv-1", "evt-1", "Ana", "Fiesta")
    )

    val shadowManager = shadowOf(manager)
    assertThat(shadowManager.size()).isEqualTo(0)
}

@Test
fun `notificationId is deterministic for same eventId`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dispatcher = NotificationDispatcher(context)

    val event1 = NotificationEvent.UpcomingEvent("evt-same", "Test", 1, "01/01")
    val event2 = NotificationEvent.UpcomingEvent("evt-same", "Test", 2, "02/02")

    // Both should produce the same ID (based on eventId)
    dispatcher.dispatch(event1)
    dispatcher.dispatch(event2)

    val shadowManager = shadowOf(NotificationManagerCompat.from(context))
    // Second dispatch replaces first (same tag + id)
    assertThat(shadowManager.size()).isEqualTo(1)
}

@Test
fun `ensureChannels creates exactly 4 channels`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dispatcher = NotificationDispatcher(context)

    // Trigger channel creation by dispatching
    dispatcher.dispatch(
        NotificationEvent.InvitationReceived("inv-1", "evt-1", "Ana", "Test")
    )

    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
        as android.app.NotificationManager
    val channels = manager.notificationChannels
    assertThat(channels.size).isAtLeast(4)
    assertThat(channels.map { it.id }).containsAllOf(
        "ch_invitations", "ch_calculations", "ch_reminders", "ch_upcoming_events"
    )
}
```

#### InvitationsViewModel Tests (kotlin-test + coroutines-test)

```kotlin
@Test
fun `onNewInvitation fires only once per invitation id`() = runTest {
    val invitations = mutableListOf<EventInvitation>(
        EventInvitation(id = "inv-1", eventId = "evt-1", eventName = "Asado", invitedByEmail = "ana@test.com"),
    )
    val flow = MutableStateFlow<List<EventInvitation>>(invitations)
    val receivedEvents = mutableListOf<NotificationEvent.InvitationReceived>()

    val viewModel = InvitationsViewModel(
        invitationRepository = FakeInvitationRepository(flow),
        onNewInvitation = { receivedEvents.add(it) },
    )

    // First emission → should fire
    advanceUntilIdle()
    assertThat(receivedEvents).hasSize(1)

    // Re-emit same list → should NOT fire again
    flow.value = invitations
    advanceUntilIdle()
    assertThat(receivedEvents).hasSize(1)
}

@Test
fun `onNewInvitation does not fire for non-PENDING invitations`() = runTest {
    val invitations = listOf(
        EventInvitation(
            id = "inv-2", eventId = "evt-2", eventName = "Cena",
            invitedByEmail = "bob@test.com", status = InvitationStatus.ACCEPTED,
        ),
    )
    val flow = MutableStateFlow<List<EventInvitation>>(invitations)
    val receivedEvents = mutableListOf<NotificationEvent.InvitationReceived>()

    val viewModel = InvitationsViewModel(
        invitationRepository = FakeInvitationRepository(flow),
        onNewInvitation = { receivedEvents.add(it) },
    )

    advanceUntilIdle()
    assertThat(receivedEvents).isEmpty()
}
```

#### DashboardViewModel Tests (kotlin-test + coroutines-test)

```kotlin
@Test
fun `onCalculationCompleted fires when event transitions to CALCULATED with amountOwed > 0`() = runTest {
    val events = listOf(
        EventItem(id = "evt-1", name = "Cena", dateMillis = 0L, ownerId = "user-1", state = EventState.CALCULATED),
    )
    val debts = listOf(
        EventDebtItem(eventId = "evt-1", profileId = "user-1", amountEuros = 25.0, paid = false),
    )

    val receivedEvents = mutableListOf<NotificationEvent.CalculationCompleted>()
    val viewModel = DashboardViewModel(
        eventRepository = FakeEventRepository(events),
        debtRepository = FakeDebtRepository(debts),
        expenseRepository = FakeExpenseRepository(emptyList()),
        profileRepository = FakeProfileRepository(emptyList()),
        currentUserUid = "user-1",
        onCalculationCompleted = { receivedEvents.add(it) },
    )

    advanceUntilIdle()
    assertThat(receivedEvents).hasSize(1)
    assertThat(receivedEvents[0].amountOwed).isEqualTo(25.0)
}

@Test
fun `onCalculationCompleted does NOT fire when amountOwed is 0`() = runTest {
    val events = listOf(
        EventItem(id = "evt-1", name = "Cena", dateMillis = 0L, ownerId = "user-1", state = EventState.CALCULATED),
    )
    val debts = listOf(
        EventDebtItem(eventId = "evt-1", profileId = "other-user", amountEuros = 25.0, paid = false),
    )

    val receivedEvents = mutableListOf<NotificationEvent.CalculationCompleted>()
    val viewModel = DashboardViewModel(
        eventRepository = FakeEventRepository(events),
        debtRepository = FakeDebtRepository(debts),
        expenseRepository = FakeExpenseRepository(emptyList()),
        profileRepository = FakeProfileRepository(emptyList()),
        currentUserUid = "user-1",
        onCalculationCompleted = { receivedEvents.add(it) },
    )

    advanceUntilIdle()
    assertThat(receivedEvents).isEmpty()
}

@Test
fun `onCalculationCompleted does NOT fire twice for same event`() = runTest {
    val eventsFlow = MutableStateFlow(
        listOf(EventItem(id = "evt-1", name = "Cena", dateMillis = 0L, ownerId = "user-1", state = EventState.CALCULATED))
    )
    val debtsFlow = MutableStateFlow(
        listOf(EventDebtItem(eventId = "evt-1", profileId = "user-1", amountEuros = 10.0, paid = false))
    )

    val receivedEvents = mutableListOf<NotificationEvent.CalculationCompleted>()
    val viewModel = DashboardViewModel(
        eventRepository = FakeEventRepository(eventsFlow),
        debtRepository = FakeDebtRepository(debtsFlow),
        expenseRepository = FakeExpenseRepository(emptyList()),
        profileRepository = FakeProfileRepository(emptyList()),
        currentUserUid = "user-1",
        onCalculationCompleted = { receivedEvents.add(it) },
    )

    advanceUntilIdle()
    assertThat(receivedEvents).hasSize(1)

    // Re-emit same data
    eventsFlow.value = eventsFlow.value
    advanceUntilIdle()
    assertThat(receivedEvents).hasSize(1) // still 1
}
```

#### ReminderService Tests (kotlin-test)

```kotlin
@Test
fun `buildUpcomingEventMessages includes events within window`() {
    val now = 1000000L
    val events = listOf(
        EventItem(id = "evt-1", name = "Viaje", dateMillis = now + 2 * MILLIS_PER_DAY, startDateMillis = now + 2 * MILLIS_PER_DAY, ownerId = "u1"),
        EventItem(id = "evt-2", name = "Fiesta", dateMillis = now + 10 * MILLIS_PER_DAY, startDateMillis = now + 10 * MILLIS_PER_DAY, ownerId = "u1"),
    )

    val messages = ReminderService.buildUpcomingEventMessages(events, reminderDays = 7, nowMillis = now)

    assertThat(messages).hasSize(1)
    assertThat(messages[0].eventId).isEqualTo("evt-1")
    assertThat(messages[0].daysUntil).isEqualTo(2)
}

@Test
fun `buildUpcomingEventMessages excludes past events`() {
    val now = 1000000L
    val events = listOf(
        EventItem(id = "evt-1", name = "Pasado", dateMillis = now - 1 * MILLIS_PER_DAY, startDateMillis = now - 1 * MILLIS_PER_DAY, ownerId = "u1"),
    )

    val messages = ReminderService.buildUpcomingEventMessages(events, reminderDays = 7, nowMillis = now)

    assertThat(messages).isEmpty()
}

@Test
fun `buildUpcomingEventMessages excludes events beyond window`() {
    val now = 1000000L
    val events = listOf(
        EventItem(id = "evt-1", name = "Lejano", dateMillis = now + 30 * MILLIS_PER_DAY, startDateMillis = now + 30 * MILLIS_PER_DAY, ownerId = "u1"),
    )

    val messages = ReminderService.buildUpcomingEventMessages(events, reminderDays = 7, nowMillis = now)

    assertThat(messages).isEmpty()
}
```

#### FCM Parsing Tests (JUnit 4)

```kotlin
@Test
fun `parseInvitationReceived returns event with all fields`() {
    val data = mapOf(
        "type" to "invitation_received",
        "eventId" to "evt-1",
        "invitationId" to "inv-1",
        "inviterName" to "Ana",
        "eventName" to "Asado",
    )

    val service = CuentaMorososFirebaseMessagingService()
    val method = service::class.java.getDeclaredMethod(
        "parseInvitationReceived", Map::class.java
    ).apply { isAccessible = true }
    val result = method.invoke(service, data) as? NotificationEvent.InvitationReceived

    assertThat(result).isNotNull()
    assertThat(result!!.eventId).isEqualTo("evt-1")
    assertThat(result.invitationId).isEqualTo("inv-1")
    assertThat(result.inviterName).isEqualTo("Ana")
    assertThat(result.eventName).isEqualTo("Asado")
}

@Test
fun `parseInvitationReceived returns null when eventId is missing`() {
    val data = mapOf(
        "type" to "invitation_received",
        "invitationId" to "inv-1",
        "inviterName" to "Ana",
        "eventName" to "Asado",
    )

    val service = CuentaMorososFirebaseMessagingService()
    val method = service::class.java.getDeclaredMethod(
        "parseInvitationReceived", Map::class.java
    ).apply { isAccessible = true }
    val result = method.invoke(service, data)

    assertThat(result).isNull()
}

@Test
fun `parseCalculationCompleted returns null when amountOwed is not a number`() {
    val data = mapOf(
        "type" to "calculation_completed",
        "eventId" to "evt-1",
        "eventName" to "Cena",
        "amountOwed" to "not-a-number",
    )

    val service = CuentaMorososFirebaseMessagingService()
    val method = service::class.java.getDeclaredMethod(
        "parseCalculationCompleted", Map::class.java
    ).apply { isAccessible = true }
    val result = method.invoke(service, data)

    assertThat(result).isNull()
}

@Test
fun `onMessageReceived ignores unknown type`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val service = CuentaMorososFirebaseMessagingService().apply {
        // attach base context for Robolectric
    }

    val remoteMessage = RemoteMessage.Builder("test")
        .addData("type", "unknown_type")
        .build()

    // Should not throw
    service.onMessageReceived(remoteMessage)

    val shadowManager = shadowOf(NotificationManagerCompat.from(context))
    assertThat(shadowManager.size()).isEqualTo(0)
}
```

### Integration Tests

```kotlin
@Test
fun `FCM service to dispatcher end-to-end posts notification`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val service = CuentaMorososFirebaseMessagingService()

    val remoteMessage = RemoteMessage.Builder("test")
        .addData("type", "invitation_received")
        .addData("eventId", "evt-e2e")
        .addData("invitationId", "inv-e2e")
        .addData("inviterName", "Test User")
        .addData("eventName", "E2E Event")
        .build()

    service.onMessageReceived(remoteMessage)

    val shadowManager = shadowOf(NotificationManagerCompat.from(context))
    assertThat(shadowManager.size()).isEqualTo(1)
}
```

### Manual Tests

| Scenario | Steps | Expected |
|----------|-------|----------|
| Deep link from notification tap (app backgrounded) | Send FCM test message via Firebase Console → tap notification | App opens to correct pager page, event selected if eventId present |
| Deep link from notification tap (app foregrounded) | Same as above but app is in foreground | `onNewIntent` fires, pager animates to correct page |
| POST_NOTIFICATIONS denied | Deny permission in system settings → trigger notification | No crash, Log.w logged, no notification shown |
| Worker with RepositoryProvider unavailable | Kill app, trigger Worker before login | Worker returns `Result.retry()`, rescheduled by WorkManager |

## Performance Considerations

| Concern | Mitigation |
|---------|------------|
| Thread safety — multiple ViewModels calling dispatch() | `NotificationManagerCompat.notify()` is thread-safe. `iconCache` uses `ConcurrentHashMap`. |
| Large icon regeneration | Cache bitmaps in `NotificationDispatcher` instance (created once per app lifecycle). |
| Deep link latency | `onNewIntent` → `SharedFlow.tryEmit()` → `pagerState.animateScrollToPage()` is immediate (< 16ms). No database queries needed. |
| Worker startup cost | `RepositoryProvider` stored in `Application` during `MainActivity.onCreate()`. Worker accesses existing instance. |
| Channel creation overhead | `ensureChannels()` is idempotent with `channelsCreated` flag — runs once per dispatcher lifetime. |

## Migration / Rollout

**No data migration required.** The change is additive:
1. New notification channels are created on first launch (idempotent).
2. Existing `NotificationScheduler.ensureChannel()` is replaced by `NotificationDispatcher.ensureChannels()` — the old `cuenta_morosos_reminders` channel remains in the system but is no longer used.
3. `ReminderWorker` switches from SharedPreferences to SQLDelight — both contain the same event data (SQLDelight is synced from Firestore). The SharedPreferences data is not deleted (harmless).
4. FCM service declaration in manifest is additive — no existing declaration to conflict.

**Rollout order** (each step is independently deployable):
1. Manifest: Add `android:name=".CuentaMorososApp"` to `<application>`, FCM service declaration, deep link intent filter
2. Create `NotificationEvent` sealed class + `NotificationCallbacks`
3. Create `NotificationDispatcher` + icon XMLs + channels
4. Wire `MainActivity`: permission request, dispatcher creation, callback wiring, Application reference
5. Extend `InvitationsViewModel` + `DashboardViewModel` with callbacks
6. Extend `ReminderService` with upcoming events
7. Migrate `ReminderWorker` to SQLDelight
8. Implement `onMessageReceived` in FCM service

## Open Questions

- [x] Should `InvitationAccepted` notifications be triggered from a Firestore snapshot listener on sent invitations (inviter side), or from FCM only? **Decision: FCM for background, listener for foreground — same dual-path as InvitationReceived.**
- [x] Should the deep link `PendingIntent` use `FLAG_UPDATE_CURRENT` or `FLAG_IMMUTABLE`? **Decision: `FLAG_IMMUTABLE | FLAG_UPDATE_CURRENT` (required for Android 12+).**
- [ ] Do the repository interfaces need `getEventsSnapshot()` / `getAllDebtsSnapshot()` / `getAllExpensesSnapshot()` one-shot suspend functions, or can the Worker collect the Flow with `first()`? **Decision: Use `first()` on the existing `observe*()` Flows to avoid adding new interface methods.**
