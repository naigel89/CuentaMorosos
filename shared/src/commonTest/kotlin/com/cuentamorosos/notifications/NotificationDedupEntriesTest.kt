package com.cuentamorosos.notifications

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * La codificación del registro de deduplicación es compartida entre Android
 * (SharedPreferences) e iOS (NSUserDefaults). Estos tests la fijan para que un
 * registro escrito por una plataforma siga siendo legible por la otra.
 */
class NotificationDedupEntriesTest {

    private val now = 1_700_000_000_000L
    private val day = 24L * 60 * 60 * 1000

    @Test
    fun `una huella registrada se encuentra despues`() {
        val entries = NotificationDedupEntries.record(emptySet(), "CALC:ev-1", now)
        assertTrue(NotificationDedupEntries.contains(entries, "CALC:ev-1"))
    }

    @Test
    fun `una huella no registrada no se encuentra`() {
        val entries = NotificationDedupEntries.record(emptySet(), "CALC:ev-1", now)
        assertFalse(NotificationDedupEntries.contains(entries, "CALC:ev-2"))
    }

    @Test
    fun `una huella en blanco nunca se registra ni se encuentra`() {
        assertEquals(emptySet(), NotificationDedupEntries.record(emptySet(), "", now))
        assertEquals(emptySet(), NotificationDedupEntries.record(emptySet(), "   ", now))
        assertFalse(NotificationDedupEntries.contains(setOf("$now|algo"), ""))
    }

    @Test
    fun `el formato de entrada es epochMillis separado por barra vertical`() {
        val entries = NotificationDedupEntries.record(emptySet(), "CALC:ev-1", now)
        assertEquals(setOf("$now|CALC:ev-1"), entries)
    }

    @Test
    fun `una huella que contiene dos puntos se decodifica entera`() {
        // Las huellas reales llevan dos puntos: "PAYMENT_REMINDER:ev-3:Luis".
        // Solo el primer separador cuenta.
        val entry = "$now|PAYMENT_REMINDER:ev-3:Luis"
        assertEquals("PAYMENT_REMINDER:ev-3:Luis", NotificationDedupEntries.fingerprintOf(entry))
        assertEquals(now, NotificationDedupEntries.timestampOf(entry))
    }

    @Test
    fun `la poda conserva lo reciente y descarta lo antiguo`() {
        val reciente = "${now - 5 * day}|CALC:nuevo"
        val antiguo = "${now - 40 * day}|CALC:viejo"

        val pruned = NotificationDedupEntries.prune(setOf(reciente, antiguo), now)

        assertEquals(setOf(reciente), pruned)
    }

    @Test
    fun `la poda respeta el limite exacto de treinta dias`() {
        val justoEnElLimite = "${now - 30 * day}|CALC:limite"
        val unMsMasViejo = "${now - 30 * day - 1}|CALC:pasado"

        val pruned = NotificationDedupEntries.prune(setOf(justoEnElLimite, unMsMasViejo), now)

        assertEquals(setOf(justoEnElLimite), pruned)
    }

    @Test
    fun `la poda descarta entradas mal formadas`() {
        val sinSeparador = "esto-no-tiene-separador"
        val marcaNoNumerica = "ayer|CALC:ev-1"
        val valida = "$now|CALC:ev-1"

        val pruned = NotificationDedupEntries.prune(
            setOf(sinSeparador, marcaNoNumerica, valida),
            now,
        )

        assertEquals(setOf(valida), pruned)
    }

    @Test
    fun `podar un registro vacio no falla`() {
        assertEquals(emptySet(), NotificationDedupEntries.prune(emptySet(), now))
    }

    @Test
    fun `una entrada mal formada no tiene huella ni marca`() {
        assertNull(NotificationDedupEntries.fingerprintOf("sin-separador"))
        assertNull(NotificationDedupEntries.timestampOf("sin-separador"))
        assertNull(NotificationDedupEntries.timestampOf("ayer|CALC:ev-1"))
    }

    @Test
    fun `registrar dos veces la misma huella no duplica la entrada`() {
        var entries = NotificationDedupEntries.record(emptySet(), "CALC:ev-1", now)
        entries = NotificationDedupEntries.record(entries, "CALC:ev-1", now)
        assertEquals(1, entries.size)
    }
}

/**
 * Las acciones deben poder derivarse del tipo sin un evento a mano: iOS registra
 * sus UNNotificationCategory por adelantado.
 */
class NotificationActionsByTypeTest {

    @Test
    fun `una invitacion recibida ofrece aceptar y rechazar`() {
        val acciones = NotificationContentFactory.actionsForType(NotificationType.INVITATION)
        assertEquals(listOf("Aceptar", "Rechazar"), acciones.map { it.label })
        assertEquals(
            listOf(
                NotificationContentFactory.ACTION_ACCEPT_INVITATION,
                NotificationContentFactory.ACTION_REJECT_INVITATION,
            ),
            acciones.map { it.id },
        )
    }

    @Test
    fun `el resto de tipos solo ofrece ver detalles`() {
        listOf(
            NotificationType.INVITATION_ACCEPTED,
            NotificationType.CALCULATION,
            NotificationType.PAYMENT_REMINDER,
        ).forEach { tipo ->
            val acciones = NotificationContentFactory.actionsForType(tipo)
            assertEquals(listOf("Ver detalles"), acciones.map { it.label }, "tipo $tipo")
        }
    }

    @Test
    fun `derivar por tipo coincide con derivar por evento`() {
        val eventos = listOf(
            NotificationEvent.InvitationReceived("inv-1", "ev-1", "Ana", "Asado"),
            NotificationEvent.InvitationAccepted("ev-1", "Bob", "Fiesta"),
            NotificationEvent.CalculationCompleted("ev-2", "Cena", 10.0),
            NotificationEvent.PaymentReminder("ev-3", "Luis", 5.0, true),
        )
        eventos.forEach { evento ->
            assertEquals(
                NotificationContentFactory.actionsFor(evento),
                NotificationContentFactory.actionsForType(
                    NotificationContentFactory.typeFor(evento)
                ),
            )
        }
    }
}
