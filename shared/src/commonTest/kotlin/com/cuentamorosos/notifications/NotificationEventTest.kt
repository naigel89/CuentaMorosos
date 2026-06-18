package com.cuentamorosos.notifications

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationEventTest {

    // ── PaymentReminder construction ────────────────────────────────────

    @Test
    fun `PaymentReminder with isOwedToYou=true for debt owed to current user`() {
        val event = NotificationEvent.PaymentReminder(
            eventId = "evt-1",
            profileName = "Luis",
            amountEuros = 15.50,
            isOwedToYou = true,
        )

        assertEquals("evt-1", event.eventId)
        assertEquals("Luis", event.profileName)
        assertEquals(15.50, event.amountEuros)
        assertTrue(event.isOwedToYou)
    }

    @Test
    fun `PaymentReminder with isOwedToYou=false for debt owed by current user`() {
        val event = NotificationEvent.PaymentReminder(
            eventId = "evt-2",
            profileName = "Ana",
            amountEuros = 8.00,
            isOwedToYou = false,
        )

        assertEquals("evt-2", event.eventId)
        assertEquals("Ana", event.profileName)
        assertEquals(8.00, event.amountEuros)
        assertFalse(event.isOwedToYou)
    }

    @Test
    fun `PaymentReminder eventId is non-null as required by base class`() {
        val event = NotificationEvent.PaymentReminder(
            eventId = "evt-non-null",
            profileName = "Test",
            amountEuros = 1.0,
            isOwedToYou = true,
        )

        // eventId from sealed class should be the same String
        assertEquals("evt-non-null", event.eventId)
    }
}
