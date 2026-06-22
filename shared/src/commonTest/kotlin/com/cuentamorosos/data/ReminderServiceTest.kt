package com.cuentamorosos.data

import com.cuentamorosos.model.EventDebtItem
import com.cuentamorosos.model.EventExpenseItem
import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.ProfileItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReminderServiceTest {

    private val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

    // ── "te debe" direction: debt owed TO current user ──────────────────

    @Test
    fun `per-debt generates te-debe message when debt profileId differs from currentUserUid`() {
        val now = 1_000_000L
        val profiles = listOf(
            ProfileItem(id = "profile-luis", name = "Luis García"),
        )
        val events = listOf(
            EventItem(id = "evt-1", name = "Cena", dateMillis = now - 8 * MILLIS_PER_DAY, ownerId = "user-1"),
        )
        val debts = listOf(
            EventDebtItem(id = "dbt-1", eventId = "evt-1", profileId = "profile-luis", amountEuros = 15.50, paid = false),
        )
        val expenses = listOf(
            EventExpenseItem(id = "exp-1", eventId = "evt-1", name = "Comida", amountEuros = 15.50, paidByProfileId = "user-1"),
        )

        val messages = ReminderService.buildReminderMessages(
            events = events, debts = debts, expenses = expenses,
            profiles = profiles, currentUserUid = "user-1",
            reminderDays = 7, remindersEnabled = true, nowMillis = now,
        )

        assertEquals(1, messages.size)
        val msg = messages[0]
        assertEquals("Pendientes en Cena", msg.title)
        assertEquals("Luis García te debe 15.50 €", msg.body)
        assertEquals(ReminderType.PENDING_DEBT, msg.type)
        assertEquals("evt-1", msg.eventId)
        assertEquals("Luis García", msg.profileName)
        assertEquals(15.50, msg.amountEuros)
        assertTrue(msg.isOwedToYou)
    }

    // ── "debés a" direction: debt owed BY current user ──────────────────

    @Test
    fun `per-debt generates debes-a message when debt profileId equals currentUserUid with expense payer as creditor`() {
        val now = 1_000_000L
        val profiles = listOf(
            ProfileItem(id = "profile-ana", name = "Ana Pérez"),
        )
        val events = listOf(
            EventItem(id = "evt-2", name = "Cena", dateMillis = now - 8 * MILLIS_PER_DAY, ownerId = "profile-ana"),
        )
        val debts = listOf(
            EventDebtItem(id = "dbt-2", eventId = "evt-2", profileId = "user-me", amountEuros = 8.00, paid = false),
        )
        val expenses = listOf(
            EventExpenseItem(id = "exp-2", eventId = "evt-2", name = "Bebidas", amountEuros = 8.00, paidByProfileId = "profile-ana"),
        )

        val messages = ReminderService.buildReminderMessages(
            events = events, debts = debts, expenses = expenses,
            profiles = profiles, currentUserUid = "user-me",
            reminderDays = 7, remindersEnabled = true, nowMillis = now,
        )

        assertEquals(1, messages.size)
        val msg = messages[0]
        assertEquals("Pendientes en Cena", msg.title)
        assertEquals("Debes 8.00 € a Ana Pérez", msg.body)
        assertEquals(ReminderType.PENDING_DEBT, msg.type)
        assertEquals("evt-2", msg.eventId)
        assertEquals("Ana Pérez", msg.profileName)
        assertEquals(8.00, msg.amountEuros)
        assertFalse(msg.isOwedToYou)
    }

    // ── "debés a" with event owner as fallback creditor ─────────────────

    @Test
    fun `per-debt uses event owner as creditor when no payer in expenses`() {
        val now = 1_000_000L
        val profiles = listOf(
            ProfileItem(id = "owner-1", name = "Dueño Evento"),
        )
        val events = listOf(
            EventItem(id = "evt-3", name = "Fiesta", dateMillis = now - 8 * MILLIS_PER_DAY, ownerId = "owner-1"),
        )
        val debts = listOf(
            EventDebtItem(id = "dbt-3", eventId = "evt-3", profileId = "user-me", amountEuros = 20.00, paid = false),
        )
        val expenses = emptyList<EventExpenseItem>()

        val messages = ReminderService.buildReminderMessages(
            events = events, debts = debts, expenses = expenses,
            profiles = profiles, currentUserUid = "user-me",
            reminderDays = 7, remindersEnabled = true, nowMillis = now,
        )

        assertEquals(1, messages.size)
        assertEquals("Dueño Evento", messages[0].profileName)
        assertEquals("Debes 20.00 € a Dueño Evento", messages[0].body)
    }

    // ── Profile name resolution uses name field ─────────────────────────

    @Test
    fun `per-debt uses name from profile`() {
        val now = 1_000_000L
        val profiles = listOf(
            ProfileItem(id = "profile-x", name = "Ximena"),
        )
        val events = listOf(
            EventItem(id = "evt-x", name = "Evento", dateMillis = now - 8 * MILLIS_PER_DAY, ownerId = "user-1"),
        )
        val debts = listOf(
            EventDebtItem(id = "dbt-x", eventId = "evt-x", profileId = "profile-x", amountEuros = 10.00, paid = false),
        )
        val expenses = emptyList<EventExpenseItem>()

        val messages = ReminderService.buildReminderMessages(
            events = events, debts = debts, expenses = expenses,
            profiles = profiles, currentUserUid = "user-1",
            reminderDays = 7, remindersEnabled = true, nowMillis = now,
        )

        assertEquals(1, messages.size)
        assertEquals("Ximena", messages[0].profileName)
    }

}
