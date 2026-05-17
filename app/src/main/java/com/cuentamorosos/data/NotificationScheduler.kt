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

    fun postReminders(context: Context, messages: List<ReminderMessage>) {
        if (messages.isEmpty()) return

        ensureChannel(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
            as? NotificationManager ?: return

        messages.forEachIndexed { index, message ->
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(message.title)
                .setContentText(message.body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            manager.notify(NOTIFICATION_TAG, index, notification)
        }
    }
}
