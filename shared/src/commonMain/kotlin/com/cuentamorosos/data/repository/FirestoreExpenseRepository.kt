package com.cuentamorosos.data.repository

import com.cuentamorosos.model.EventExpenseItem
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import com.cuentamorosos.data.LogSanitizer
class FirestoreExpenseRepository : ExpenseRepository {

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    override fun observeExpenses(eventId: String): Flow<List<EventExpenseItem>> =
        db.collection("events")
            .document(eventId)
            .collection("expenses")
            .snapshots
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toExpenseItem() }
            }

    override fun observeAllExpenses(): Flow<List<EventExpenseItem>> = flow {
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
        val expenseFlows = eventIds.map { eventId ->
            db.collection("events")
                .document(eventId)
                .collection("expenses")
                .snapshots
                .map { snapshot -> snapshot.documents.mapNotNull { it.toExpenseItem() } }
        }

        // 3. Combine all flows into single emission
        combine(expenseFlows) { expenseLists ->
            expenseLists.flatMap { it }
        }.collect { emit(it) }
    }

    override suspend fun saveExpense(expense: EventExpenseItem) {
        db.collection("events")
            .document(expense.eventId)
            .collection("expenses")
            .document(expense.id)
            .set(expense.toMap())
    }

    override suspend fun deleteExpense(eventId: String, expenseId: String) {
        db.collection("events")
            .document(eventId)
            .collection("expenses")
            .document(expenseId)
            .delete()
    }

    override suspend fun replaceProfileId(oldId: String, newId: String) {
        val uid = auth.currentUser?.uid ?: return

        val eventsSnapshot = db.collection("events")
            .where { "ownerId" equalTo uid }
            .get()

        eventsSnapshot.documents.forEach { eventDoc ->
            val expensesSnapshot = db.collection("events")
                .document(eventDoc.id)
                .collection("expenses")
                .get()

            if (expensesSnapshot.documents.isEmpty()) return@forEach

            expensesSnapshot.documents.chunked(499).forEach { chunk ->
                db.batch().apply {
                    chunk.forEach { doc ->
                        val data = doc.getRawData() ?: return@forEach
                        val assignedIds = (data["assignedProfileIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                        val weights = (data["profileWeights"] as? Map<*, *>) ?: emptyMap<Any, Any>()

                        val needsUpdate = assignedIds.contains(oldId) || weights.containsKey(oldId)

                        if (needsUpdate) {
                            val newAssignedIds = assignedIds.map { if (it == oldId) newId else it }
                            val newWeights = weights.mapKeys { (k, _) -> if (k == oldId) newId else k.toString() }

                            update(
                                db.collection("events").document(eventDoc.id).collection("expenses").document(doc.id),
                                mapOf(
                                    "assignedProfileIds" to newAssignedIds,
                                    "profileWeights" to newWeights
                                )
                            )
                        }
                    }
                    commit()
                }
            }
        }
    }

    override suspend fun fetchExpensesForEvent(eventId: String): List<EventExpenseItem> {
        return try {
            db.collection("events").document(eventId).collection("expenses").get()
                .documents.mapNotNull { it.toExpenseItem() }
        } catch (e: Exception) {
            LogSanitizer.log("FirestoreExpenseRepo", "fetchExpensesForEvent failed: ${e.message}")
            emptyList()
        }
    }

    override suspend fun deleteAllExpensesForEvent(eventId: String) {
        try {
            val expenses = db.collection("events")
                .document(eventId)
                .collection("expenses")
                .get()
            expenses.documents.forEach { doc ->
                db.collection("events")
                    .document(eventId)
                    .collection("expenses")
                    .document(doc.id)
                    .delete()
            }
        } catch (e: Exception) {
            LogSanitizer.log("FirestoreExpenseRepo", "deleteAllExpensesForEvent failed: ${e.message}")
        }
    }

    override suspend fun fetchAllExpenses(): List<EventExpenseItem> {
        val uid = auth.currentUser?.uid ?: return emptyList()

        val ownerSnapshot = db.collection("events").where { "ownerId" equalTo uid }.get()
        val memberSnapshot = db.collection("events").where { "memberIds" contains uid }.get()
        val participantSnapshot = db.collection("events").where { "participantIds" contains uid }.get()

        val eventIds = (ownerSnapshot.documents + memberSnapshot.documents + participantSnapshot.documents)
            .map { it.id }
            .distinct()

        if (eventIds.isEmpty()) return emptyList()

        val allExpenses = mutableListOf<EventExpenseItem>()
        for (eventId in eventIds) {
            val expenses = try {
                db.collection("events").document(eventId).collection("expenses").get()
                    .documents.mapNotNull { it.toExpenseItem() }
            } catch (e: Exception) {
                LogSanitizer.log("FirestoreExpenseRepo", "fetchAllExpenses for event $eventId FAILED: ${e.message}")
                emptyList()
            }
            allExpenses.addAll(expenses)
        }
        LogSanitizer.log("FirestoreExpenseRepo", "fetchAllExpenses: ${eventIds.size} events → ${allExpenses.size} expenses")
        return allExpenses
    }

    private fun EventExpenseItem.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "eventId" to eventId,
        "name" to name,
        "amountEuros" to amountEuros,
        "category" to category,
        "assignedProfileIds" to assignedProfileIds,
        "profileWeights" to profileWeights,
        "paidByProfileId" to paidByProfileId,
        "splitMode" to splitMode,
        "debtorIds" to debtorIds,
        "payerContributions" to payerContributions,
        "exchangeRate" to exchangeRate,
        "itemCurrency" to itemCurrency,
        "createdAtMillis" to createdAtMillis,
        "createdByProfileId" to createdByProfileId
    )

    private fun dev.gitlive.firebase.firestore.DocumentSnapshot.toExpenseItem(): EventExpenseItem? {
        return try {
            val data = this.getRawData() ?: return null
            EventExpenseItem(
                id = data["id"] as? String ?: return null,
                eventId = data["eventId"] as? String ?: return null,
                name = data["name"] as? String ?: return null,
                amountEuros = (data["amountEuros"] as? Number)?.toDouble() ?: 0.0,
                category = data["category"] as? String ?: "shared",
                assignedProfileIds = (data["assignedProfileIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                profileWeights = (data["profileWeights"] as? Map<*, *>)?.entries?.associate {
                    (it.key as String) to ((it.value as? Number)?.toDouble() ?: 0.0)
                } ?: emptyMap(),
                paidByProfileId = data["paidByProfileId"] as? String ?: "",
                splitMode = data["splitMode"] as? String ?: "SIMPLE_AVG",
                debtorIds = (data["debtorIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                payerContributions = (data["payerContributions"] as? Map<*, *>)?.entries?.associate {
                    (it.key as String) to ((it.value as? Number)?.toDouble() ?: 0.0)
                } ?: emptyMap(),
                exchangeRate = (data["exchangeRate"] as? Number)?.toDouble(),
                itemCurrency = data["itemCurrency"] as? String,
                createdAtMillis = (data["createdAtMillis"] as? Number)?.toLong() ?: 0L,
                createdByProfileId = data["createdByProfileId"] as? String ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }
}
