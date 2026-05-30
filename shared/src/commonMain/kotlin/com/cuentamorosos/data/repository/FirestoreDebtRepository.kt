package com.cuentamorosos.data.repository

import com.cuentamorosos.model.EventDebtItem
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreDebtRepository : DebtRepository {

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    override fun observeDebts(eventId: String): Flow<List<EventDebtItem>> =
        db.collection("events")
            .document(eventId)
            .collection("debts")
            .snapshots
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toDebtItem() }
            }

    override fun observeAllDebts(): Flow<List<EventDebtItem>> = flow {
        val uid = auth.currentUser?.uid ?: run {
            emit(emptyList())
            return@flow
        }

        val ownerSnapshot = db.collection("events").where { "ownerId" equalTo uid }.get()
        val memberSnapshot = db.collection("events").where { "memberIds" contains uid }.get()
        val participantSnapshot = db.collection("events").where { "participantIds" contains uid }.get()

        val eventIds = (ownerSnapshot.documents + memberSnapshot.documents + participantSnapshot.documents)
            .map { it.id }
            .distinct()

        val allDebts = mutableListOf<EventDebtItem>()
        for (eventId in eventIds) {
            val debtsSnapshot = db.collection("events")
                .document(eventId)
                .collection("debts")
                .get()

            for (debtDoc in debtsSnapshot.documents) {
                debtDoc.toDebtItem()?.let { allDebts.add(it) }
            }
        }
        emit(allDebts)
    }

    override suspend fun saveDebt(debt: EventDebtItem) {
        db.collection("events")
            .document(debt.eventId)
            .collection("debts")
            .document(debt.id)
            .set(debt.toMap())
    }

    override suspend fun deleteDebt(eventId: String, debtId: String) {
        db.collection("events")
            .document(eventId)
            .collection("debts")
            .document(debtId)
            .delete()
    }

    override suspend fun deleteDebtsForProfile(profileId: String) {
        val uid = auth.currentUser?.uid ?: return

        val eventsSnapshot = db.collection("events")
            .where { "ownerId" equalTo uid }
            .get()

        eventsSnapshot.documents.forEach { eventDoc ->
            val debtsSnapshot = db.collection("events")
                .document(eventDoc.id)
                .collection("debts")
                .where { "profileId" equalTo profileId }
                .get()

            if (debtsSnapshot.documents.isEmpty()) return@forEach

            debtsSnapshot.documents.chunked(499).forEach { chunk ->
                db.batch().apply {
                    chunk.forEach { doc ->
                        delete(db.collection("events").document(eventDoc.id).collection("debts").document(doc.id))
                    }
                    commit()
                }
            }
        }
    }

    override suspend fun replaceProfileId(oldId: String, newId: String) {
        val uid = auth.currentUser?.uid ?: return

        val eventsSnapshot = db.collection("events")
            .where { "ownerId" equalTo uid }
            .get()

        eventsSnapshot.documents.forEach { eventDoc ->
            val debtsSnapshot = db.collection("events")
                .document(eventDoc.id)
                .collection("debts")
                .where { "profileId" equalTo oldId }
                .get()

            if (debtsSnapshot.documents.isEmpty()) return@forEach

            debtsSnapshot.documents.chunked(499).forEach { chunk ->
                db.batch().apply {
                    chunk.forEach { doc ->
                        update(
                            db.collection("events").document(eventDoc.id).collection("debts").document(doc.id),
                            mapOf("profileId" to newId)
                        )
                    }
                    commit()
                }
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

    private fun dev.gitlive.firebase.firestore.DocumentSnapshot.toDebtItem(): EventDebtItem? {
        return try {
            val data = data<Map<String, Any?>>()
            EventDebtItem(
                id = data["id"] as? String ?: return null,
                eventId = data["eventId"] as? String ?: return null,
                profileId = data["profileId"] as? String ?: return null,
                amountEuros = (data["amountEuros"] as? Number)?.toDouble() ?: 0.0,
                notes = data["notes"] as? String ?: "",
                paid = data["paid"] as? Boolean ?: false,
                calculationMode = data["calculationMode"] as? String
            )
        } catch (e: Exception) {
            null
        }
    }
}
