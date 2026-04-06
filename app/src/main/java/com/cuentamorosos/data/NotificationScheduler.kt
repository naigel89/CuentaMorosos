package com.cuentamorosos.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationScheduler {
    private const val CHANNEL_ID = "cuenta_morosos_reminders"
    private const val CHANNEL_NAME = "Recordatorios de CuentaMorosos"

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
}
