package com.cuentamorosos.model

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for Fix 7 — Correct Creditor Resolution.
 *
 * Verifies that resolveEventCreditor() resolves creditors from
 * payerContributions.keys (multi-payer map) rather than the legacy
 * paidByProfileId single field.
 */
class EventCreditorResolverTest {

    private val currentUserUid = "user-current"

    // ── Helper factories ──────────────────────────────────────────────────

    private fun makeExpense(
        eventId: String = "ev-1",
        paidByProfileId: String = "",
        payerContributions: Map<String, Double> = emptyMap(),
    ) = EventExpenseItem(
        eventId = eventId,
        name = "Test expense",
        amountEuros = 50.0,
        paidByProfileId = paidByProfileId,
        payerContributions = payerContributions,
    )

    private fun makeDebt(
        eventId: String = "ev-1",
        profileId: String = "user-current",
        creditorId: String? = null,
    ) = EventDebtItem(
        eventId = eventId,
        profileId = profileId,
        creditorId = creditorId,
        amountEuros = 25.0,
    )

    private fun makeEvent(
        id: String = "ev-1",
        ownerId: String = "user-owner",
    ) = EventItem(
        id = id,
        name = "Test Event",
        dateMillis = 0L,
        ownerId = ownerId,
    )

    // ── Scenario 1: Explicit creditorId — direct return ──────────────────

    @Test
    fun `explicit creditorId returns directly without fallback`() {
        val debt = makeDebt(creditorId = "Bob")
        val expenses = listOf(makeExpense(payerContributions = mapOf("Alice" to 50.0)))
        val eventMap = mapOf("ev-1" to makeEvent())

        val result = resolveEventCreditor(debt, expenses, eventMap, currentUserUid)

        assertEquals("Bob", result, "Explicit creditorId should be returned directly")
    }

    // ── Scenario 2: Single payer via payerContributions ──────────────────

    @Test
    fun `single payer resolved from payerContributions`() {
        val debt = makeDebt(creditorId = null)
        val expenses = listOf(makeExpense(payerContributions = mapOf("Alice" to 57.74)))
        val eventMap = mapOf("ev-1" to makeEvent())

        val result = resolveEventCreditor(debt, expenses, eventMap, currentUserUid)

        assertEquals("Alice", result, "Should resolve from payerContributions keys")
    }

    // ── Scenario 3: Multiple payers via payerContributions — sorted ──────

    @Test
    fun `multiple payers returns first alphabetically sorted`() {
        val debt = makeDebt(creditorId = null)
        val expenses = listOf(
            makeExpense(payerContributions = mapOf("Charlie" to 30.0, "Alice" to 27.74))
        )
        val eventMap = mapOf("ev-1" to makeEvent())

        val result = resolveEventCreditor(debt, expenses, eventMap, currentUserUid)

        assertEquals("Alice", result, "Should return first payer alphabetically (Alice < Charlie)")
    }

    // ── Scenario 4: payerContributions excludes current user ─────────────

    @Test
    fun `payerContributions excludes current user and falls to next payer`() {
        val debt = makeDebt(creditorId = null)
        val expenses = listOf(
            makeExpense(payerContributions = mapOf(currentUserUid to 30.0, "Bob" to 27.74))
        )
        val eventMap = mapOf("ev-1" to makeEvent())

        val result = resolveEventCreditor(debt, expenses, eventMap, currentUserUid)

        assertEquals("Bob", result, "Should skip current user and return Bob")
    }

    // ── Scenario 5: Empty payerContributions → paidByProfileId fallback ──

    @Test
    fun `empty payerContributions falls back to paidByProfileId`() {
        val debt = makeDebt(creditorId = null)
        val expenses = listOf(
            makeExpense(paidByProfileId = "LegacyPayer", payerContributions = emptyMap())
        )
        val eventMap = mapOf("ev-1" to makeEvent())

        val result = resolveEventCreditor(debt, expenses, eventMap, currentUserUid)

        assertEquals("LegacyPayer", result, "Should fall back to paidByProfileId when payerContributions is empty")
    }

    // ── Scenario 6: Both payerContributions and paidByProfileId present ──

    @Test
    fun `payerContributions takes precedence over paidByProfileId`() {
        val debt = makeDebt(creditorId = null)
        val expenses = listOf(
            makeExpense(
                paidByProfileId = "LegacyPayer",
                payerContributions = mapOf("NewPayer" to 50.0)
            )
        )
        val eventMap = mapOf("ev-1" to makeEvent())

        val result = resolveEventCreditor(debt, expenses, eventMap, currentUserUid)

        assertEquals("NewPayer", result, "payerContributions should take precedence over paidByProfileId")
    }

    // ── Scenario 7: No payer info → event owner fallback ─────────────────

    @Test
    fun `no payer info falls back to event owner`() {
        val debt = makeDebt(creditorId = null)
        val expenses = listOf(
            makeExpense(paidByProfileId = "", payerContributions = emptyMap())
        )
        val eventMap = mapOf("ev-1" to makeEvent(ownerId = "event-owner"))

        val result = resolveEventCreditor(debt, expenses, eventMap, currentUserUid)

        assertEquals("event-owner", result, "Should fall back to event owner when no payer info")
    }

    // ── Scenario 8: Event owner is current user → eventId fallback ───────

    @Test
    fun `event owner is current user falls back to eventId`() {
        val debt = makeDebt(creditorId = null)
        val expenses = listOf(
            makeExpense(paidByProfileId = "", payerContributions = emptyMap())
        )
        val eventMap = mapOf("ev-1" to makeEvent(ownerId = currentUserUid))

        val result = resolveEventCreditor(debt, expenses, eventMap, currentUserUid)

        assertEquals("ev-1", result, "Should fall back to eventId when owner is current user")
    }

    // ── Scenario 9: Multiple expenses with mixed payers ──────────────────

    @Test
    fun `multiple expenses aggregate all payerContributions keys`() {
        val debt = makeDebt(creditorId = null)
        val expenses = listOf(
            makeExpense(payerContributions = mapOf("Alice" to 30.0)),
            makeExpense(payerContributions = mapOf("Bob" to 20.0)),
        )
        val eventMap = mapOf("ev-1" to makeEvent())

        val result = resolveEventCreditor(debt, expenses, eventMap, currentUserUid)

        assertEquals("Alice", result, "Should aggregate payers from all expenses and return first sorted")
    }
}
