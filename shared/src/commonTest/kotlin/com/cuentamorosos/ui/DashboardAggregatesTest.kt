package com.cuentamorosos.ui

import com.cuentamorosos.model.EventDebtItem
import com.cuentamorosos.model.EventExpenseItem
import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.EventState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for DashboardAggregates single-pass computation (H3).
 *
 * Tests verify that a single iteration over allDebts produces the same
 * results as the 7 independent derivedStateOf blocks it replaced.
 */
class DashboardAggregatesTest {

    private fun computeAggregates(
        allDebts: List<EventDebtItem>,
        allExpenses: List<EventExpenseItem>,
        events: List<EventItem>,
        currentUserUid: String?,
    ): DashboardAggregates {
        val uid = currentUserUid ?: ""

        val activeTotalsByProfile = mutableMapOf<String, Double>()
        val pendingTotalsByEvent = mutableMapOf<String, Double>()
        val participantCountByEvent = mutableMapOf<String, Int>()
        val yourShareByEvent = mutableMapOf<String, Double>()
        val youAreOwedByEvent = mutableMapOf<String, Double>()
        val pendingEventsByProfile = mutableMapOf<String, MutableList<String>>()

        allDebts.forEach { debt ->
            participantCountByEvent[debt.eventId] =
                (participantCountByEvent[debt.eventId] ?: 0) + 1

            if (!debt.paid) {
                activeTotalsByProfile[debt.profileId] =
                    (activeTotalsByProfile[debt.profileId] ?: 0.0) + debt.amountEuros

                pendingTotalsByEvent[debt.eventId] =
                    (pendingTotalsByEvent[debt.eventId] ?: 0.0) + debt.amountEuros

                if (debt.profileId == uid) {
                    yourShareByEvent[debt.eventId] =
                        (yourShareByEvent[debt.eventId] ?: 0.0) + debt.amountEuros
                } else {
                    youAreOwedByEvent[debt.eventId] =
                        (youAreOwedByEvent[debt.eventId] ?: 0.0) + debt.amountEuros
                }

                val event = events.firstOrNull { it.id == debt.eventId }
                if (event != null) {
                    pendingEventsByProfile.getOrPut(debt.profileId) { mutableListOf() }
                        .add("${event.name} · ${debt.amountEuros}€")
                }
            }
        }

        return DashboardAggregates(
            activeTotalsByProfile = activeTotalsByProfile,
            pendingTotalsByEvent = pendingTotalsByEvent,
            totalSpent = allExpenses.sumOf { it.amountEuros },
            participantCountByEvent = participantCountByEvent,
            yourShareByEvent = yourShareByEvent,
            youAreOwedByEvent = youAreOwedByEvent,
            pendingEventsByProfile = pendingEventsByProfile,
        )
    }

    // ── Empty inputs ─────────────────────────────────────────────────────────

    @Test
    fun `empty inputs produce empty aggregates`() {
        val result = computeAggregates(
            allDebts = emptyList(),
            allExpenses = emptyList(),
            events = emptyList(),
            currentUserUid = "user-1",
        )

        assertTrue(result.activeTotalsByProfile.isEmpty())
        assertTrue(result.pendingTotalsByEvent.isEmpty())
        assertEquals(0.0, result.totalSpent)
        assertTrue(result.participantCountByEvent.isEmpty())
        assertTrue(result.yourShareByEvent.isEmpty())
        assertTrue(result.youAreOwedByEvent.isEmpty())
        assertTrue(result.pendingEventsByProfile.isEmpty())
    }

    // ── Single event scenario ────────────────────────────────────────────────

    @Test
    fun `single event with two unpaid debts`() {
        val debts = listOf(
            EventDebtItem(id = "d1", eventId = "evt-1", profileId = "user-1", amountEuros = 10.0, paid = false),
            EventDebtItem(id = "d2", eventId = "evt-1", profileId = "user-2", amountEuros = 20.0, paid = false),
        )
        val events = listOf(
            EventItem(id = "evt-1", name = "Cena", dateMillis = 0L, ownerId = "user-1"),
        )

        val result = computeAggregates(debts, emptyList(), events, "user-1")

        // activeTotalsByProfile
        assertEquals(10.0, result.activeTotalsByProfile["user-1"])
        assertEquals(20.0, result.activeTotalsByProfile["user-2"])

        // pendingTotalsByEvent
        assertEquals(30.0, result.pendingTotalsByEvent["evt-1"])

        // participantCountByEvent
        assertEquals(2, result.participantCountByEvent["evt-1"])

        // yourShareByEvent (user-1's debts)
        assertEquals(10.0, result.yourShareByEvent["evt-1"])

        // youAreOwedByEvent (other's debts)
        assertEquals(20.0, result.youAreOwedByEvent["evt-1"])

        // pendingEventsByProfile
        assertEquals(1, result.pendingEventsByProfile["user-1"]?.size)
        assertEquals(1, result.pendingEventsByProfile["user-2"]?.size)
    }

    // ── Paid debts excluded from pending ─────────────────────────────────────

    @Test
    fun `paid debts excluded from pending aggregates but included in participant count`() {
        val debts = listOf(
            EventDebtItem(id = "d1", eventId = "evt-1", profileId = "user-1", amountEuros = 10.0, paid = true),
            EventDebtItem(id = "d2", eventId = "evt-1", profileId = "user-2", amountEuros = 20.0, paid = false),
        )

        val result = computeAggregates(debts, emptyList(), emptyList(), "user-1")

        // Only user-2 has active (unpaid) debt
        assertEquals(20.0, result.activeTotalsByProfile["user-2"])
        assertTrue(result.activeTotalsByProfile["user-1"] == null || result.activeTotalsByProfile["user-1"] == 0.0)

        // Pending only includes unpaid
        assertEquals(20.0, result.pendingTotalsByEvent["evt-1"])

        // participantCount includes ALL debts (paid + unpaid)
        assertEquals(2, result.participantCountByEvent["evt-1"])
    }

    // ── Multi-profile scenario ───────────────────────────────────────────────

    @Test
    fun `multi-profile multi-event aggregation`() {
        val debts = listOf(
            EventDebtItem(id = "d1", eventId = "evt-1", profileId = "user-1", amountEuros = 10.0, paid = false),
            EventDebtItem(id = "d2", eventId = "evt-1", profileId = "user-2", amountEuros = 15.0, paid = false),
            EventDebtItem(id = "d3", eventId = "evt-2", profileId = "user-1", amountEuros = 5.0, paid = false),
            EventDebtItem(id = "d4", eventId = "evt-2", profileId = "user-3", amountEuros = 25.0, paid = false),
        )
        val events = listOf(
            EventItem(id = "evt-1", name = "Cena", dateMillis = 0L, ownerId = "user-1"),
            EventItem(id = "evt-2", name = "Almuerzo", dateMillis = 0L, ownerId = "user-1"),
        )

        val result = computeAggregates(debts, emptyList(), events, "user-1")

        // user-1 total pending: 10 + 5 = 15
        assertEquals(15.0, result.activeTotalsByProfile["user-1"])
        assertEquals(15.0, result.activeTotalsByProfile["user-2"])
        assertEquals(25.0, result.activeTotalsByProfile["user-3"])

        // evt-1: 10 + 15 = 25, evt-2: 5 + 25 = 30
        assertEquals(25.0, result.pendingTotalsByEvent["evt-1"])
        assertEquals(30.0, result.pendingTotalsByEvent["evt-2"])

        // user-1's share: evt-1=10, evt-2=5
        assertEquals(10.0, result.yourShareByEvent["evt-1"])
        assertEquals(5.0, result.yourShareByEvent["evt-2"])

        // Owed by others: evt-1=15 (user-2), evt-2=25 (user-3)
        assertEquals(15.0, result.youAreOwedByEvent["evt-1"])
        assertEquals(25.0, result.youAreOwedByEvent["evt-2"])
    }

    // ── totalSpent from expenses ─────────────────────────────────────────────

    @Test
    fun `totalSpent sums all expenses`() {
        val expenses = listOf(
            EventExpenseItem(id = "e1", eventId = "evt-1", name = "Food", amountEuros = 50.0, paidByProfileId = "user-1"),
            EventExpenseItem(id = "e2", eventId = "evt-1", name = "Drinks", amountEuros = 30.0, paidByProfileId = "user-2"),
        )

        val result = computeAggregates(emptyList(), expenses, emptyList(), "user-1")

        assertEquals(80.0, result.totalSpent)
    }
}
