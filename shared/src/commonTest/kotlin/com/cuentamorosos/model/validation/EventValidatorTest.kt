package com.cuentamorosos.model.validation

import com.cuentamorosos.model.EventItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EventValidatorTest {

    private fun testEvent(
        name: String = "Evento de prueba",
        startDateMillis: Long = 1000L,
        endDateMillis: Long = 2000L,
        baseCurrency: String = "EUR",
        memberIds: List<String> = listOf("a", "b"),
        creatorId: String = "a",
    ) = EventItem(
        name = name,
        dateMillis = startDateMillis,
        ownerId = "a",
        memberIds = memberIds,
        startDateMillis = startDateMillis,
        endDateMillis = endDateMillis,
        baseCurrency = baseCurrency,
        creatorId = creatorId,
    )

    // ── EV-01: Name validation ──────────────────────────────────────────────

    @Test
    fun `EV-01 empty name returns error`() {
        val event = testEvent(name = "")
        val result = EventValidator.validate(event)
        assertTrue(result.hasErrors())
        assertEquals("El nombre es obligatorio", result.allErrors().first().message)
    }

    @Test
    fun `EV-01 blank name returns error`() {
        val event = testEvent(name = "   ")
        val result = EventValidator.validate(event)
        assertTrue(result.hasErrors())
        assertEquals("El nombre es obligatorio", result.allErrors().first().message)
    }

    @Test
    fun `EV-01 name too short returns error`() {
        val event = testEvent(name = "A")
        val result = EventValidator.validate(event)
        assertTrue(result.hasErrors())
        assertEquals("El nombre debe tener al menos 2 caracteres", result.allErrors().first().message)
    }

    @Test
    fun `EV-01 name too long returns error`() {
        val longName = "A".repeat(101)
        val event = testEvent(name = longName)
        val result = EventValidator.validate(event)
        assertTrue(result.hasErrors())
        assertEquals("El nombre no puede tener más de 100 caracteres", result.allErrors().first().message)
    }

    @Test
    fun `EV-01 valid name passes`() {
        val event = testEvent(name = "Cena fin de año")
        val result = EventValidator.validate(event)
        assertFalse(result.allErrors().any { it.field == "name" })
    }

    @Test
    fun `EV-01 boundary — exactly 2 characters passes`() {
        val event = testEvent(name = "AB")
        val result = EventValidator.validate(event)
        assertFalse(result.allErrors().any { it.field == "name" })
    }

    @Test
    fun `EV-01 boundary — exactly 100 characters passes`() {
        val event = testEvent(name = "A".repeat(100))
        val result = EventValidator.validate(event)
        assertFalse(result.allErrors().any { it.field == "name" })
    }

    // ── EV-02: Date order ───────────────────────────────────────────────────

    @Test
    fun `EV-02 start before end passes`() {
        val event = testEvent(startDateMillis = 1000, endDateMillis = 2000)
        val result = EventValidator.validate(event)
        assertFalse(result.allErrors().any { it.field == "dates" })
    }

    @Test
    fun `EV-02 start equals end passes`() {
        val event = testEvent(startDateMillis = 1000, endDateMillis = 1000)
        val result = EventValidator.validate(event)
        assertFalse(result.allErrors().any { it.field == "dates" })
    }

    @Test
    fun `EV-02 end before start returns error`() {
        val event = testEvent(startDateMillis = 2000, endDateMillis = 1000)
        val result = EventValidator.validate(event)
        assertTrue(result.hasErrors())
        assertEquals("La fecha de inicio debe ser anterior o igual a la fecha de fin", result.allErrors().first { it.field == "dates" }.message)
    }

    // ── EV-03: Duration limit ───────────────────────────────────────────────

    @Test
    fun `EV-03 duration within limit passes`() {
        val maxSpan = MAX_EVENT_SPAN_DAYS * 24L * 60L * 60L * 1000L
        val event = testEvent(startDateMillis = 0, endDateMillis = maxSpan)
        val result = EventValidator.validate(event)
        assertFalse(result.allErrors().any { it.message.contains("5 años") })
    }

    @Test
    fun `EV-03 duration exceeds limit returns error`() {
        val maxSpan = MAX_EVENT_SPAN_DAYS * 24L * 60L * 60L * 1000L
        val event = testEvent(startDateMillis = 0, endDateMillis = maxSpan + 1)
        val result = EventValidator.validate(event)
        assertTrue(result.hasErrors())
        assertEquals("El evento no puede durar más de 5 años", result.allErrors().first { it.message.contains("5 años") }.message)
    }

    // ── EV-04: Base currency must be EUR ─────────────────────────────────────

    @Test
    fun `EV-04 EUR currency passes`() {
        val event = testEvent(baseCurrency = "EUR")
        val result = EventValidator.validate(event)
        assertFalse(result.allErrors().any { it.field == "baseCurrency" })
    }

    @Test
    fun `EV-04 currency empty returns error`() {
        val event = testEvent(baseCurrency = "")
        val result = EventValidator.validate(event)
        assertTrue(result.hasErrors())
        assertEquals("La divisa base es obligatoria", result.allErrors().first { it.field == "baseCurrency" }.message)
    }

    @Test
    fun `EV-04 non-EUR currency returns error`() {
        val event = testEvent(baseCurrency = "USD")
        val result = EventValidator.validate(event)
        assertTrue(result.hasErrors())
        assertEquals("La moneda del evento debe ser EUR", result.allErrors().first { it.field == "baseCurrency" }.message)
    }

    // ── EV-05: Minimum participants ─────────────────────────────────────────

    @Test
    fun `EV-05 exactly 2 participants passes`() {
        val event = testEvent(memberIds = listOf("a", "b"))
        val result = EventValidator.validate(event)
        assertFalse(result.allErrors().any { it.field == "members" })
    }

    @Test
    fun `EV-05 fewer than 2 participants returns error`() {
        val event = testEvent(memberIds = listOf("a"))
        val result = EventValidator.validate(event)
        assertTrue(result.hasErrors())
        assertEquals("Se necesitan al menos 2 participantes para calcular", result.allErrors().first { it.field == "members" }.message)
    }

    @Test
    fun `EV-05 zero participants returns error`() {
        val event = testEvent(memberIds = emptyList())
        val result = EventValidator.validate(event)
        assertTrue(result.hasErrors())
        assertEquals("Se necesitan al menos 2 participantes para calcular", result.allErrors().first { it.field == "members" }.message)
    }

    // ── EV-06: No items warning ─────────────────────────────────────────────

    @Test
    fun `EV-06 event has items — no warning`() {
        val event = testEvent()
        val result = EventValidator.validate(event, itemCount = 3)
        assertFalse(result.hasWarnings())
    }

    @Test
    fun `EV-06 event has no items — warning`() {
        val event = testEvent()
        val result = EventValidator.validate(event, itemCount = 0)
        assertTrue(result.hasWarnings())
        assertEquals("El evento no tiene gastos registrados", result.allWarnings().first().message)
    }

    // ── EV-07: Creator membership warning ───────────────────────────────────

    @Test
    fun `EV-07 creator is participant — no warning`() {
        val event = testEvent(creatorId = "a", memberIds = listOf("a", "b"))
        val result = EventValidator.validate(event)
        assertFalse(result.allWarnings().any { it.message.contains("creador") || it.message.contains("participante") })
    }

    @Test
    fun `EV-07 creator not in participants — warning`() {
        val event = testEvent(creatorId = "x", memberIds = listOf("a", "b"))
        val result = EventValidator.validate(event)
        assertTrue(result.hasWarnings())
        assertTrue(result.allWarnings().any { it.message == "El creador no figura como participante" })
    }

    @Test
    fun `EV-07 creatorId empty — warning`() {
        val event = testEvent(creatorId = "", memberIds = listOf("a", "b"))
        val result = EventValidator.validate(event)
        assertTrue(result.hasWarnings())
        assertTrue(result.allWarnings().any { it.message == "No se identificó el creador del evento" })
    }

    // ── Combined scenarios ──────────────────────────────────────────────────

    @Test
    fun `fully valid event returns Success`() {
        val event = testEvent()
        val result = EventValidator.validate(event, itemCount = 1)
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `valid event with warnings returns SuccessWithWarnings`() {
        val event = testEvent(creatorId = "x", memberIds = listOf("a", "b"))
        val result = EventValidator.validate(event, itemCount = 0)
        assertTrue(result is ValidationResult.SuccessWithWarnings)
        assertEquals(2, result.warnings.size) // no items + creator not participant
    }

    @Test
    fun `event with errors and warnings returns Failure with both`() {
        val event = testEvent(name = "", baseCurrency = "", creatorId = "x", memberIds = listOf("a", "b"))
        val result = EventValidator.validate(event, itemCount = 0)
        assertTrue(result is ValidationResult.Failure)
        assertTrue(result.hasErrors())
        assertTrue(result.hasWarnings())
    }
}
