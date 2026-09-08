package com.cuentamorosos.notifications

/**
 * Categoría de notificación. Determina canal, icono y prioridad en cada plataforma.
 */
enum class NotificationType {
    INVITATION,
    INVITATION_ACCEPTED,
    CALCULATION,
    PAYMENT_REMINDER,
}

/** Importancia relativa; cada host la traduce a su propio sistema. */
enum class NotificationImportance { HIGH, DEFAULT }

/**
 * Canal de notificación. Android los crea literalmente; iOS los usa como
 * categorías de `UNNotificationCategory`.
 */
enum class NotificationChannel(
    val id: String,
    val displayName: String,
    val description: String,
    val importance: NotificationImportance,
) {
    INVITATIONS(
        id = "ch_invitations",
        displayName = "Invitaciones",
        description = "Notificaciones de invitaciones a eventos",
        importance = NotificationImportance.HIGH,
    ),
    CALCULATIONS(
        id = "ch_calculations",
        displayName = "Cálculos y liquidaciones",
        description = "Notificaciones cuando se calculan gastos de eventos",
        importance = NotificationImportance.DEFAULT,
    ),
    REMINDERS(
        id = "ch_reminders",
        displayName = "Recordatorios",
        description = "Recordatorios de deudas pendientes",
        importance = NotificationImportance.DEFAULT,
    ),
}

/** Botón de acción. [id] es el identificador que el host despacha al pulsarlo. */
data class NotificationAction(val id: String, val label: String)

/**
 * Descripción completa de una notificación, ya resuelta y sin dependencias de
 * plataforma. El host solo tiene que pintarla.
 */
data class NotificationContent(
    val type: NotificationType,
    val channel: NotificationChannel,
    val title: String,
    val body: String,
    val tag: String,
    val id: Int,
    val fingerprint: String,
    val deepLink: DeepLinkTarget,
    val actions: List<NotificationAction>,
    /** Solo presente en invitaciones; lo necesitan las acciones aceptar/rechazar. */
    val invitationId: String? = null,
)

/**
 * Traduce un [NotificationEvent] a su [NotificationContent].
 *
 * Toda la lógica que antes vivía en el `NotificationDispatcher` de Android:
 * textos, canales, huellas de deduplicación, destino del deep link y acciones.
 * Es pura, así que iOS y Android muestran exactamente lo mismo.
 */
object NotificationContentFactory {

    // Tags de tipo (también usados como clave de deduplicación)
    const val TAG_INVITATION_RECEIVED = "INVITATION_RECEIVED"
    const val TAG_INVITATION_ACCEPTED = "INVITATION_ACCEPTED"
    const val TAG_CALCULATION_COMPLETED = "CALCULATION_COMPLETED"
    const val TAG_PAYMENT_REMINDER = "PAYMENT_REMINDER"

    // Índices de página del pager (coinciden con el orden de MainSection)
    const val PAGE_DASHBOARD = 0
    const val PAGE_EVENTS = 1
    const val PAGE_PROFILES = 2
    const val PAGE_INVITATIONS = 3
    const val PAGE_SETTINGS = 4

    // Identificadores de acción
    const val ACTION_ACCEPT_INVITATION = "ACTION_ACCEPT_INVITATION"
    const val ACTION_REJECT_INVITATION = "ACTION_REJECT_INVITATION"
    const val ACTION_VIEW_DETAILS = "ACTION_VIEW_DETAILS"

    fun from(event: NotificationEvent): NotificationContent {
        val type = typeFor(event)
        val tag = tagFor(event)
        return NotificationContent(
            type = type,
            channel = channelFor(type),
            title = titleFor(event),
            body = bodyFor(event),
            tag = tag,
            id = idFor(event),
            fingerprint = fingerprintFor(event),
            deepLink = DeepLinkTarget(
                pagerPage = pagerPageFor(event),
                eventId = event.eventId,
                notificationType = tag,
            ),
            actions = actionsFor(event),
            invitationId = (event as? NotificationEvent.InvitationReceived)?.invitationId,
        )
    }

    /**
     * Huella determinista de un evento: mismo tipo + mismos IDs → misma huella.
     * Es lo que impide que la misma notificación se emita dos veces.
     */
    fun fingerprintFor(event: NotificationEvent): String = when (event) {
        is NotificationEvent.InvitationReceived ->
            "$TAG_INVITATION_RECEIVED:${event.eventId}:${event.invitationId}"
        is NotificationEvent.InvitationAccepted ->
            "$TAG_INVITATION_ACCEPTED:${event.eventId}:${event.inviteeName}"
        is NotificationEvent.CalculationCompleted ->
            "$TAG_CALCULATION_COMPLETED:${event.eventId}"
        is NotificationEvent.PaymentReminder ->
            "$TAG_PAYMENT_REMINDER:${event.eventId}:${event.profileName}"
    }

    fun typeFor(event: NotificationEvent): NotificationType = when (event) {
        is NotificationEvent.InvitationReceived -> NotificationType.INVITATION
        is NotificationEvent.InvitationAccepted -> NotificationType.INVITATION_ACCEPTED
        is NotificationEvent.CalculationCompleted -> NotificationType.CALCULATION
        is NotificationEvent.PaymentReminder -> NotificationType.PAYMENT_REMINDER
    }

    fun channelFor(type: NotificationType): NotificationChannel = when (type) {
        NotificationType.INVITATION, NotificationType.INVITATION_ACCEPTED ->
            NotificationChannel.INVITATIONS
        NotificationType.CALCULATION -> NotificationChannel.CALCULATIONS
        NotificationType.PAYMENT_REMINDER -> NotificationChannel.REMINDERS
    }

    fun titleFor(event: NotificationEvent): String = when (event) {
        is NotificationEvent.InvitationReceived -> "Invitación recibida"
        is NotificationEvent.InvitationAccepted -> "Invitación aceptada"
        is NotificationEvent.CalculationCompleted -> "Cálculo completado"
        is NotificationEvent.PaymentReminder -> "Recordatorio de pago"
    }

    fun bodyFor(event: NotificationEvent): String = when (event) {
        is NotificationEvent.InvitationReceived ->
            "${event.inviterName} te invitó al evento '${event.eventName}'"
        is NotificationEvent.InvitationAccepted ->
            "${event.inviteeName} aceptó tu invitación a '${event.eventName}'"
        is NotificationEvent.CalculationCompleted ->
            "Se calcularon los gastos de '${event.eventName}'. Debes ${money(event.amountOwed)}"
        is NotificationEvent.PaymentReminder ->
            if (event.isOwedToYou) {
                "${event.profileName} te debe ${money(event.amountEuros)}"
            } else {
                "Debes ${money(event.amountEuros)} a ${event.profileName}"
            }
    }

    fun tagFor(event: NotificationEvent): String = when (event) {
        is NotificationEvent.InvitationReceived -> TAG_INVITATION_RECEIVED
        is NotificationEvent.InvitationAccepted -> TAG_INVITATION_ACCEPTED
        is NotificationEvent.CalculationCompleted -> TAG_CALCULATION_COMPLETED
        is NotificationEvent.PaymentReminder -> TAG_PAYMENT_REMINDER
    }

    fun idFor(event: NotificationEvent): Int {
        val key = event.eventId ?: event.hashCode().toString()
        return key.hashCode()
    }

    fun pagerPageFor(event: NotificationEvent): Int = when (event) {
        is NotificationEvent.InvitationReceived -> PAGE_INVITATIONS
        else -> PAGE_DASHBOARD
    }

    fun actionsFor(event: NotificationEvent): List<NotificationAction> =
        actionsForType(typeFor(event))

    /**
     * Las acciones dependen solo del tipo, no de los datos del evento.
     *
     * iOS lo necesita así: sus `UNNotificationCategory` se registran por
     * adelantado, cuando todavía no hay ningún evento del que derivarlas.
     */
    fun actionsForType(type: NotificationType): List<NotificationAction> = when (type) {
        NotificationType.INVITATION -> listOf(
            NotificationAction(ACTION_ACCEPT_INVITATION, "Aceptar"),
            NotificationAction(ACTION_REJECT_INVITATION, "Rechazar"),
        )
        NotificationType.INVITATION_ACCEPTED,
        NotificationType.CALCULATION,
        NotificationType.PAYMENT_REMINDER,
        -> listOf(NotificationAction(ACTION_VIEW_DETAILS, "Ver detalles"))
    }

    /**
     * Formatea un importe como "€42.50".
     *
     * Reproduce a propósito el formato que producía `String.format("%.2f", …)`
     * en Android — punto decimal y sin separador de millares — para que la
     * portabilidad no cambie ni un texto. Ojo: difiere de `formatAmount()` de la
     * UI, que usa convención española ("€42,50"); unificarlos es una decisión de
     * producto aparte, no de este refactor.
     */
    internal fun money(amount: Double): String {
        val scaled = kotlin.math.round(amount * 100.0)
        val negative = scaled < 0
        val abs = kotlin.math.abs(scaled).toLong()
        val cents = (abs % 100L).toString().padStart(2, '0')
        return "€${if (negative) "-" else ""}${abs / 100L}.$cents"
    }
}
