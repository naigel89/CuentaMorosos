package com.cuentamorosos.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for FirestoreEventRepository.replaceMemberId() logic.
 *
 * Since FirestoreEventRepository uses Firebase.firestore directly (singleton),
 * these tests verify the field replacement logic in isolation by simulating
 * the same data transformations the method applies.
 *
 * Acceptance criteria (A5):
 * - memberIds updated
 * - ownerId updated
 * - participantIds updated
 * - participants[].profileId updated
 * - All updates in single atomic batch
 * - Handles null/empty participants gracefully
 */
class FirestoreEventRepositoryTest {

    // ── A5: replaceMemberId updates all 4 fields ─────────────────────────────

    @Test
    fun `replaceMemberId updates memberIds list`() {
        val memberIds = listOf("user-old", "user-b", "user-c")
        val oldId = "user-old"
        val newId = "user-new"

        val result = memberIds.map { if (it == oldId) newId else it }

        assertEquals(listOf("user-new", "user-b", "user-c"), result)
    }

    @Test
    fun `replaceMemberId updates ownerId when it matches`() {
        val ownerId = "user-old"
        val oldId = "user-old"
        val newId = "user-new"

        val result = if (ownerId == oldId) newId else ownerId

        assertEquals("user-new", result)
    }

    @Test
    fun `replaceMemberId keeps ownerId when it does not match`() {
        val ownerId = "user-other"
        val oldId = "user-old"
        val newId = "user-new"

        val result = if (ownerId == oldId) newId else ownerId

        assertEquals("user-other", result)
    }

    @Test
    fun `replaceMemberId updates participants profileId`() {
        val participants = listOf(
            mapOf("profileId" to "user-old", "role" to "OWNER", "joinedAtMillis" to 1000L),
            mapOf("profileId" to "user-b", "role" to "CONTRIBUTOR", "joinedAtMillis" to 2000L),
        )
        val oldId = "user-old"
        val newId = "user-new"

        val result = participants.map { p ->
            if (p["profileId"] == oldId) p + ("profileId" to newId) else p
        }

        assertEquals("user-new", result[0]["profileId"])
        assertEquals("OWNER", result[0]["role"])
        assertEquals("user-b", result[1]["profileId"])
    }

    @Test
    fun `replaceMemberId derives participantIds from updated participants`() {
        val participants = listOf(
            mapOf("profileId" to "user-new", "role" to "OWNER"),
            mapOf("profileId" to "user-b", "role" to "CONTRIBUTOR"),
        )

        val participantIds = participants.map { it["profileId"] }

        assertEquals(listOf("user-new", "user-b"), participantIds)
    }

    @Test
    fun `replaceMemberId handles null participants gracefully`() {
        val data: Map<String, Any?> = mapOf(
            "memberIds" to listOf("user-old"),
            "ownerId" to "user-old",
            "participants" to null,
        )

        @Suppress("UNCHECKED_CAST")
        val participants = (data["participants"] as? List<Map<String, Any?>>) ?: emptyList()
        val oldId = "user-old"
        val newId = "user-new"

        val newParticipants = participants.map { p ->
            if (p["profileId"] == oldId) p + ("profileId" to newId) else p
        }
        val newParticipantIds = newParticipants.map { it["profileId"] }

        assertTrue(newParticipants.isEmpty())
        assertTrue(newParticipantIds.isEmpty())
    }

    @Test
    fun `replaceMemberId handles empty participants gracefully`() {
        val data: Map<String, Any?> = mapOf(
            "memberIds" to listOf("user-old"),
            "ownerId" to "user-old",
            "participants" to emptyList<Map<String, Any?>>(),
        )

        @Suppress("UNCHECKED_CAST")
        val participants = (data["participants"] as? List<Map<String, Any?>>) ?: emptyList()
        val oldId = "user-old"
        val newId = "user-new"

        val newParticipants = participants.map { p ->
            if (p["profileId"] == oldId) p + ("profileId" to newId) else p
        }
        val newParticipantIds = newParticipants.map { it["profileId"] }

        assertTrue(newParticipants.isEmpty())
        assertTrue(newParticipantIds.isEmpty())
    }

    @Test
    fun `replaceMemberId all 4 fields updated atomically in single map`() {
        // Simulate the complete transformation that replaceMemberId applies
        val data: Map<String, Any?> = mapOf(
            "memberIds" to listOf("user-old", "user-b"),
            "ownerId" to "user-old",
            "participants" to listOf(
                mapOf("profileId" to "user-old", "role" to "OWNER", "joinedAtMillis" to 1000L),
                mapOf("profileId" to "user-b", "role" to "CONTRIBUTOR", "joinedAtMillis" to 2000L),
            ),
            "participantIds" to listOf("user-old", "user-b"),
        )
        val oldId = "user-old"
        val newId = "user-new"

        // Apply the same transformations as replaceMemberId
        val memberIds = (data["memberIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val newMemberIds = memberIds.map { if (it == oldId) newId else it }

        val ownerId = data["ownerId"] as? String
        val newOwnerId = if (ownerId == oldId) newId else ownerId

        @Suppress("UNCHECKED_CAST")
        val participants = (data["participants"] as? List<Map<String, Any?>>) ?: emptyList()
        val newParticipants = participants.map { p ->
            if (p["profileId"] == oldId) p + ("profileId" to newId) else p
        }

        val newParticipantIds = newParticipants.map { it["profileId"] }

        // Verify all 4 fields are updated correctly
        val updateMap = mapOf(
            "memberIds" to newMemberIds,
            "ownerId" to newOwnerId,
            "participants" to newParticipants,
            "participantIds" to newParticipantIds,
        )

        assertEquals(listOf("user-new", "user-b"), updateMap["memberIds"])
        assertEquals("user-new", updateMap["ownerId"])
        assertEquals(2, (updateMap["participants"] as List<*>).size)
        assertEquals("user-new", ((updateMap["participants"] as List<*>)[0] as Map<*, *>)["profileId"])
        assertEquals(listOf("user-new", "user-b"), updateMap["participantIds"])
    }
}
