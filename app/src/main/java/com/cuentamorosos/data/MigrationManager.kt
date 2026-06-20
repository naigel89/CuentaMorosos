package com.cuentamorosos.data

import android.content.Context
import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.EventParticipant
import com.cuentamorosos.model.EventRole
import com.cuentamorosos.model.EventState
import com.cuentamorosos.model.SUPPORTED_CURRENCY
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

object MigrationManager {

    /**
     * Retorna true si el usuario ya ha sido migrado según el flag en Firestore.
     */
    suspend fun isMigrated(uid: String): Boolean {
        return try {
            val doc = FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .await()
            doc.getBoolean("migrated") == true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Retorna true si existen datos relevantes en SharedPreferences que migrar.
     */
    fun hasLocalData(context: Context): Boolean {
        val store = CuentaMorososLocalStore(context)
        return store.loadEvents().isNotEmpty() || store.loadProfiles().isNotEmpty()
    }

    /**
     * Lee los datos locales, los sube a Firestore en batch y marca el flag migrated.
     * Lanza una excepción si el proceso falla, para que el llamador pueda reintentar.
     */
    suspend fun migrate(uid: String, context: Context) {
        val store = CuentaMorososLocalStore(context)
        val events = store.loadEvents()
        val profiles = store.loadProfiles()
        val debts = store.loadDebts()
        val expenses = store.loadExpenses()

        val db = FirebaseFirestore.getInstance()

        // Firestore limita los batches a 500 operaciones; dividimos si hace falta.
        val allOps = buildList {
            events.forEach { event ->
                add(Pair("events/${event.id}", event.toMigrationMap(uid)))
            }
            debts.forEach { debt ->
                add(Pair("events/${debt.eventId}/debts/${debt.id}", debt.toMigrationMap()))
            }
            expenses.forEach { expense ->
                add(Pair("events/${expense.eventId}/expenses/${expense.id}", expense.toMigrationMap()))
            }
            profiles.forEach { profile ->
                add(Pair("profiles/${profile.id}", profile.toMigrationMap(uid)))
            }
        }

        allOps.chunked(499).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { (path, data) ->
                val parts = path.split("/")
                var ref = db.collection(parts[0]).document(parts[1])
                var i = 2
                while (i < parts.size - 1) {
                    ref = ref.collection(parts[i]).document(parts[i + 1])
                    i += 2
                }
                batch.set(ref, data)
            }
            batch.commit().await()
        }

        // Marcar migración completada
        db.collection("users").document(uid)
            .set(mapOf("uid" to uid, "migrated" to true), SetOptions.merge())
            .await()
    }

    // ── Mapas de serialización para la migración ──────────────────────────────

    private fun EventItem.toMigrationMap(uid: String): Map<String, Any?> {
        val participants = buildList {
            if (uid.isNotBlank()) {
                add(EventParticipant(profileId = uid, role = EventRole.OWNER, joinedAtMillis = dateMillis))
            }
            memberIds.filter { it != uid }.forEach { mid ->
                add(EventParticipant(profileId = mid, role = EventRole.CONTRIBUTOR, joinedAtMillis = dateMillis))
            }
        }
        return mapOf(
            "id" to id,
            "name" to name,
            "dateMillis" to dateMillis,
            "ownerId" to uid,
            "memberIds" to listOf(uid),
            "participants" to participants.map { p ->
                mapOf("profileId" to p.profileId, "role" to p.role.name, "joinedAtMillis" to p.joinedAtMillis)
            },
            "participantIds" to participants.map { it.profileId },
            "baseCurrency" to (baseCurrency.takeIf { it.isNotBlank() } ?: SUPPORTED_CURRENCY),
            "state" to state.name,
            "lastCalculationMode" to lastCalculationMode,
            "lastCalculationTotal" to lastCalculationTotal,
            "lastCalculationTimestamp" to lastCalculationTimestamp,
            "lastCalculationSummary" to lastCalculationSummary
        )
    }

    private fun com.cuentamorosos.model.EventDebtItem.toMigrationMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "eventId" to eventId,
        "profileId" to profileId,
        "amountEuros" to amountEuros,
        "notes" to notes,
        "paid" to paid,
        "calculationMode" to calculationMode
    )

    private fun com.cuentamorosos.model.EventExpenseItem.toMigrationMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "eventId" to eventId,
        "name" to name,
        "amountEuros" to amountEuros,
        "category" to category,
        "assignedProfileIds" to assignedProfileIds
    )

    private fun com.cuentamorosos.model.ProfileItem.toMigrationMap(uid: String): Map<String, Any?> = mapOf(
        "id" to id,
        "name" to name,
        "totalPendingEuros" to totalPendingEuros,
        "ownerId" to uid
    )
}
