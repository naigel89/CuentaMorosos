package com.cuentamorosos.data

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.cuentamorosos.CuentaMorososApp
import com.cuentamorosos.notifications.NotificationDispatcher
import com.cuentamorosos.notifications.NotificationEvent
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
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

    companion object {
        private const val TAG = "ReminderWorker"
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
            val store = CuentaMorososLocalStore(applicationContext)
            val preferences = store.loadPreferences()
            if (!preferences.remindersEnabled) return Result.success()

            // Fetch data from SQLDelight repositories using first() on Flows
            val events = repoProvider.eventRepository.observeEvents().first()
            val debts = repoProvider.debtRepository.observeAllDebts().first()
            val expenses = repoProvider.expenseRepository.observeAllExpenses().first()

            // First-launch migration: seed fingerprints for already-calculated events
            store.seedDedupMigration(events)

            // Fetch live data via repositories
            val profiles = repoProvider.profileRepository.observeProfiles().first()
            val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

            // Build payment reminder messages per unpaid debt
            val messages = ReminderService.buildReminderMessages(
                events = events,
                debts = debts,
                expenses = expenses,
                profiles = profiles,
                currentUserUid = currentUserUid,
                reminderDays = preferences.reminderDays,
                remindersEnabled = preferences.remindersEnabled,
            )

            // Dispatch all notifications with dedup guard
            val dispatcher = NotificationDispatcher(applicationContext, localStore = store)

            messages.forEach { message ->
                dispatcher.dispatch(
                    NotificationEvent.PaymentReminder(
                        eventId = message.eventId ?: "",
                        profileName = message.profileName ?: "",
                        amountEuros = message.amountEuros ?: 0.0,
                        isOwedToYou = message.isOwedToYou,
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
