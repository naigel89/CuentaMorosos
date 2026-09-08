package com.cuentamorosos.data

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.cuentamorosos.notifications.ReminderScheduler
import java.util.concurrent.TimeUnit

/**
 * Implementación Android de [ReminderScheduler]: un trabajo periódico único de
 * WorkManager que se ejecuta una vez cada 24 horas.
 *
 * iOS no tiene equivalente garantizado; su implementación tendrá que apoyarse en
 * `BGTaskScheduler` (best-effort) o en notificaciones locales programadas.
 */
class WorkManagerReminderScheduler(
    private val context: Context,
) : ReminderScheduler {

    override fun schedule() {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(
            repeatInterval = REPEAT_INTERVAL_HOURS,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            // UPDATE, no KEEP: al reprogramar queremos que gane la configuración nueva.
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    override fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    companion object {
        private const val WORK_NAME = "cuenta_morosos_daily_reminder"
        private const val REPEAT_INTERVAL_HOURS = 24L
    }
}
