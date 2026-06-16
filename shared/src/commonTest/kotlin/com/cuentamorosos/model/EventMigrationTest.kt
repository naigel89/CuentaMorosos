package com.cuentamorosos.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventMigrationTest {

    private fun legacyEvent(
        ownerId: String = "owner1",
        memberIds: List<String> = listOf("owner1", "user2", "user3"),
        participants: List<EventParticipant> = emptyList(),
        dateMillis: Long = 1000L,
    ) = EventItem(
        id = "evt1",
        name = "Test Event",
        dateMillis = dateMillis,
        ownerId = ownerId,
        memberIds = memberIds,
        participants = participants,
    )

    // ── migrateMemberIdsToParticipants ────────────────────────────────────────

    @Test
    fun `migration populates participants from memberIds when participants is empty`() {
        val event = legacyEvent(
            ownerId = "owner1",
            memberIds = listOf("owner1", "user2", "user3"),
            participants = emptyList(),
            dateMillis = 5000L,
        )

        val migrated = migrateMemberIdsToParticipants(event)

        assertEquals(3, migrated.participants.size)
        assertEquals("owner1", migrated.participants[0].profileId)
        assertEquals(EventRole.OWNER, migrated.participants[0].role)
        assertEquals(5000L, migrated.participants[0].joinedAtMillis)
        assertEquals("user2", migrated.participants[1].profileId)
        assertEquals(EventRole.CONTRIBUTOR, migrated.participants[1].role)
        assertEquals("user3", migrated.participants[2].profileId)
        assertEquals(EventRole.CONTRIBUTOR, migrated.participants[2].role)
    }

    @Test
    fun `migration does nothing when participants is already populated`() {
        val existingParticipants = listOf(
            EventParticipant(profileId = "owner1", role = EventRole.OWNER, joinedAtMillis = 999L),
            EventParticipant(profileId = "user2", role = EventRole.CONTRIBUTOR, joinedAtMillis = 888L),
        )
        val event = legacyEvent(
            ownerId = "owner1",
            memberIds = listOf("owner1", "user2", "user3"),
            participants = existingParticipants,
        )

        val migrated = migrateMemberIdsToParticipants(event)

        // Should be unchanged — participants already populated
        assertEquals(existingParticipants, migrated.participants)
        assertEquals(event, migrated)
    }

    @Test
    fun `migration does nothing when both memberIds and participants are empty`() {
        val event = legacyEvent(
            ownerId = "owner1",
            memberIds = emptyList(),
            participants = emptyList(),
        )

        val migrated = migrateMemberIdsToParticipants(event)

        assertEquals(event, migrated)
        assertTrue(migrated.participants.isEmpty())
    }

    @Test
    fun `migration assigns OWNER role only to ownerId`() {
        val event = legacyEvent(
            ownerId = "alice",
            memberIds = listOf("alice", "bob", "charlie"),
        )

        val migrated = migrateMemberIdsToParticipants(event)

        val ownerParticipants = migrated.participants.filter { it.role == EventRole.OWNER }
        assertEquals(1, ownerParticipants.size)
        assertEquals("alice", ownerParticipants[0].profileId)

        val contributorParticipants = migrated.participants.filter { it.role == EventRole.CONTRIBUTOR }
        assertEquals(2, contributorParticipants.size)
    }

    @Test
    fun `migration is idempotent — running twice yields same result`() {
        val event = legacyEvent(
            ownerId = "owner1",
            memberIds = listOf("owner1", "user2"),
        )

        val firstPass = migrateMemberIdsToParticipants(event)
        val secondPass = migrateMemberIdsToParticipants(firstPass)

        assertEquals(firstPass, secondPass)
    }

    // ── effectiveMemberIds after migration ────────────────────────────────────

    @Test
    fun `effectiveMemberIds returns participant profileIds after migration`() {
        val event = legacyEvent(
            ownerId = "owner1",
            memberIds = listOf("owner1", "user2", "user3"),
        )
        val migrated = migrateMemberIdsToParticipants(event)

        assertEquals(listOf("owner1", "user2", "user3"), migrated.effectiveMemberIds)
    }

    @Test
    fun `effectiveMemberIds returns participant profileIds when participants pre-populated`() {
        val event = legacyEvent(
            ownerId = "owner1",
            memberIds = listOf("owner1", "user2"),
            participants = listOf(
                EventParticipant(profileId = "owner1", role = EventRole.OWNER),
                EventParticipant(profileId = "user2", role = EventRole.CONTRIBUTOR),
                EventParticipant(profileId = "user3", role = EventRole.CONTRIBUTOR),
            ),
        )

        // effectiveMemberIds should reflect participants, not memberIds
        assertEquals(listOf("owner1", "user2", "user3"), event.effectiveMemberIds)
    }

    @Test
    fun `effectiveMemberIds falls back to memberIds when participants is empty`() {
        val event = legacyEvent(
            ownerId = "owner1",
            memberIds = listOf("owner1", "user2"),
            participants = emptyList(),
        )

        // Legacy fallback: returns memberIds when participants is empty
        assertEquals(listOf("owner1", "user2"), event.effectiveMemberIds)
    }

    @Test
    fun `effectiveMemberIds is empty when both participants and memberIds are empty`() {
        val event = legacyEvent(
            ownerId = "owner1",
            memberIds = emptyList(),
            participants = emptyList(),
        )

        assertTrue(event.effectiveMemberIds.isEmpty())
    }
}
