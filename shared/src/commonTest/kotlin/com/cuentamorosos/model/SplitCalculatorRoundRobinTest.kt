package com.cuentamorosos.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for round-robin remainder distribution in SplitCalculator.calculateEqual().
 *
 * The seed parameter controls the starting offset for distributing remainder cents.
 * seed=0 (default) starts distributing from index 0 — backward compatible.
 */
class SplitCalculatorRoundRobinTest {

    private val delta = 0.001

    // ── Scenario: Five debtors with remainder (57.74€ ÷ 5) ──────────────────

    @Test
    fun `57_74 divided by 5 debtors — all within 1 cent of average, no single outlier`() {
        val result = SplitCalculator.calculateEqual(57.74, listOf("A", "B", "C", "D", "E"), seed = 0)

        // Average is 11.548. Every share must be 11.54 or 11.55 (within 1 cent).
        val amounts = result.values
        assertTrue(amounts.all { it == 11.54 || it == 11.55 }, "All amounts should be 11.54 or 11.55, got $amounts")

        // No single outlier at 11.58 (old behaviour: index 0 got all 4 remainder cents)
        assertTrue(amounts.none { it >= 11.56 }, "No amount should be 11.58 (old outlier)")

        // Total must sum to 57.74
        assertTrue(SplitCalculator.verifySum(result, 57.74), "Must sum to 57.74")
    }

    // ── Scenario: Exact division (100€ ÷ 4) ──────────────────────────────────

    @Test
    fun `exact division — zero remainder`() {
        val result = SplitCalculator.calculateEqual(100.00, listOf("A", "B", "C", "D"), seed = 0)

        assertEquals(25.00, result["A"]!!, delta)
        assertEquals(25.00, result["B"]!!, delta)
        assertEquals(25.00, result["C"]!!, delta)
        assertEquals(25.00, result["D"]!!, delta)
        assertTrue(SplitCalculator.verifySum(result, 100.00))
    }

    // ── Scenario: Pennies with more debtors than cents (0.03€ ÷ 5) ───────────

    @Test
    fun `pennies — more debtors than cents, 0_03 divided by 5`() {
        // 3 cents / 5 debtors: base 0, remainder 3
        // Round-robin from index 0: indices 0, 1, 2 get +1 cent each
        val result = SplitCalculator.calculateEqual(0.03, listOf("A", "B", "C", "D", "E"), seed = 0)

        assertEquals(0.01, result["A"]!!, delta, "A (index 0) should get 0.01")
        assertEquals(0.01, result["B"]!!, delta, "B (index 1) should get 0.01")
        assertEquals(0.01, result["C"]!!, delta, "C (index 2) should get 0.01")
        assertEquals(0.00, result["D"]!!, delta, "D (index 3) should get 0.00")
        assertEquals(0.00, result["E"]!!, delta, "E (index 4) should get 0.00")

        assertTrue(SplitCalculator.verifySum(result, 0.03))
    }

    // ── Scenario: Multi-cent remainder smaller than debtor count (0.05€ ÷ 3) ─

    @Test
    fun `remainder cents distributed — 0_05 divided by 3`() {
        // 5 cents / 3 debtors: base 1, remainder 2
        // Round-robin from index 0: indices 0, 1 get +1 cent each → 2, 2, 1 cents
        val result = SplitCalculator.calculateEqual(0.05, listOf("A", "B", "C"), seed = 0)

        assertEquals(0.02, result["A"]!!, delta, "A (index 0) gets remainder → 0.02")
        assertEquals(0.02, result["B"]!!, delta, "B (index 1) gets remainder → 0.02")
        assertEquals(0.01, result["C"]!!, delta, "C (index 2) no remainder → 0.01")

        assertTrue(SplitCalculator.verifySum(result, 0.05))
    }

    // ── Scenario: Different seed changes start position ──────────────────────

    @Test
    fun `seed offset changes which debtor gets remainder`() {
        // 0.07€ = 7 cents, 3 debtors: base 2, remainder 1
        // seed=0 → startIndex=0 → debtor A gets +1 (3,2,2 cents)
        val r0 = SplitCalculator.calculateEqual(0.07, listOf("A", "B", "C"), seed = 0)
        assertEquals(0.03, r0["A"]!!, delta, "seed=0: A gets remainder")
        assertEquals(0.02, r0["B"]!!, delta)
        assertEquals(0.02, r0["C"]!!, delta)
        assertTrue(SplitCalculator.verifySum(r0, 0.07))

        // seed=2 → startIndex=2 → debtor C gets +1 (2,2,3 cents)
        val r2 = SplitCalculator.calculateEqual(0.07, listOf("A", "B", "C"), seed = 2)
        assertEquals(0.02, r2["A"]!!, delta, "seed=2: C gets remainder")
        assertEquals(0.02, r2["B"]!!, delta)
        assertEquals(0.03, r2["C"]!!, delta)
        assertTrue(SplitCalculator.verifySum(r2, 0.07))

        // seed=1 → startIndex=1 → debtor B gets +1 (2,3,2 cents)
        val r1 = SplitCalculator.calculateEqual(0.07, listOf("A", "B", "C"), seed = 1)
        assertEquals(0.02, r1["A"]!!, delta)
        assertEquals(0.03, r1["B"]!!, delta, "seed=1: B gets remainder")
        assertEquals(0.02, r1["C"]!!, delta)
        assertTrue(SplitCalculator.verifySum(r1, 0.07))
    }

    // ── Scenario: Round-robin wraps around ───────────────────────────────────

    @Test
    fun `round-robin wraps when startIndex plus remainder exceeds size`() {
        // 0.10€ = 10 cents, 4 debtors: base 2, remainder 2
        // seed=3 → startIndex=3 → indices 3 and 0 get +1
        val result = SplitCalculator.calculateEqual(0.10, listOf("A", "B", "C", "D"), seed = 3)

        assertEquals(0.03, result["A"]!!, delta, "A (index 0) gets wrapped remainder → 0.03")
        assertEquals(0.02, result["B"]!!, delta, "B (index 1) → 0.02")
        assertEquals(0.02, result["C"]!!, delta, "C (index 2) → 0.02")
        assertEquals(0.03, result["D"]!!, delta, "D (index 3, start) gets remainder → 0.03")

        assertTrue(SplitCalculator.verifySum(result, 0.10))
    }

    // ── Scenario: Deterministic reproducibility ──────────────────────────────

    @Test
    fun `same seed produces same distribution every invocation`() {
        val ids = listOf("A", "B", "C", "D", "E")
        val seed = 42

        val run1 = SplitCalculator.calculateEqual(57.74, ids, seed = seed)
        val run2 = SplitCalculator.calculateEqual(57.74, ids, seed = seed)
        val run3 = SplitCalculator.calculateEqual(57.74, ids, seed = seed)

        ids.forEach { id ->
            assertEquals(run1[id]!!, run2[id]!!, delta, "$id differs between run1 and run2")
            assertEquals(run1[id]!!, run3[id]!!, delta, "$id differs between run1 and run3")
        }
    }

    // ── Scenario: Seed=0 default preserves backward compatibility ────────────

    @Test
    fun `seed equals 0 default — first debtor gets all remainder`() {
        // With seed=0 and small remainder, index 0 still gets the extra
        // This is the same behavior as the old code (backward compatible)
        val result = SplitCalculator.calculateEqual(1.00, listOf("A", "B", "C"), seed = 0)

        // 100 cents / 3 = 33 base, 1 remainder → A gets +1 → 34, B,C get 33
        assertEquals(0.34, result["A"]!!, delta, "A (index 0, seed=0) gets remainder")
        assertEquals(0.33, result["B"]!!, delta)
        assertEquals(0.33, result["C"]!!, delta)
        assertTrue(SplitCalculator.verifySum(result, 1.00))
    }

    @Test
    fun `seed default parameter — backward compatible call without seed`() {
        // Calling calculateEqual WITHOUT seed should work (seed defaults to 0)
        val result = SplitCalculator.calculateEqual(1.00, listOf("A", "B", "C"))

        // Same as seed=0: first debtor gets the 1 cent remainder
        assertEquals(0.34, result["A"]!!, delta)
        assertEquals(0.33, result["B"]!!, delta)
        assertEquals(0.33, result["C"]!!, delta)
        assertTrue(SplitCalculator.verifySum(result, 1.00))
    }

    // ── Scenario: Single debtor with seed ────────────────────────────────────

    @Test
    fun `single debtor with seed param works`() {
        val result = SplitCalculator.calculateEqual(42.50, listOf("A"), seed = 7)

        assertEquals(1, result.size)
        assertEquals(42.50, result["A"]!!, delta)
        assertTrue(SplitCalculator.verifySum(result, 42.50))
    }
}
