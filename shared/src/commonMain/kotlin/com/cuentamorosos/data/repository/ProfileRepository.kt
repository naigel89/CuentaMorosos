package com.cuentamorosos.data.repository

import com.cuentamorosos.model.ProfileItem
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeProfiles(): Flow<List<ProfileItem>>
    suspend fun saveProfile(profile: ProfileItem)
    suspend fun deleteProfile(profileId: String)
    suspend fun linkGhostProfile(userEmail: String, userUid: String)

    // ── Profile & Account Settings Phase 2 ─────────────────────────────────

    /** Updates the profile photo URL (URL after upload to storage). */
    suspend fun updateProfilePhoto(photoUrl: String): Result<String>

    /** Updates the unique username. */
    suspend fun updateUsername(username: String): Result<Unit>

    /** Updates the display name shown in cards and lists. */
    suspend fun updateDisplayName(displayName: String): Result<Unit>

    /** Sets a custom name for a viewed profile, stored in subcollection. */
    suspend fun setCustomName(profileId: String, customName: String): Result<Unit>

    /** Checks if a username is available (not taken by another profile). */
    suspend fun isUsernameAvailable(username: String): Boolean

    /** Changes the password via Firebase Auth reauthentication. Remote-only. */
    suspend fun updatePassword(currentPassword: String, newPassword: String): Result<Unit>

    /**
     * Deletes the profile photo from Firebase Storage and sets photoUrl to null.
     * Returns success when the photo URL has been cleared.
     */
    suspend fun deleteProfilePhoto(): Result<Unit>

    /** Searches ALL Firestore profiles whose username starts with [prefix].
     *  Ghost profiles (username=null) and profiles without linkedEmail are excluded.
     *  Results are limited to 20 profiles. Case-insensitive. */
    suspend fun searchByUsername(prefix: String): List<ProfileItem>

    // ── Orphan cleanup (GPS-REQ-006) ─────────────────────────────────────────

    /**
     * Idempotent orphan cleanup: scans debts, expenses, and events for references
     * to non-existent profile UUIDs and removes them.
     * Default no-op; FirestoreProfileRepository provides the real implementation.
     */
    suspend fun cleanupOrphans() {}
}
