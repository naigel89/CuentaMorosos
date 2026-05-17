package com.cuentamorosos.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StateMachineTest {

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun defaultContext(
        eventName: String = "Asado Domingo",
        eventBaseCurrency: String = "ARS",
        memberCount: Int = 3,
        expenseCount: Int = 2,
        hasInvalidExpenses: Boolean = false,
        isOwner: Boolean = true,
        transfersPaidPercentage: Double = 0.0,
        pendingPayments: Int = 0,
    ) = TransitionContext(
        eventName = eventName,
        eventBaseCurrency = eventBaseCurrency,
        memberCount = memberCount,
        expenseCount = expenseCount,
        hasInvalidExpenses = hasInvalidExpenses,
        isOwner = isOwner,
        transfersPaidPercentage = transfersPaidPercentage,
        pendingPayments = pendingPayments,
    )

    // ── ST-01: DRAFT → OPEN ─────────────────────────────────────────────────

    @Test
    fun `ST-01 successful DRAFT to OPEN transition`() {
        val ctx = defaultContext()
        val result = attemptTransition(EventState.DRAFT, EventState.OPEN, ctx)
        assertTrue(result is StateTransitionResult.Allowed)
        assertEquals(EventState.OPEN, (result as StateTransitionResult.Allowed).newState)
    }

    @Test
    fun `ST-01 DRAFT to OPEN blocked — blank name`() {
        val ctx = defaultContext(eventName = "")
        val result = attemptTransition(EventState.DRAFT, EventState.OPEN, ctx)
        assertTrue(result is StateTransitionResult.Blocked)
        val reasons = (result as StateTransitionResult.Blocked).reasons
        assertTrue(reasons.any { it.contains("nombre") })
    }

    @Test
    fun `ST-01 DRAFT to OPEN blocked — blank baseCurrency`() {
        val ctx = defaultContext(eventBaseCurrency = "")
        val result = attemptTransition(EventState.DRAFT, EventState.OPEN, ctx)
        assertTrue(result is StateTransitionResult.Blocked)
        val reasons = (result as StateTransitionResult.Blocked).reasons
        assertTrue(reasons.any { it.contains("divisa") })
    }

    @Test
    fun `ST-01 DRAFT to OPEN blocked — insufficient members`() {
        val ctx = defaultContext(memberCount = 1)
        val result = attemptTransition(EventState.DRAFT, EventState.OPEN, ctx)
        assertTrue(result is StateTransitionResult.Blocked)
        val reasons = (result as StateTransitionResult.Blocked).reasons
        assertTrue(reasons.any { it.contains("2 participantes") })
    }

    @Test
    fun `ST-01 DRAFT to OPEN blocked — multiple preconditions fail`() {
        val ctx = defaultContext(eventName = "", eventBaseCurrency = "", memberCount = 1)
        val result = attemptTransition(EventState.DRAFT, EventState.OPEN, ctx)
        assertTrue(result is StateTransitionResult.Blocked)
        val reasons = (result as StateTransitionResult.Blocked).reasons
        assertEquals(3, reasons.size)
    }

    // ── ST-02: OPEN → CALCULATED ────────────────────────────────────────────

    @Test
    fun `ST-02 successful OPEN to CALCULATED transition`() {
        val ctx = defaultContext()
        val result = attemptTransition(EventState.OPEN, EventState.CALCULATED, ctx)
        assertTrue(result is StateTransitionResult.Allowed)
        assertEquals(EventState.CALCULATED, (result as StateTransitionResult.Allowed).newState)
    }

    @Test
    fun `ST-02 OPEN to CALCULATED blocked — no expenses`() {
        val ctx = defaultContext(expenseCount = 0)
        val result = attemptTransition(EventState.OPEN, EventState.CALCULATED, ctx)
        assertTrue(result is StateTransitionResult.Blocked)
        val reasons = (result as StateTransitionResult.Blocked).reasons
        assertTrue(reasons.any { it.contains("gastos") })
    }

    @Test
    fun `ST-02 OPEN to CALCULATED blocked — invalid expenses`() {
        val ctx = defaultContext(hasInvalidExpenses = true)
        val result = attemptTransition(EventState.OPEN, EventState.CALCULATED, ctx)
        assertTrue(result is StateTransitionResult.Blocked)
        val reasons = (result as StateTransitionResult.Blocked).reasons
        assertTrue(reasons.any { it.contains("inválidos") })
    }

    // ── ST-03: CALCULATED → OPEN (Recalculate) ──────────────────────────────

    @Test
    fun `ST-03 successful CALCULATED to OPEN recalculation`() {
        val ctx = defaultContext(transfersPaidPercentage = 60.0)
        val result = attemptTransition(EventState.CALCULATED, EventState.OPEN, ctx)
        assertTrue(result is StateTransitionResult.AllowedWithWarning)
        val warning = result as StateTransitionResult.AllowedWithWarning
        assertEquals(EventState.OPEN, warning.newState)
        assertTrue(warning.warning.contains("revertirán"))
    }

    @Test
    fun `ST-03 CALCULATED to OPEN blocked — all transfers paid`() {
        val ctx = defaultContext(transfersPaidPercentage = 100.0)
        val result = attemptTransition(EventState.CALCULATED, EventState.OPEN, ctx)
        assertTrue(result is StateTransitionResult.Blocked)
        val reasons = (result as StateTransitionResult.Blocked).reasons
        assertTrue(reasons.any { it.contains("recalcular") })
    }

    // ── ST-04: CALCULATED → CLOSED ──────────────────────────────────────────

    @Test
    fun `ST-04 successful CALCULATED to CLOSED by owner`() {
        val ctx = defaultContext(isOwner = true, pendingPayments = 0)
        val result = attemptTransition(EventState.CALCULATED, EventState.CLOSED, ctx)
        assertTrue(result is StateTransitionResult.Allowed)
        assertEquals(EventState.CLOSED, (result as StateTransitionResult.Allowed).newState)
    }

    @Test
    fun `ST-04 CALCULATED to CLOSED with warning — pending payments`() {
        val ctx = defaultContext(isOwner = true, pendingPayments = 3)
        val result = attemptTransition(EventState.CALCULATED, EventState.CLOSED, ctx)
        assertTrue(result is StateTransitionResult.AllowedWithWarning)
        val warning = result as StateTransitionResult.AllowedWithWarning
        assertEquals(EventState.CLOSED, warning.newState)
        assertTrue(warning.warning.contains("pendientes"))
    }

    @Test
    fun `ST-04 CALCULATED to CLOSED blocked — not owner`() {
        val ctx = defaultContext(isOwner = false)
        val result = attemptTransition(EventState.CALCULATED, EventState.CLOSED, ctx)
        assertTrue(result is StateTransitionResult.Blocked)
        val reasons = (result as StateTransitionResult.Blocked).reasons
        assertTrue(reasons.any { it.contains("propietario") })
    }

    // ── ST-05: CLOSED State Immutability ─────────────────────────────────────

    @Test
    fun `ST-05 CLOSED to DRAFT blocked`() {
        val ctx = defaultContext()
        val result = attemptTransition(EventState.CLOSED, EventState.DRAFT, ctx)
        assertTrue(result is StateTransitionResult.Blocked)
        val reasons = (result as StateTransitionResult.Blocked).reasons
        assertTrue(reasons.any { it.contains("cerrado") })
    }

    @Test
    fun `ST-05 CLOSED to OPEN blocked`() {
        val ctx = defaultContext()
        val result = attemptTransition(EventState.CLOSED, EventState.OPEN, ctx)
        assertTrue(result is StateTransitionResult.Blocked)
    }

    @Test
    fun `ST-05 CLOSED to CALCULATED blocked`() {
        val ctx = defaultContext()
        val result = attemptTransition(EventState.CLOSED, EventState.CALCULATED, ctx)
        assertTrue(result is StateTransitionResult.Blocked)
    }

    // ── Invalid transitions ──────────────────────────────────────────────────

    @Test
    fun `DRAFT to CALCULATED is invalid (skip state)`() {
        val ctx = defaultContext()
        val result = attemptTransition(EventState.DRAFT, EventState.CALCULATED, ctx)
        assertTrue(result is StateTransitionResult.Blocked)
        val reasons = (result as StateTransitionResult.Blocked).reasons
        assertTrue(reasons.any { it.contains("Transición no válida") })
    }

    @Test
    fun `DRAFT to CLOSED is invalid (skip state)`() {
        val ctx = defaultContext()
        val result = attemptTransition(EventState.DRAFT, EventState.CLOSED, ctx)
        assertTrue(result is StateTransitionResult.Blocked)
    }

    @Test
    fun `OPEN to DRAFT is invalid (backward)`() {
        val ctx = defaultContext()
        val result = attemptTransition(EventState.OPEN, EventState.DRAFT, ctx)
        assertTrue(result is StateTransitionResult.Blocked)
    }

    @Test
    fun `OPEN to CLOSED is invalid (skip state)`() {
        val ctx = defaultContext()
        val result = attemptTransition(EventState.OPEN, EventState.CLOSED, ctx)
        assertTrue(result is StateTransitionResult.Blocked)
    }

    // ── Same-state transitions ───────────────────────────────────────────────

    @Test
    fun `same-state transition is blocked`() {
        val ctx = defaultContext()
        for (state in EventState.entries) {
            val result = attemptTransition(state, state, ctx)
            assertTrue(result is StateTransitionResult.Blocked, "Same-state $state → $state should be blocked")
        }
    }

    // ── Helper functions ─────────────────────────────────────────────────────

    @Test
    fun `isDraft returns true for DRAFT state`() {
        val event = EventItem(name = "Test", dateMillis = 0L, ownerId = "u1", state = EventState.DRAFT)
        assertTrue(event.isDraft())
    }

    @Test
    fun `isOpen returns true for OPEN state`() {
        val event = EventItem(name = "Test", dateMillis = 0L, ownerId = "u1", state = EventState.OPEN)
        assertTrue(event.isOpen())
    }

    @Test
    fun `isCalculated returns true for CALCULATED state`() {
        val event = EventItem(name = "Test", dateMillis = 0L, ownerId = "u1", state = EventState.CALCULATED)
        assertTrue(event.isCalculated())
    }

    @Test
    fun `isClosed returns true for CLOSED state`() {
        val event = EventItem(name = "Test", dateMillis = 0L, ownerId = "u1", state = EventState.CLOSED)
        assertTrue(event.isClosed())
    }

    @Test
    fun `stateLabel returns correct Spanish labels`() {
        assertEquals("Borrador", EventItem(name = "T", dateMillis = 0, ownerId = "u1", state = EventState.DRAFT).stateLabel())
        assertEquals("Abierto", EventItem(name = "T", dateMillis = 0, ownerId = "u1", state = EventState.OPEN).stateLabel())
        assertEquals("Calculado", EventItem(name = "T", dateMillis = 0, ownerId = "u1", state = EventState.CALCULATED).stateLabel())
        assertEquals("Cerrado", EventItem(name = "T", dateMillis = 0, ownerId = "u1", state = EventState.CLOSED).stateLabel())
    }

    @Test
    fun `canTransitionTo delegates to attemptTransition`() {
        val event = EventItem(name = "Test", dateMillis = 0L, ownerId = "u1", state = EventState.DRAFT)
        val ctx = defaultContext()
        val result = event.canTransitionTo(EventState.OPEN, ctx)
        assertTrue(result is StateTransitionResult.Allowed)
    }

    // ── StateTransitionResult sealed class ───────────────────────────────────

    @Test
    fun `Allowed result carries target state`() {
        val allowed = StateTransitionResult.Allowed(EventState.OPEN)
        assertEquals(EventState.OPEN, allowed.newState)
    }

    @Test
    fun `Blocked result carries all applicable reasons`() {
        val reasons = listOf("Reason 1", "Reason 2")
        val blocked = StateTransitionResult.Blocked(reasons)
        assertEquals(2, blocked.reasons.size)
        assertEquals("Reason 1", blocked.reasons[0])
        assertEquals("Reason 2", blocked.reasons[1])
    }

    @Test
    fun `AllowedWithWarning result carries state and warning text`() {
        val warning = StateTransitionResult.AllowedWithWarning(
            EventState.CLOSED,
            "There are pending payments",
        )
        assertEquals(EventState.CLOSED, warning.newState)
        assertEquals("There are pending payments", warning.warning)
    }

    // ── TransitionContext data class ─────────────────────────────────────────

    @Test
    fun `Context carries complete precondition data`() {
        val ctx = TransitionContext(
            memberCount = 3,
            expenseCount = 5,
            hasInvalidExpenses = true,
            isOwner = true,
            transfersPaidPercentage = 40.0,
            pendingPayments = 2,
        )
        assertEquals(3, ctx.memberCount)
        assertEquals(5, ctx.expenseCount)
        assertTrue(ctx.hasInvalidExpenses)
        assertTrue(ctx.isOwner)
        assertEquals(40.0, ctx.transfersPaidPercentage)
        assertEquals(2, ctx.pendingPayments)
    }

    @Test
    fun `Context has sensible defaults`() {
        val ctx = TransitionContext()
        assertEquals("", ctx.eventName)
        assertEquals("", ctx.eventBaseCurrency)
        assertEquals(0, ctx.memberCount)
        assertEquals(0, ctx.expenseCount)
        assertFalse(ctx.hasInvalidExpenses)
        assertFalse(ctx.isOwner)
        assertEquals(0.0, ctx.transfersPaidPercentage)
        assertEquals(0, ctx.pendingPayments)
    }
}
