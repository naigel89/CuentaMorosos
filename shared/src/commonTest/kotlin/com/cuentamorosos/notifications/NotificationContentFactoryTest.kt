package com.cuentamorosos.notifications

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Estos tests cubren la lógica de notificaciones que antes vivía en el
 * `NotificationDispatcher` de Android. Al estar en commonTest se ejecutan
 * también en iOS, garantizando que ambas plataformas muestran lo mismo.
 */
class NotificationContentFactoryTest {

    private val invitation = NotificationEvent.InvitationReceived(
        invitationId = "inv-1",
        eventId = "ev-1",
        inviterName = "Ana",
        eventName = "Asado",
    )

    private val accepted = NotificationEvent.InvitationAccepted(
        eventId = "ev-1",
        inviteeName = "Bob",
        eventName = "Fiesta",
    )

    private val calculation = NotificationEvent.CalculationCompleted(
        eventId = "ev-2",
        eventName = "Cena",
        amountOwed = 42.50,
    )

    private val owedToYou = NotificationEvent.PaymentReminder(
        eventId = "ev-3",
        profileName = "Luis",
        amountEuros = 15.50,
        isOwedToYou = true,
    )

    private val youOwe = NotificationEvent.PaymentReminder(
        eventId = "ev-3",
        profileName = "Ana",
        amountEuros = 8.0,
        isOwedToYou = false,
    )

    // ── Textos ──────────────────────────────────────────────────────────────

    @Test
    fun `invitacion recibida nombra al invitante y al evento`() {
        val c = NotificationContentFactory.from(invitation)
        assertEquals("Invitación recibida", c.title)
        assertEquals("Ana te invitó al evento 'Asado'", c.body)
    }

    @Test
    fun `invitacion aceptada nombra al invitado y al evento`() {
        val c = NotificationContentFactory.from(accepted)
        assertEquals("Invitación aceptada", c.title)
        assertEquals("Bob aceptó tu invitación a 'Fiesta'", c.body)
    }

    @Test
    fun `calculo completado formatea el importe con dos decimales`() {
        val c = NotificationContentFactory.from(calculation)
        assertEquals("Cálculo completado", c.title)
        assertEquals("Se calcularon los gastos de 'Cena'. Debes €42.50", c.body)
    }

    @Test
    fun `recordatorio en direccion te-debe`() {
        assertEquals("Luis te debe €15.50", NotificationContentFactory.from(owedToYou).body)
    }

    @Test
    fun `recordatorio en direccion debes-a`() {
        assertEquals("Debes €8.00 a Ana", NotificationContentFactory.from(youOwe).body)
    }

    @Test
    fun `los importes se redondean y rellenan a dos decimales`() {
        assertEquals("€0.00", NotificationContentFactory.money(0.0))
        assertEquals("€0.05", NotificationContentFactory.money(0.05))
        assertEquals("€1.10", NotificationContentFactory.money(1.1))
        assertEquals("€1234.50", NotificationContentFactory.money(1234.5))
        assertEquals("€-3.25", NotificationContentFactory.money(-3.25))
        // Redondeo, no truncado
        assertEquals("€2.35", NotificationContentFactory.money(2.345))
    }

    // ── Canales ─────────────────────────────────────────────────────────────

    @Test
    fun `cada evento cae en su canal`() {
        assertEquals("ch_invitations", NotificationContentFactory.from(invitation).channel.id)
        assertEquals("ch_invitations", NotificationContentFactory.from(accepted).channel.id)
        assertEquals("ch_calculations", NotificationContentFactory.from(calculation).channel.id)
        assertEquals("ch_reminders", NotificationContentFactory.from(owedToYou).channel.id)
    }

    @Test
    fun `existen exactamente tres canales y ninguno es ch_upcoming_events`() {
        val ids = NotificationChannel.entries.map { it.id }
        assertEquals(3, ids.size)
        assertEquals(setOf("ch_invitations", "ch_calculations", "ch_reminders"), ids.toSet())
    }

    // ── Deduplicación ───────────────────────────────────────────────────────

    @Test
    fun `la huella de invitacion usa eventId e invitationId`() {
        assertEquals(
            "INVITATION_RECEIVED:ev-1:inv-1",
            NotificationContentFactory.fingerprintFor(invitation),
        )
    }

    @Test
    fun `la huella de calculo usa solo el eventId`() {
        assertEquals(
            "CALCULATION_COMPLETED:ev-2",
            NotificationContentFactory.fingerprintFor(calculation),
        )
    }

    @Test
    fun `la huella de recordatorio usa eventId y nombre de perfil`() {
        assertEquals(
            "PAYMENT_REMINDER:ev-3:Luis",
            NotificationContentFactory.fingerprintFor(owedToYou),
        )
    }

    @Test
    fun `la huella de invitacion aceptada usa eventId y nombre del invitado`() {
        assertEquals(
            "INVITATION_ACCEPTED:ev-1:Bob",
            NotificationContentFactory.fingerprintFor(accepted),
        )
    }

    @Test
    fun `mismo evento produce la misma huella y distinto evento otra distinta`() {
        assertEquals(
            NotificationContentFactory.fingerprintFor(invitation),
            NotificationContentFactory.fingerprintFor(invitation.copy()),
        )
        assertNotEquals(
            NotificationContentFactory.fingerprintFor(invitation),
            NotificationContentFactory.fingerprintFor(invitation.copy(invitationId = "inv-2")),
        )
    }

    @Test
    fun `el id es determinista para el mismo eventId`() {
        assertEquals(
            NotificationContentFactory.from(owedToYou).id,
            NotificationContentFactory.from(owedToYou.copy(profileName = "Otro")).id,
        )
    }

    // ── Deep link y acciones ────────────────────────────────────────────────

    @Test
    fun `la invitacion enlaza a la pagina de invitaciones y el resto al panel`() {
        assertEquals(
            NotificationContentFactory.PAGE_INVITATIONS,
            NotificationContentFactory.from(invitation).deepLink.pagerPage,
        )
        assertEquals(
            NotificationContentFactory.PAGE_DASHBOARD,
            NotificationContentFactory.from(owedToYou).deepLink.pagerPage,
        )
        assertEquals(
            NotificationContentFactory.PAGE_DASHBOARD,
            NotificationContentFactory.from(calculation).deepLink.pagerPage,
        )
    }

    @Test
    fun `el deep link lleva el tag como tipo de notificacion`() {
        val c = NotificationContentFactory.from(calculation)
        assertEquals("CALCULATION_COMPLETED", c.deepLink.notificationType)
        assertEquals("ev-2", c.deepLink.eventId)
    }

    @Test
    fun `solo la invitacion recibida ofrece aceptar y rechazar`() {
        val c = NotificationContentFactory.from(invitation)
        assertEquals(listOf("Aceptar", "Rechazar"), c.actions.map { it.label })
        assertEquals("inv-1", c.invitationId)

        val otras = listOf(accepted, calculation, owedToYou).map {
            NotificationContentFactory.from(it)
        }
        otras.forEach {
            assertEquals(listOf("Ver detalles"), it.actions.map { a -> a.label })
            assertEquals(null, it.invitationId)
        }
    }
}

/**
 * El coordinador es la regla de deduplicación compartida: comprueba permisos,
 * salta lo ya enviado y registra lo emitido.
 */
class NotificationCoordinatorTest {

    private class FakePresenter(
        var enabled: Boolean = true,
    ) : NotificationPresenter {
        val presented = mutableListOf<NotificationContent>()
        var channelsEnsured = 0
        override fun areNotificationsEnabled() = enabled
        override fun ensureChannels() { channelsEnsured++ }
        override fun present(content: NotificationContent) { presented += content }
    }

    private class FakeDedupStore : NotificationDedupStore {
        val sent = mutableSetOf<String>()
        override fun hasBeenSent(fingerprint: String) = fingerprint in sent
        override fun recordSent(fingerprint: String) { sent += fingerprint }
    }

    private val event = NotificationEvent.CalculationCompleted(
        eventId = "ev-1",
        eventName = "Cena",
        amountOwed = 10.0,
    )

    @Test
    fun `emite la notificacion y registra su huella`() {
        val presenter = FakePresenter()
        val store = FakeDedupStore()

        assertTrue(NotificationCoordinator(presenter, store).dispatch(event))
        assertEquals(1, presenter.presented.size)
        assertTrue("CALCULATION_COMPLETED:ev-1" in store.sent)
    }

    @Test
    fun `no repite una notificacion ya enviada`() {
        val presenter = FakePresenter()
        val store = FakeDedupStore()
        val coordinator = NotificationCoordinator(presenter, store)

        assertTrue(coordinator.dispatch(event))
        assertFalse(coordinator.dispatch(event))
        assertEquals(1, presenter.presented.size)
    }

    @Test
    fun `no emite nada si el usuario desactivo las notificaciones`() {
        val presenter = FakePresenter(enabled = false)
        val store = FakeDedupStore()

        assertFalse(NotificationCoordinator(presenter, store).dispatch(event))
        assertEquals(0, presenter.presented.size)
        assertEquals(0, presenter.channelsEnsured)
        // Sin huella registrada: si el usuario las reactiva, la recibirá
        assertTrue(store.sent.isEmpty())
    }

    @Test
    fun `sin almacen de deduplicacion emite siempre`() {
        val presenter = FakePresenter()
        val coordinator = NotificationCoordinator(presenter, dedupStore = null)

        assertTrue(coordinator.dispatch(event))
        assertTrue(coordinator.dispatch(event))
        assertEquals(2, presenter.presented.size)
    }
}
