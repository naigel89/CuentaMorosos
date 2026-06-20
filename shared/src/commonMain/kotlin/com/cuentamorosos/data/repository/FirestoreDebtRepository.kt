package com.cuentamorosos.data.repository

import com.cuentamorosos.model.EventDebtItem
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
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

        // 1. Resolve event IDs (3 queries, constant cost)
        val ownerSnapshot = db.collection("events").where { "ownerId" equalTo uid }.get()
        val memberSnapshot = db.collection("events").where { "memberIds" contains uid }.get()
        val participantSnapshot = db.collection("events").where { "participantIds" contains uid }.get()

        val eventIds = (ownerSnapshot.documents + memberSnapshot.documents + participantSnapshot.documents)
            .map { it.id }
            .distinct()

        if (eventIds.isEmpty()) {
            emit(emptyList())
            return@flow
        }

        // 2. Create per-event snapshot listeners (long-lived, not polling)
        val debtFlows = eventIds.map { eventId ->
            db.collection("events")
                .document(eventId)
                .collection("debts")
                .snapshots
                .map { snapshot -> snapshot.documents.mapNotNull { it.toDebtItem() } }
        }

        // 3. Combine all flows into single emission
        combine(debtFlows) { debtLists ->
            debtLists.flatMap { it }
        }.collect { emit(it) }
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

    override suspend fun fetchDebtsForEvent(eventId: String): List<EventDebtItem> {
        return try {
            db.collection("events").document(eventId).collection("debts").get()
                .documents.mapNotNull { it.toDebtItem() }
        } catch (e: Exception) {
            println("[FirestoreDebtRepo] fetchDebtsForEvent failed: ${e.message}")
            emptyList()
        }
    }

    override suspend fun fetchAllDebts(): List<EventDebtItem> {
        val uid = auth.currentUser?.uid ?: return emptyList()

        // Resolve all event IDs for this user (same logic as observeAllDebts)
        val ownerSnapshot = db.collection("events").where { "ownerId" equalTo uid }.get()
        val memberSnapshot = db.collection("events").where { "memberIds" contains uid }.get()
        val participantSnapshot = db.collection("events").where { "participantIds" contains uid }.get()

        val eventIds = (ownerSnapshot.documents + memberSnapshot.documents + participantSnapshot.documents)
            .map { it.id }
            .distinct()

        if (eventIds.isEmpty()) return emptyList()

        // One-shot fetch per event, then flatten
        val allDebts = mutableListOf<EventDebtItem>()
        for (eventId in eventIds) {
            val debts = try {
                db.collection("events").document(eventId).collection("debts").get()
                    .documents.mapNotNull { it.toDebtItem() }
            } catch (e: Exception) {
                println("[FirestoreDebtRepo] fetchAllDebts for event $eventId failed: ${e.message}")
                emptyList()
            }
            allDebts.addAll(debts)
        }
        return allDebts
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
            val data = this.getRawData() ?: return null
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
