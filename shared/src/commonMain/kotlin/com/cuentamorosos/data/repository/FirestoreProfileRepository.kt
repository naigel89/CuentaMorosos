package com.cuentamorosos.data.repository

import com.cuentamorosos.currentTimeMillis
import com.cuentamorosos.model.ProfileItem
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.EmailAuthProvider
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.delay
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
        // NOTE: intentionally no ownerId filter — users need to see other participants'
        // profiles in shared events. The UI (ProfilesScreen) already separates own vs others
        // by matching currentUid, and Firestore rules allow any authenticated read.
        return collection.snapshots
            .map { snapshot ->
                val items = snapshot.documents
                    .mapNotNull { doc ->
                        val item = doc.toProfileItem()
                        if (item == null) println("[FirestoreProfileRepo] Failed to parse doc ${doc.id}")
                        item
                    }
                    .sortedBy { it.name }
                println("[FirestoreProfileRepo] Snapshot emitted: ${items.size} profiles")
                items.forEach { p ->
                    println("[FirestoreProfileRepo]   id=${p.id} name='${p.name}' username='${p.username}' isGhost=${p.isGhost} linkedEmail=${p.linkedEmail}")
                }
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
        println("[FirestoreProfileRepo] linkGhostProfile: email=$userEmail, uid=$userUid")

        // 1. Query ghosts by linkedEmail (isGhost filter done client-side
        //    because KMP Firebase where-chaining may not support two conditions)
        val ghostSnapshot = collection
            .where { "linkedEmail" equalTo userEmail }
            .get()

        val ghosts = ghostSnapshot.documents.mapNotNull { it.toProfileItem() }
            .filter { it.isGhost }

        if (ghosts.isEmpty()) {
            println("[FirestoreProfileRepo] linkGhostProfile: no ghosts found for $userEmail — no-op")
            return
        }

        println("[FirestoreProfileRepo] linkGhostProfile: found ${ghosts.size} ghost(s): ${ghosts.map { it.id }}")

        // 2. Resolve all event IDs the current user can access
        val uid = auth.currentUser?.uid ?: return
        val eventsCollection = db.collection("events")

        val ownerSnapshot = eventsCollection.where { "ownerId" equalTo uid }.get()
        val memberSnapshot = eventsCollection.where { "memberIds" contains uid }.get()
        val participantSnapshot = eventsCollection.where { "participantIds" contains uid }.get()

        val eventIds = (ownerSnapshot.documents + memberSnapshot.documents + participantSnapshot.documents)
            .map { it.id }
            .distinct()

        println("[FirestoreProfileRepo] linkGhostProfile: accessible events=${eventIds.size}")

        // 3. Collect all debts/expenses/event documents that reference ghost IDs
        data class DocOp(
            val ref: dev.gitlive.firebase.firestore.DocumentReference,
            val type: String, // "debt", "expense", "event"
        )

        repeat(3) { attempt ->
            try {
                db.runTransaction {
                    for (ghost in ghosts) {
                        val ghostId = ghost.id

                        // ── Debts ───────────────────────────────────────────────
                        for (eventId in eventIds) {
                            val debtsCollection = eventsCollection
                                .document(eventId)
                                .collection("debts")

                            // Query debts where ghostId is debtor or creditor
                            val asDebtor = try {
                                debtsCollection.where { "profileId" equalTo ghostId }.get()
                            } catch (e: Exception) { null }
                            val asCreditor = try {
                                debtsCollection.where { "creditorId" equalTo ghostId }.get()
                            } catch (e: Exception) { null }

                            val debtDocs = (asDebtor?.documents.orEmpty() +
                                asCreditor?.documents.orEmpty())
                                .associateBy { it.id }
                                .values

                            for (debtDoc in debtDocs) {
                                val ref = debtsCollection.document(debtDoc.id)
                                // Re-read within transaction for consistency
                                val current = get(ref)
                                val data = current.getRawData() ?: continue
                                val profileId = data["profileId"] as? String ?: ""
                                val creditorId = data["creditorId"] as? String

                                val updates = mutableMapOf<String, Any?>()
                                if (profileId == ghostId) {
                                    updates["profileId"] = userUid
                                }
                                if (creditorId == ghostId) {
                                    updates["creditorId"] = userUid
                                }
                                if (updates.isNotEmpty()) {
                                    println("[FirestoreProfileRepo] linkGhostProfile: updating debt ${debtDoc.id} (event=$eventId)")
                                    update(ref, updates)
                                }
                            }
                        }

                        // ── Expenses ─────────────────────────────────────────────
                        for (eventId in eventIds) {
                            val expensesCollection = eventsCollection
                                .document(eventId)
                                .collection("expenses")

                            val allExpenses = try {
                                expensesCollection.get()
                            } catch (e: Exception) { null }

                            for (expDoc in allExpenses?.documents.orEmpty()) {
                                val data = expDoc.getRawData() ?: continue
                                var needsUpdate = false
                                val updates = mutableMapOf<String, Any?>()

                                // assignedProfileIds
                                @Suppress("UNCHECKED_CAST")
                                val assignedIds = (data["assignedProfileIds"] as? List<String>) ?: emptyList()
                                if (assignedIds.contains(ghostId)) {
                                    updates["assignedProfileIds"] = assignedIds.map {
                                        if (it == ghostId) userUid else it
                                    }
                                    needsUpdate = true
                                }

                                // profileWeights
                                @Suppress("UNCHECKED_CAST")
                                val weights = (data["profileWeights"] as? Map<String, *>)?.toMutableMap()
                                    ?: mutableMapOf()
                                if (weights.containsKey(ghostId)) {
                                    val newWeights = weights.mapKeys { (k, _) ->
                                        if (k == ghostId) userUid else k
                                    }
                                    updates["profileWeights"] = newWeights
                                    needsUpdate = true
                                }

                                // paidByProfileId
                                val paidBy = data["paidByProfileId"] as? String
                                if (paidBy == ghostId) {
                                    updates["paidByProfileId"] = userUid
                                    needsUpdate = true
                                }

                                // debtorIds
                                @Suppress("UNCHECKED_CAST")
                                val debtorIds = (data["debtorIds"] as? List<String>) ?: emptyList()
                                if (debtorIds.contains(ghostId)) {
                                    updates["debtorIds"] = debtorIds.map {
                                        if (it == ghostId) userUid else it
                                    }
                                    needsUpdate = true
                                }

                                // payerContributions
                                @Suppress("UNCHECKED_CAST")
                                val payerContribs = (data["payerContributions"] as? Map<String, *>)?.toMutableMap()
                                    ?: mutableMapOf()
                                if (payerContribs.containsKey(ghostId)) {
                                    val newContribs = payerContribs.mapKeys { (k, _) ->
                                        if (k == ghostId) userUid else k
                                    }
                                    updates["payerContributions"] = newContribs
                                    needsUpdate = true
                                }

                                if (needsUpdate) {
                                    val ref = expensesCollection.document(expDoc.id)
                                    println("[FirestoreProfileRepo] linkGhostProfile: updating expense ${expDoc.id} (event=$eventId)")
                                    update(ref, updates)
                                }
                            }
                        }

                        // ── Events (participants, memberIds, ownerId) ────────────
                        for (eventId in eventIds) {
                            val eventRef = eventsCollection.document(eventId)
                            val eventDoc = try {
                                get(eventRef)
                            } catch (e: Exception) { null }
                            val data = eventDoc?.getRawData() ?: continue

                            val updates = mutableMapOf<String, Any?>()

                            // participants array
                            @Suppress("UNCHECKED_CAST")
                            val participants = (data["participants"] as? List<Map<String, Any?>>) ?: emptyList()
                            val hasGhostInParticipants = participants.any {
                                it["profileId"] == ghostId
                            }
                            if (hasGhostInParticipants) {
                                updates["participants"] = participants.map { p ->
                                    if (p["profileId"] == ghostId) p + ("profileId" to userUid) else p
                                }
                            }

                            // participantIds
                            @Suppress("UNCHECKED_CAST")
                            val participantIds = (data["participantIds"] as? List<String>) ?: emptyList()
                            if (participantIds.contains(ghostId)) {
                                updates["participantIds"] = participantIds.map {
                                    if (it == ghostId) userUid else it
                                }
                            }

                            // memberIds
                            @Suppress("UNCHECKED_CAST")
                            val memberIds = (data["memberIds"] as? List<String>) ?: emptyList()
                            if (memberIds.contains(ghostId)) {
                                updates["memberIds"] = memberIds.map {
                                    if (it == ghostId) userUid else it
                                }
                            }

                            // ownerId
                            val ownerId = data["ownerId"] as? String
                            if (ownerId == ghostId) {
                                updates["ownerId"] = userUid
                            }

                            if (updates.isNotEmpty()) {
                                println("[FirestoreProfileRepo] linkGhostProfile: updating event $eventId")
                                update(eventRef, updates)
                            }
                        }

                        // ── Delete ghost profile ─────────────────────────────────
                        val ghostRef = collection.document(ghostId)
                        println("[FirestoreProfileRepo] linkGhostProfile: deleting ghost $ghostId")
                        delete(ghostRef)
                    }
                }
                // If we get here, transaction succeeded
                println("[FirestoreProfileRepo] linkGhostProfile: merge completed successfully")
                return
            } catch (e: Exception) {
                println("[FirestoreProfileRepo] linkGhostProfile attempt ${attempt + 1}/3 FAILED: ${e.message}")
                if (attempt == 2) {
                    println("[FirestoreProfileRepo] linkGhostProfile: all retries exhausted")
                    throw e
                }
                // Exponential backoff: 1s, 2s, 4s
                delay((1000L * (1 shl attempt)))
            }
        }
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
            println("[FirestoreProfileRepo] updateUsername: uid=$uid, newUsername='$username' → updating Firestore document profiles/$uid")
            collection.document(uid).update("username" to username, "updatedAt" to currentTimeMillis())
            println("[FirestoreProfileRepo] updateUsername: Firestore update SUCCESS for uid=$uid")
            Result.success(Unit)
        } catch (e: Exception) {
            println("[FirestoreProfileRepo] updateUsername: Firestore update FAILED — ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun updateDisplayName(displayName: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
            println("[FirestoreProfileRepo] updateDisplayName: uid=$uid, newName='$displayName' → updating Firestore document profiles/$uid")
            collection.document(uid).update("name" to displayName, "updatedAt" to currentTimeMillis())
            println("[FirestoreProfileRepo] updateDisplayName: Firestore update SUCCESS for uid=$uid")
            Result.success(Unit)
        } catch (e: Exception) {
            println("[FirestoreProfileRepo] updateDisplayName: Firestore update FAILED — ${e.message}")
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

    // ── GPS-REQ-006: One-time orphan cleanup ─────────────────────────────────

    /**
     * Idempotent orphan cleanup: scans all debts, expenses, and events for
     * references to non-existent profile UUIDs and removes them.
     *
     * Call once after first post-upgrade launch, guarded by
     * CuentaMorososLocalStore.isOrphanCleanupDone() / markOrphanCleanupDone().
     */
    override suspend fun cleanupOrphans() {
        println("[FirestoreProfileRepo] cleanupOrphans: starting...")

        // 1. Fetch all known profile IDs
        val allProfiles = try {
            collection.get().documents.mapNotNull { it.get<String>("id") }
        } catch (e: Exception) {
            println("[FirestoreProfileRepo] cleanupOrphans: failed to fetch profiles: ${e.message}")
            return
        }
        val knownIds = allProfiles.toSet()
        println("[FirestoreProfileRepo] cleanupOrphans: ${knownIds.size} known profiles")

        // 2. Resolve all event IDs the current user can access
        val uid = auth.currentUser?.uid ?: return
        val eventsCollection = db.collection("events")

        val ownerSnapshot = eventsCollection.where { "ownerId" equalTo uid }.get()
        val memberSnapshot = eventsCollection.where { "memberIds" contains uid }.get()
        val participantSnapshot = eventsCollection.where { "participantIds" contains uid }.get()

        val eventIds = (ownerSnapshot.documents + memberSnapshot.documents + participantSnapshot.documents)
            .map { it.id }
            .distinct()

        println("[FirestoreProfileRepo] cleanupOrphans: scanning ${eventIds.size} events")

        for (eventId in eventIds) {
            try {
                // ── Debts ───────────────────────────────────────────────────
                val debtsCollection = eventsCollection.document(eventId).collection("debts")
                val debts = try {
                    debtsCollection.get().documents
                } catch (e: Exception) {
                    println("[FirestoreProfileRepo] cleanupOrphans: failed to query debts for $eventId: ${e.message}")
                    emptyList()
                }

                for (debtDoc in debts) {
                    val data = debtDoc.getRawData() ?: continue
                    val profileId = data["profileId"] as? String ?: ""
                    val creditorId = data["creditorId"] as? String

                    val isOrphanProfile = profileId.isNotBlank() && profileId !in knownIds
                    val isOrphanCreditor = creditorId != null && creditorId.isNotBlank() && creditorId !in knownIds

                    if (isOrphanProfile || isOrphanCreditor) {
                        println("[FirestoreProfileRepo] cleanupOrphans: deleting orphan debt ${debtDoc.id} (event=$eventId, profile=$profileId, creditor=$creditorId)")
                        debtsCollection.document(debtDoc.id).delete()
                    }
                }

                // ── Expenses ────────────────────────────────────────────────
                val expensesCollection = eventsCollection.document(eventId).collection("expenses")
                val expenses = try {
                    expensesCollection.get().documents
                } catch (e: Exception) {
                    println("[FirestoreProfileRepo] cleanupOrphans: failed to query expenses for $eventId: ${e.message}")
                    emptyList()
                }

                for (expDoc in expenses) {
                    val data = expDoc.getRawData() ?: continue
                    val updates = mutableMapOf<String, Any?>()
                    var needsUpdate = false

                    // assignedProfileIds
                    @Suppress("UNCHECKED_CAST")
                    val assignedIds = (data["assignedProfileIds"] as? List<String>) ?: emptyList()
                    val orphansInAssigned = assignedIds.filter { it !in knownIds }
                    if (orphansInAssigned.isNotEmpty()) {
                        updates["assignedProfileIds"] = assignedIds - orphansInAssigned.toSet()
                        needsUpdate = true
                        println("[FirestoreProfileRepo] cleanupOrphans: removing orphan ids ${orphansInAssigned} from expense ${expDoc.id} (assigned)")
                    }

                    // profileWeights
                    @Suppress("UNCHECKED_CAST")
                    val weights = (data["profileWeights"] as? Map<String, *>)?.toMutableMap()
                        ?: mutableMapOf()
                    val orphanWeightKeys = weights.keys.filter { it !in knownIds }
                    if (orphanWeightKeys.isNotEmpty()) {
                        orphanWeightKeys.forEach { weights.remove(it) }
                        updates["profileWeights"] = weights
                        needsUpdate = true
                        println("[FirestoreProfileRepo] cleanupOrphans: removing orphan weights $orphanWeightKeys from expense ${expDoc.id}")
                    }

                    // paidByProfileId
                    val paidBy = data["paidByProfileId"] as? String
                    if (paidBy != null && paidBy.isNotBlank() && paidBy !in knownIds) {
                        updates["paidByProfileId"] = ""
                        needsUpdate = true
                        println("[FirestoreProfileRepo] cleanupOrphans: clearing orphan paidBy=$paidBy from expense ${expDoc.id}")
                    }

                    // debtorIds
                    @Suppress("UNCHECKED_CAST")
                    val debtorIds = (data["debtorIds"] as? List<String>) ?: emptyList()
                    val orphanDebtors = debtorIds.filter { it !in knownIds }
                    if (orphanDebtors.isNotEmpty()) {
                        updates["debtorIds"] = debtorIds - orphanDebtors.toSet()
                        needsUpdate = true
                        println("[FirestoreProfileRepo] cleanupOrphans: removing orphan debtors $orphanDebtors from expense ${expDoc.id}")
                    }

                    // payerContributions
                    @Suppress("UNCHECKED_CAST")
                    val payerContribs = (data["payerContributions"] as? Map<String, *>)?.toMutableMap()
                        ?: mutableMapOf()
                    val orphanPayerKeys = payerContribs.keys.filter { it !in knownIds }
                    if (orphanPayerKeys.isNotEmpty()) {
                        orphanPayerKeys.forEach { payerContribs.remove(it) }
                        updates["payerContributions"] = payerContribs
                        needsUpdate = true
                        println("[FirestoreProfileRepo] cleanupOrphans: removing orphan payer contributions $orphanPayerKeys from expense ${expDoc.id}")
                    }

                    if (needsUpdate) {
                        expensesCollection.document(expDoc.id).update(updates)
                    }
                }

                // ── Events ──────────────────────────────────────────────────
                val eventRef = eventsCollection.document(eventId)
                val eventDoc = try { eventRef.get() } catch (e: Exception) { null }
                val eventData = eventDoc?.getRawData() ?: continue
                val eventUpdates = mutableMapOf<String, Any?>()

                // participantIds
                @Suppress("UNCHECKED_CAST")
                val participantIds = (eventData["participantIds"] as? List<String>) ?: emptyList()
                val orphanParticipantIds = participantIds.filter { it !in knownIds }
                if (orphanParticipantIds.isNotEmpty()) {
                    eventUpdates["participantIds"] = participantIds - orphanParticipantIds.toSet()
                    println("[FirestoreProfileRepo] cleanupOrphans: removing orphan participantIds $orphanParticipantIds from event $eventId")
                }

                // participants array
                @Suppress("UNCHECKED_CAST")
                val participants = (eventData["participants"] as? List<Map<String, Any?>>)
                if (participants != null && orphanParticipantIds.isNotEmpty()) {
                    val cleanedParticipants = participants.filter { p ->
                        val pid = p["profileId"] as? String
                        pid == null || pid in knownIds
                    }
                    eventUpdates["participants"] = cleanedParticipants
                }

                // memberIds
                @Suppress("UNCHECKED_CAST")
                val memberIds = (eventData["memberIds"] as? List<String>) ?: emptyList()
                val orphanMemberIds = memberIds.filter { it !in knownIds }
                if (orphanMemberIds.isNotEmpty()) {
                    eventUpdates["memberIds"] = memberIds - orphanMemberIds.toSet()
                    println("[FirestoreProfileRepo] cleanupOrphans: removing orphan memberIds $orphanMemberIds from event $eventId")
                }

                if (eventUpdates.isNotEmpty()) {
                    eventRef.update(eventUpdates)
                }
            } catch (e: Exception) {
                println("[FirestoreProfileRepo] cleanupOrphans: error processing event $eventId: ${e.message}")
            }
        }

        println("[FirestoreProfileRepo] cleanupOrphans: complete")
    }

    // ── Phase 3: Username Search ────────────────────────────────────────────

    override suspend fun searchByUsername(prefix: String): List<ProfileItem> {
        val lower = prefix.lowercase().trim()
        if (lower.isEmpty()) return emptyList()
        val end = lower + "\uf8ff"
        return collection
            .where { "username" greaterThanOrEqualTo lower }
            .where { "username" lessThanOrEqualTo end }
            .limit(20)
            .get()
            .documents
            .mapNotNull { it.toProfileItem() }
            .filter { !it.isGhost && it.linkedEmail != null }
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
            ProfileItem(
                id = id,
                name = name,
                totalPendingEuros = totalPendingEuros,
                isGhost = isGhost,
                linkedEmail = linkedEmail,
                ownerId = ownerId,
                // Ghost profiles force null for photoUrl, username
                photoUrl = photoUrl,
                username = username,
                customNames = emptyMap(), // loaded separately via customNames subcollection
            )
        } catch (e: Exception) {
            println("[FirestoreProfileRepo] FATAL: toProfileItem exception for doc ${this.id}: ${e::class.simpleName}: ${e.message}")
            null
        }
    }
}
