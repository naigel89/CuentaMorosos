package com.cuentamorosos.notifications

/**
 * Traduce el payload de datos de una push a un [NotificationEvent].
 *
 * El backend envía el mismo diccionario `data` a Android (FCM) y a iOS (APNs vía
 * Firebase), así que este parseo es común: si divergiera, las dos plataformas
 * mostrarían notificaciones distintas ante el mismo mensaje.
 *
 * Devuelve `null` cuando el payload no tiene `type` reconocible o le falta algún
 * campo obligatorio; el host debe descartar el mensaje en ese caso.
 */
object PushPayloadParser {

    const val TYPE_INVITATION_RECEIVED = "invitation_received"
    const val TYPE_INVITATION_ACCEPTED = "invitation_accepted"
    const val TYPE_CALCULATION_COMPLETED = "calculation_completed"

    fun parse(data: Map<String, String>): NotificationEvent? {
        return when (data["type"]) {
            TYPE_INVITATION_RECEIVED -> NotificationEvent.InvitationReceived(
                invitationId = data["invitationId"] ?: return null,
                eventId = data["eventId"] ?: return null,
                inviterName = data["inviterName"] ?: return null,
                eventName = data["eventName"] ?: return null,
            )

            TYPE_INVITATION_ACCEPTED -> NotificationEvent.InvitationAccepted(
                eventId = data["eventId"] ?: return null,
                inviteeName = data["inviteeName"] ?: return null,
                eventName = data["eventName"] ?: return null,
            )

            TYPE_CALCULATION_COMPLETED -> NotificationEvent.CalculationCompleted(
                eventId = data["eventId"] ?: return null,
                eventName = data["eventName"] ?: return null,
                amountOwed = data["amountOwed"]?.toDoubleOrNull() ?: return null,
            )

            else -> null
        }
    }
}
