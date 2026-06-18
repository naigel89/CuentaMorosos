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

    // ── totalOwedToYou aggregation ─────────────────────────────────────────

    @Test
    fun `totalOwedToYou is zero when no debts`() {
        val result = calculateTotalOwedToYou(emptyList(), "user-alice")
        assertEquals(0.0, result)
    }

    @Test
    fun `totalOwedToYou excludes current user own unpaid debts`() {
        val debts = listOf(
            testDebt("evt1", "user-alice", 50.0),
            testDebt("evt1", "user-alice", 30.0),
        )
        val result = calculateTotalOwedToYou(debts, "user-alice")
        assertEquals(0.0, result, "Own debts must be excluded from totalOwedToYou")
    }

    @Test
    fun `totalOwedToYou sums only other profiles unpaid debts`() {
        val debts = listOf(
            testDebt("evt1", "bob", 50.0),
            testDebt("evt2", "charlie", 75.0),
        )
        val result = calculateTotalOwedToYou(debts, "user-alice")
        assertEquals(125.0, result)
    }

    @Test
    fun `totalOwedToYou excludes paid debts and own debts in mixed scenario`() {
        val debts = listOf(
            testDebt("evt1", "bob", 50.0, paid = false),      // included: 50
            testDebt("evt1", "user-alice", 30.0, paid = false), // excluded: own
            testDebt("evt2", "charlie", 20.0, paid = true),     // excluded: paid
            testDebt("evt2", "dave", 40.0, paid = false),       // included: 40
        )
        val result = calculateTotalOwedToYou(debts, "user-alice")
        assertEquals(90.0, result) // 50 + 40
    }

    @Test
    fun `totalOwedToYou is zero when all debts are paid`() {
        val debts = listOf(
            testDebt("evt1", "bob", 50.0, paid = true),
            testDebt("evt1", "charlie", 30.0, paid = true),
        )
        val result = calculateTotalOwedToYou(debts, "user-alice")
        assertEquals(0.0, result)
    }

    // ── buildUnifiedBreakdown profile netting ─────────────────────────

    @Test
    fun `dual-direction profile nets positive owed-to-you`() {
        val owedToYou = listOf(
            DebtBreakdownItem(
                profileId = "bob",
                profileName = "Bob",
                amount = 80.0,
                events = listOf(EventDebt("evt1", "Evento 1", 80.0)),
            ),
        )
        val youOwe = listOf(
            DebtBreakdownItem(
                profileId = "bob",
                profileName = "Bob",
                amount = 20.0,
                events = listOf(EventDebt("evt2", "Evento 2", 20.0)),
            ),
        )

        val result = buildUnifiedBreakdown(owedToYou, youOwe)

        assertEquals(1, result.size)
        assertEquals("Bob", result[0].profileName)
        assertEquals(60.0, result[0].amount)
        assertEquals(DebtDirection.OWED_TO_YOU, result[0].direction)
    }

    @Test
    fun `dual-direction profile nets negative you-owe`() {
        val owedToYou = listOf(
            DebtBreakdownItem(
                profileId = "bob",
                profileName = "Bob",
                amount = 30.0,
                events = listOf(EventDebt("evt1", "Evento 1", 30.0)),
            ),
        )
        val youOwe = listOf(
            DebtBreakdownItem(
                profileId = "bob",
                profileName = "Bob",
                amount = 80.0,
                events = listOf(EventDebt("evt2", "Evento 2", 80.0)),
            ),
        )

        val result = buildUnifiedBreakdown(owedToYou, youOwe)

        assertEquals(1, result.size)
        assertEquals("Bob", result[0].profileName)
        assertEquals(50.0, result[0].amount)
        assertEquals(DebtDirection.YOU_OWE, result[0].direction)
    }

    @Test
    fun `zero net profile is excluded from results`() {
        val owedToYou = listOf(
            DebtBreakdownItem(
                profileId = "bob",
                profileName = "Bob",
                amount = 50.0,
                events = listOf(EventDebt("evt1", "Evento 1", 50.0)),
            ),
        )
        val youOwe = listOf(
            DebtBreakdownItem(
                profileId = "bob",
                profileName = "Bob",
                amount = 50.0,
                events = listOf(EventDebt("evt2", "Evento 2", 50.0)),
            ),
        )

        val result = buildUnifiedBreakdown(owedToYou, youOwe)

        assertTrue(result.isEmpty(), "Zero net profile should be excluded")
    }

    @Test
    fun `single-direction owed-to-you profile unchanged`() {
        val owedToYou = listOf(
            DebtBreakdownItem(
                profileId = "bob",
                profileName = "Bob",
                amount = 30.0,
                events = listOf(EventDebt("evt1", "Evento 1", 30.0)),
            ),
        )
        val youOwe = emptyList<DebtBreakdownItem>()

        val result = buildUnifiedBreakdown(owedToYou, youOwe)

        assertEquals(1, result.size)
        assertEquals("Bob", result[0].profileName)
        assertEquals(30.0, result[0].amount)
        assertEquals(DebtDirection.OWED_TO_YOU, result[0].direction)
        assertEquals(1, result[0].events.size)
        assertEquals(30.0, result[0].events[0].amount)
    }

    @Test
    fun `multiple events same direction aggregation works`() {
        val owedToYou = listOf(
            DebtBreakdownItem(
                profileId = "bob",
                profileName = "Bob",
                amount = 80.0, // 50 + 30
                events = listOf(
                    EventDebt("evt1", "Evento 1", 50.0),
                    EventDebt("evt2", "Evento 2", 30.0),
                ),
            ),
        )
        val youOwe = listOf(
            DebtBreakdownItem(
                profileId = "bob",
                profileName = "Bob",
                amount = 20.0,
                events = listOf(EventDebt("evt3", "Evento 3", 20.0)),
            ),
        )

        val result = buildUnifiedBreakdown(owedToYou, youOwe)

        assertEquals(1, result.size)
        assertEquals("Bob", result[0].profileName)
        assertEquals(60.0, result[0].amount)
        assertEquals(DebtDirection.OWED_TO_YOU, result[0].direction)
        assertEquals(3, result[0].events.size)
    }

    @Test
    fun `merged events preserve correct sign for each source`() {
        val owedToYou = listOf(
            DebtBreakdownItem(
                profileId = "bob",
                profileName = "Bob",
                amount = 80.0,
                events = listOf(EventDebt("evt1", "Evento 1", 80.0)),
            ),
        )
        val youOwe = listOf(
            DebtBreakdownItem(
                profileId = "bob",
                profileName = "Bob",
                amount = 20.0,
                events = listOf(EventDebt("evt2", "Evento 2", 20.0)),
            ),
        )

        val result = buildUnifiedBreakdown(owedToYou, youOwe)

        val events = result[0].events
        assertEquals(2, events.size)

        val ev1 = events.first { it.eventId == "evt1" }
        assertTrue(ev1.amount >= 0, "OwedToYou event should be positive")
        assertEquals(80.0, ev1.amount)

        val ev2 = events.first { it.eventId == "evt2" }
        assertTrue(ev2.amount < 0, "YouOwe event should be negative in merged list")
        assertEquals(-20.0, ev2.amount)
    }

    @Test
    fun `unified breakdown sorted by amount descending`() {
        val owedToYou = listOf(
            DebtBreakdownItem(profileId = "bob", profileName = "Bob", amount = 10.0, events = listOf(EventDebt("evt1", "E1", 10.0))),
            DebtBreakdownItem(profileId = "charlie", profileName = "Charlie", amount = 50.0, events = listOf(EventDebt("evt2", "E2", 50.0))),
            DebtBreakdownItem(profileId = "dave", profileName = "Dave", amount = 30.0, events = listOf(EventDebt("evt3", "E3", 30.0))),
        )
        val youOwe = emptyList<DebtBreakdownItem>()

        val result = buildUnifiedBreakdown(owedToYou, youOwe)

        assertEquals(listOf("Charlie", "Dave", "Bob"), result.map { it.profileName })
    }
}
