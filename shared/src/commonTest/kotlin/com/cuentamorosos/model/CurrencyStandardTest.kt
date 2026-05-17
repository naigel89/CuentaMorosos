package com.cuentamorosos.model

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for D6 currency standardization — constant and model defaults.
 */
class CurrencyStandardTest {

    @Test
    fun `SUPPORTED_CURRENCY resolves to EUR`() {
        assertEquals("EUR", SUPPORTED_CURRENCY)
    }

    @Test
    fun `new EventItem without explicit baseCurrency defaults to EUR`() {
        val event = EventItem(
            name = "Test event",
            dateMillis = 1000L,
            ownerId = "owner-1",
            memberIds = listOf("a", "b"),
        )
        assertEquals("EUR", event.baseCurrency)
    }

    @Test
    fun `EventItem with explicit EUR baseCurrency keeps EUR`() {
        val event = EventItem(
            name = "Test event",
            dateMillis = 1000L,
            ownerId = "owner-1",
            memberIds = listOf("a", "b"),
            baseCurrency = "EUR",
        )
        assertEquals("EUR", event.baseCurrency)
    }
}
