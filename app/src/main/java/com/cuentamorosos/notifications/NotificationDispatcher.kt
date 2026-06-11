package com.cuentamorosos.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
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

    fun ensureChannels() {
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
    ): Intent = Intent(action).apply {
        setPackage(context.packageName)
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
        NotificationType.INVITATION -> 0xFF191C1D.toInt()       // NeoFintech onSurface (black)
        NotificationType.INVITATION_ACCEPTED -> 0xFF191C1D.toInt() // same black
        NotificationType.CALCULATION -> 0xFF191C1D.toInt()          // same black
        NotificationType.UPCOMING_EVENT -> 0xFF191C1D.toInt()       // same black
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

        // Draw vector icon centered, tinted neon green
        val drawable = ContextCompat.getDrawable(context, iconRes) ?: return bitmap
        drawable.setTint(0xFF39FF14.toInt())
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
