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
                cachedProfiles.map { it.toProfileItem() }
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
                            println("[OfflineFirstProfileRepo] Sync update: ${remoteProfiles.size} profiles")
                            upsertProfiles(remoteProfiles)
                        }
                        .collect()

                    backoffMs = 1000L
                } catch (e: Exception) {
                    println("[OfflineFirstProfileRepo] Sync error: ${e.message}")
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
                queries.upsert(
                    id = profile.id,
                    name = profile.name,
                    email = profile.linkedEmail ?: "",
                    isGhost = if (profile.isGhost) 1 else 0,
                    totalPendingEuros = profile.totalPendingEuros,
                    updatedAt = currentTimeMillis(),
                    ownerId = profile.ownerId,
                    photo_url = pending?.get("photo_url") ?: (profile.photoUrl ?: ""),
                    username = pending?.get("username") ?: (profile.username ?: ""),
                    display_name = pending?.get("display_name") ?: (profile.displayName ?: ""),
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
            display_name = profile.displayName ?: "",
            custom_names = serializeCustomNames(profile.customNames),
        )
        try {
            remoteRepository.saveProfile(profile)
        } catch (e: Exception) {
            println("[OfflineFirstProfileRepo] saveProfile remote FAILED for ${profile.id}: ${e.message}")
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
            println("[OfflineFirstProfileRepo] deleteProfile remote FAILED for $profileId: ${e.message}")
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
        remoteRepository.linkGhostProfile(userEmail, userUid)
    }

    // ── Phase 2: Profile & Account Settings ─────────────────────────────────

    override suspend fun updateProfilePhoto(photoUrl: String): Result<String> {
        return try {
            println("[OfflineFirstProfileRepo] updateProfilePhoto called url='$photoUrl'")
            val ownProfile = findOwnProfile()
            if (ownProfile != null) {
                val pending = pendingLocalChanges[ownProfile.id]?.toMutableMap() ?: mutableMapOf()
                pending["photo_url"] = photoUrl
                pendingLocalChanges[ownProfile.id] = pending
                println("[OfflineFirstProfileRepo] updateProfilePhoto pending set for ${ownProfile.id}")

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
                    display_name = ownProfile.display_name,
                    custom_names = ownProfile.custom_names,
                )
                println("[OfflineFirstProfileRepo] updateProfilePhoto local cache updated")
            } else {
                println("[OfflineFirstProfileRepo] updateProfilePhoto: own profile not found!")
            }
            val result = remoteRepository.updateProfilePhoto(photoUrl)
            println("[OfflineFirstProfileRepo] updateProfilePhoto remote: success=${result.isSuccess} error=${result.exceptionOrNull()?.message}")
            if (result.isSuccess) {
                pendingLocalChanges.remove(ownProfile?.id)
                println("[OfflineFirstProfileRepo] updateProfilePhoto pending cleared")
            } else {
                pendingQueue.enqueue(
                    id = "photo_${currentTimeMillis()}",
                    entityType = "profile",
                    entityId = ownProfile?.id ?: "",
                    operation = "updatePhoto",
                    payload = photoUrl
                )
                println("[OfflineFirstProfileRepo] updateProfilePhoto enqueued to pending")
            }
            result.map { photoUrl }
        } catch (e: Exception) {
            println("[OfflineFirstProfileRepo] updateProfilePhoto exception: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun updateUsername(username: String): Result<Unit> {
        return try {
            println("[OfflineFirstProfileRepo] updateUsername called '$username'")
            val ownProfile = findOwnProfile()
            if (ownProfile != null) {
                val pending = pendingLocalChanges[ownProfile.id]?.toMutableMap() ?: mutableMapOf()
                pending["username"] = username
                pendingLocalChanges[ownProfile.id] = pending
                println("[OfflineFirstProfileRepo] updateUsername pending set for ${ownProfile.id}")

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
                    display_name = ownProfile.display_name,
                    custom_names = ownProfile.custom_names,
                )
                println("[OfflineFirstProfileRepo] updateUsername local cache updated")
            } else {
                println("[OfflineFirstProfileRepo] updateUsername: own profile not found!")
            }
            val result = remoteRepository.updateUsername(username)
            println("[OfflineFirstProfileRepo] updateUsername remote: success=${result.isSuccess} error=${result.exceptionOrNull()?.message}")
            if (result.isSuccess) {
                pendingLocalChanges.remove(ownProfile?.id)
                println("[OfflineFirstProfileRepo] updateUsername pending cleared")
            } else {
                pendingQueue.enqueue(
                    id = "username_${currentTimeMillis()}",
                    entityType = "profile",
                    entityId = ownProfile?.id ?: "",
                    operation = "updateUsername",
                    payload = username
                )
                println("[OfflineFirstProfileRepo] updateUsername enqueued to pending")
            }
            result
        } catch (e: Exception) {
            println("[OfflineFirstProfileRepo] updateUsername exception: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun updateDisplayName(displayName: String): Result<Unit> {
        return try {
            println("[OfflineFirstProfileRepo] updateDisplayName called '$displayName'")
            val ownProfile = findOwnProfile()
            if (ownProfile != null) {
                val pending = pendingLocalChanges[ownProfile.id]?.toMutableMap() ?: mutableMapOf()
                pending["display_name"] = displayName
                pendingLocalChanges[ownProfile.id] = pending
                println("[OfflineFirstProfileRepo] updateDisplayName pending set for ${ownProfile.id}")

                queries.upsert(
                    id = ownProfile.id,
                    name = ownProfile.name,
                    email = ownProfile.email,
                    isGhost = ownProfile.isGhost,
                    totalPendingEuros = ownProfile.totalPendingEuros,
                    updatedAt = currentTimeMillis(),
                    ownerId = ownProfile.ownerId,
                    photo_url = ownProfile.photo_url,
                    username = ownProfile.username,
                    display_name = displayName,
                    custom_names = ownProfile.custom_names,
                )
                println("[OfflineFirstProfileRepo] updateDisplayName local cache updated")
            } else {
                println("[OfflineFirstProfileRepo] updateDisplayName: own profile not found!")
            }
            val result = remoteRepository.updateDisplayName(displayName)
            println("[OfflineFirstProfileRepo] updateDisplayName remote: success=${result.isSuccess} error=${result.exceptionOrNull()?.message}")
            if (result.isSuccess) {
                pendingLocalChanges.remove(ownProfile?.id)
                println("[OfflineFirstProfileRepo] updateDisplayName pending cleared")
            } else {
                pendingQueue.enqueue(
                    id = "displayname_${currentTimeMillis()}",
                    entityType = "profile",
                    entityId = ownProfile?.id ?: "",
                    operation = "updateDisplayName",
                    payload = displayName
                )
                println("[OfflineFirstProfileRepo] updateDisplayName enqueued to pending")
            }
            result
        } catch (e: Exception) {
            println("[OfflineFirstProfileRepo] updateDisplayName exception: ${e.message}")
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
            println("[OfflineFirstProfileRepo] deleteProfilePhoto called")
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
                    display_name = ownProfile.display_name,
                    custom_names = ownProfile.custom_names,
                )
                println("[OfflineFirstProfileRepo] deleteProfilePhoto local cache updated")
            }
            val result = remoteRepository.deleteProfilePhoto()
            println("[OfflineFirstProfileRepo] deleteProfilePhoto remote: success=${result.isSuccess}")
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
            println("[OfflineFirstProfileRepo] deleteProfilePhoto exception: ${e.message}")
            Result.failure(e)
        }
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
            displayName = display_name.takeIf { it.isNotBlank() },
            customNames = parseCustomNames(custom_names),
        )
    }
}
