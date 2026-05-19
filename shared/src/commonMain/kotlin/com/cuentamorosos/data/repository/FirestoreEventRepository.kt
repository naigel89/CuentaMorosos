package com.cuentamorosos.data.repository

import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.EventParticipant
import com.cuentamorosos.model.EventRole
import com.cuentamorosos.model.EventState
import com.cuentamorosos.model.SUPPORTED_CURRENCY
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class FirestoreEventRepository : EventRepository {

    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val collection = db.collection("events")

    override fun observeEvents(): Flow<List<EventItem>> = flow {
        val uid = auth.currentUser?.uid ?: run {
            emit(emptyList())
            return@flow
        }

        val ownerFlow = collection.where { "ownerId" equalTo uid }.snapshots
        val memberFlow = collection.where { "memberIds" contains uid }.snapshots
        val participantFlow = collection.where { "participantIds" contains uid }.snapshots

        combine(ownerFlow, memberFlow, participantFlow) { ownerSnap, memberSnap, participantSnap ->
            val ownerEvents = ownerSnap.documents.mapNotNull { it.toEventItem() }
            val memberEvents = memberSnap.documents.mapNotNull { it.toEventItem() }
            val participantEvents = participantSnap.documents.mapNotNull { it.toEventItem() }
            (ownerEvents + memberEvents + participantEvents)
                .associateBy { it.id }
                .values
                .sortedByDescending { it.dateMillis }
        }.collect { emit(it) }
    }

    override fun observeEvent(eventId: String): Flow<EventItem?> =
        observeEvents().map { events ->
            events.find { it.id == eventId }
        }

    override suspend fun saveEvent(event: EventItem) {
        collection.document(event.id).set(event.toMap())
    }

    override suspend fun deleteEvent(eventId: String) {
        collection.document(eventId).delete()
    }

    override suspend fun removeMember(eventId: String, memberUid: String) {
        val doc = collection.document(eventId).get()
        // Remove from memberIds (legacy)
        val current = (doc.get("memberIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val newMemberIds = current.filter { it != memberUid }
        // Remove from participants
        @Suppress("UNCHECKED_CAST")
        val currentParticipants = (doc.get("participants") as? List<Map<String, Any?>>) ?: emptyList()
        val newParticipants = currentParticipants.filter { it["profileId"] != memberUid }
        val updates = mutableMapOf<String, Any?>(
            "memberIds" to newMemberIds,
            "participants" to newParticipants,
            "participantIds" to newParticipants.map { it["profileId"] }
        )
        // If removing the owner, apply onOwnerLeave logic
        val ownerId = doc.get("ownerId") as? String
        if (ownerId == memberUid && newParticipants.isNotEmpty()) {
            // Find oldest contributor to promote
            val oldestContributor = newParticipants
                .filter { it["role"] == "CONTRIBUTOR" }
                .minByOrNull { (it["joinedAtMillis"] as? Number)?.toLong() ?: 0L }
            if (oldestContributor != null) {
                updates["ownerId"] = oldestContributor["profileId"]
            }
        }
        collection.document(eventId).update(updates)
    }

    override suspend fun replaceMemberId(oldId: String, newId: String) {
        val snapshot = collection.where { "memberIds" contains oldId }.get()
        snapshot.documents.chunked(499).forEach { chunk ->
            db.batch().apply {
                chunk.forEach { doc ->
                    val data = doc.data<Map<String, Any?>>()
                    val memberIds = (data["memberIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    val newMemberIds = memberIds.map { if (it == oldId) newId else it }
                    update(collection.document(doc.id), mapOf("memberIds" to newMemberIds))
                    val ownerId = data["ownerId"] as? String
                    if (ownerId == oldId) {
                        update(collection.document(doc.id), mapOf("ownerId" to newId))
                    }
                }
                commit()
            }
        }
    }

    override suspend fun findUidByEmail(email: String): String? {
        return try {
            val snapshot = db.collection("users")
                .where { "email" equalTo email }
                .limit(1)
                .get()
            snapshot.documents.firstOrNull()?.get("uid") as? String
        } catch (e: Exception) {
            null
        }
    }

    private fun EventItem.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "name" to name,
        "dateMillis" to dateMillis,
        "ownerId" to ownerId,
        "memberIds" to memberIds,
        "participants" to participants.map { p ->
            mapOf(
                "profileId" to p.profileId,
                "role" to p.role.name,
                "joinedAtMillis" to p.joinedAtMillis,
            )
        },
        "participantIds" to participants.map { it.profileId },
        "baseCurrency" to baseCurrency,
        "lastCalculationMode" to lastCalculationMode,
        "lastCalculationTotal" to lastCalculationTotal,
        "lastCalculationTimestamp" to lastCalculationTimestamp,
        "lastCalculationSummary" to lastCalculationSummary,
        "state" to state.name
    )

    private fun dev.gitlive.firebase.firestore.DocumentSnapshot.toEventItem(): EventItem? {
        return try {
            val data = data<Map<String, Any?>>()
            val participants = loadParticipantsFromFirestore(data)
            EventItem(
                id = data["id"] as? String ?: return null,
                name = data["name"] as? String ?: return null,
                dateMillis = (data["dateMillis"] as? Number)?.toLong() ?: return null,
                ownerId = data["ownerId"] as? String ?: return null,
                memberIds = (data["memberIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                participants = participants,
                baseCurrency = (data["baseCurrency"] as? String)?.takeIf { it.isNotBlank() } ?: SUPPORTED_CURRENCY,
                lastCalculationMode = data["lastCalculationMode"] as? String,
                lastCalculationTotal = (data["lastCalculationTotal"] as? Number)?.toDouble(),
                lastCalculationTimestamp = (data["lastCalculationTimestamp"] as? Number)?.toLong(),
                lastCalculationSummary = data["lastCalculationSummary"] as? String,
                state = computeStateFromFirestore(data, participants)
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Computes event state from Firestore data, handling old events that lack the `state` field.
     *
     * Heuristic for missing state:
     * - If `lastCalculationMode` is set → CALCULATED (event was calculated)
     * - If participants exist OR memberIds is non-empty → OPEN (event was opened)
     * - Otherwise → DRAFT (new or untouched event)
     */
    private fun computeStateFromFirestore(
        data: Map<String, Any?>,
        participants: List<EventParticipant>,
    ): EventState {
        val stateStr = data["state"] as? String
        if (!stateStr.isNullOrBlank()) {
            return runCatching { EventState.valueOf(stateStr) }.getOrDefault(EventState.DRAFT)
        }
        // Heuristic for old events without state field
        val hasCalculation = data["lastCalculationMode"] != null
        val hasParticipants = participants.isNotEmpty()
        val memberIds = (data["memberIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val hasMembers = memberIds.isNotEmpty()
        val participantIds = (data["participantIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val hasParticipantIds = participantIds.isNotEmpty()

        println("[FirestoreEventRepo] State heuristic for '${data["name"]}': " +
            "hasCalculation=$hasCalculation, hasParticipants=$hasParticipants, " +
            "hasMembers=$hasMembers (count=${memberIds.size}), " +
            "hasParticipantIds=$hasParticipantIds (count=${participantIds.size})")

        return when {
            hasCalculation -> EventState.CALCULATED
            hasParticipants || hasMembers || hasParticipantIds -> EventState.OPEN
            else -> EventState.DRAFT
        }
    }

    private fun loadParticipantsFromFirestore(data: Map<String, Any?>): List<EventParticipant> {
        // Try participants array first
        @Suppress("UNCHECKED_CAST")
        val participantsList = data["participants"] as? List<Map<String, Any?>>
        if (!participantsList.isNullOrEmpty()) {
            return participantsList.mapNotNull { p ->
                val profileId = p["profileId"] as? String ?: return@mapNotNull null
                val roleStr = p["role"] as? String ?: "CONTRIBUTOR"
                val role = runCatching { EventRole.valueOf(roleStr) }.getOrDefault(EventRole.CONTRIBUTOR)
                val joinedAt = (p["joinedAtMillis"] as? Number)?.toLong() ?: 0L
                EventParticipant(profileId = profileId, role = role, joinedAtMillis = joinedAt)
            }
        }
        // Migration: derive from memberIds
        val ownerId = data["ownerId"] as? String ?: ""
        val dateMillis = (data["dateMillis"] as? Number)?.toLong() ?: 0L
        val memberIds = (data["memberIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        return buildList {
            if (ownerId.isNotBlank()) {
                add(EventParticipant(profileId = ownerId, role = EventRole.OWNER, joinedAtMillis = dateMillis))
            }
            memberIds.filter { it != ownerId }.forEach { mid ->
                add(EventParticipant(profileId = mid, role = EventRole.CONTRIBUTOR, joinedAtMillis = dateMillis))
            }
        }
    }
}
