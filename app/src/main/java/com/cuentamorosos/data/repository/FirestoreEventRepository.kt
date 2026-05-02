package com.cuentamorosos.data.repository

import com.cuentamorosos.model.EventItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreEventRepository : EventRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val collection = db.collection("events")

    override fun observeEvents(): Flow<List<EventItem>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        var ownerEvents: List<EventItem> = emptyList()
        var memberEvents: List<EventItem> = emptyList()

        fun emitMergedEvents() {
            val merged = (ownerEvents + memberEvents)
                .associateBy { it.id }
                .values
                .sortedByDescending { it.dateMillis }
            trySend(merged)
        }

        val ownerRegistration = collection.whereEqualTo("ownerId", uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            ownerEvents = snapshot?.documents?.mapNotNull { doc ->
                doc.toEventItem()
            } ?: emptyList()

            emitMergedEvents()
        }

        val memberRegistration = collection.whereArrayContains("memberIds", uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            memberEvents = snapshot?.documents?.mapNotNull { doc ->
                doc.toEventItem()
            } ?: emptyList()

            emitMergedEvents()
        }

        awaitClose {
            ownerRegistration.remove()
            memberRegistration.remove()
        }
    }

    override suspend fun saveEvent(event: EventItem) {
        collection.document(event.id).set(event.toMap()).await()
    }

    override suspend fun deleteEvent(eventId: String) {
        collection.document(eventId).delete().await()
    }

    override suspend fun removeMember(eventId: String, memberUid: String) {
        collection.document(eventId)
            .update("memberIds", FieldValue.arrayRemove(memberUid))
            .await()
    }

    override suspend fun replaceMemberId(oldId: String, newId: String) {
        val snapshot = collection.whereArrayContains("memberIds", oldId).get().await()
        
        if (snapshot.isEmpty) return

        snapshot.documents.chunked(499).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { doc ->
                val data = doc.data
                val memberIds = (data?.get("memberIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                val newMemberIds = memberIds.map { if (it == oldId) newId else it }
                batch.update(doc.reference, mapOf("memberIds" to newMemberIds))
                
                // Also check ownerId
                val ownerId = doc.getString("ownerId")
                if (ownerId == oldId) {
                    batch.update(doc.reference, mapOf("ownerId" to newId))
                }
            }
            batch.commit().await()
        }
    }

    override suspend fun findUidByEmail(email: String): String? {
        return try {
            val snapshot = db.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()
            snapshot.documents.firstOrNull()?.getString("uid")
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

    private fun com.google.firebase.firestore.DocumentSnapshot.toEventItem(): EventItem? {
        val data = this.data ?: return null
        return EventItem(
            id = data["id"] as? String ?: return null,
            name = data["name"] as? String ?: return null,
            dateMillis = (data["dateMillis"] as? Long) ?: System.currentTimeMillis(),
            ownerId = data["ownerId"] as? String ?: return null,
            memberIds = (data["memberIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            lastCalculationMode = data["lastCalculationMode"] as? String,
            lastCalculationTotal = (data["lastCalculationTotal"] as? Number)?.toDouble(),
            lastCalculationTimestamp = data["lastCalculationTimestamp"] as? Long,
            lastCalculationSummary = data["lastCalculationSummary"] as? String
        )
    }
}
