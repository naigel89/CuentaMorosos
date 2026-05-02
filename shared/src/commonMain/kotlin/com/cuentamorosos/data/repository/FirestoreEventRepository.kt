package com.cuentamorosos.data.repository

import com.cuentamorosos.model.EventItem
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.combine

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

        combine(ownerFlow, memberFlow) { ownerSnap, memberSnap ->
            val ownerEvents = ownerSnap.documents.mapNotNull { it.toEventItem() }
            val memberEvents = memberSnap.documents.mapNotNull { it.toEventItem() }
            (ownerEvents + memberEvents)
                .associateBy { it.id }
                .values
                .sortedByDescending { it.dateMillis }
        }.collect { emit(it) }
    }

    override suspend fun saveEvent(event: EventItem) {
        collection.document(event.id).set(event.toMap())
    }

    override suspend fun deleteEvent(eventId: String) {
        collection.document(eventId).delete()
    }

    override suspend fun removeMember(eventId: String, memberUid: String) {
        val doc = collection.document(eventId).get()
        val current = (doc.get("memberIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        collection.document(eventId).update("memberIds" to current.filter { it != memberUid })
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
        "lastCalculationMode" to lastCalculationMode,
        "lastCalculationTotal" to lastCalculationTotal,
        "lastCalculationTimestamp" to lastCalculationTimestamp,
        "lastCalculationSummary" to lastCalculationSummary
    )

    private fun dev.gitlive.firebase.firestore.DocumentSnapshot.toEventItem(): EventItem? {
        return try {
            val data = data<Map<String, Any?>>()
            EventItem(
                id = data["id"] as? String ?: return null,
                name = data["name"] as? String ?: return null,
                dateMillis = (data["dateMillis"] as? Number)?.toLong() ?: return null,
                ownerId = data["ownerId"] as? String ?: return null,
                memberIds = (data["memberIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                lastCalculationMode = data["lastCalculationMode"] as? String,
                lastCalculationTotal = (data["lastCalculationTotal"] as? Number)?.toDouble(),
                lastCalculationTimestamp = (data["lastCalculationTimestamp"] as? Number)?.toLong(),
                lastCalculationSummary = data["lastCalculationSummary"] as? String
            )
        } catch (e: Exception) {
            null
        }
    }
}
