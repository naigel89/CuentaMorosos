package com.cuentamorosos.notifications

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PushPayloadParserTest {

    @Test
    fun `parsea una invitacion recibida`() {
        val event = PushPayloadParser.parse(
            mapOf(
                "type" to "invitation_received",
                "eventId" to "ev-1",
                "invitationId" to "inv-1",
                "inviterName" to "Ana",
                "eventName" to "Asado",
            )
        )
        assertEquals(
            NotificationEvent.InvitationReceived("inv-1", "ev-1", "Ana", "Asado"),
            event,
        )
    }

    @Test
    fun `parsea una invitacion aceptada`() {
        val event = PushPayloadParser.parse(
            mapOf(
                "type" to "invitation_accepted",
                "eventId" to "ev-1",
                "inviteeName" to "Bob",
                "eventName" to "Fiesta",
            )
        )
        assertEquals(NotificationEvent.InvitationAccepted("ev-1", "Bob", "Fiesta"), event)
    }

    @Test
    fun `parsea un calculo completado con importe decimal`() {
        val event = PushPayloadParser.parse(
            mapOf(
                "type" to "calculation_completed",
                "eventId" to "ev-2",
                "eventName" to "Cena",
                "amountOwed" to "42.5",
            )
        )
        assertEquals(NotificationEvent.CalculationCompleted("ev-2", "Cena", 42.5), event)
    }

    @Test
    fun `descarta un payload vacio`() {
        assertNull(PushPayloadParser.parse(emptyMap()))
    }

    @Test
    fun `descarta un payload sin type`() {
        assertNull(PushPayloadParser.parse(mapOf("eventId" to "ev-1")))
    }

    @Test
    fun `descarta un type desconocido`() {
        assertNull(PushPayloadParser.parse(mapOf("type" to "algo_que_no_existe")))
    }

    @Test
    fun `descarta una invitacion a la que le falta un campo`() {
        val completo = mapOf(
            "type" to "invitation_received",
            "eventId" to "ev-1",
            "invitationId" to "inv-1",
            "inviterName" to "Ana",
            "eventName" to "Asado",
        )
        listOf("eventId", "invitationId", "inviterName", "eventName").forEach { campo ->
            assertNull(
                PushPayloadParser.parse(completo - campo),
                "faltando '$campo' debería descartarse",
            )
        }
    }

    @Test
    fun `descarta un importe que no es un numero`() {
        assertNull(
            PushPayloadParser.parse(
                mapOf(
                    "type" to "calculation_completed",
                    "eventId" to "ev-2",
                    "eventName" to "Cena",
                    "amountOwed" to "cuarenta y dos",
                )
            )
        )
    }
}
