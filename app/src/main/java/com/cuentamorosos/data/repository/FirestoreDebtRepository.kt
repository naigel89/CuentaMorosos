package com.cuentamorosos.data.repository

import com.cuentamorosos.model.EventDebtItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreDebtRepository : DebtRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun observeDebts(eventId: String): Flow<List<EventDebtItem>> = callbackFlow {
        val registration = db.collection("events")
            .document(eventId)
            .collection("debts")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val debts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toDebtItem()
                } ?: emptyList()
                
                trySend(debts)
            }

        awaitClose { registration.remove() }
    }

    override suspend fun saveDebt(debt: EventDebtItem) {
        db.collection("events")
            .document(debt.eventId)
            .collection("debts")
            .document(debt.id)
            .set(debt.toMap())
            .await()
    }

    override suspend fun deleteDebt(eventId: String, debtId: String) {
        db.collection("events")
            .document(eventId)
            .collection("debts")
            .document(debtId)
            .delete()
            .await()
    }

    override suspend fun deleteDebtsForProfile(profileId: String) {
        val uid = auth.currentUser?.uid ?: return

        // Obtain all events owned by or containing this user, then delete debts
        // by iterating subcollections. Avoids collectionGroup queries that require
        // a composite Firestore index which may not be deployed.
        val eventsSnapshot = db.collection("events")
            .whereEqualTo("ownerId", uid)
            .get()
            .await()

        eventsSnapshot.documents.forEach { eventDoc ->
            val debtsSnapshot = eventDoc.reference
                .collection("debts")
                .whereEqualTo("profileId", profileId)
                .get()
                .await()

            if (debtsSnapshot.isEmpty) return@forEach

            debtsSnapshot.documents.chunked(499).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { doc -> batch.delete(doc.reference) }
                batch.commit().await()
            }
        }
    }

    override suspend fun replaceProfileId(oldId: String, newId: String) {
        val uid = auth.currentUser?.uid ?: return

        val eventsSnapshot = db.collection("events")
            .whereEqualTo("ownerId", uid)
            .get()
            .await()

        eventsSnapshot.documents.forEach { eventDoc ->
            val debtsSnapshot = eventDoc.reference
                .collection("debts")
                .whereEqualTo("profileId", oldId)
                .get()
                .await()

            if (debtsSnapshot.isEmpty) return@forEach

            debtsSnapshot.documents.chunked(499).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { doc ->
                    batch.update(doc.reference, mapOf("profileId" to newId))
                }
                batch.commit().await()
            }
        }
    }


    private fun EventDebtItem.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "eventId" to eventId,
        "profileId" to profileId,
        "amountEuros" to amountEuros,
        "notes" to notes,
        "paid" to paid,
        "calculationMode" to calculationMode
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.toDebtItem(): EventDebtItem? {
        val data = this.data ?: return null
        return EventDebtItem(
            id = data["id"] as? String ?: return null,
            eventId = data["eventId"] as? String ?: return null,
            profileId = data["profileId"] as? String ?: return null,
            amountEuros = (data["amountEuros"] as? Number)?.toDouble() ?: 0.0,
            notes = data["notes"] as? String ?: "",
            paid = data["paid"] as? Boolean ?: false,
            calculationMode = data["calculationMode"] as? String
        )
    }
}
