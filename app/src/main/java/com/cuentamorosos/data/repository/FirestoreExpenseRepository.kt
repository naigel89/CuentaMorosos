package com.cuentamorosos.data.repository

import com.cuentamorosos.model.EventExpenseItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreExpenseRepository : ExpenseRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun observeExpenses(eventId: String): Flow<List<EventExpenseItem>> = callbackFlow {
        val registration = db.collection("events")
            .document(eventId)
            .collection("expenses")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val expenses = snapshot?.documents?.mapNotNull { doc ->
                    doc.toExpenseItem()
                } ?: emptyList()
                
                trySend(expenses)
            }

        awaitClose { registration.remove() }
    }

    override suspend fun saveExpense(expense: EventExpenseItem) {
        db.collection("events")
            .document(expense.eventId)
            .collection("expenses")
            .document(expense.id)
            .set(expense.toMap())
            .await()
    }

    override suspend fun deleteExpense(eventId: String, expenseId: String) {
        db.collection("events")
            .document(eventId)
            .collection("expenses")
            .document(expenseId)
            .delete()
            .await()
    }

    override suspend fun replaceProfileId(oldId: String, newId: String) {
        val uid = auth.currentUser?.uid ?: return

        val eventsSnapshot = db.collection("events")
            .whereEqualTo("ownerId", uid)
            .get()
            .await()

        eventsSnapshot.documents.forEach { eventDoc ->
            val expensesSnapshot = eventDoc.reference
                .collection("expenses")
                .get()
                .await()

            if (expensesSnapshot.isEmpty) return@forEach

            expensesSnapshot.documents.chunked(499).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { doc ->
                    val data = doc.data ?: return@forEach
                    val assignedIds = (data["assignedProfileIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    val weights = (data["profileWeights"] as? Map<*, *>) ?: emptyMap<Any, Any>()

                    val needsUpdate = assignedIds.contains(oldId) || weights.containsKey(oldId)

                    if (needsUpdate) {
                        val newAssignedIds = assignedIds.map { if (it == oldId) newId else it }
                        val newWeights = weights.mapKeys { (k, _) -> if (k == oldId) newId else k.toString() }

                        batch.update(doc.reference, mapOf(
                            "assignedProfileIds" to newAssignedIds,
                            "profileWeights" to newWeights
                        ))
                    }
                }
                batch.commit().await()
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
        "profileWeights" to profileWeights
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.toExpenseItem(): EventExpenseItem? {
        val data = this.data ?: return null
        return EventExpenseItem(
            id = data["id"] as? String ?: return null,
            eventId = data["eventId"] as? String ?: return null,
            name = data["name"] as? String ?: return null,
            amountEuros = (data["amountEuros"] as? Number)?.toDouble() ?: 0.0,
            category = data["category"] as? String ?: "shared",
            assignedProfileIds = (data["assignedProfileIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            profileWeights = (data["profileWeights"] as? Map<*, *>)?.entries?.associate { 
                (it.key as String) to ((it.value as? Number)?.toDouble() ?: 0.0) 
            } ?: emptyMap()
        )
    }
}
