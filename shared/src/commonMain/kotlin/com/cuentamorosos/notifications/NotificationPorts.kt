package com.cuentamorosos.notifications

/**
 * Pinta una notificación ya resuelta. Android lo implementa con
 * `NotificationManagerCompat`; iOS con `UNUserNotificationCenter`.
 */
interface NotificationPresenter {
    /** `false` si el usuario ha desactivado las notificaciones a nivel de sistema. */
    fun areNotificationsEnabled(): Boolean

    /** Crea los canales/categorías si aún no existen. Idempotente. */
    fun ensureChannels()

    /** Publica la notificación. */
    fun present(content: NotificationContent)
}

/**
 * Recuerda qué notificaciones ya se emitieron, para no repetirlas.
 *
 * En Android lo respalda `CuentaMorososLocalStore` (SharedPreferences); en iOS
 * cualquier almacén persistente sirve — `NSUserDefaults` o SQLDelight.
 */
interface NotificationDedupStore {
    fun hasBeenSent(fingerprint: String): Boolean
    fun recordSent(fingerprint: String)
}

/**
 * Programa el trabajo periódico que emite recordatorios con la app cerrada.
 *
 * Android usa `WorkManager` (trabajo periódico único cada 24 h); iOS tendrá que
 * usar `BGTaskScheduler` o notificaciones locales con `UNCalendarNotificationTrigger`,
 * ya que no existe equivalente directo a un worker periódico garantizado.
 */
interface ReminderScheduler {
    fun schedule()
    fun cancel()

    companion object {
        /**
         * Texto del recordatorio genérico que usa iOS.
         *
         * Android calcula el contenido al dispararse y puede nombrar deudor e
         * importe. iOS programa la notificación por adelantado, así que su
         * contenido se congela al programarla: prometer una cifra ahí sería
         * mostrar un dato potencialmente caduco. Por eso solo invita a abrir la
         * app, donde el detalle se calcula con datos frescos.
         */
        const val GENERIC_REMINDER_TITLE = "Recordatorio de pago"
        const val GENERIC_REMINDER_BODY = "Revisa tus deudas pendientes en CuentaMorosos"
    }
}

/**
 * Orquesta el envío: comprueba permisos, deduplica y delega el pintado.
 *
 * Esta es la parte que antes estaba enterrada en el `NotificationDispatcher` de
 * Android y que ahora comparten ambas plataformas, de modo que la regla de
 * deduplicación no puede divergir entre ellas.
 */
class NotificationCoordinator(
    private val presenter: NotificationPresenter,
    private val dedupStore: NotificationDedupStore? = null,
) {
    /**
     * Emite la notificación correspondiente a [event], salvo que las
     * notificaciones estén desactivadas o esta ya se haya enviado antes.
     *
     * @return `true` si se publicó, `false` si se omitió.
     */
    fun dispatch(event: NotificationEvent): Boolean {
        if (!presenter.areNotificationsEnabled()) return false

        val content = NotificationContentFactory.from(event)
        if (dedupStore?.hasBeenSent(content.fingerprint) == true) return false

        presenter.ensureChannels()
        presenter.present(content)
        dedupStore?.recordSent(content.fingerprint)
        return true
    }
}
