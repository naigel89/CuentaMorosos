package com.cuentamorosos.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PermissionEngineTest {

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun testEvent(
        ownerId: String = "owner1",
        participants: List<EventParticipant> = emptyList(),
    ) = EventItem(
        id = "evt1",
        name = "Test Event",
        dateMillis = 1000L,
        ownerId = ownerId,
        participants = participants,
    )

    private fun participant(id: String, role: EventRole, joinedAt: Long = 1000L) =
        EventParticipant(profileId = id, role = role, joinedAtMillis = joinedAt)

    // ── getRole ──────────────────────────────────────────────────────────────

    @Test
    fun `getRole returns OWNER for event owner`() {
        val event = testEvent(ownerId = "alice")
        assertEquals(EventRole.OWNER, PermissionEngine.getRole("alice", event))
    }

    @Test
    fun `getRole returns participant role for non-owner`() {
        val event = testEvent(
            ownerId = "alice",
            participants = listOf(participant("bob", EventRole.CONTRIBUTOR)),
        )
        assertEquals(EventRole.CONTRIBUTOR, PermissionEngine.getRole("bob", event))
    }

    @Test
    fun `getRole returns READER for unknown profile`() {
        val event = testEvent(ownerId = "alice")
        assertEquals(EventRole.READER, PermissionEngine.getRole("stranger", event))
    }

    @Test
    fun `getRole returns READER for blank profileId`() {
        val event = testEvent(ownerId = "alice")
        assertEquals(EventRole.READER, PermissionEngine.getRole("", event))
    }

    @Test
    fun `getRole returns READER for memberIds-only event (no participants)`() {
        val event = EventItem(
            id = "evt1",
            name = "Test",
            dateMillis = 1000L,
            ownerId = "alice",
            memberIds = listOf("bob", "charlie"),
        )
        // Without participants, only ownerId is OWNER; everyone else is READER
        assertEquals(EventRole.READER, PermissionEngine.getRole("bob", event))
    }

    // ── hasPermission — role × action matrix ─────────────────────────────────

    @Test
    fun `OWNER can create expense`() {
        assertTrue(PermissionEngine.hasPermission(EventRole.OWNER, EventAction.CreateExpense))
    }

    @Test
    fun `CONTRIBUTOR can create expense`() {
        assertTrue(PermissionEngine.hasPermission(EventRole.CONTRIBUTOR, EventAction.CreateExpense))
    }

    @Test
    fun `READER cannot create expense`() {
        assertFalse(PermissionEngine.hasPermission(EventRole.READER, EventAction.CreateExpense))
    }

    @Test
    fun `OWNER can edit expense`() {
        assertTrue(PermissionEngine.hasPermission(EventRole.OWNER, EventAction.EditExpense("other")))
    }

    @Test
    fun `CONTRIBUTOR can edit expense (hasPermission checks role only)`() {
        assertTrue(PermissionEngine.hasPermission(EventRole.CONTRIBUTOR, EventAction.EditExpense("self")))
    }

    @Test
    fun `READER cannot edit expense`() {
        assertFalse(PermissionEngine.hasPermission(EventRole.READER, EventAction.EditExpense("self")))
    }

    @Test
    fun `OWNER can delete expense`() {
        assertTrue(PermissionEngine.hasPermission(EventRole.OWNER, EventAction.DeleteExpense("other")))
    }

    @Test
    fun `CONTRIBUTOR can delete expense (hasPermission checks role only)`() {
        assertTrue(PermissionEngine.hasPermission(EventRole.CONTRIBUTOR, EventAction.DeleteExpense("self")))
    }

    @Test
    fun `READER cannot delete expense`() {
        assertFalse(PermissionEngine.hasPermission(EventRole.READER, EventAction.DeleteExpense("self")))
    }

    @Test
    fun `OWNER can manage participants`() {
        assertTrue(PermissionEngine.hasPermission(EventRole.OWNER, EventAction.ManageParticipants))
    }

    @Test
    fun `CONTRIBUTOR cannot manage participants`() {
        assertFalse(PermissionEngine.hasPermission(EventRole.CONTRIBUTOR, EventAction.ManageParticipants))
    }

    @Test
    fun `READER cannot manage participants`() {
        assertFalse(PermissionEngine.hasPermission(EventRole.READER, EventAction.ManageParticipants))
    }

    @Test
    fun `OWNER can assign roles`() {
        assertTrue(PermissionEngine.hasPermission(EventRole.OWNER, EventAction.AssignRoles))
    }

    @Test
    fun `CONTRIBUTOR cannot assign roles`() {
        assertFalse(PermissionEngine.hasPermission(EventRole.CONTRIBUTOR, EventAction.AssignRoles))
    }

    @Test
    fun `READER cannot assign roles`() {
        assertFalse(PermissionEngine.hasPermission(EventRole.READER, EventAction.AssignRoles))
    }

    @Test
    fun `OWNER can calculate`() {
        assertTrue(PermissionEngine.hasPermission(EventRole.OWNER, EventAction.Calculate))
    }

    @Test
    fun `CONTRIBUTOR cannot calculate`() {
        assertFalse(PermissionEngine.hasPermission(EventRole.CONTRIBUTOR, EventAction.Calculate))
    }

    @Test
    fun `READER cannot calculate`() {
        assertFalse(PermissionEngine.hasPermission(EventRole.READER, EventAction.Calculate))
    }

    @Test
    fun `OWNER can close`() {
        assertTrue(PermissionEngine.hasPermission(EventRole.OWNER, EventAction.Close))
    }

    @Test
    fun `CONTRIBUTOR cannot close`() {
        assertFalse(PermissionEngine.hasPermission(EventRole.CONTRIBUTOR, EventAction.Close))
    }

    @Test
    fun `READER cannot close`() {
        assertFalse(PermissionEngine.hasPermission(EventRole.READER, EventAction.Close))
    }

    @Test
    fun `OWNER can delete event`() {
        assertTrue(PermissionEngine.hasPermission(EventRole.OWNER, EventAction.DeleteEvent))
    }

    @Test
    fun `CONTRIBUTOR cannot delete event`() {
        assertFalse(PermissionEngine.hasPermission(EventRole.CONTRIBUTOR, EventAction.DeleteEvent))
    }

    @Test
    fun `READER cannot delete event`() {
        assertFalse(PermissionEngine.hasPermission(EventRole.READER, EventAction.DeleteEvent))
    }

    @Test
    fun `OWNER can reopen`() {
        assertTrue(PermissionEngine.hasPermission(EventRole.OWNER, EventAction.Reopen))
    }

    @Test
    fun `CONTRIBUTOR cannot reopen`() {
        assertFalse(PermissionEngine.hasPermission(EventRole.CONTRIBUTOR, EventAction.Reopen))
    }

    @Test
    fun `READER cannot reopen`() {
        assertFalse(PermissionEngine.hasPermission(EventRole.READER, EventAction.Reopen))
    }

    @Test
    fun `anyone can leave event`() {
        assertTrue(PermissionEngine.hasPermission(EventRole.OWNER, EventAction.LeaveEvent))
        assertTrue(PermissionEngine.hasPermission(EventRole.CONTRIBUTOR, EventAction.LeaveEvent))
        assertTrue(PermissionEngine.hasPermission(EventRole.READER, EventAction.LeaveEvent))
    }

    @Test
    fun `OWNER can run calculation`() {
        assertTrue(PermissionEngine.hasPermission(EventRole.OWNER, EventAction.RunCalculation))
    }

    @Test
    fun `CONTRIBUTOR cannot run calculation`() {
        assertFalse(PermissionEngine.hasPermission(EventRole.CONTRIBUTOR, EventAction.RunCalculation))
    }

    @Test
    fun `READER cannot run calculation`() {
        assertFalse(PermissionEngine.hasPermission(EventRole.READER, EventAction.RunCalculation))
    }

    @Test
    fun `OWNER can settle debts`() {
        assertTrue(PermissionEngine.hasPermission(EventRole.OWNER, EventAction.SettleDebts))
    }

    @Test
    fun `CONTRIBUTOR cannot settle debts`() {
        assertFalse(PermissionEngine.hasPermission(EventRole.CONTRIBUTOR, EventAction.SettleDebts))
    }

    @Test
    fun `READER cannot settle debts`() {
        assertFalse(PermissionEngine.hasPermission(EventRole.READER, EventAction.SettleDebts))
    }

    // ── canDo — full permission check with profileId ─────────────────────────

    @Test
    fun `canDo — contributor can edit own expense`() {
        val event = testEvent(
            ownerId = "alice",
            participants = listOf(participant("bob", EventRole.CONTRIBUTOR)),
        )
        assertTrue(PermissionEngine.canDo("bob", event, EventAction.EditExpense("bob")))
    }

    @Test
    fun `canDo — contributor cannot edit other expense`() {
        val event = testEvent(
            ownerId = "alice",
            participants = listOf(participant("bob", EventRole.CONTRIBUTOR)),
        )
        assertFalse(PermissionEngine.canDo("bob", event, EventAction.EditExpense("charlie")))
    }

    @Test
    fun `canDo — owner can edit any expense`() {
        val event = testEvent(
            ownerId = "alice",
            participants = listOf(participant("bob", EventRole.CONTRIBUTOR)),
        )
        assertTrue(PermissionEngine.canDo("alice", event, EventAction.EditExpense("bob")))
    }

    @Test
    fun `canDo — reader cannot create expense`() {
        val event = testEvent(
            ownerId = "alice",
            participants = listOf(participant("bob", EventRole.READER)),
        )
        assertFalse(PermissionEngine.canDo("bob", event, EventAction.CreateExpense))
    }

    @Test
    fun `canDo — contributor can delete own expense`() {
        val event = testEvent(
            ownerId = "alice",
            participants = listOf(participant("bob", EventRole.CONTRIBUTOR)),
        )
        assertTrue(PermissionEngine.canDo("bob", event, EventAction.DeleteExpense("bob")))
    }

    @Test
    fun `canDo — contributor cannot delete other expense`() {
        val event = testEvent(
            ownerId = "alice",
            participants = listOf(participant("bob", EventRole.CONTRIBUTOR)),
        )
        assertFalse(PermissionEngine.canDo("bob", event, EventAction.DeleteExpense("charlie")))
    }

    @Test
    fun `canDo — contributor cannot edit expense with empty creatorId`() {
        val event = testEvent(
            ownerId = "alice",
            participants = listOf(participant("bob", EventRole.CONTRIBUTOR)),
        )
        assertFalse(PermissionEngine.canDo("bob", event, EventAction.EditExpense("")))
    }

    @Test
    fun `canDo — contributor cannot delete expense with empty creatorId`() {
        val event = testEvent(
            ownerId = "alice",
            participants = listOf(participant("bob", EventRole.CONTRIBUTOR)),
        )
        assertFalse(PermissionEngine.canDo("bob", event, EventAction.DeleteExpense("")))
    }

    @Test
    fun `canDo — contributor cannot manage participants`() {
        val event = testEvent(
            ownerId = "alice",
            participants = listOf(participant("bob", EventRole.CONTRIBUTOR)),
        )
        assertFalse(PermissionEngine.canDo("bob", event, EventAction.ManageParticipants))
    }

    @Test
    fun `canDo — only owner can run calculation`() {
        val event = testEvent(
            ownerId = "alice",
            participants = listOf(participant("bob", EventRole.CONTRIBUTOR)),
        )
        assertTrue(PermissionEngine.canDo("alice", event, EventAction.RunCalculation))
        assertFalse(PermissionEngine.canDo("bob", event, EventAction.RunCalculation))
    }

    @Test
    fun `canDo — only owner can settle debts`() {
        val event = testEvent(
            ownerId = "alice",
            participants = listOf(participant("bob", EventRole.CONTRIBUTOR)),
        )
        assertTrue(PermissionEngine.canDo("alice", event, EventAction.SettleDebts))
        assertFalse(PermissionEngine.canDo("bob", event, EventAction.SettleDebts))
    }

    // ── onOwnerLeave ─────────────────────────────────────────────────────────

    @Test
    fun `onOwnerLeave promotes oldest contributor`() {
        val participants = listOf(
            participant("alice", EventRole.OWNER, 1000),
            participant("bob", EventRole.CONTRIBUTOR, 2000),
            participant("charlie", EventRole.CONTRIBUTOR, 1500),
        )
        val result = PermissionEngine.onOwnerLeave(participants, "alice")
        assertTrue(result.isSuccess)
        val updated = result.getOrThrow()
        val newOwner = updated.find { it.role == EventRole.OWNER }
        assertEquals("charlie", newOwner?.profileId) // charlie joined earlier (1500 < 2000)
    }

    @Test
    fun `onOwnerLeave with no contributors removes owner`() {
        val participants = listOf(
            participant("alice", EventRole.OWNER, 1000),
            participant("bob", EventRole.READER, 2000),
            participant("charlie", EventRole.READER, 3000),
        )
        val result = PermissionEngine.onOwnerLeave(participants, "alice")
        assertTrue(result.isSuccess)
        val updated = result.getOrThrow()
        assertFalse(updated.any { it.profileId == "alice" })
        assertFalse(updated.any { it.role == EventRole.OWNER })
    }

    @Test
    fun `onOwnerLeave blocked when owner is last participant`() {
        val participants = listOf(
            participant("alice", EventRole.OWNER, 1000),
        )
        val result = PermissionEngine.onOwnerLeave(participants, "alice")
        assertTrue(result.isFailure)
    }

    // ── canRemoveParticipant ─────────────────────────────────────────────────

    @Test
    fun `canRemoveParticipant blocks payer`() {
        val expenses = listOf(
            EventExpenseItem(
                id = "exp1",
                eventId = "evt1",
                name = "Dinner",
                amountEuros = 50.0,
                payerContributions = mapOf("bob" to 50.0),
            )
        )
        val result = PermissionEngine.canRemoveParticipant("bob", expenses)
        assertTrue(result.isFailure)
    }

    @Test
    fun `canRemoveParticipant blocks debtor`() {
        val expenses = listOf(
            EventExpenseItem(
                id = "exp1",
                eventId = "evt1",
                name = "Dinner",
                amountEuros = 50.0,
                debtorIds = listOf("bob"),
            )
        )
        val result = PermissionEngine.canRemoveParticipant("bob", expenses)
        assertTrue(result.isFailure)
    }

    @Test
    fun `canRemoveParticipant allows uninvolved participant`() {
        val expenses = listOf(
            EventExpenseItem(
                id = "exp1",
                eventId = "evt1",
                name = "Dinner",
                amountEuros = 50.0,
                payerContributions = mapOf("alice" to 50.0),
                debtorIds = listOf("charlie"),
            )
        )
        val result = PermissionEngine.canRemoveParticipant("bob", expenses)
        assertTrue(result.isSuccess)
    }

    // ── transferOwnership ────────────────────────────────────────────────────

    @Test
    fun `transferOwnership successful transfer`() {
        val participants = listOf(
            participant("alice", EventRole.OWNER, 1000),
            participant("bob", EventRole.CONTRIBUTOR, 2000),
        )
        val result = PermissionEngine.transferOwnership("alice", "bob", participants)
        assertTrue(result.isSuccess)
        val updated = result.getOrThrow()
        assertEquals(EventRole.CONTRIBUTOR, updated.find { it.profileId == "alice" }?.role)
        assertEquals(EventRole.OWNER, updated.find { it.profileId == "bob" }?.role)
    }

    @Test
    fun `transferOwnership fails when target not participant`() {
        val participants = listOf(
            participant("alice", EventRole.OWNER, 1000),
        )
        val result = PermissionEngine.transferOwnership("alice", "stranger", participants)
        assertTrue(result.isFailure)
    }

    @Test
    fun `transferOwnership fails for self-transfer`() {
        val participants = listOf(
            participant("alice", EventRole.OWNER, 1000),
        )
        val result = PermissionEngine.transferOwnership("alice", "alice", participants)
        assertTrue(result.isFailure)
    }

    // ── Extension functions ──────────────────────────────────────────────────

    @Test
    fun `getParticipantRole extension delegates to PermissionEngine`() {
        val event = testEvent(
            ownerId = "alice",
            participants = listOf(participant("bob", EventRole.READER)),
        )
        assertEquals(EventRole.READER, event.getParticipantRole("bob"))
    }

    @Test
    fun `canDo extension delegates to PermissionEngine`() {
        val event = testEvent(
            ownerId = "alice",
            participants = listOf(participant("bob", EventRole.READER)),
        )
        assertFalse(event.canDo("bob", EventAction.CreateExpense))
    }

    // ── Participant serialization ────────────────────────────────────────────

    @Test
    fun `serializeParticipants produces correct format`() {
        val participants = listOf(
            participant("alice", EventRole.OWNER, 1000),
            participant("bob", EventRole.CONTRIBUTOR, 2000),
        )
        val serialized = participants.serializeParticipants()
        assertTrue(serialized.contains("alice|OWNER|1000"))
        assertTrue(serialized.contains("bob|CONTRIBUTOR|2000"))
    }

    @Test
    fun `deserializeParticipants round-trips correctly`() {
        val original = listOf(
            participant("alice", EventRole.OWNER, 1000),
            participant("bob", EventRole.CONTRIBUTOR, 2000),
        )
        val serialized = original.serializeParticipants()
        val deserialized = deserializeParticipants(serialized)
        assertEquals(2, deserialized.size)
        assertEquals("alice", deserialized[0].profileId)
        assertEquals(EventRole.OWNER, deserialized[0].role)
        assertEquals(1000L, deserialized[0].joinedAtMillis)
        assertEquals("bob", deserialized[1].profileId)
        assertEquals(EventRole.CONTRIBUTOR, deserialized[1].role)
        assertEquals(2000L, deserialized[1].joinedAtMillis)
    }

    @Test
    fun `deserializeParticipants returns empty for blank string`() {
        assertEquals(emptyList<EventParticipant>(), deserializeParticipants(""))
        assertEquals(emptyList<EventParticipant>(), deserializeParticipants("   "))
    }

    @Test
    fun `serializeParticipants returns empty string for empty list`() {
        assertEquals("", emptyList<EventParticipant>().serializeParticipants())
    }
}
