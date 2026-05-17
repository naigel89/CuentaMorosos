package com.cuentamorosos.data.repository

import com.cuentamorosos.model.EventExpenseItem
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

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
        val eventsSnapshot = db.collection("events").get()
        val allExpenses = mutableListOf<EventExpenseItem>()

        for (eventDoc in eventsSnapshot.documents) {
            val expensesSnapshot = db.collection("events")
                .document(eventDoc.id)
                .collection("expenses")
                .get()

            for (expenseDoc in expensesSnapshot.documents) {
                expenseDoc.toExpenseItem()?.let { allExpenses.add(it) }
            }
        }
        emit(allExpenses)
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
                        val data = doc.data<Map<String, Any?>>()
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
            val data = data<Map<String, Any?>>()
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
