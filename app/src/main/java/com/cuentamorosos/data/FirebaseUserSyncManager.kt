package com.cuentamorosos.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

object FirebaseUserSyncManager {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    suspend fun syncCurrentUser(defaultMigrated: Boolean? = null) {
        syncCurrentUserProfile(defaultMigrated = defaultMigrated)
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
     */
    suspend fun ensureOwnProfile() {
        val user = auth.currentUser ?: return
        val displayName = user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: "Yo"
        val payload = mapOf(
            "id" to user.uid,
            "name" to displayName,
            "icon" to "😀",
            "isGhost" to false,
            "linkedEmail" to null,
            "ownerId" to user.uid,
        )
        runCatching {
            db.collection("profiles")
                .document(user.uid)
                .set(payload, SetOptions.merge())
                .await()
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
        }
    }
}
