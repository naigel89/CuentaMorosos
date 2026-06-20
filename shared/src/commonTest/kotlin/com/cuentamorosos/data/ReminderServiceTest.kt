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
            ProfileItem(id = "profile-luis", name = "Luis", displayName = "Luis García"),
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
            ProfileItem(id = "profile-ana", name = "Ana", displayName = "Ana Pérez"),
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
            ProfileItem(id = "owner-1", name = "Owner", displayName = "Dueño Evento"),
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

    // ── Fallback to displayName for profile name resolution ─────────────

    @Test
    fun `per-debt uses displayName when available`() {
        val now = 1_000_000L
        val profiles = listOf(
            ProfileItem(id = "profile-x", name = "X", displayName = "Ximena"),
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

    // ── Fallback to ProfileItem.name when displayName is null ───────────

    @Test
    fun `per-debt falls back to name when displayName is null`() {
        val now = 1_000_000L
        val profiles = listOf(
            ProfileItem(id = "profile-no-display", name = "NameOnly", displayName = null),
        )
        val events = listOf(
            EventItem(id = "evt-name", name = "Cena", dateMillis = now - 8 * MILLIS_PER_DAY, ownerId = "user-1"),
        )
        val debts = listOf(
            EventDebtItem(id = "dbt-name", eventId = "evt-name", profileId = "profile-no-display", amountEuros = 5.00, paid = false),
        )
        val expenses = emptyList<EventExpenseItem>()

        val messages = ReminderService.buildReminderMessages(
            events = events, debts = debts, expenses = expenses,
            profiles = profiles, currentUserUid = "user-1",
            reminderDays = 7, remindersEnabled = true, nowMillis = now,
        )

        assertEquals(1, messages.size)
        assertEquals("NameOnly", messages[0].profileName)
    }

    // ── Multiple debts produce per-debt messages ────────────────────────

    @Test
    fun `multiple unpaid debts produce one message per debt`() {
        val now = 1_000_000L
        val profiles = listOf(
            ProfileItem(id = "p-a", name = "A"),
            ProfileItem(id = "p-b", name = "B"),
            ProfileItem(id = "p-c", name = "C"),
        )
        val events = listOf(
            EventItem(id = "evt-multi", name = "Fiesta", dateMillis = now - 8 * MILLIS_PER_DAY, ownerId = "user-me"),
        )
        val debts = listOf(
            EventDebtItem(id = "d-1", eventId = "evt-multi", profileId = "p-a", amountEuros = 10.00, paid = false),
            EventDebtItem(id = "d-2", eventId = "evt-multi", profileId = "p-b", amountEuros = 20.00, paid = false),
            EventDebtItem(id = "d-3", eventId = "evt-multi", profileId = "p-c", amountEuros = 30.00, paid = false),
        )
        val expenses = emptyList<EventExpenseItem>()

        val messages = ReminderService.buildReminderMessages(
            events = events, debts = debts, expenses = expenses,
            profiles = profiles, currentUserUid = "user-me",
            reminderDays = 7, remindersEnabled = true, nowMillis = now,
        )

        assertEquals(3, messages.size)
        val profileNames = messages.map { it.profileName }
        assertTrue(profileNames.contains("A"))
        assertTrue(profileNames.contains("B"))
        assertTrue(profileNames.contains("C"))
    }

    // ── Empty results ───────────────────────────────────────────────────

    @Test
    fun `returns empty list when reminders are disabled`() {
        val now = 1_000_000L
        val events = listOf(
            EventItem(id = "evt-1", name = "Cena", dateMillis = now - 8 * MILLIS_PER_DAY, ownerId = "user-1"),
        )
        val debts = listOf(
            EventDebtItem(id = "dbt-1", eventId = "evt-1", profileId = "profile-x", amountEuros = 10.00, paid = false),
        )

        val messages = ReminderService.buildReminderMessages(
            events = events, debts = debts,
            expenses = emptyList(), profiles = emptyList(),
            currentUserUid = "user-1",
            reminderDays = 7, remindersEnabled = false, nowMillis = now,
        )

        assertEquals(0, messages.size)
    }

    @Test
    fun `returns empty list when no unpaid debts exist`() {
        val now = 1_000_000L
        val profiles = listOf(
            ProfileItem(id = "profile-x", name = "X"),
        )
        val events = listOf(
            EventItem(id = "evt-1", name = "Cena", dateMillis = now - 8 * MILLIS_PER_DAY, ownerId = "user-1"),
        )
        val debts = listOf(
            EventDebtItem(id = "dbt-paid", eventId = "evt-1", profileId = "profile-x", amountEuros = 10.00, paid = true),
        )

        val messages = ReminderService.buildReminderMessages(
            events = events, debts = debts,
            expenses = emptyList(), profiles = profiles,
            currentUserUid = "user-1",
            reminderDays = 7, remindersEnabled = true, nowMillis = now,
        )

        assertEquals(0, messages.size)
    }

    @Test
    fun `returns empty list when debt amount is zero`() {
        val now = 1_000_000L
        val profiles = listOf(
            ProfileItem(id = "profile-x", name = "X"),
        )
        val events = listOf(
            EventItem(id = "evt-1", name = "Cena", dateMillis = now - 8 * MILLIS_PER_DAY, ownerId = "user-1"),
        )
        val debts = listOf(
            EventDebtItem(id = "dbt-zero", eventId = "evt-1", profileId = "profile-x", amountEuros = 0.0, paid = false),
        )

        val messages = ReminderService.buildReminderMessages(
            events = events, debts = debts,
            expenses = emptyList(), profiles = profiles,
            currentUserUid = "user-1",
            reminderDays = 7, remindersEnabled = true, nowMillis = now,
        )

        assertEquals(0, messages.size)
    }

    // ── Threshold logic ─────────────────────────────────────────────────

    @Test
    fun `excludes debts from events whose age is below threshold`() {
        val now = 1_000_000L
        val profiles = listOf(
            ProfileItem(id = "profile-x", name = "X"),
        )
        val events = listOf(
            EventItem(id = "evt-recent", name = "Reciente", dateMillis = now - 1 * MILLIS_PER_DAY, ownerId = "user-1"),
        )
        val debts = listOf(
            EventDebtItem(id = "dbt-rec", eventId = "evt-recent", profileId = "profile-x", amountEuros = 10.00, paid = false),
        )

        val messages = ReminderService.buildReminderMessages(
            events = events, debts = debts,
            expenses = emptyList(), profiles = profiles,
            currentUserUid = "user-1",
            reminderDays = 7, remindersEnabled = true, nowMillis = now,
        )

        assertEquals(0, messages.size)
    }

    @Test
    fun `includes debts from events whose age is exactly at threshold`() {
        val now = 1_000_000L
        val profiles = listOf(
            ProfileItem(id = "profile-x", name = "X"),
        )
        val events = listOf(
            EventItem(id = "evt-edge", name = "Al límite", dateMillis = now - 7 * MILLIS_PER_DAY, ownerId = "user-1"),
        )
        val debts = listOf(
            EventDebtItem(id = "dbt-edge", eventId = "evt-edge", profileId = "profile-x", amountEuros = 10.00, paid = false),
        )

        val messages = ReminderService.buildReminderMessages(
            events = events, debts = debts,
            expenses = emptyList(), profiles = profiles,
            currentUserUid = "user-1",
            reminderDays = 7, remindersEnabled = true, nowMillis = now,
        )

        assertEquals(1, messages.size)
    }

    // ── INCOMPLETE_EVENT no longer generated ────────────────────────────

    @Test
    fun `no INCOMPLETE_EVENT messages are generated for events without debts`() {
        val now = 1_000_000L
        val events = listOf(
            EventItem(id = "evt-no-debts", name = "Incompleto", dateMillis = now - 8 * MILLIS_PER_DAY, ownerId = "user-1"),
        )
        val debts = listOf(
            EventDebtItem(id = "dbt-zero", eventId = "evt-no-debts", profileId = "profile-x", amountEuros = 0.0, paid = false),
        )

        val messages = ReminderService.buildReminderMessages(
            events = events, debts = debts,
            expenses = emptyList(), profiles = emptyList(),
            currentUserUid = "user-1",
            reminderDays = 7, remindersEnabled = true, nowMillis = now,
        )

        assertEquals(0, messages.size)
    }
}
