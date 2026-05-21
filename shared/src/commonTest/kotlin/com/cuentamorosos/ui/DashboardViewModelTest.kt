package com.cuentamorosos.ui

import com.cuentamorosos.model.EventDebtItem
import com.cuentamorosos.model.ProfileItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for the per-profile debt breakdown computation.
 */
class DashboardViewModelTest {

    private val currentUser = "user-alice"

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun testProfile(id: String, name: String) = ProfileItem(
        id = id,
        name = name,
        icon = "\uD83D\uDC64",
    )

    private fun testDebt(
        eventId: String,
        profileId: String,
        amount: Double,
        paid: Boolean = false,
    ) = EventDebtItem(
        eventId = eventId,
        profileId = profileId,
        amountEuros = amount,
        paid = paid,
    )

    // ── Empty debts → empty breakdowns ───────────────────────────────────────

    @Test
    fun `empty debts produces empty breakdowns`() {
        val profiles = listOf(testProfile("bob", "Bob"))
        val debts = emptyList<EventDebtItem>()

        val result = computeProfileBreakdown(debts, profiles, currentUser)

        assertTrue(result.owedToYouBreakdown.isEmpty())
        assertTrue(result.youOweBreakdown.isEmpty())
    }

    @Test
    fun `only paid debts produces empty breakdowns`() {
        val profiles = listOf(testProfile("bob", "Bob"))
        val debts = listOf(
            testDebt("evt1", "bob", 50.0, paid = true),
            testDebt("evt1", currentUser, 30.0, paid = true),
        )

        val result = computeProfileBreakdown(debts, profiles, currentUser)

        assertTrue(result.owedToYouBreakdown.isEmpty())
        assertTrue(result.youOweBreakdown.isEmpty())
    }

    // ── Separation of owed vs owe ────────────────────────────────────────────

    @Test
    fun `separates owed-to-you from you-owe correctly`() {
        val profiles = listOf(
            testProfile("bob", "Bob"),
            testProfile(currentUser, "Alice"),
        )
        val debts = listOf(
            // Bob owes 50 (owed to you)
            testDebt("evt1", "bob", 50.0),
            // Alice (current user) owes 30 (you owe)
            testDebt("evt1", currentUser, 30.0),
        )

        val result = computeProfileBreakdown(debts, profiles, currentUser)

        assertEquals(1, result.owedToYouBreakdown.size)
        assertEquals("Bob", result.owedToYouBreakdown[0].profileName)
        assertEquals(50.0, result.owedToYouBreakdown[0].amount)

        assertEquals(1, result.youOweBreakdown.size)
        assertEquals("Alice", result.youOweBreakdown[0].profileName)
        assertEquals(30.0, result.youOweBreakdown[0].amount)
    }

    @Test
    fun `event with only others owing appears only in owed-to-you list`() {
        val profiles = listOf(
            testProfile("bob", "Bob"),
            testProfile("charlie", "Charlie"),
        )
        val debts = listOf(
            testDebt("evt1", "bob", 20.0),
            testDebt("evt1", "charlie", 15.0),
        )

        val result = computeProfileBreakdown(debts, profiles, currentUser)

        assertEquals(2, result.owedToYouBreakdown.size)
        assertEquals(35.0, result.owedToYouBreakdown.sumOf { it.amount })
        assertTrue(result.youOweBreakdown.isEmpty())
    }

    @Test
    fun `event with only current user owing appears only in you-owe list`() {
        val profiles = listOf(testProfile(currentUser, "Alice"))
        val debts = listOf(
            testDebt("evt1", currentUser, 40.0),
        )

        val result = computeProfileBreakdown(debts, profiles, currentUser)

        assertTrue(result.owedToYouBreakdown.isEmpty())
        assertEquals(1, result.youOweBreakdown.size)
        assertEquals(40.0, result.youOweBreakdown[0].amount)
    }

    // ── Descending sort order ────────────────────────────────────────────────

    @Test
    fun `owed-to-you breakdown sorted descending by amount`() {
        val profiles = listOf(
            testProfile("bob", "Bob"),
            testProfile("charlie", "Charlie"),
            testProfile("dave", "Dave"),
        )
        val debts = listOf(
            testDebt("evt1", "bob", 10.0),    // smallest
            testDebt("evt2", "charlie", 50.0), // largest
            testDebt("evt3", "dave", 30.0),    // middle
        )

        val result = computeProfileBreakdown(debts, profiles, currentUser)

        val owedNames = result.owedToYouBreakdown.map { it.profileName }
        assertEquals(listOf("Charlie", "Dave", "Bob"), owedNames)
    }

    @Test
    fun `you-owe breakdown sorted descending by amount`() {
        val profiles = listOf(testProfile(currentUser, "Alice"))
        val debts = listOf(
            testDebt("evt1", currentUser, 25.0),
            testDebt("evt2", currentUser, 75.0),
            testDebt("evt3", currentUser, 50.0),
        )

        val result = computeProfileBreakdown(debts, profiles, currentUser)

        val oweNames = result.youOweBreakdown.map { it.profileName }
        // All debts are same profile, so aggregated into one entry
        assertEquals(1, oweNames.size)
        assertEquals("Alice", oweNames[0])
        assertEquals(150.0, result.youOweBreakdown[0].amount)
    }

    // ── Profile name resolution ──────────────────────────────────────────────

    @Test
    fun `unknown profile uses Desconocido fallback`() {
        val profiles = emptyList<ProfileItem>()
        val debts = listOf(
            testDebt("evt1", "unknown-id", 25.0),
        )

        val result = computeProfileBreakdown(debts, profiles, currentUser)

        assertEquals(1, result.owedToYouBreakdown.size)
        assertEquals("Desconocido", result.owedToYouBreakdown[0].profileName)
        assertEquals(25.0, result.owedToYouBreakdown[0].amount)
    }

    // ── Multi-debt aggregation per profile ───────────────────────────────────

    @Test
    fun `multiple debts per profile are aggregated correctly`() {
        val profiles = listOf(
            testProfile("bob", "Bob"),
            testProfile(currentUser, "Alice"),
        )
        val debts = listOf(
            testDebt("evt1", "bob", 20.0),
            testDebt("evt2", "bob", 30.0),
            testDebt("evt1", currentUser, 10.0),
            testDebt("evt2", currentUser, 15.0),
        )

        val result = computeProfileBreakdown(debts, profiles, currentUser)

        assertEquals(1, result.owedToYouBreakdown.size)
        assertEquals(50.0, result.owedToYouBreakdown[0].amount) // 20 + 30
        assertEquals("Bob", result.owedToYouBreakdown[0].profileName)

        assertEquals(1, result.youOweBreakdown.size)
        assertEquals(25.0, result.youOweBreakdown[0].amount) // 10 + 15
        assertEquals("Alice", result.youOweBreakdown[0].profileName)
    }
}
