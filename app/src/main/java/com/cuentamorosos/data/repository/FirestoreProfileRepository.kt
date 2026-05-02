package com.cuentamorosos.data.repository

import com.cuentamorosos.model.ProfileItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreProfileRepository : ProfileRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val collection = db.collection("profiles")

    override fun observeProfiles(): Flow<List<ProfileItem>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val query = collection.whereEqualTo("ownerId", uid)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val profiles = snapshot?.documents?.mapNotNull { doc ->
                doc.toProfileItem()
            } ?: emptyList()

            trySend(profiles.sortedBy { it.name })
        }

        awaitClose { registration.remove() }
    }

    override suspend fun saveProfile(profile: ProfileItem) {
        val uid = auth.currentUser?.uid ?: return
        collection.document(profile.id).set(profile.toMap(uid)).await()
    }

    override suspend fun deleteProfile(profileId: String) {
        collection.document(profileId).delete().await()
    }

    override suspend fun linkGhostProfile(userEmail: String, userUid: String) {
        // Handled by CompositeProfileRepository
    }

    private fun ProfileItem.toMap(uid: String): Map<String, Any?> = mapOf(
        "id" to id,
        "name" to name,
        "icon" to icon,
        "totalPendingEuros" to totalPendingEuros,
        "isGhost" to isGhost,
        "linkedEmail" to linkedEmail,
        "ownerId" to uid
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.toProfileItem(): ProfileItem? {
        val data = this.data ?: return null
        return ProfileItem(
            id = data["id"] as? String ?: return null,
            name = data["name"] as? String ?: return null,
            icon = data["icon"] as? String ?: return null,
            totalPendingEuros = (data["totalPendingEuros"] as? Number)?.toDouble() ?: 0.0,
            isGhost = data["isGhost"] as? Boolean ?: false,
            linkedEmail = data["linkedEmail"] as? String,
        )
    }
}
