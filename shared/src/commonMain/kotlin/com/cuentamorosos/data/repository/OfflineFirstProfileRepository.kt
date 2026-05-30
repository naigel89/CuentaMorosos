package com.cuentamorosos.data.repository

import com.cuentamorosos.currentTimeMillis
import com.cuentamorosos.data.NetworkMonitor
import com.cuentamorosos.data.PendingOperationQueue
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
import kotlinx.coroutines.withTimeoutOrNull

class OfflineFirstProfileRepository(
    private val remoteRepository: ProfileRepository,
    private val database: CuentaMorososDatabase,
    private val networkMonitor: NetworkMonitor,
    private val syncScope: CoroutineScope,
    private val pendingQueue: PendingOperationQueue
) : ProfileRepository {

    private val queries = database.cachedProfileQueries
    private var syncJob: Job? = null
    private var started = false

    private val pendingLocalChanges = mutableMapOf<String, Map<String, String>>()

    fun startSync() {
        if (started) return
        started = true
        networkMonitor.isOnline
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
                    val firstEmission = withTimeoutOrNull(15_000) {
                        remoteRepository.observeProfiles().first()
                    }
                    if (firstEmission != null) {
                        println("[OfflineFirstProfileRepo] First emission received: ${firstEmission.size} profiles")
                        upsertProfiles(firstEmission)
                    } else {
                        println("[OfflineFirstProfileRepo] Sync timeout after 15s, retrying with backoff")
                    }
                    remoteRepository.observeProfiles().collect { remoteProfiles ->
                        println("[OfflineFirstProfileRepo] Sync update: ${remoteProfiles.size} profiles")
                        upsertProfiles(remoteProfiles)
                    }
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
                    icon = profile.icon,
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
            icon = profile.icon,
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
                    icon = ownProfile.icon,
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
                    icon = ownProfile.icon,
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
                    icon = ownProfile.icon,
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
                    icon = ownProfile.icon,
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
            "${key}=${value}"
        }
    }

    private fun parseCustomNames(json: String): Map<String, String> {
        if (json.isBlank()) return emptyMap()
        return json.split("|").mapNotNull { part ->
            val eq = part.indexOf('=')
            if (eq > 0) part.substring(0, eq) to part.substring(eq + 1) else null
        }.toMap()
    }

    // ── Mapping Helpers ─────────────────────────────────────────────────────

    private fun com.cuentamorosos.db.CachedProfile.toProfileItem(): ProfileItem {
        val isGhost = isGhost == 1L
        return ProfileItem(
            id = id,
            name = name,
            icon = icon,
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
