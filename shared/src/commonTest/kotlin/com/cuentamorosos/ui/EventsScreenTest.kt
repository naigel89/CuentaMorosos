package com.cuentamorosos.ui

import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.EventState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for EventsScreen filtering and search logic.
 *
 * Verifies that events are correctly filtered by state, search query, and
 * debt status — independently of UI composable removal.
 */
class EventsScreenTest {

    private fun event(
        id: String,
        name: String = "Evento $id",
        state: EventState = EventState.OPEN,
    ) = EventItem(
        id = id,
        name = name,
        dateMillis = 0L,
        ownerId = "user-1",
        state = state,
    )

    // ── Empty / trivial inputs ──────────────────────────────────────────────

    @Test
    fun `empty events returns empty list`() {
        val result = filterEventsList(
            events = emptyList(),
            searchQuery = "",
            activeFilter = 0,
            pendingTotalsByEvent = emptyMap(),
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `all open events returned with default filter`() {
        val events = listOf(
            event("1", "Cena"),
            event("2", "Almuerzo"),
            event("3", "Viaje", state = EventState.CLOSED),
        )

        val result = filterEventsList(
            events = events,
            searchQuery = "",
            activeFilter = 0, // Todos (non-closed)
            pendingTotalsByEvent = emptyMap(),
        )

        assertEquals(2, result.size) // CLOSED excluded
        assertEquals(listOf("Cena", "Almuerzo"), result.map { it.name })
    }

    // ── Search by name ──────────────────────────────────────────────────────

    @Test
    fun `search filters by event name case insensitive`() {
        val events = listOf(
            event("1", "Cena familiar"),
            event("2", "Almuerzo trabajo"),
            event("3", "cena amigos"),
        )

        val result = filterEventsList(
            events = events,
            searchQuery = "cena",
            activeFilter = 0,
            pendingTotalsByEvent = emptyMap(),
        )

        assertEquals(2, result.size)
        assertEquals(listOf("Cena familiar", "cena amigos"), result.map { it.name })
    }

    @Test
    fun `search with whitespace is trimmed`() {
        val events = listOf(
            event("1", "Cena"),
            event("2", "Almuerzo"),
        )

        val result = filterEventsList(
            events = events,
            searchQuery = "  cena  ",
            activeFilter = 0,
            pendingTotalsByEvent = emptyMap(),
        )

        assertEquals(1, result.size)
        assertEquals("Cena", result[0].name)
    }

    @Test
    fun `blank search returns all non-closed events`() {
        val events = listOf(
            event("1", "Cena"),
            event("2", "Almuerzo"),
            event("3", "Cerrado", state = EventState.CLOSED),
        )

        val result = filterEventsList(
            events = events,
            searchQuery = "",
            activeFilter = 0,
            pendingTotalsByEvent = emptyMap(),
        )

        assertEquals(2, result.size)
    }

    // ── Debt-based filters ──────────────────────────────────────────────────

    @Test
    fun `filter con deuda shows only events with pending total greater than zero`() {
        val events = listOf(
            event("1", "Con deuda"),
            event("2", "Sin deuda"),
            event("3", "Otro con deuda"),
        )
        val pendingTotals = mapOf(
            "1" to 50.0,
            "2" to 0.0,
            "3" to 25.0,
        )

        val result = filterEventsList(
            events = events,
            searchQuery = "",
            activeFilter = 1, // Con deuda
            pendingTotalsByEvent = pendingTotals,
        )

        assertEquals(2, result.size)
        assertEquals(listOf("Con deuda", "Otro con deuda"), result.map { it.name })
    }

    @Test
    fun `filter sin deuda shows only events with zero pending total`() {
        val events = listOf(
            event("1", "Con deuda"),
            event("2", "Sin deuda"),
        )
        val pendingTotals = mapOf(
            "1" to 50.0,
            "2" to 0.0,
        )

        val result = filterEventsList(
            events = events,
            searchQuery = "",
            activeFilter = 2, // Sin deuda
            pendingTotalsByEvent = pendingTotals,
        )

        assertEquals(1, result.size)
        assertEquals("Sin deuda", result[0].name)
    }

    // ── CLOSED filter ───────────────────────────────────────────────────────

    @Test
    fun `filter cerrados shows only closed events`() {
        val events = listOf(
            event("1", "Abierto", state = EventState.OPEN),
            event("2", "Cerrado 1", state = EventState.CLOSED),
            event("3", "Cerrado 2", state = EventState.CLOSED),
        )

        val result = filterEventsList(
            events = events,
            searchQuery = "",
            activeFilter = 3, // Cerrados
            pendingTotalsByEvent = emptyMap(),
        )

        assertEquals(2, result.size)
        assertTrue(result.all { it.state == EventState.CLOSED })
    }

    // ── Combined search + filter ────────────────────────────────────────────

    @Test
    fun `combined search and debt filter`() {
        val events = listOf(
            event("1", "Cena con amigos"),
            event("2", "Cena trabajo"),
            event("3", "Almuerzo"),
        )
        val pendingTotals = mapOf(
            "1" to 50.0,
            "2" to 0.0,
            "3" to 30.0,
        )

        val result = filterEventsList(
            events = events,
            searchQuery = "cena",
            activeFilter = 1, // Con deuda
            pendingTotalsByEvent = pendingTotals,
        )

        // Only "Cena con amigos" matches search "cena" AND has pending > 0
        assertEquals(1, result.size)
        assertEquals("Cena con amigos", result[0].name)
    }

    @Test
    fun `CLOSED events excluded from non-CLOSED filters`() {
        val events = listOf(
            event("1", "Cena", state = EventState.OPEN),
            event("2", "Viaje viejo", state = EventState.CLOSED),
        )
        val pendingTotals = mapOf(
            "1" to 50.0,
            "2" to 100.0,
        )

        val result = filterEventsList(
            events = events,
            searchQuery = "",
            activeFilter = 1, // Con deuda (non-CLOSED only)
            pendingTotalsByEvent = pendingTotals,
        )

        assertEquals(1, result.size)
        assertEquals("Cena", result[0].name)
    }
}
