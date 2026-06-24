package com.cuentamorosos.data

import com.cuentamorosos.data.repository.ProfileRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

object FirebaseUserSyncManager {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    suspend fun syncCurrentUser(
        defaultMigrated: Boolean? = null,
        profileRepository: ProfileRepository? = null,
    ) {
        syncCurrentUserProfile(defaultMigrated = defaultMigrated)
        ensureOwnProfile(profileRepository = profileRepository)
        syncCurrentUserToken()
    }

    suspend fun syncCurrentUserProfile(defaultMigrated: Boolean? = null) {
        val user = auth.currentUser ?: return
        val normalizedEmail = user.email?.trim()?.lowercase().orEmpty()
        val payload = mutableMapOf<String, Any>(
            "uid" to user.uid,
            "email" to normalizedEmail,
            "updatedAt" to FieldValue.serverTimestamp(),
        )

        user.displayName?.takeIf { it.isNotBlank() }?.let { displayName ->
            payload["displayName"] = displayName
        }

        defaultMigrated?.let { migrated ->
            payload["migrated"] = migrated
        }

        runCatching {
            db.collection("users")
                .document(user.uid)
                .set(payload, SetOptions.merge())
                .await()
        }
    }

    /**
     * Creates or updates the user's own ProfileItem in the "profiles" Firestore collection.
     * This ensures the user always appears as a participant in their own events.
     * Uses merge so existing data (e.g. totalPendingEuros) is not overwritten.
     *
     * Sets a default @username from the email prefix ONLY if the profile does not
     * already have one, so existing custom usernames are never overwritten.
     */
    suspend fun ensureOwnProfile(profileRepository: ProfileRepository? = null) {
        val user = auth.currentUser ?: return

        // Read existing profile first — preserve user-edited fields
        val existingProfile = runCatching {
            db.collection("profiles")
                .document(user.uid)
                .get()
                .await()
        }.getOrNull()

        // Preserve existing name if present; otherwise derive from Firebase Auth
        val existingName = existingProfile?.getString("name")?.takeIf { it.isNotBlank() }
        val fbDisplayName = user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: "Yo"
        val finalName = existingName ?: fbDisplayName

        // Only derive a default username if the profile doesn't already have one
        val username = existingProfile?.getString("username")
            ?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore("@")
                ?.takeIf { it.matches(Regex("^[a-zA-Z0-9_]{3,}$")) }
                ?: user.uid.take(8)

        LogSanitizer.log("FirebaseUserSyncManager", "ensureOwnProfile: uid=${user.uid} fbDisplayName='$fbDisplayName' existingName='$existingName' finalName='$finalName' username='$username'")

        val payload = mapOf(
            "id" to user.uid,
            "name" to finalName,
            "username" to username,
            "isGhost" to false,
            "linkedEmail" to user.email?.trim()?.lowercase(),
            "ownerId" to user.uid,
        )
        runCatching {
            db.collection("profiles")
                .document(user.uid)
                .set(payload, SetOptions.merge())
                .await()
        }.onFailure { e ->
            LogSanitizer.log("FirebaseUserSyncManager", "ensureOwnProfile set FAILED: ${e.message}")
        }

        // Fire-and-forget: reconcile ghost profiles (GPS-REQ-002)
        // Merge failure SHALL NOT block registration
        if (profileRepository != null) {
            user.email?.trim()?.lowercase()?.let { email ->
                runCatching {
                    profileRepository.linkGhostProfile(email, user.uid)
                }.onFailure { e ->
                    LogSanitizer.log("FirebaseUserSyncManager", "linkGhostProfile FAILED: ${e.message}")
                }
            }
        }
    }

    suspend fun syncCurrentUserToken() {
        val token = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull() ?: return
        saveTokenForCurrentUser(token)
    }

    suspend fun saveTokenForCurrentUser(token: String) {
        val user = auth.currentUser ?: return
        val normalizedEmail = user.email?.trim()?.lowercase().orEmpty()
        val payload = mapOf(
            "uid" to user.uid,
            "email" to normalizedEmail,
            "fcmToken" to token,
            "updatedAt" to FieldValue.serverTimestamp(),
        )

        runCatching {
            db.collection("users")
                .document(user.uid)
                .set(payload, SetOptions.merge())
                .await()
        }.onFailure { e ->
            LogSanitizer.log("FirebaseUserSyncManager", "saveTokenForCurrentUser FAILED: ${e.message}")
        }
    }
}
