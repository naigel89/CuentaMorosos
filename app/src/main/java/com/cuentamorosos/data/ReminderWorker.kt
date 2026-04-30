package com.cuentamorosos.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Worker que se ejecuta periódicamente en segundo plano para generar y publicar
 * notificaciones de recordatorio cuando la app está cerrada.
 *
 * Se programa con WorkManager una vez al día y solo lanza notificaciones
 * si los recordatorios están habilitados en las preferencias del usuario.
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val store = CuentaMorososLocalStore(context)
        val preferences = store.loadPreferences()

        if (!preferences.remindersEnabled) return Result.success()

        val messages = ReminderService.buildReminderMessages(
            events = store.loadEvents(),
            debts = store.loadDebts(),
            expenses = store.loadExpenses(),
            reminderDays = preferences.reminderDays,
            remindersEnabled = preferences.remindersEnabled,
        )

        NotificationScheduler.postReminders(context, messages)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "cuenta_morosos_daily_reminder"

        /**
         * Programa el worker periódico (1 vez cada 24 horas).
         * Si ya existe, lo reemplaza para respetar la configuración actualizada.
         */
        fun schedule(context: Context) {
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

        /** Cancela el worker periódico (cuando el usuario desactiva los recordatorios). */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
