package com.cuentamorosos.model.validation

import com.cuentamorosos.model.EventExpenseItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ItemValidatorTest {

    private val memberIds = setOf("a", "b", "c")
    private val profileNameResolver: (String) -> String = { id ->
        when (id) {
            "a" -> "Alice"
            "b" -> "Bob"
            "c" -> "Charlie"
            else -> id
        }
    }

    private fun testItem(
        name: String = "Cena restaurante",
        amountEuros: Double = 1000.0,
        payerContributions: Map<String, Double> = mapOf("a" to 1000.0),
        debtorIds: List<String> = listOf("b", "c"),
        splitMode: String = "SIMPLE_AVG",
        exchangeRate: Double? = null,
        itemCurrency: String? = null,
    ) = EventExpenseItem(
        eventId = "evt1",
        name = name,
        amountEuros = amountEuros,
        paidByProfileId = "a",
        payerContributions = payerContributions,
        debtorIds = debtorIds,
        splitMode = splitMode,
        exchangeRate = exchangeRate,
        itemCurrency = itemCurrency,
    )

    // ── IV-01: Amount must be positive ──────────────────────────────────────

    @Test
    fun `IV-01 positive amount passes`() {
        val item = testItem(amountEuros = 1500.0)
        val result = ItemValidator.validate(item, memberIds, profileNameResolver)
        assertFalse(result.allErrors().any { it.field == "amount" })
    }

    @Test
    fun `IV-01 zero amount returns error`() {
        val item = testItem(amountEuros = 0.0)
        val result = ItemValidator.validate(item, memberIds, profileNameResolver)
        assertTrue(result.hasErrors())
        assertEquals("El importe debe ser mayor a cero", result.allErrors().first { it.field == "amount" }.message)
    }

    @Test
    fun `IV-01 negative amount returns error`() {
        val item = testItem(amountEuros = -500.0)
        val result = ItemValidator.validate(item, memberIds, profileNameResolver)
        assertTrue(result.hasErrors())
        assertEquals("El importe debe ser mayor a cero", result.allErrors().first { it.field == "amount" }.message)
    }

    // ── IV-02: At least one payer ───────────────────────────────────────────

    @Test
    fun `IV-02 one or more payers passes`() {
        val item = testItem(payerContributions = mapOf("a" to 1000.0))
        val result = ItemValidator.validate(item, memberIds, profileNameResolver)
        assertFalse(result.allErrors().any { it.field == "payers" })
    }

    @Test
    fun `IV-02 no payers returns error`() {
        val item = testItem(payerContributions = emptyMap())
        val result = ItemValidator.validate(item, memberIds, profileNameResolver)
        assertTrue(result.hasErrors())
        assertEquals("Debe haber al menos un pagador", result.allErrors().first { it.field == "payers" }.message)
    }

    // ── IV-03: Payer contributions sum match ────────────────────────────────

    @Test
    fun `IV-03 exact match passes`() {
        val item = testItem(amountEuros = 1000.0, payerContributions = mapOf("a" to 500.0, "b" to 500.0))
        val result = ItemValidator.validate(item, memberIds, profileNameResolver)
        assertFalse(result.allErrors().any { it.field == "contributions" })
    }

    @Test
    fun `IV-03 within tolerance passes`() {
        val item = testItem(amountEuros = 1000.0, payerContributions = mapOf("a" to 1000.01))
        val result = ItemValidator.validate(item, memberIds, profileNameResolver)
        assertFalse(result.allErrors().any { it.field == "contributions" })
    }

    @Test
    fun `IV-03 exceeds tolerance returns error`() {
        val item = testItem(amountEuros = 1000.0, payerContributions = mapOf("a" to 1000.02))
        val result = ItemValidator.validate(item, memberIds, profileNameResolver)
        assertTrue(result.hasErrors())
        assertEquals("La suma de las contribuciones no coincide con el importe total", result.allErrors().first { it.field == "contributions" }.message)
    }

    // ── IV-04: At least one debtor ──────────────────────────────────────────

    @Test
    fun `IV-04 one or more debtors passes`() {
        val item = testItem(debtorIds = listOf("b"))
        val result = ItemValidator.validate(item, memberIds, profileNameResolver)
        assertFalse(result.allErrors().any { it.field == "debtors" })
    }

    @Test
    fun `IV-04 no debtors returns error`() {
        val item = testItem(debtorIds = emptyList())
        val result = ItemValidator.validate(item, memberIds, profileNameResolver)
        assertTrue(result.hasErrors())
        assertEquals("Debe haber al menos un deudor", result.allErrors().first { it.field == "debtors" }.message)
    }

    // ── IV-05: Split mode required ──────────────────────────────────────────

    @Test
    fun `IV-05 split mode defined passes`() {
        val item = testItem(splitMode = "EQUAL")
        val result = ItemValidator.validate(item, memberIds, profileNameResolver)
        assertFalse(result.allErrors().any { it.field == "splitMode" })
    }

    @Test
    fun `IV-05 split mode empty returns error`() {
        val item = testItem(splitMode = "")
        val result = ItemValidator.validate(item, memberIds, profileNameResolver)
        assertTrue(result.hasErrors())
        assertEquals("El modo de reparto es obligatorio", result.allErrors().first { it.field == "splitMode" }.message)
    }

    // ── IV-06: Payers must be event participants ────────────────────────────

    @Test
    fun `IV-06 all payers are participants passes`() {
        val item = testItem(payerContributions = mapOf("a" to 500.0, "b" to 500.0))
        val result = ItemValidator.validate(item, memberIds, profileNameResolver)
        assertFalse(result.allErrors().any { it.message.contains("pagador") })
    }

    @Test
    fun `IV-06 payer not in event returns error with name`() {
        val item = testItem(payerContributions = mapOf("x" to 1000.0))
        val result = ItemValidator.validate(item, memberIds, profileNameResolver)
        assertTrue(result.hasErrors())
        assertEquals("El pagador 'x' no es participante del evento", result.allErrors().first { it.message.contains("pagador") }.message)
    }

    // ── IV-07: Debtors must be event participants ───────────────────────────

    @Test
    fun `IV-07 all debtors are participants passes`() {
        val item = testItem(debtorIds = listOf("a", "b"))
        val result = ItemValidator.validate(item, memberIds, profileNameResolver)
        assertFalse(result.allErrors().any { it.message.contains("deudor") })
    }

    @Test
    fun `IV-07 debtor not in event returns error with name`() {
        val item = testItem(debtorIds = listOf("x"))
        val result = ItemValidator.validate(item, memberIds, profileNameResolver)
        assertTrue(result.hasErrors())
        assertEquals("El deudor 'x' no es participante del evento", result.allErrors().first { it.message.contains("deudor") }.message)
    }

    // ── IV-08: Item name warning ────────────────────────────────────────────

    @Test
    fun `IV-08 descriptive name provided — no warning`() {
        val item = testItem(name = "Cena restaurante")
        val result = ItemValidator.validate(item, memberIds, profileNameResolver)
        assertFalse(result.allWarnings().any { it.message.contains("nombre") })
    }

    @Test
    fun `IV-08 empty name returns warning`() {
        val item = testItem(name = "")
        val result = ItemValidator.validate(item, memberIds, profileNameResolver)
        assertTrue(result.hasWarnings())
        assertEquals("El gasto no tiene nombre descriptivo", result.allWarnings().first().message)
    }

    @Test
    fun `IV-08 blank name returns warning`() {
        val item = testItem(name = "   ")
        val result = ItemValidator.validate(item, memberIds, profileNameResolver)
        assertTrue(result.hasWarnings())
        assertEquals("El gasto no tiene nombre descriptivo", result.allWarnings().first().message)
    }

    // ── IV-10: Only EUR supported for item currency ─────────────────────────

    @Test
    fun `IV-10 null itemCurrency passes`() {
        val item = testItem(itemCurrency = null)
        val result = ItemValidator.validate(item, memberIds, profileNameResolver)
        assertFalse(result.allErrors().any { it.field == "itemCurrency" })
    }

    @Test
    fun `IV-10 EUR itemCurrency passes`() {
        val item = testItem(itemCurrency = "EUR")
        val result = ItemValidator.validate(item, memberIds, profileNameResolver)
        assertFalse(result.allErrors().any { it.field == "itemCurrency" })
    }

    @Test
    fun `IV-10 non-EUR itemCurrency returns error`() {
        val item = testItem(itemCurrency = "USD")
        val result = ItemValidator.validate(item, memberIds, profileNameResolver)
        assertTrue(result.hasErrors())
        assertEquals("Solo se admite EUR como moneda por el momento", result.allErrors().first { it.field == "itemCurrency" }.message)
    }

    // ── IV-09: Exchange rate for cross-currency items ───────────────────────

    @Test
    fun `IV-09 same currency — no exchange rate needed`() {
        val item = testItem(itemCurrency = "ARS", exchangeRate = null)
        val result = ItemValidator.validate(item, memberIds, profileNameResolver, eventBaseCurrency = "ARS")
        assertFalse(result.allErrors().any { it.field == "exchangeRate" })
    }

    @Test
    fun `IV-09 different currency with valid exchange rate passes`() {
        val item = testItem(itemCurrency = "USD", exchangeRate = 350.50)
        val result = ItemValidator.validate(item, memberIds, profileNameResolver, eventBaseCurrency = "ARS")
        assertFalse(result.allErrors().any { it.field == "exchangeRate" })
    }

    @Test
    fun `IV-09 different currency with zero exchange rate returns error`() {
        val item = testItem(itemCurrency = "USD", exchangeRate = 0.0)
        val result = ItemValidator.validate(item, memberIds, profileNameResolver, eventBaseCurrency = "ARS")
        assertTrue(result.hasErrors())
        assertEquals("El tipo de cambio debe ser mayor a cero", result.allErrors().first { it.field == "exchangeRate" }.message)
    }

    @Test
    fun `IV-09 different currency with null exchange rate returns error`() {
        val item = testItem(itemCurrency = "USD", exchangeRate = null)
        val result = ItemValidator.validate(item, memberIds, profileNameResolver, eventBaseCurrency = "ARS")
        assertTrue(result.hasErrors())
        assertEquals("El tipo de cambio debe ser mayor a cero", result.allErrors().first { it.field == "exchangeRate" }.message)
    }

    @Test
    fun `IV-09 different currency with negative exchange rate returns error`() {
        val item = testItem(itemCurrency = "USD", exchangeRate = -1.0)
        val result = ItemValidator.validate(item, memberIds, profileNameResolver, eventBaseCurrency = "ARS")
        assertTrue(result.hasErrors())
        assertEquals("El tipo de cambio debe ser mayor a cero", result.allErrors().first { it.field == "exchangeRate" }.message)
    }

    // ── Combined scenarios ──────────────────────────────────────────────────

    @Test
    fun `fully valid item returns Success`() {
        val item = testItem()
        val result = ItemValidator.validate(item, memberIds, profileNameResolver)
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `valid item with blank name returns SuccessWithWarnings`() {
        val item = testItem(name = "")
        val result = ItemValidator.validate(item, memberIds, profileNameResolver)
        assertTrue(result is ValidationResult.SuccessWithWarnings)
        assertEquals(1, result.warnings.size)
    }

    @Test
    fun `item with multiple errors aggregates all`() {
        val item = testItem(
            amountEuros = 0.0,
            payerContributions = emptyMap(),
            debtorIds = emptyList(),
            splitMode = "",
        )
        val result = ItemValidator.validate(item, memberIds, profileNameResolver)
        assertTrue(result is ValidationResult.Failure)
        assertEquals(4, result.errors.size)
    }
}
