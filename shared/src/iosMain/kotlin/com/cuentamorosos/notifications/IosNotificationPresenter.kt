package com.cuentamorosos.notifications

import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationAction
import platform.UserNotifications.UNNotificationActionOptionForeground
import platform.UserNotifications.UNNotificationCategory
import platform.UserNotifications.UNNotificationCategoryOptionNone
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

/**
 * Implementación iOS de [NotificationPresenter] sobre `UNUserNotificationCenter`.
 *
 * Ningún texto se construye aquí: título, cuerpo, categoría y acciones vienen ya
 * resueltos en el [NotificationContent] que produce [NotificationContentFactory],
 * el mismo objeto que usa Android. Esta clase solo traduce ese contenido a las
 * APIs de UserNotifications.
 *
 * ## Sobre [areNotificationsEnabled]
 *
 * El puerto es síncrono, pero iOS solo ofrece
 * `getNotificationSettingsWithCompletionHandler`, que es asíncrono: no existe
 * ninguna forma de consultar el permiso sin bloquear. Por eso se cachea el estado
 * y el host debe refrescarlo con [refreshAuthorizationStatus] al arrancar y al
 * volver a primer plano.
 *
 * El valor inicial es optimista (`true`) porque en iOS el propio sistema es el
 * filtro real: si el usuario denegó el permiso, `addNotificationRequest` no
 * muestra nada. La comprobación aquí evita trabajo inútil, no es la garantía.
 * Ser optimista al arrancar es lo correcto: una push en frío llega antes de que
 * el refresco haya podido completarse, y descartarla sería perderla.
 */
class IosNotificationPresenter(
    private val center: UNUserNotificationCenter =
        UNUserNotificationCenter.currentNotificationCenter(),
) : NotificationPresenter {

    private var authorized: Boolean = true
    private var categoriesRegistered: Boolean = false

    /**
     * Relee el permiso de notificaciones del sistema y actualiza la caché.
     * El host debe llamarlo al arrancar y en `applicationWillEnterForeground`.
     */
    fun refreshAuthorizationStatus() {
        center.getNotificationSettingsWithCompletionHandler { settings ->
            val status = settings?.authorizationStatus
            authorized = status == UNAuthorizationStatusAuthorized ||
                status == UNAuthorizationStatusProvisional
        }
    }

    override fun areNotificationsEnabled(): Boolean = authorized

    /**
     * Registra una `UNNotificationCategory` por cada [NotificationType].
     *
     * Se agrupa por tipo y no por canal porque las acciones dependen del tipo:
     * una invitación recibida ofrece aceptar/rechazar y una aceptada solo ver
     * detalles, aunque ambas caigan en el canal de invitaciones.
     */
    override fun ensureChannels() {
        if (categoriesRegistered) return

        val categories = NotificationType.entries.map { type ->
            val actions = NotificationContentFactory.actionsForType(type).map { action ->
                UNNotificationAction.actionWithIdentifier(
                    identifier = action.id,
                    title = action.label,
                    options = UNNotificationActionOptionForeground,
                )
            }
            UNNotificationCategory.categoryWithIdentifier(
                identifier = type.name,
                actions = actions,
                intentIdentifiers = emptyList<String>(),
                options = UNNotificationCategoryOptionNone,
            )
        }

        center.setNotificationCategories(categories.toSet())
        categoriesRegistered = true
    }

    override fun present(content: NotificationContent) {
        val payload = UNMutableNotificationContent().apply {
            setTitle(content.title)
            setBody(content.body)
            setCategoryIdentifier(content.type.name)
            setSound(UNNotificationSound.defaultSound)
            // El host lee esto al pulsar para reconstruir el DeepLinkTarget.
            setUserInfo(
                buildMap {
                    put(USER_INFO_NOTIFICATION_TYPE, content.deepLink.notificationType)
                    put(USER_INFO_PAGER_PAGE, content.deepLink.pagerPage.toString())
                    content.deepLink.eventId?.let { put(USER_INFO_EVENT_ID, it) }
                    content.invitationId?.let { put(USER_INFO_INVITATION_ID, it) }
                }
            )
        }

        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = "${content.tag}:${content.id}",
            content = payload,
            // Sin trigger, iOS la entrega de inmediato.
            trigger = null,
        )

        center.addNotificationRequest(request, null)
    }

    companion object {
        /** Claves de `userInfo`; el host las usa para reconstruir el deep link. */
        const val USER_INFO_NOTIFICATION_TYPE = "notificationType"
        const val USER_INFO_PAGER_PAGE = "pagerPage"
        const val USER_INFO_EVENT_ID = "eventId"
        const val USER_INFO_INVITATION_ID = "invitationId"
    }
}
