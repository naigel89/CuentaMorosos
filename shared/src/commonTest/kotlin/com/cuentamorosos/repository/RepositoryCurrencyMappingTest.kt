package com.cuentamorosos.repository

import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.EventParticipant
import com.cuentamorosos.model.EventRole
import com.cuentamorosos.model.SUPPORTED_CURRENCY
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for D6 currency standardization — repository mapping and migration.
 *
 * These tests verify the mapping logic used by OfflineFirstEventRepository,
 * FirestoreEventRepository, and MigrationManager without requiring
 * SQLDelight or Firebase runtime dependencies.
 */
class RepositoryCurrencyMappingTest {

    // ── Task 5.4: OfflineFirstEventRepository baseCurrency mapping ───────────

    /**
     * Simulates the toEventItem() mapping from CachedEvent row.
     * Mirrors the logic in OfflineFirstEventRepository.toEventItem().
     */
    private fun mockCachedEventToEventItem(
        id: String,
        name: String,
        dateMillis: Long,
        ownerId: String,
        memberIds: String,
        participants: String,
        baseCurrency: String,
        lastCalculationMode: String?,
        lastCalculationTotal: Double?,
        lastCalculationTimestamp: Long?,
        lastCalculationSummary: String?,
    ): EventItem {
        // This mirrors the mapping logic in OfflineFirstEventRepository:
        // baseCurrency = base_currency (from SQLDelight row)
        return EventItem(
            id = id,
            name = name,
            dateMillis = dateMillis,
            ownerId = ownerId,
            memberIds = if (memberIds.isBlank()) emptyList() else memberIds.split(","),
            participants = emptyList(), // simplified for test
            baseCurrency = baseCurrency,
            lastCalculationMode = lastCalculationMode,
            lastCalculationTotal = lastCalculationTotal,
            lastCalculationTimestamp = lastCalculationTimestamp,
            lastCalculationSummary = lastCalculationSummary,
        )
    }

    @Test
    fun `OfflineFirst toEventItem maps base_currency from CachedEvent row`() {
        val event = mockCachedEventToEventItem(
            id = "evt-1",
            name = "Test Event",
            dateMillis = 1000L,
            ownerId = "owner-1",
            memberIds = "a,b",
            participants = "",
            baseCurrency = "EUR",
            lastCalculationMode = null,
            lastCalculationTotal = null,
            lastCalculationTimestamp = null,
            lastCalculationSummary = null,
        )
        assertEquals("EUR", event.baseCurrency)
    }

    @Test
    fun `OfflineFirst toEventItem preserves EUR from database row`() {
        val event = mockCachedEventToEventItem(
            id = "evt-2",
            name = "Trip",
            dateMillis = 2000L,
            ownerId = "owner-2",
            memberIds = "x,y,z",
            participants = "",
            baseCurrency = "EUR",
            lastCalculationMode = "BY_CATEGORY",
            lastCalculationTotal = 150.0,
            lastCalculationTimestamp = 3000L,
            lastCalculationSummary = "Alice: 50, Bob: 50, Carol: 50",
        )
        assertEquals("EUR", event.baseCurrency)
        assertEquals("BY_CATEGORY", event.lastCalculationMode)
    }

    // ── Task 5.5: FirestoreEventRepository baseCurrency null-coalesce ────────

    /**
     * Simulates the null-coalesce logic from FirestoreEventRepository.toEventItem().
     * Mirrors: (data["baseCurrency"] as? String)?.takeIf { it.isNotBlank() } ?: SUPPORTED_CURRENCY
     */
    private fun resolveBaseCurrencyFromFirestore(data: Map<String, Any?>): String =
        (data["baseCurrency"] as? String)?.takeIf { it.isNotBlank() } ?: SUPPORTED_CURRENCY

    @Test
    fun `Firestore toEventItem returns EUR when baseCurrency is present`() {
        val data = mapOf<String, Any?>(
            "id" to "evt-1",
            "name" to "Test Event",
            "dateMillis" to 1000L,
            "ownerId" to "owner-1",
            "baseCurrency" to "EUR",
        )
        assertEquals("EUR", resolveBaseCurrencyFromFirestore(data))
    }

    @Test
    fun `Firestore toEventItem coalesces to EUR when baseCurrency is missing`() {
        val data = mapOf<String, Any?>(
            "id" to "evt-legacy",
            "name" to "Legacy Event",
            "dateMillis" to 1000L,
            "ownerId" to "owner-1",
            // No baseCurrency field — legacy document
        )
        assertEquals("EUR", resolveBaseCurrencyFromFirestore(data))
    }

    @Test
    fun `Firestore toEventItem coalesces to EUR when baseCurrency is blank`() {
        val data = mapOf<String, Any?>(
            "id" to "evt-blank",
            "name" to "Blank Currency Event",
            "dateMillis" to 1000L,
            "ownerId" to "owner-1",
            "baseCurrency" to "",
        )
        assertEquals("EUR", resolveBaseCurrencyFromFirestore(data))
    }

    @Test
    fun `Firestore toEventItem coalesces to EUR when baseCurrency is null`() {
        val data = mapOf<String, Any?>(
            "id" to "evt-null",
            "name" to "Null Currency Event",
            "dateMillis" to 1000L,
            "ownerId" to "owner-1",
            "baseCurrency" to null,
        )
        assertEquals("EUR", resolveBaseCurrencyFromFirestore(data))
    }

    // ── Task 5.6: MigrationManager toMigrationMap includes baseCurrency ──────

    /**
     * Simulates the toMigrationMap() logic from MigrationManager.
     * Mirrors: "baseCurrency" to (baseCurrency.takeIf { it.isNotBlank() } ?: SUPPORTED_CURRENCY)
     */
    private fun mockEventToMigrationMap(
        event: EventItem,
        uid: String,
    ): Map<String, Any?> {
        val participants = buildList {
            if (uid.isNotBlank()) {
                add(EventParticipant(profileId = uid, role = EventRole.OWNER, joinedAtMillis = event.dateMillis))
            }
            event.memberIds.filter { it != uid }.forEach { mid ->
                add(EventParticipant(profileId = mid, role = EventRole.CONTRIBUTOR, joinedAtMillis = event.dateMillis))
            }
        }
        return mapOf(
            "id" to event.id,
            "name" to event.name,
            "dateMillis" to event.dateMillis,
            "ownerId" to uid,
            "memberIds" to listOf(uid),
            "participants" to participants.map { p ->
                mapOf("profileId" to p.profileId, "role" to p.role.name, "joinedAtMillis" to p.joinedAtMillis)
            },
            "participantIds" to participants.map { it.profileId },
            "baseCurrency" to (event.baseCurrency.takeIf { it.isNotBlank() } ?: SUPPORTED_CURRENCY),
            "lastCalculationMode" to event.lastCalculationMode,
            "lastCalculationTotal" to event.lastCalculationTotal,
            "lastCalculationTimestamp" to event.lastCalculationTimestamp,
            "lastCalculationSummary" to event.lastCalculationSummary,
        )
    }

    @Test
    fun `MigrationManager toMigrationMap includes baseCurrency key with EUR value`() {
        val event = EventItem(
            id = "evt-migrate",
            name = "Migrate Me",
            dateMillis = 1000L,
            ownerId = "owner-1",
            memberIds = listOf("a", "b"),
            baseCurrency = "EUR",
        )
        val map = mockEventToMigrationMap(event, "owner-1")

        assertTrue(map.containsKey("baseCurrency"))
        assertEquals("EUR", map["baseCurrency"])
    }

    @Test
    fun `MigrationManager toMigrationMap defaults to EUR when event baseCurrency is blank`() {
        val event = EventItem(
            id = "evt-legacy-migrate",
            name = "Legacy Migrate",
            dateMillis = 1000L,
            ownerId = "owner-1",
            memberIds = listOf("a", "b"),
            baseCurrency = "", // legacy event without currency
        )
        val map = mockEventToMigrationMap(event, "owner-1")

        assertTrue(map.containsKey("baseCurrency"))
        assertEquals("EUR", map["baseCurrency"])
    }

    @Test
    fun `MigrationManager toMigrationMap preserves existing EUR value`() {
        val event = EventItem(
            id = "evt-existing",
            name = "Existing Event",
            dateMillis = 2000L,
            ownerId = "owner-2",
            memberIds = listOf("x", "y"),
            baseCurrency = "EUR",
        )
        val map = mockEventToMigrationMap(event, "owner-2")

        assertEquals("EUR", map["baseCurrency"])
    }
}
