package com.cuentamorosos.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.cuentamorosos.MainActivity
import com.cuentamorosos.R
import com.cuentamorosos.data.CuentaMorososLocalStore
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementación Android de [NotificationPresenter].
 *
 * Los textos, canales, huellas de deduplicación y destinos de deep link viven en
 * [NotificationContentFactory] (commonMain) y los comparte con iOS. Aquí solo
 * queda lo que es irreduciblemente Android: `NotificationCompat`, los canales
 * del sistema, los `PendingIntent` y el dibujado del icono grande.
 */
class NotificationDispatcher(
    private val context: Context,
    localStore: CuentaMorososLocalStore? = null,
) : NotificationPresenter {

    companion object {
        private const val TAG = "NotificationDispatcher"

        // Claves de extras de los Intent
        const val EXTRA_NOTIFICATION_TYPE = "extra_notification_type"
        const val EXTRA_EVENT_ID = "extra_event_id"
        const val EXTRA_INVITATION_ID = "extra_invitation_id"
        const val EXTRA_PAGER_PAGE = "extra_pager_page"

        /** Delegado a [NotificationContentFactory]; se mantiene por compatibilidad. */
        fun fingerprintFor(event: NotificationEvent): String =
            NotificationContentFactory.fingerprintFor(event)
    }

    private val coordinator = NotificationCoordinator(
        presenter = this,
        dedupStore = localStore?.let { LocalStoreDedupStore(it) },
    )

    private val iconCache = ConcurrentHashMap<NotificationType, Bitmap>(4)
    private var channelsCreated = false

    // ── API pública ─────────────────────────────────────────────────────────

    /** Emite la notificación de [event] (deduplicada). */
    fun dispatch(event: NotificationEvent) {
        val sent = coordinator.dispatch(event)
        if (!sent) Log.d(TAG, "Notificación omitida: ${event::class.simpleName}")
    }

    // ── NotificationPresenter ───────────────────────────────────────────────

    override fun areNotificationsEnabled(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    override fun ensureChannels() {
        if (channelsCreated) return
        val manager = NotificationManagerCompat.from(context)

        val channels = NotificationChannel.entries.map { channel ->
            androidx.core.app.NotificationChannelCompat.Builder(
                channel.id,
                when (channel.importance) {
                    NotificationImportance.HIGH -> NotificationManagerCompat.IMPORTANCE_HIGH
                    NotificationImportance.DEFAULT -> NotificationManagerCompat.IMPORTANCE_DEFAULT
                },
            )
                .setName(channel.displayName)
                .setDescription(channel.description)
                .build()
        }

        manager.createNotificationChannelsCompat(channels)
        channelsCreated = true
    }

    override fun present(content: NotificationContent) {
        val notification = buildNotification(content)
        NotificationManagerCompat.from(context).notify(content.tag, content.id, notification)
        Log.d(TAG, "Notificación emitida: tag=${content.tag}, id=${content.id}")
    }

    // ── Construcción de la Notification ─────────────────────────────────────

    private fun buildNotification(content: NotificationContent): Notification =
        NotificationCompat.Builder(context, content.channel.id)
            .setSmallIcon(smallIconResFor(content.type))
            .setLargeIcon(largeIconFor(content.type))
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setContentIntent(createContentIntent(content))
            .setAutoCancel(true)
            .setPriority(priorityFor(content.channel.importance))
            .apply { addActions(content, this) }
            .build()

    private fun smallIconResFor(type: NotificationType): Int = when (type) {
        NotificationType.INVITATION, NotificationType.INVITATION_ACCEPTED ->
            R.drawable.ic_notification_invitation
        NotificationType.CALCULATION, NotificationType.PAYMENT_REMINDER ->
            R.drawable.ic_notification_calc
    }

    private fun priorityFor(importance: NotificationImportance): Int = when (importance) {
        NotificationImportance.HIGH -> NotificationCompat.PRIORITY_HIGH
        NotificationImportance.DEFAULT -> NotificationCompat.PRIORITY_DEFAULT
    }

    // ── Acciones ────────────────────────────────────────────────────────────

    private fun addActions(content: NotificationContent, builder: NotificationCompat.Builder) {
        content.actions.forEachIndexed { index, action ->
            when (action.id) {
                NotificationContentFactory.ACTION_ACCEPT_INVITATION,
                NotificationContentFactory.ACTION_REJECT_INVITATION -> {
                    val invitationId = content.invitationId ?: return@forEachIndexed
                    val intent = Intent(action.id).apply {
                        setPackage(context.packageName)
                        putExtra(EXTRA_EVENT_ID, content.deepLink.eventId)
                        putExtra(EXTRA_INVITATION_ID, invitationId)
                    }
                    val pending = PendingIntent.getBroadcast(
                        context,
                        invitationId.hashCode() + index,
                        intent,
                        pendingIntentFlags(),
                    )
                    val icon =
                        if (action.id == NotificationContentFactory.ACTION_ACCEPT_INVITATION) {
                            R.drawable.ic_notification_check
                        } else {
                            android.R.drawable.ic_menu_close_clear_cancel
                        }
                    builder.addAction(icon, action.label, pending)
                }

                else -> builder.addAction(
                    android.R.drawable.ic_menu_view,
                    action.label,
                    createContentIntent(content),
                )
            }
        }
    }

    // ── Content Intent (deep link) ──────────────────────────────────────────

    private fun createContentIntent(content: NotificationContent): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTIFICATION_TYPE, content.deepLink.notificationType)
            putExtra(EXTRA_PAGER_PAGE, content.deepLink.pagerPage)
            content.deepLink.eventId?.let { putExtra(EXTRA_EVENT_ID, it) }
            content.invitationId?.let { putExtra(EXTRA_INVITATION_ID, it) }
        }
        return PendingIntent.getActivity(context, content.id, intent, pendingIntentFlags())
    }

    private fun pendingIntentFlags(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

    // ── Icono grande ────────────────────────────────────────────────────────

    private fun largeIconFor(type: NotificationType): Bitmap = iconCache.getOrPut(type) {
        if (type == NotificationType.PAYMENT_REMINDER) {
            BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
        } else {
            createLargeIcon(smallIconResFor(type), BADGE_BACKGROUND)
        }
    }

    private fun createLargeIcon(iconRes: Int, bgColor: Int): Bitmap {
        val sizePx = (48 * context.resources.displayMetrics.density).toInt()
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, paint)

        val drawable = ContextCompat.getDrawable(context, iconRes) ?: return bitmap
        drawable.setTint(BADGE_TINT)
        val iconSize = (sizePx * 0.6).toInt()
        val offset = (sizePx - iconSize) / 2
        drawable.setBounds(offset, offset, offset + iconSize, offset + iconSize)
        drawable.draw(canvas)

        return bitmap
    }
}

/** NeoFintech onSurface (negro) de fondo, verde neón para el icono. */
private const val BADGE_BACKGROUND = 0xFF191C1D.toInt()
private const val BADGE_TINT = 0xFF39FF14.toInt()

/** Adapta [CuentaMorososLocalStore] al puerto [NotificationDedupStore]. */
private class LocalStoreDedupStore(
    private val store: CuentaMorososLocalStore,
) : NotificationDedupStore {
    override fun hasBeenSent(fingerprint: String) = store.hasNotificationBeenSent(fingerprint)
    override fun recordSent(fingerprint: String) = store.recordNotificationSent(fingerprint)
}
