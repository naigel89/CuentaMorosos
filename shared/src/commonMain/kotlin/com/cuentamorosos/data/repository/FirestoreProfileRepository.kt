package com.cuentamorosos.data.repository

import com.cuentamorosos.currentTimeMillis
import com.cuentamorosos.model.ProfileItem
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.EmailAuthProvider
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class FirestoreProfileRepository : ProfileRepository {

    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val collection = db.collection("profiles")

    override fun observeProfiles(): Flow<List<ProfileItem>> {
        val uid = auth.currentUser?.uid ?: return flowOf(emptyList())
        println("[FirestoreProfileRepo] observeProfiles called, uid=$uid")
        return collection.where { "ownerId" equalTo uid }
            .snapshots
            .map { snapshot ->
                val items = snapshot.documents
                    .mapNotNull { doc ->
                        val item = doc.toProfileItem()
                        if (item == null) println("[FirestoreProfileRepo] Failed to parse doc ${doc.id}")
                        item
                    }
                    .sortedBy { it.name }
                println("[FirestoreProfileRepo] Snapshot emitted: ${items.size} profiles: ${items.map { it.name }}")
                items
            }
            .catch { e ->
                println("[FirestoreProfileRepo] Error in snapshot: ${e.message}")
                emit(emptyList())
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

    // ── Phase 2: Profile & Account Settings ─────────────────────────────────

    override suspend fun updateProfilePhoto(photoUrl: String): Result<String> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
            collection.document(uid).update("photoUrl" to photoUrl, "updatedAt" to currentTimeMillis())
            Result.success(photoUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUsername(username: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
            collection.document(uid).update("username" to username, "updatedAt" to currentTimeMillis())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateDisplayName(displayName: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
            collection.document(uid).update("displayName" to displayName, "updatedAt" to currentTimeMillis())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setCustomName(profileId: String, customName: String): Result<Unit> {
        return try {
            val viewerId = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
            setCustomNameInternal(profileId, viewerId, customName)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isUsernameAvailable(username: String): Boolean {
        return try {
            val uid = auth.currentUser?.uid ?: return false
            val results = collection.where { "username" equalTo username }.limit(2).get()
            // Available if no matches, or only match is the current user's own profile
            results.documents.none { it.id != uid }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun deleteProfilePhoto(): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
            // Set photoUrl to null in Firestore profile document
            collection.document(uid).update("photoUrl" to null, "updatedAt" to currentTimeMillis())
            // Note: The actual Storage file deletion should happen client-side
            // before calling this method, or via a Firebase Cloud Function
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePassword(currentPassword: String, newPassword: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))
            val email = user.email ?: return Result.failure(Exception("User has no email"))
            val credential = EmailAuthProvider.credential(email, currentPassword)
            user.reauthenticate(credential)
            user.updatePassword(newPassword)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Custom Names Subcollection ──────────────────────────────────────────

    /**
     * Reads a custom name from profiles/{profileId}/customNames/{viewerId}.
     * Returns null if no custom name has been set.
     */
    suspend fun getCustomName(profileId: String, viewerId: String): String? {
        return try {
            val doc = collection.document(profileId).collection("customNames").document(viewerId).get()
            runCatching { doc.get<String>("customName") }.getOrNull()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Writes a custom name to profiles/{profileId}/customNames/{viewerId}.
     */
    suspend fun setCustomNameInternal(profileId: String, viewerId: String, customName: String) {
        collection.document(profileId).collection("customNames").document(viewerId)
            .set(mapOf("customName" to customName, "updatedAt" to currentTimeMillis()))
    }

    // ── Mapping Helpers ─────────────────────────────────────────────────────

    private fun ProfileItem.toMap(uid: String): Map<String, Any?> = mapOf(
        "id" to id,
        "name" to name,
        "totalPendingEuros" to totalPendingEuros,
        "isGhost" to isGhost,
        "linkedEmail" to linkedEmail,
        "ownerId" to uid,
        "photoUrl" to photoUrl,
        "username" to username,
        "displayName" to displayName,
    )

    private fun dev.gitlive.firebase.firestore.DocumentSnapshot.toProfileItem(): ProfileItem? {
        return try {
            // Read fields individually via get<T>() — data<Map>() fails in KMP Firebase 1.13+
            // because kotlinx.serialization has no serializer for Any
            val id = runCatching { get<String>("id") }.getOrNull() ?: return null
            val name = runCatching { get<String>("name") }.getOrNull() ?: return null
            val totalPendingEuros = runCatching { get<Double>("totalPendingEuros") }.getOrDefault(0.0)
            val isGhost = runCatching { get<Boolean>("isGhost") }.getOrDefault(false)
            val linkedEmail = runCatching { get<String>("linkedEmail") }.getOrNull()
            val ownerId = runCatching { get<String>("ownerId") }.getOrDefault("")
            val photoUrl = if (isGhost) null else runCatching { get<String>("photoUrl") }.getOrNull()
            val username = if (isGhost) null else runCatching { get<String>("username") }.getOrNull()
            val displayName = runCatching { get<String>("displayName") }.getOrNull()
            ProfileItem(
                id = id,
                name = name,
                totalPendingEuros = totalPendingEuros,
                isGhost = isGhost,
                linkedEmail = linkedEmail,
                ownerId = ownerId,
                // Ghost profiles force null for photoUrl, username, displayName
                photoUrl = photoUrl,
                username = username,
                displayName = displayName,
                customNames = emptyMap(), // loaded separately via customNames subcollection
            )
        } catch (e: Exception) {
            println("[FirestoreProfileRepo] FATAL: toProfileItem exception for doc ${this.id}: ${e::class.simpleName}: ${e.message}")
            null
        }
    }
}
