package com.cuentamorosos.data.repository

import com.cuentamorosos.currentTimeMillis
import com.cuentamorosos.data.NetworkMonitor
import com.cuentamorosos.data.PendingOperationQueue
import com.cuentamorosos.data.RemoteOperations
import com.cuentamorosos.db.CuentaMorososDatabase
import com.cuentamorosos.model.ProfileItem
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.cuentamorosos.data.LogSanitizer

class OfflineFirstProfileRepository(
    private val remoteRepository: ProfileRepository,
    private val database: CuentaMorososDatabase,
    private val networkMonitor: NetworkMonitor,
    private val syncScope: CoroutineScope,
    private val pendingQueue: PendingOperationQueue
) : ProfileRepository {

    private val queries = database.cachedProfileQueries
    private var syncJob: Job? = null

    private val pendingLocalChanges = mutableMapOf<String, Map<String, String>>()

    private val profileRemoteOps = object : RemoteOperations {
        override suspend fun saveEvent(entityId: String) = throw UnsupportedOperationException()
        override suspend fun deleteEvent(entityId: String) = throw UnsupportedOperationException()
        override suspend fun saveDebt(entityId: String) = throw UnsupportedOperationException()
        override suspend fun deleteDebt(entityId: String) = throw UnsupportedOperationException()
        override suspend fun saveExpense(entityId: String) = throw UnsupportedOperationException()
        override suspend fun deleteExpense(entityId: String) = throw UnsupportedOperationException()
        override suspend fun saveProfile(entityId: String) {
            val profiles = remoteRepository.observeProfiles().first()
            profiles.find { it.id == entityId }?.let { remoteRepository.saveProfile(it) }
        }
        override suspend fun deleteProfile(entityId: String) = remoteRepository.deleteProfile(entityId)
        override suspend fun updateProfilePhoto(profileId: String, photoUrl: String) {
            remoteRepository.updateProfilePhoto(photoUrl)
        }
        override suspend fun updateProfileUsername(profileId: String, username: String) {
            remoteRepository.updateUsername(username)
        }
        override suspend fun updateProfileDisplayName(profileId: String, displayName: String) {
            remoteRepository.updateDisplayName(displayName)
        }
        override suspend fun deleteProfilePhoto(profileId: String) {
            remoteRepository.deleteProfilePhoto()
        }
        override suspend fun linkGhostProfile(email: String, realUid: String) {
            remoteRepository.linkGhostProfile(email, realUid)
        }
    }

    fun startSync() {
        stopSyncLoop()
        // Start sync loop IMMEDIATELY — don't wait for network monitor
        startSyncLoop()
        // Also subscribe to reconnection events
        networkMonitor.isOnline
            .drop(1) // Skip initial emission (already handled above)
            .onEach { isOnline ->
                if (isOnline) startSyncLoop() else stopSyncLoop()
            }
            .launchIn(syncScope)
    }

    override fun observeProfiles(): Flow<List<ProfileItem>> {
        return queries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { cachedProfiles ->
                val items = cachedProfiles.map { it.toProfileItem() }
                items.forEach { p ->
                    if (p.id == p.ownerId) {
                        LogSanitizer.log("OfflineFirstProfileRepo", "observeProfiles → OWN: id=${p.id} name='${p.name}' username='${p.username}'")
                    }
                }
                items
            }
    }

    private fun findOwnProfile(): com.cuentamorosos.db.CachedProfile? {
        val allProfiles = queries.selectAll().executeAsList()
        return allProfiles.firstOrNull { it.id.isNotBlank() && it.id == it.ownerId }
    }

    private fun startSyncLoop() {
        syncJob?.cancel()
        syncJob = syncScope.launch(Dispatchers.Default) {
            var backoffMs = 1000L
            val maxBackoffMs = 30000L
            while (isActive) {
                try {
                    // 1. Drain pending operations FIRST
                    pendingQueue.drainAll(profileRemoteOps)

                    // 2. Single subscription to remote
                    remoteRepository.observeProfiles()
                        .onEach { remoteProfiles ->
                            LogSanitizer.log("OfflineFirstProfileRepo", "Sync update: ${remoteProfiles.size} profiles")
                            upsertProfiles(remoteProfiles)
                        }
                        .collect()

                    backoffMs = 1000L
                } catch (e: Exception) {
                    LogSanitizer.log("OfflineFirstProfileRepo", "Sync error: ${e.message}")
                    delay(backoffMs)
                    backoffMs = minOf(backoffMs * 2, maxBackoffMs)
                }
            }
        }
    }

    private fun stopSyncLoop() {
        syncJob?.cancel()
        syncJob = null
    }

    private fun upsertProfiles(profiles: List<ProfileItem>) {
        queries.transaction {
            profiles.forEach { profile ->
                val pending = pendingLocalChanges[profile.id]
                val finalUsername = pending?.get("username") ?: (profile.username ?: "")
                LogSanitizer.log("OfflineFirstProfileRepo", "upsertProfiles: id=${profile.id} name='${profile.name}' username='$finalUsername' (pending=${pending != null})")
                queries.upsert(
                    id = profile.id,
                    name = profile.name,
                    email = profile.linkedEmail ?: "",
                    isGhost = if (profile.isGhost) 1 else 0,
                    totalPendingEuros = profile.totalPendingEuros,
                    updatedAt = currentTimeMillis(),
                    ownerId = profile.ownerId,
                    photo_url = pending?.get("photo_url") ?: (profile.photoUrl ?: ""),
                    username = finalUsername,
                    display_name = "",
                    custom_names = serializeCustomNames(profile.customNames),
                )
            }
        }
    }

    override suspend fun saveProfile(profile: ProfileItem) {
        queries.upsert(
            id = profile.id,
            name = profile.name,
            email = profile.linkedEmail ?: "",
            isGhost = if (profile.isGhost) 1 else 0,
            totalPendingEuros = profile.totalPendingEuros,
            updatedAt = currentTimeMillis(),
            ownerId = profile.ownerId,
            photo_url = profile.photoUrl ?: "",
            username = profile.username ?: "",
            display_name = "",
            custom_names = serializeCustomNames(profile.customNames),
        )
        try {
            remoteRepository.saveProfile(profile)
        } catch (e: Exception) {
            LogSanitizer.log("OfflineFirstProfileRepo", "saveProfile remote FAILED for ${profile.id}: ${e.message}")
            e.printStackTrace()
            pendingQueue.enqueue(
                id = "profile_${profile.id}_${currentTimeMillis()}",
                entityType = "profile",
                entityId = profile.id,
                operation = "save",
                payload = ""
            )
        }
    }

    override suspend fun deleteProfile(profileId: String) {
        queries.deleteById(profileId)
        try {
            remoteRepository.deleteProfile(profileId)
        } catch (e: Exception) {
            LogSanitizer.log("OfflineFirstProfileRepo", "deleteProfile remote FAILED for $profileId: ${e.message}")
            e.printStackTrace()
            pendingQueue.enqueue(
                id = "profile_${profileId}_${currentTimeMillis()}",
                entityType = "profile",
                entityId = profileId,
                operation = "delete",
                payload = ""
            )
        }
    }

    override suspend fun linkGhostProfile(userEmail: String, userUid: String) {
        try {
            remoteRepository.linkGhostProfile(userEmail, userUid)
        } catch (e: Exception) {
            LogSanitizer.log("OfflineFirstProfileRepo", "linkGhostProfile remote FAILED for $userUid: ${e.message}")
            e.printStackTrace()
            pendingQueue.enqueue(
                id = "linkghost_${userUid}_${currentTimeMillis()}",
                entityType = "profile",
                entityId = userUid,
                operation = "linkGhost",
                payload = "$userEmail|$userUid"
            )
        }
    }

    // ── Phase 2: Profile & Account Settings ─────────────────────────────────

    override suspend fun updateProfilePhoto(photoUrl: String): Result<String> {
        return try {
            LogSanitizer.log("OfflineFirstProfileRepo", "updateProfilePhoto called url='$photoUrl'")
            val ownProfile = findOwnProfile()
            if (ownProfile != null) {
                val pending = pendingLocalChanges[ownProfile.id]?.toMutableMap() ?: mutableMapOf()
                pending["photo_url"] = photoUrl
                pendingLocalChanges[ownProfile.id] = pending
                LogSanitizer.log("OfflineFirstProfileRepo", "updateProfilePhoto pending set for ${ownProfile.id}")

                queries.upsert(
                    id = ownProfile.id,
                    name = ownProfile.name,
                    email = ownProfile.email,
                    isGhost = ownProfile.isGhost,
                    totalPendingEuros = ownProfile.totalPendingEuros,
                    updatedAt = currentTimeMillis(),
                    ownerId = ownProfile.ownerId,
                    photo_url = photoUrl,
                    username = ownProfile.username,
                    display_name = "",
                    custom_names = ownProfile.custom_names,
                )
                LogSanitizer.log("OfflineFirstProfileRepo", "updateProfilePhoto local cache updated")
            } else {
                LogSanitizer.log("OfflineFirstProfileRepo", "updateProfilePhoto: own profile not found!")
            }
            val result = remoteRepository.updateProfilePhoto(photoUrl)
            LogSanitizer.log("OfflineFirstProfileRepo", "updateProfilePhoto remote: success=${result.isSuccess} error=${result.exceptionOrNull()?.message}")
            if (result.isSuccess) {
                pendingLocalChanges.remove(ownProfile?.id)
                LogSanitizer.log("OfflineFirstProfileRepo", "updateProfilePhoto pending cleared")
            } else {
                pendingQueue.enqueue(
                    id = "photo_${currentTimeMillis()}",
                    entityType = "profile",
                    entityId = ownProfile?.id ?: "",
                    operation = "updatePhoto",
                    payload = photoUrl
                )
                LogSanitizer.log("OfflineFirstProfileRepo", "updateProfilePhoto enqueued to pending")
            }
            result.map { photoUrl }
        } catch (e: Exception) {
            LogSanitizer.log("OfflineFirstProfileRepo", "updateProfilePhoto exception: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun updateUsername(username: String): Result<Unit> {
        return try {
            LogSanitizer.log("OfflineFirstProfileRepo", "updateUsername called '$username'")
            val ownProfile = findOwnProfile()
            if (ownProfile != null) {
                val pending = pendingLocalChanges[ownProfile.id]?.toMutableMap() ?: mutableMapOf()
                pending["username"] = username
                pendingLocalChanges[ownProfile.id] = pending
                LogSanitizer.log("OfflineFirstProfileRepo", "updateUsername pending set for ${ownProfile.id}")

                queries.upsert(
                    id = ownProfile.id,
                    name = ownProfile.name,
                    email = ownProfile.email,
                    isGhost = ownProfile.isGhost,
                    totalPendingEuros = ownProfile.totalPendingEuros,
                    updatedAt = currentTimeMillis(),
                    ownerId = ownProfile.ownerId,
                    photo_url = ownProfile.photo_url,
                    username = username,
                    display_name = "",
                    custom_names = ownProfile.custom_names,
                )
                LogSanitizer.log("OfflineFirstProfileRepo", "updateUsername local cache updated")
            } else {
                LogSanitizer.log("OfflineFirstProfileRepo", "updateUsername: own profile not found!")
            }
            val result = remoteRepository.updateUsername(username)
            LogSanitizer.log("OfflineFirstProfileRepo", "updateUsername remote: success=${result.isSuccess} error=${result.exceptionOrNull()?.message}")
            if (result.isSuccess) {
                pendingLocalChanges.remove(ownProfile?.id)
                LogSanitizer.log("OfflineFirstProfileRepo", "updateUsername pending cleared")
            } else {
                pendingQueue.enqueue(
                    id = "username_${currentTimeMillis()}",
                    entityType = "profile",
                    entityId = ownProfile?.id ?: "",
                    operation = "updateUsername",
                    payload = username
                )
                LogSanitizer.log("OfflineFirstProfileRepo", "updateUsername enqueued to pending")
            }
            result
        } catch (e: Exception) {
            LogSanitizer.log("OfflineFirstProfileRepo", "updateUsername exception: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun updateDisplayName(displayName: String): Result<Unit> {
        return try {
            LogSanitizer.log("OfflineFirstProfileRepo", "updateDisplayName called '$displayName'")
            val ownProfile = findOwnProfile()
            if (ownProfile != null) {
                queries.upsert(
                    id = ownProfile.id,
                    name = displayName,
                    email = ownProfile.email,
                    isGhost = ownProfile.isGhost,
                    totalPendingEuros = ownProfile.totalPendingEuros,
                    updatedAt = currentTimeMillis(),
                    ownerId = ownProfile.ownerId,
                    photo_url = ownProfile.photo_url,
                    username = ownProfile.username,
                    display_name = "",
                    custom_names = ownProfile.custom_names,
                )
                LogSanitizer.log("OfflineFirstProfileRepo", "updateDisplayName: oldName='${ownProfile.name}' → newName='$displayName' (local cache updated)")
            } else {
                LogSanitizer.log("OfflineFirstProfileRepo", "updateDisplayName: own profile not found!")
            }
            val result = remoteRepository.updateDisplayName(displayName)
            LogSanitizer.log("OfflineFirstProfileRepo", "updateDisplayName remote: success=${result.isSuccess} error=${result.exceptionOrNull()?.message}")
            result
        } catch (e: Exception) {
            LogSanitizer.log("OfflineFirstProfileRepo", "updateDisplayName exception: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun setCustomName(profileId: String, customName: String): Result<Unit> {
        return try {
            remoteRepository.setCustomName(profileId, customName)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isUsernameAvailable(username: String): Boolean {
        return try {
            remoteRepository.isUsernameAvailable(username)
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun deleteProfilePhoto(): Result<Unit> {
        return try {
            LogSanitizer.log("OfflineFirstProfileRepo", "deleteProfilePhoto called")
            val ownProfile = findOwnProfile()
            if (ownProfile != null) {
                val pending = pendingLocalChanges[ownProfile.id]?.toMutableMap() ?: mutableMapOf()
                pending["photo_url"] = ""
                pendingLocalChanges[ownProfile.id] = pending

                queries.upsert(
                    id = ownProfile.id,
                    name = ownProfile.name,
                    email = ownProfile.email,
                    isGhost = ownProfile.isGhost,
                    totalPendingEuros = ownProfile.totalPendingEuros,
                    updatedAt = currentTimeMillis(),
                    ownerId = ownProfile.ownerId,
                    photo_url = "",
                    username = ownProfile.username,
                    display_name = "",
                    custom_names = ownProfile.custom_names,
                )
                LogSanitizer.log("OfflineFirstProfileRepo", "deleteProfilePhoto local cache updated")
            }
            val result = remoteRepository.deleteProfilePhoto()
            LogSanitizer.log("OfflineFirstProfileRepo", "deleteProfilePhoto remote: success=${result.isSuccess}")
            if (result.isSuccess) {
                pendingLocalChanges.remove(ownProfile?.id)
            } else {
                pendingQueue.enqueue(
                    id = "deletephoto_${currentTimeMillis()}",
                    entityType = "profile",
                    entityId = ownProfile?.id ?: "",
                    operation = "deletePhoto",
                    payload = ""
                )
            }
            result
        } catch (e: Exception) {
            LogSanitizer.log("OfflineFirstProfileRepo", "deleteProfilePhoto exception: ${e.message}")
            Result.failure(e)
        }
    }

    // ── Phase 3: Username Search (remote-only) ───────────────────────────────

    override suspend fun searchByUsername(prefix: String): List<ProfileItem> {
        throw UnsupportedOperationException("searchByUsername is remote-only; use FirestoreProfileRepository")
    }

    override suspend fun updatePassword(currentPassword: String, newPassword: String): Result<Unit> {
        return try {
            remoteRepository.updatePassword(currentPassword, newPassword)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Serialization Helpers ───────────────────────────────────────────────

    private fun serializeCustomNames(customNames: Map<String, String>): String {
        if (customNames.isEmpty()) return ""
        return customNames.entries.joinToString("|") { (key, value) ->
            "$key=${value.replace("|", "\\|")}"
        }
    }

    private fun parseCustomNames(json: String): Map<String, String> {
        if (json.isBlank()) return emptyMap()
        val result = mutableMapOf<String, String>()
        val parts = splitAtUnescapedPipe(json)
        for (part in parts) {
            val eq = part.indexOf('=')
            if (eq > 0) {
                result[part.substring(0, eq)] = part.substring(eq + 1).unescapePipe()
            }
        }
        return result
    }

    /** Splits on `|` that is NOT preceded by `\`. */
    private fun splitAtUnescapedPipe(input: String): List<String> {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var i = 0
        while (i < input.length) {
            when {
                input[i] == '\\' && i + 1 < input.length && input[i + 1] == '|' -> {
                    current.append("\\|")
                    i += 2
                }
                input[i] == '|' -> {
                    parts.add(current.toString())
                    current.clear()
                    i++
                }
                else -> {
                    current.append(input[i])
                    i++
                }
            }
        }
        parts.add(current.toString())
        return parts
    }

    private fun String.unescapePipe(): String = replace("\\|", "|")

    // ── Mapping Helpers ─────────────────────────────────────────────────────

    private fun com.cuentamorosos.db.CachedProfile.toProfileItem(): ProfileItem {
        val isGhost = isGhost == 1L
        return ProfileItem(
            id = id,
            name = name,
            totalPendingEuros = totalPendingEuros,
            isGhost = isGhost,
            linkedEmail = if (email.isBlank()) null else email,
            ownerId = ownerId,
            photoUrl = if (isGhost) null else photo_url.takeIf { it.isNotBlank() },
            username = if (isGhost) null else username.takeIf { it.isNotBlank() },
            customNames = parseCustomNames(custom_names),
        )
    }
}
