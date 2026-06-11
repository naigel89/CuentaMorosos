package com.cuentamorosos.data

import com.cuentamorosos.model.EventItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReminderServiceUpcomingTest {

    private val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

    private fun event(
        id: String,
        name: String,
        startDateMillis: Long,
    ) = EventItem(
        id = id,
        name = name,
        dateMillis = startDateMillis,
        ownerId = "owner-1",
        startDateMillis = startDateMillis,
    )

    @Test
    fun `buildUpcomingEventMessages includes events within window`() {
        val now = 1_000_000L
        val events = listOf(
            event("evt-1", "Viaje", now + 2 * MILLIS_PER_DAY),
            event("evt-2", "Fiesta", now + 10 * MILLIS_PER_DAY),
        )

        val messages = ReminderService.buildUpcomingEventMessages(
            events = events,
            reminderDays = 7,
            nowMillis = now,
        )

        assertEquals(1, messages.size)
        assertEquals("evt-1", messages[0].eventId)
        assertEquals(2, messages[0].daysUntil)
        assertEquals(ReminderType.UPCOMING_EVENT, messages[0].type)
    }

    @Test
    fun `buildUpcomingEventMessages excludes past events`() {
        val now = 1_000_000L
        val events = listOf(
            event("evt-1", "Pasado", now - 1 * MILLIS_PER_DAY),
        )

        val messages = ReminderService.buildUpcomingEventMessages(
            events = events,
            reminderDays = 7,
            nowMillis = now,
        )

        assertTrue(messages.isEmpty())
    }

    @Test
    fun `buildUpcomingEventMessages excludes events beyond window`() {
        val now = 1_000_000L
        val events = listOf(
            event("evt-1", "Lejano", now + 30 * MILLIS_PER_DAY),
        )

        val messages = ReminderService.buildUpcomingEventMessages(
            events = events,
            reminderDays = 7,
            nowMillis = now,
        )

        assertTrue(messages.isEmpty())
    }

    @Test
    fun `buildUpcomingEventMessages includes event at exact window boundary`() {
        val now = 1_000_000L
        val events = listOf(
            event("evt-1", "Justo al límite", now + 7 * MILLIS_PER_DAY),
        )

        val messages = ReminderService.buildUpcomingEventMessages(
            events = events,
            reminderDays = 7,
            nowMillis = now,
        )

        assertEquals(1, messages.size)
        assertEquals("evt-1", messages[0].eventId)
    }

    @Test
    fun `buildUpcomingEventMessages deduplicates identical messages`() {
        val now = 1_000_000L
        val events = listOf(
            event("evt-1", "Duplicado", now + 2 * MILLIS_PER_DAY),
            event("evt-1", "Duplicado", now + 2 * MILLIS_PER_DAY),
        )

        val messages = ReminderService.buildUpcomingEventMessages(
            events = events,
            reminderDays = 7,
            nowMillis = now,
        )

        assertEquals(1, messages.size)
    }
}
