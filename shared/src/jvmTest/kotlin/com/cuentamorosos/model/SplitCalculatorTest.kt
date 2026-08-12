package com.cuentamorosos.model

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SplitCalculatorTest {

    private val delta = 0.001

    // ── IGUAL (Equal Split) ─────────────────────────────────────────────────

    @Test
    fun `IGUAL - standard 3-way split with remainder`() {
        val result = SplitCalculator.calculateEqual(10.00, listOf("A", "B", "C"))
        assertEquals(3, result.size)
        assertEquals(3.34, result["A"]!!, delta)
        assertEquals(3.33, result["B"]!!, delta)
        assertEquals(3.33, result["C"]!!, delta)
        assertTrue(SplitCalculator.verifySum(result, 10.00))
    }

    @Test
    fun `IGUAL - exact division no remainder`() {
        val result = SplitCalculator.calculateEqual(9.00, listOf("A", "B", "C"))
        assertEquals(3.00, result["A"]!!, delta)
        assertEquals(3.00, result["B"]!!, delta)
        assertEquals(3.00, result["C"]!!, delta)
        assertTrue(SplitCalculator.verifySum(result, 9.00))
    }

    @Test
    fun `IGUAL - single debtor gets full amount`() {
        val result = SplitCalculator.calculateEqual(42.50, listOf("A"))
        assertEquals(1, result.size)
        assertEquals(42.50, result["A"]!!, delta)
        assertTrue(SplitCalculator.verifySum(result, 42.50))
    }

    @Test
    fun `IGUAL - empty debtor list throws`() {
        assertFailsWith<IllegalArgumentException> {
            SplitCalculator.calculateEqual(10.00, emptyList())
        }
    }

    @Test
    fun `IGUAL - 1 cent remainder goes to first debtor`() {
        val result = SplitCalculator.calculateEqual(1.00, listOf("A", "B", "C"))
        assertEquals(0.34, result["A"]!!, delta)
        assertEquals(0.33, result["B"]!!, delta)
        assertEquals(0.33, result["C"]!!, delta)
        assertTrue(SplitCalculator.verifySum(result, 1.00))
    }

    @Test
    fun `IGUAL - large amount many debtors`() {
        val ids = listOf("A", "B", "C", "D", "E", "F", "G")
        val result = SplitCalculator.calculateEqual(10000.00, ids)
        assertEquals(1428.58, result["A"]!!, delta)
        for (i in 1 until ids.size) {
            assertEquals(1428.57, result[ids[i]]!!, delta)
        }
        assertTrue(SplitCalculator.verifySum(result, 10000.00))
    }

    @Test
    fun `IGUAL - one cent split among three`() {
        val result = SplitCalculator.calculateEqual(0.01, listOf("A", "B", "C"))
        assertEquals(0.01, result["A"]!!, delta)
        assertEquals(0.00, result["B"]!!, delta)
        assertEquals(0.00, result["C"]!!, delta)
        assertTrue(SplitCalculator.verifySum(result, 0.01))
    }

    // ── EXACTO (Exact Amounts) ──────────────────────────────────────────────

    @Test
    fun `EXACTO - exact match passes`() {
        val amounts = mapOf("A" to 30.00, "B" to 45.50, "C" to 24.50)
        val result = SplitCalculator.calculateExact(100.00, amounts)
        assertEquals(30.00, result["A"]!!, delta)
        assertEquals(45.50, result["B"]!!, delta)
        assertEquals(24.50, result["C"]!!, delta)
        assertTrue(SplitCalculator.verifySum(result, 100.00))
    }

    @Test
    fun `EXACTO - within tolerance passes`() {
        val amounts = mapOf("A" to 30.00, "B" to 45.50, "C" to 24.51)
        val result = SplitCalculator.calculateExact(100.00, amounts)
        assertEquals(3, result.size)
        assertTrue(SplitCalculator.verifySum(result, 100.00))
    }

    @Test
    fun `EXACTO - sum mismatch throws`() {
        val amounts = mapOf("A" to 30.00, "B" to 45.50, "C" to 25.00)
        assertFailsWith<IllegalArgumentException> {
            SplitCalculator.calculateExact(100.00, amounts)
        }
    }

    @Test
    fun `EXACTO - amount below minimum throws`() {
        val amounts = mapOf("A" to 0.00, "B" to 10.00)
        assertFailsWith<IllegalArgumentException> {
            SplitCalculator.calculateExact(10.00, amounts)
        }
    }

    @Test
    fun `EXACTO - single debtor`() {
        val amounts = mapOf("A" to 50.00)
        val result = SplitCalculator.calculateExact(50.00, amounts)
        assertEquals(50.00, result["A"]!!, delta)
    }

    @Test
    fun `EXACTO - empty amounts throws`() {
        assertFailsWith<IllegalArgumentException> {
            SplitCalculator.calculateExact(10.00, emptyMap())
        }
    }

    // ── PORCENTAJE (Percentage) ─────────────────────────────────────────────

    @Test
    fun `PORCENTAJE - standard 50-30-20 split`() {
        val pcts = mapOf("A" to 50.0, "B" to 30.0, "C" to 20.0)
        val result = SplitCalculator.calculatePercentage(100.00, pcts)
        assertEquals(50.00, result["A"]!!, delta)
        assertEquals(30.00, result["B"]!!, delta)
        assertEquals(20.00, result["C"]!!, delta)
        assertTrue(SplitCalculator.verifySum(result, 100.00))
    }

    @Test
    fun `PORCENTAJE - 33-33-34 remainder to highest`() {
        val pcts = mapOf("A" to 33.33, "B" to 33.33, "C" to 33.34)
        val result = SplitCalculator.calculatePercentage(10.00, pcts)
        assertTrue(SplitCalculator.verifySum(result, 10.00))
        // C has highest percentage, should get the remainder
        assertTrue(result["C"]!! >= 3.33)
    }

    @Test
    fun `PORCENTAJE - sum not 100 throws`() {
        val pcts = mapOf("A" to 50.0, "B" to 40.0)
        assertFailsWith<IllegalArgumentException> {
            SplitCalculator.calculatePercentage(100.00, pcts)
        }
    }

    @Test
    fun `PORCENTAJE - percentage out of range throws`() {
        val pcts = mapOf("A" to 110.0, "B" to -10.0)
        assertFailsWith<IllegalArgumentException> {
            SplitCalculator.calculatePercentage(100.00, pcts)
        }
    }

    @Test
    fun `PORCENTAJE - zero percentage allowed`() {
        val pcts = mapOf("A" to 100.0, "B" to 0.0)
        val result = SplitCalculator.calculatePercentage(100.00, pcts)
        assertEquals(100.00, result["A"]!!, delta)
        assertEquals(0.00, result["B"]!!, delta)
        assertTrue(SplitCalculator.verifySum(result, 100.00))
    }

    @Test
    fun `PORCENTAJE - single debtor 100 percent`() {
        val pcts = mapOf("A" to 100.0)
        val result = SplitCalculator.calculatePercentage(75.50, pcts)
        assertEquals(75.50, result["A"]!!, delta)
        assertTrue(SplitCalculator.verifySum(result, 75.50))
    }

    @Test
    fun `PORCENTAJE - empty percentages throws`() {
        assertFailsWith<IllegalArgumentException> {
            SplitCalculator.calculatePercentage(10.00, emptyMap())
        }
    }

    // ── PARTES (Parts) ──────────────────────────────────────────────────────

    @Test
    fun `PARTES - standard 3-2-1 ratio`() {
        val parts = mapOf("A" to 3, "B" to 2, "C" to 1)
        val result = SplitCalculator.calculateParts(100.00, parts)
        // totalParts=6, partValue=1666 cents, remainder=4 cents → A gets +4
        assertEquals(50.02, result["A"]!!, delta)
        assertEquals(33.32, result["B"]!!, delta)
        assertEquals(16.66, result["C"]!!, delta)
        assertTrue(SplitCalculator.verifySum(result, 100.00))
    }

    @Test
    fun `PARTES - equal parts same as IGUAL`() {
        val parts = mapOf("A" to 1, "B" to 1, "C" to 1)
        val result = SplitCalculator.calculateParts(9.00, parts)
        assertEquals(3.00, result["A"]!!, delta)
        assertEquals(3.00, result["B"]!!, delta)
        assertEquals(3.00, result["C"]!!, delta)
        assertTrue(SplitCalculator.verifySum(result, 9.00))
    }

    @Test
    fun `PARTES - sum zero throws`() {
        assertFailsWith<IllegalArgumentException> {
            SplitCalculator.calculateParts(100.00, emptyMap())
        }
    }

    @Test
    fun `PARTES - part value zero throws`() {
        val parts = mapOf("A" to 0, "B" to 5)
        assertFailsWith<IllegalArgumentException> {
            SplitCalculator.calculateParts(100.00, parts)
        }
    }

    @Test
    fun `PARTES - part value exceeds 100 throws`() {
        val parts = mapOf("A" to 101, "B" to 5)
        assertFailsWith<IllegalArgumentException> {
            SplitCalculator.calculateParts(100.00, parts)
        }
    }

    @Test
    fun `PARTES - single debtor`() {
        val parts = mapOf("A" to 5)
        val result = SplitCalculator.calculateParts(50.00, parts)
        assertEquals(50.00, result["A"]!!, delta)
        assertTrue(SplitCalculator.verifySum(result, 50.00))
    }

    // ── INVARIANT: verifySum ────────────────────────────────────────────────

    @Test
    fun `verifySum - returns true for matching sum`() {
        val amounts = mapOf("A" to 3.33, "B" to 3.33, "C" to 3.34)
        assertTrue(SplitCalculator.verifySum(amounts, 10.00))
    }

    @Test
    fun `verifySum - returns true within tolerance`() {
        val amounts = mapOf("A" to 3.33, "B" to 3.33, "C" to 3.33)
        assertTrue(SplitCalculator.verifySum(amounts, 9.99))
    }

    @Test
    fun `verifySum - returns false for mismatch beyond tolerance`() {
        val amounts = mapOf("A" to 3.00, "B" to 3.00, "C" to 3.00)
        assertTrue(!SplitCalculator.verifySum(amounts, 10.00))
    }

    @Test
    fun `verifySum - empty amounts returns true for zero total`() {
        assertTrue(SplitCalculator.verifySum(emptyMap(), 0.00))
    }

    // ── INVARIANT: all happy-path results sum to total ──────────────────────

    @Test
    fun `INVARIANT - IGUAL always sums to total`() {
        val testCases = listOf(
            10.00 to listOf("A", "B", "C"),
            9.00 to listOf("A", "B", "C"),
            42.50 to listOf("A"),
            10000.00 to listOf("A", "B", "C", "D", "E", "F", "G"),
            0.01 to listOf("A", "B", "C"),
        )
        testCases.forEach { (total, ids) ->
            val result = SplitCalculator.calculateEqual(total, ids)
            assertTrue(
                SplitCalculator.verifySum(result, total),
                "IGUAL failed for total=$total, ids=$ids"
            )
        }
    }

    @Test
    fun `INVARIANT - PORCENTAJE always sums to total`() {
        val testCases = listOf(
            100.00 to mapOf("A" to 50.0, "B" to 30.0, "C" to 20.0),
            10.00 to mapOf("A" to 33.33, "B" to 33.33, "C" to 33.34),
            75.50 to mapOf("A" to 100.0),
            100.00 to mapOf("A" to 100.0, "B" to 0.0),
        )
        testCases.forEach { (total, pcts) ->
            val result = SplitCalculator.calculatePercentage(total, pcts)
            assertTrue(
                SplitCalculator.verifySum(result, total),
                "PORCENTAJE failed for total=$total, pcts=$pcts"
            )
        }
    }

    @Test
    fun `INVARIANT - PARTES always sums to total`() {
        val testCases = listOf(
            100.00 to mapOf("A" to 3, "B" to 2, "C" to 1),
            9.00 to mapOf("A" to 1, "B" to 1, "C" to 1),
            50.00 to mapOf("A" to 5),
        )
        testCases.forEach { (total, parts) ->
            val result = SplitCalculator.calculateParts(total, parts)
            assertTrue(
                SplitCalculator.verifySum(result, total),
                "PARTES failed for total=$total, parts=$parts"
            )
        }
    }

    // ── UI-LEVEL: EXACT mode sum validation (string inputs, ±0.01€ tolerance) ──

    /**
     * Simulates the EXACT mode validation logic from CalculatorSheet:
     * parse string inputs → sum → compare with total ±0.01 tolerance.
     */
    private fun validateExactSum(inputs: List<String>, total: Double?): Boolean {
        val parsed = inputs.mapNotNull { value ->
            val trimmed = value.trim().replace(',', '.')
            if (trimmed.isEmpty()) null else trimmed.toBigDecimalOrNull()
        }
        val sum = parsed.fold(BigDecimal.ZERO) { acc, bd -> acc.add(bd) }
        return total?.let {
            val diff = sum.subtract(it.toBigDecimal()).abs()
            diff <= BigDecimal("0.01")
        } == true
    }

    @Test
    fun `EXACT UI validation - sum matches total exactly`() {
        val inputs = listOf("30.00", "45.50", "24.50")
        assertTrue(validateExactSum(inputs, 100.00))
    }

    @Test
    fun `EXACT UI validation - within 1 cent tolerance passes`() {
        val inputs = listOf("30.00", "45.50", "24.51")
        assertTrue(validateExactSum(inputs, 100.00))
    }

    @Test
    fun `EXACT UI validation - exceeds tolerance fails`() {
        val inputs = listOf("30.00", "45.50", "25.00")
        assertTrue(!validateExactSum(inputs, 100.00))
    }

    @Test
    fun `EXACT UI validation - comma decimal separator works`() {
        val inputs = listOf("30,00", "45,50", "24,50")
        assertTrue(validateExactSum(inputs, 100.00))
    }

    @Test
    fun `EXACT UI validation - empty strings are ignored in sum`() {
        val inputs = listOf("50.00", "", "50.00")
        assertTrue(validateExactSum(inputs, 100.00))
    }

    @Test
    fun `EXACT UI validation - all empty strings fails`() {
        val inputs = listOf("", "", "")
        // Sum = 0, total = 100 → fails
        assertTrue(!validateExactSum(inputs, 100.00))
    }

    @Test
    fun `EXACT UI validation - null total always fails`() {
        val inputs = listOf("30.00", "45.50", "24.50")
        assertTrue(!validateExactSum(inputs, null))
    }

    @Test
    fun `EXACT UI validation - negative 1 cent tolerance passes`() {
        val inputs = listOf("30.00", "45.50", "24.49")
        assertTrue(validateExactSum(inputs, 100.00))
    }

    // ── UI-LEVEL: PARTS mode validation (string inputs, sum > 0, range 1-100) ──

    /**
     * Simulates the PARTS mode validation logic from CalculatorSheet:
     * parse string inputs as integers → clamp 1-100 → sum must be > 0.
     */
    private fun validatePartsSum(inputs: List<String>): Boolean {
        val parsed = inputs.mapNotNull { it.toIntOrNull() }
        return parsed.sum() > 0
    }

    private fun clampPartsValue(value: String): String {
        val intValue = value.toIntOrNull()
        return when {
            intValue == null -> value.filter { it.isDigit() }.takeIf { it.isNotEmpty() } ?: ""
            intValue < 1 -> "1"
            intValue > 100 -> "100"
            else -> intValue.toString()
        }
    }

    @Test
    fun `PARTS UI validation - standard inputs pass`() {
        val inputs = listOf("3", "2", "1")
        assertTrue(validatePartsSum(inputs))
    }

    @Test
    fun `PARTS UI validation - all ones passes`() {
        val inputs = listOf("1", "1", "1")
        assertTrue(validatePartsSum(inputs))
    }

    @Test
    fun `PARTS UI validation - empty string treated as invalid`() {
        val inputs = listOf("1", "", "1")
        // "" → toIntOrNull() = null → filtered out → sum = 2 > 0 → passes
        assertTrue(validatePartsSum(inputs))
    }

    @Test
    fun `PARTS UI validation - all empty strings fails`() {
        val inputs = listOf("", "", "")
        assertTrue(!validatePartsSum(inputs))
    }

    @Test
    fun `PARTS UI clamping - value below 1 clamps to 1`() {
        assertEquals("1", clampPartsValue("0"))
        assertEquals("1", clampPartsValue("-5"))
    }

    @Test
    fun `PARTS UI clamping - value above 100 clamps to 100`() {
        assertEquals("100", clampPartsValue("101"))
        assertEquals("100", clampPartsValue("999"))
    }

    @Test
    fun `PARTS UI clamping - valid values pass through`() {
        assertEquals("50", clampPartsValue("50"))
        assertEquals("1", clampPartsValue("1"))
        assertEquals("100", clampPartsValue("100"))
    }

    @Test
    fun `PARTS UI clamping - non-numeric input returns empty`() {
        assertEquals("", clampPartsValue("abc"))
        assertEquals("", clampPartsValue(""))
    }

    @Test
    fun `PARTS UI clamping - digits only filter strips non-digits`() {
        assertEquals("50", clampPartsValue("5a0"))
        assertEquals("100", clampPartsValue("100!"))
    }
}
