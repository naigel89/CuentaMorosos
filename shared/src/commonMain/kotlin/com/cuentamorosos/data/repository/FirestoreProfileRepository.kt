package com.cuentamorosos.data.repository

import com.cuentamorosos.model.ProfileItem
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FirestoreProfileRepository : ProfileRepository {

    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val collection = db.collection("profiles")

    override fun observeProfiles(): Flow<List<ProfileItem>> {
        val uid = auth.currentUser?.uid ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return collection.where { "ownerId" equalTo uid }
            .snapshots
            .map { snapshot ->
                snapshot.documents
                    .mapNotNull { it.toProfileItem() }
                    .sortedBy { it.name }
            }
    }

    override suspend fun saveProfile(profile: ProfileItem) {
        val uid = auth.currentUser?.uid ?: return
        collection.document(profile.id).set(profile.toMap(uid))
    }

    override suspend fun deleteProfile(profileId: String) {
        collection.document(profileId).delete()
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

    private fun dev.gitlive.firebase.firestore.DocumentSnapshot.toProfileItem(): ProfileItem? {
        return try {
            val data = data<Map<String, Any?>>()
            ProfileItem(
                id = data["id"] as? String ?: return null,
                name = data["name"] as? String ?: return null,
                icon = data["icon"] as? String ?: return null,
                totalPendingEuros = (data["totalPendingEuros"] as? Number)?.toDouble() ?: 0.0,
                isGhost = data["isGhost"] as? Boolean ?: false,
                linkedEmail = data["linkedEmail"] as? String,
            )
        } catch (e: Exception) {
            null
        }
    }
}
