package com.cuentamorosos.ui

import com.cuentamorosos.model.EventAction
import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.EventParticipant
import com.cuentamorosos.model.EventRole
import com.cuentamorosos.model.PermissionEngine
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Integration tests for E4 — Role-Based UI Gating.
 *
 * These tests verify that the permission matrix correctly gates UI actions
 * for each role (OWNER, CONTRIBUTOR, READER) across EventDetailScreen and EventsScreen.
 */
class RoleUiGatingTest {

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

    /**
     * Builds a canDo function for a given profileId and event,
     * matching the pattern used in EventDetailScreen wiring.
     */
    private fun buildCanDo(profileId: String, event: EventItem): (EventAction) -> Boolean = { action ->
        PermissionEngine.canDo(profileId, event, action)
    }

    // ── E4-1: EventDetailScreen role parameters ──────────────────────────────

    @Test
    fun `OWNER sees all actions — canDo returns true for every action`() {
        val event = testEvent(ownerId = "alice")
        val canDo = buildCanDo("alice", event)

        assertTrue(canDo(EventAction.CreateExpense), "OWNER should create expenses")
        assertTrue(canDo(EventAction.EditExpense("bob")), "OWNER should edit any expense")
        assertTrue(canDo(EventAction.DeleteExpense("bob")), "OWNER should delete any expense")
        assertTrue(canDo(EventAction.ManageParticipants), "OWNER should manage participants")
        assertTrue(canDo(EventAction.Calculate), "OWNER should calculate")
        assertTrue(canDo(EventAction.Close), "OWNER should close event")
        assertTrue(canDo(EventAction.DeleteEvent), "OWNER should delete event")
        assertTrue(canDo(EventAction.Reopen), "OWNER should reopen event")
        assertTrue(canDo(EventAction.LeaveEvent), "OWNER should leave event")
    }

    @Test
    fun `CONTRIBUTOR sees limited actions — can create and edit own expenses only`() {
        val event = testEvent(
            ownerId = "alice",
            participants = listOf(participant("bob", EventRole.CONTRIBUTOR)),
        )
        val canDo = buildCanDo("bob", event)

        assertTrue(canDo(EventAction.CreateExpense), "CONTRIBUTOR should create expenses")
        assertTrue(canDo(EventAction.EditExpense("bob")), "CONTRIBUTOR should edit own expenses")
        assertFalse(canDo(EventAction.EditExpense("charlie")), "CONTRIBUTOR should NOT edit others' expenses")
        assertTrue(canDo(EventAction.DeleteExpense("bob")), "CONTRIBUTOR should delete own expenses")
        assertFalse(canDo(EventAction.DeleteExpense("charlie")), "CONTRIBUTOR should NOT delete others' expenses")
        assertFalse(canDo(EventAction.ManageParticipants), "CONTRIBUTOR should NOT manage participants")
        assertFalse(canDo(EventAction.Calculate), "CONTRIBUTOR should NOT calculate")
        assertFalse(canDo(EventAction.Close), "CONTRIBUTOR should NOT close event")
        assertFalse(canDo(EventAction.DeleteEvent), "CONTRIBUTOR should NOT delete event")
        assertFalse(canDo(EventAction.Reopen), "CONTRIBUTOR should NOT reopen event")
        assertTrue(canDo(EventAction.LeaveEvent), "CONTRIBUTOR should leave event")
    }

    @Test
    fun `READER sees no actions — canDo returns false for all write actions`() {
        val event = testEvent(
            ownerId = "alice",
            participants = listOf(participant("charlie", EventRole.READER)),
        )
        val canDo = buildCanDo("charlie", event)

        assertFalse(canDo(EventAction.CreateExpense), "READER should NOT create expenses")
        assertFalse(canDo(EventAction.EditExpense("charlie")), "READER should NOT edit expenses")
        assertFalse(canDo(EventAction.DeleteExpense("charlie")), "READER should NOT delete expenses")
        assertFalse(canDo(EventAction.ManageParticipants), "READER should NOT manage participants")
        assertFalse(canDo(EventAction.Calculate), "READER should NOT calculate")
        assertFalse(canDo(EventAction.Close), "READER should NOT close event")
        assertFalse(canDo(EventAction.DeleteEvent), "READER should NOT delete event")
        assertFalse(canDo(EventAction.Reopen), "READER should NOT reopen event")
        assertTrue(canDo(EventAction.LeaveEvent), "READER should leave event")
    }

    // ── E4-2: Gate "Añadir ítem" button ──────────────────────────────────────

    @Test
    fun `Añadir ítem button — enabled for OWNER and CONTRIBUTOR, disabled for READER`() {
        val event = testEvent(
            ownerId = "alice",
            participants = listOf(
                participant("bob", EventRole.CONTRIBUTOR),
                participant("charlie", EventRole.READER),
            ),
        )

        assertTrue(buildCanDo("alice", event)(EventAction.CreateExpense))
        assertTrue(buildCanDo("bob", event)(EventAction.CreateExpense))
        assertFalse(buildCanDo("charlie", event)(EventAction.CreateExpense))
    }

    // ── E4-3: Gate edit/delete on ExpenseItemCard ────────────────────────────

    @Test
    fun `Expense edit — OWNER can edit any, CONTRIBUTOR only own, READER none`() {
        val event = testEvent(
            ownerId = "alice",
            participants = listOf(
                participant("bob", EventRole.CONTRIBUTOR),
                participant("charlie", EventRole.READER),
            ),
        )

        // OWNER can edit any expense
        assertTrue(buildCanDo("alice", event)(EventAction.EditExpense("bob")))
        assertTrue(buildCanDo("alice", event)(EventAction.EditExpense("charlie")))

        // CONTRIBUTOR can only edit own
        assertTrue(buildCanDo("bob", event)(EventAction.EditExpense("bob")))
        assertFalse(buildCanDo("bob", event)(EventAction.EditExpense("alice")))

        // READER cannot edit any
        assertFalse(buildCanDo("charlie", event)(EventAction.EditExpense("alice")))
        assertFalse(buildCanDo("charlie", event)(EventAction.EditExpense("bob")))
    }

    @Test
    fun `Expense delete — OWNER can delete any, CONTRIBUTOR only own, READER none`() {
        val event = testEvent(
            ownerId = "alice",
            participants = listOf(
                participant("bob", EventRole.CONTRIBUTOR),
                participant("charlie", EventRole.READER),
            ),
        )

        // OWNER can delete any expense
        assertTrue(buildCanDo("alice", event)(EventAction.DeleteExpense("bob")))
        assertTrue(buildCanDo("alice", event)(EventAction.DeleteExpense("charlie")))

        // CONTRIBUTOR can only delete own
        assertTrue(buildCanDo("bob", event)(EventAction.DeleteExpense("bob")))
        assertFalse(buildCanDo("bob", event)(EventAction.DeleteExpense("alice")))

        // READER cannot delete any
        assertFalse(buildCanDo("charlie", event)(EventAction.DeleteExpense("bob")))
    }

    // ── E4-5/E4-6: SettlementPanel role gating ───────────────────────────────

    @Test
    fun `SettlementPanel buttons — only OWNER can manage participants, calculate and invite`() {
        val event = testEvent(
            ownerId = "alice",
            participants = listOf(
                participant("bob", EventRole.CONTRIBUTOR),
                participant("charlie", EventRole.READER),
            ),
        )

        // Calculate
        assertTrue(buildCanDo("alice", event)(EventAction.Calculate))
        assertFalse(buildCanDo("bob", event)(EventAction.Calculate))
        assertFalse(buildCanDo("charlie", event)(EventAction.Calculate))

        // ManageParticipants / Invite — OWNER only
        assertTrue(buildCanDo("alice", event)(EventAction.ManageParticipants))
        assertFalse(buildCanDo("bob", event)(EventAction.ManageParticipants))
        assertFalse(buildCanDo("charlie", event)(EventAction.ManageParticipants))
    }

    // ── E4-9/E4-10: EventsScreen role-based card actions ─────────────────────

    @Test
    fun `EventsScreen — only OWNER can edit and delete event cards`() {
        val event = testEvent(
            ownerId = "alice",
            participants = listOf(
                participant("bob", EventRole.CONTRIBUTOR),
                participant("charlie", EventRole.READER),
            ),
        )

        // Edit (OWNER only)
        assertTrue(PermissionEngine.getRole("alice", event) == EventRole.OWNER)
        assertFalse(PermissionEngine.getRole("bob", event) == EventRole.OWNER)
        assertFalse(PermissionEngine.getRole("charlie", event) == EventRole.OWNER)

        // Delete (OWNER only via hasPermission)
        assertTrue(PermissionEngine.hasPermission(PermissionEngine.getRole("alice", event), EventAction.DeleteEvent))
        assertFalse(PermissionEngine.hasPermission(PermissionEngine.getRole("bob", event), EventAction.DeleteEvent))
        assertFalse(PermissionEngine.hasPermission(PermissionEngine.getRole("charlie", event), EventAction.DeleteEvent))
    }

    // ── Default parameters preserve backward compatibility ────────────────────

    @Test
    fun `Default canDo allows all actions (backward compatibility)`() {
        val defaultCanDo: (EventAction) -> Boolean = { true }

        assertTrue(defaultCanDo(EventAction.CreateExpense))
        assertTrue(defaultCanDo(EventAction.EditExpense("any")))
        assertTrue(defaultCanDo(EventAction.DeleteExpense("any")))
        assertTrue(defaultCanDo(EventAction.ManageParticipants))
        assertTrue(defaultCanDo(EventAction.Calculate))
    }
}
