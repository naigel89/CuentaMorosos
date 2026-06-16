package com.cuentamorosos.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationScheduler {
    private const val CHANNEL_ID = "cuenta_morosos_reminders"
    private const val CHANNEL_NAME = "Recordatorios de CuentaMorosos"
    private const val NOTIFICATION_TAG = "cuenta_morosos_reminder"

    /**
     * Computes a deterministic fingerprint for a [ReminderMessage].
     * Format: "reminder:{eventId}:{dateFormatted}:{type}"
     *
     * Same eventId + same dateFormatted + same type → same fingerprint.
     */
    fun fingerprintFor(message: ReminderMessage): String {
        val eventId = message.eventId ?: ""
        val dateFormatted = message.dateFormatted ?: ""
        return "reminder:$eventId:$dateFormatted:${message.type.name}"
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Avisos locales sobre pagos pendientes y eventos incompletos"
        }
        manager.createNotificationChannel(channel)
    }

    fun channelId(): String = CHANNEL_ID

    /**
     * Posts reminder notifications, filtering out any that have already been sent.
     *
     * @param context Android context for notification management
     * @param messages The list of [ReminderMessage] reminders to potentially post
     * @param localStore Optional dedup registry; when provided, reminders whose
     *        fingerprint is already registered are skipped. Unsent reminders are
     *        posted and their fingerprints recorded. When null (backward compat),
     *        all reminders are posted without dedup checks.
     */
    fun postReminders(
        context: Context,
        messages: List<ReminderMessage>,
        localStore: CuentaMorososLocalStore? = null,
    ) {
        if (messages.isEmpty()) return

        ensureChannel(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
            as? NotificationManager ?: return

        // Filter: skip already-sent reminders when store is available
        val unsent = if (localStore != null) {
            messages.filter { msg ->
                val fingerprint = fingerprintFor(msg)
                !localStore.hasNotificationBeenSent(fingerprint)
            }
        } else {
            messages
        }

        if (unsent.isEmpty()) return

        unsent.forEachIndexed { index, message ->
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(message.title)
                .setContentText(message.body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            manager.notify(NOTIFICATION_TAG, index, notification)

            // Record fingerprint so future calls skip this reminder
            localStore?.recordNotificationSent(fingerprintFor(message))
        }
    }
}
