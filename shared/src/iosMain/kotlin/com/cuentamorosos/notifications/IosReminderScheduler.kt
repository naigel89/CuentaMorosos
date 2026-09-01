package com.cuentamorosos.notifications

import platform.Foundation.NSDateComponents
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

/**
 * Implementación iOS de [ReminderScheduler].
 *
 * ## No es equivalente a Android, y es deliberado
 *
 * Android usa un `PeriodicWorkRequest` de WorkManager: el sistema garantiza la
 * ejecución y el worker **calcula** los recordatorios en ese momento, leyendo
 * repositorios frescos. Puede decir "Luis te debe 15,50 €" porque lo averigua al
 * dispararse.
 *
 * iOS no tiene equivalente. `BGTaskScheduler` es best-effort y el sistema puede
 * no ejecutarlo nunca si el usuario abre poco la app; confiar en él para el único
 * recordatorio diario sería poco fiable. La alternativa —notificaciones locales
 * programadas por adelantado— sí es fiable, pero invierte el modelo: el contenido
 * se congela al programarse.
 *
 * De ahí la decisión: se programa **un recordatorio genérico** que solo invita a
 * abrir la app, y el detalle por deuda se calcula al abrirla. Es peor que
 * Android; fingir paridad sería mostrar importes caducos.
 */
class IosReminderScheduler(
    private val center: UNUserNotificationCenter =
        UNUserNotificationCenter.currentNotificationCenter(),
) : ReminderScheduler {

    override fun schedule() {
        val components = NSDateComponents().apply {
            hour = REMINDER_HOUR
            minute = 0
        }

        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
            dateComponents = components,
            repeats = true,
        )

        val content = UNMutableNotificationContent().apply {
            setTitle(ReminderScheduler.GENERIC_REMINDER_TITLE)
            setBody(ReminderScheduler.GENERIC_REMINDER_BODY)
            setCategoryIdentifier(NotificationType.PAYMENT_REMINDER.name)
            setSound(UNNotificationSound.defaultSound)
        }

        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = REQUEST_ID,
            content = content,
            trigger = trigger,
        )

        // Reprogramar con el mismo identificador reemplaza la petición anterior,
        // que es lo que queremos: nunca debe haber dos recordatorios diarios.
        center.addNotificationRequest(request, null)
    }

    override fun cancel() {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(REQUEST_ID))
    }

    private companion object {
        /** Identificador único: reprogramar reemplaza, no duplica. */
        const val REQUEST_ID = "cuenta_morosos_daily_reminder"

        /** Las 10:00 hora local — pronto para ser útil, tarde para no despertar. */
        const val REMINDER_HOUR = 10L
    }
}
