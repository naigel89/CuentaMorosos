package com.cuentamorosos.data.repository

import com.cuentamorosos.model.EventAction
import com.cuentamorosos.model.EventInvitation
import com.cuentamorosos.model.EventRole
import com.cuentamorosos.model.InvitationStatus
import com.cuentamorosos.model.PermissionEngine
import com.cuentamorosos.notifications.NotificationEvent
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import com.cuentamorosos.data.LogSanitizer

class FirestoreInvitationRepository(
    private val profileRepository: ProfileRepository? = null,
) : InvitationRepository {

    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val invitationsCollection = db.collection("invitations")

    override fun observePendingInvitations(): Flow<List<EventInvitation>> {
        val email = auth.currentUser?.email?.trim()?.lowercase() ?: return flowOf(emptyList())
        return invitationsCollection
            .where {
                "invitedEmail" equalTo email
                "status" equalTo InvitationStatus.PENDING
            }
            .snapshots
            .map { snapshot ->
                snapshot.documents
                    .mapNotNull { it.toInvitation() }
                    .filter { it.invitedEmail == email } // Safety net: local cache may return docs that don't match the where filter
            }
    }

    override suspend fun sendInvitation(invitation: EventInvitation) {
        // Defense-in-depth: verify caller has ManageParticipants permission
        val eventDoc = db.collection("events").document(invitation.eventId).get()
        val data = eventDoc.getRawData()
        if (data != null) {
            val senderId = invitation.invitedByUid
            val ownerId = data["ownerId"] as? String ?: ""
            @Suppress("UNCHECKED_CAST")
            val rawParticipants = (data["participants"] as? List<Map<String, Any?>>) ?: emptyList()
            val senderRole = if (senderId == ownerId) {
                EventRole.OWNER
            } else {
                val participant = rawParticipants.find { it["profileId"] == senderId }
                val roleName = participant?.get("role") as? String
                if (roleName != null) {
                    try { EventRole.valueOf(roleName) } catch (_: Exception) { EventRole.READER }
                } else {
                    EventRole.READER
                }
            }
            if (!canSendInvitation(senderRole)) {
                return
            }
        }
        val finalEmail = invitation.invitedEmail.trim().lowercase()
        invitationsCollection.document(invitation.id)
            .set(invitation.copy(invitedEmail = finalEmail).toFirestoreMap())
    }

    override suspend fun acceptInvitation(invitation: EventInvitation, inviteeName: String) {
        runCatching {
            val uid = auth.currentUser?.uid ?: return

            // PRE-INSERT: reconcile ghost profiles first (GPS-REQ-003)
            profileRepository?.let { repo ->
                auth.currentUser?.email?.trim()?.lowercase()?.let { email ->
                    runCatching { repo.linkGhostProfile(email, uid) }
                }
            }

            val eventsCollection = db.collection("events")
            val doc = eventsCollection.document(invitation.eventId).get()

            val eventData = doc.getRawData()
            val rawMemberIds = eventData?.get("memberIds")
            val current = (rawMemberIds as? List<*>)?.filterIsInstance<String>() ?: emptyList()

            @Suppress("UNCHECKED_CAST")
            val rawParticipants = eventData?.get("participants")
            val currentParticipants = (rawParticipants as? List<Map<String, Any?>>) ?: emptyList()

            if (!current.contains(uid)) {
                val newParticipant = createAcceptanceParticipant(uid)
                val newParticipants = currentParticipants + newParticipant
                eventsCollection.document(invitation.eventId)
                    .update(
                        "memberIds" to (current + uid),
                        "participants" to newParticipants,
                        "participantIds" to newParticipants.map { it["profileId"] },
                        "contributorIds" to newParticipants
                            .filter { it["role"] == "CONTRIBUTOR" }
                            .map { it["profileId"] },
                    )
            }

            invitationsCollection.document(invitation.id)
                .update("status" to InvitationStatus.ACCEPTED)

            // Fire-and-forget: notify the inviter — acceptance succeeds even if this fails
            runCatching {
                db.collection("notifications")
                    .document(invitation.invitedByUid)
                    .collection("invitation-accepted")
                    .document(invitation.id)
                    .set(mapOf(
                        "eventId" to invitation.eventId,
                        "eventName" to invitation.eventName,
                        "inviteeName" to inviteeName,
                    ))
            }
        }.onFailure { e ->
            LogSanitizer.log("FirestoreInvitationRepo", "acceptInvitation failed: ${e.message}")
        }
    }

    override suspend fun rejectInvitation(invitationId: String) {
        invitationsCollection.document(invitationId)
            .update("status" to InvitationStatus.REJECTED)
    }

    override suspend fun sendCalculationNotification(
        eventId: String,
        eventName: String,
        participantUid: String,
        amountOwed: Double,
    ) {
        runCatching {
            db.collection("notifications")
                .document(participantUid)
                .collection("calculation-completed")
                .document(eventId)
                .set(mapOf(
                    "eventId" to eventId,
                    "eventName" to eventName,
                    "amountOwed" to amountOwed,
                ))
        }
    }

    override fun observeCalculationCompleted(): Flow<NotificationEvent.CalculationCompleted> {
        val uid = auth.currentUser?.uid ?: return flowOf()
        return db.collection("notifications")
            .document(uid)
            .collection("calculation-completed")
            .snapshots
            .flatMapConcat { snapshot ->
                val events = snapshot.documents.mapNotNull { doc ->
                    val data = doc.getRawData() ?: return@mapNotNull null
                    val eventId = data["eventId"] as? String ?: return@mapNotNull null
                    val eventName = data["eventName"] as? String ?: ""
                    val amountOwed = (data["amountOwed"] as? Number)?.toDouble() ?: 0.0
                    NotificationEvent.CalculationCompleted(
                        eventId = eventId,
                        eventName = eventName,
                        amountOwed = amountOwed,
                    )
                }
                flowOf(*events.toTypedArray())
            }
    }

    override fun observeInvitationAccepted(): Flow<NotificationEvent.InvitationAccepted> {
        val uid = auth.currentUser?.uid ?: return flowOf()
        return db.collection("notifications")
            .document(uid)
            .collection("invitation-accepted")
            .snapshots
            .flatMapConcat { snapshot ->
                val events = snapshot.documents.mapNotNull { doc ->
                    val data = doc.getRawData() ?: return@mapNotNull null
                    val eventId = data["eventId"] as? String ?: return@mapNotNull null
                    val eventName = data["eventName"] as? String ?: ""
                    val inviteeName = data["inviteeName"] as? String ?: ""
                    NotificationEvent.InvitationAccepted(
                        eventId = eventId,
                        eventName = eventName,
                        inviteeName = inviteeName,
                    )
                }
                flowOf(*events.toTypedArray())
            }
    }

    private fun dev.gitlive.firebase.firestore.DocumentSnapshot.toInvitation(): EventInvitation? {
        return try {
            val data = this.getRawData() ?: return null
            data.toEventInvitation()
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Serializes an [EventInvitation] to a Firestore-compatible map.
 */
internal fun EventInvitation.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "eventId" to eventId,
    "eventName" to eventName,
    "invitedByUid" to invitedByUid,
    "invitedByEmail" to invitedByEmail,
    "invitedEmail" to invitedEmail,
    "status" to status,
    "createdAtMillis" to createdAtMillis,
    "expiresAtMillis" to expiresAtMillis,
    "invitedByName" to invitedByName,
    "invitedByPhotoUrl" to invitedByPhotoUrl,
)

/**
 * Deserializes a Firestore document map into an [EventInvitation].
 *
 * [invitedByName] defaults to [invitedByEmail] when absent (legacy fallback).
 * [invitedByPhotoUrl] defaults to null when absent.
 */
internal fun Map<String, Any?>.toEventInvitation(): EventInvitation? {
    return try {
        EventInvitation(
            id = this["id"] as? String ?: return null,
            eventId = this["eventId"] as? String ?: return null,
            eventName = this["eventName"] as? String ?: "",
            invitedByUid = this["invitedByUid"] as? String ?: return null,
            invitedByEmail = this["invitedByEmail"] as? String ?: "",
            invitedEmail = this["invitedEmail"] as? String ?: return null,
            status = this["status"] as? String ?: InvitationStatus.PENDING,
            createdAtMillis = (this["createdAtMillis"] as? Number)?.toLong() ?: com.cuentamorosos.currentTimeMillis(),
            expiresAtMillis = (this["expiresAtMillis"] as? Number)?.toLong()
                ?: (com.cuentamorosos.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000),
            invitedByName = this["invitedByName"] as? String
                ?: (this["invitedByEmail"] as? String ?: ""),
            invitedByPhotoUrl = this["invitedByPhotoUrl"] as? String,
        )
    } catch (e: Exception) {
        null
    }
}

/**
 * Creates the participant map for a newly accepted invitation.
 * Assigns [EventRole.READER] — the user can be promoted later.
 */
internal fun createAcceptanceParticipant(uid: String): Map<String, Any?> = mapOf(
    "profileId" to uid,
    "role" to EventRole.READER.name,
    "joinedAtMillis" to com.cuentamorosos.currentTimeMillis(),
)

/**
 * Returns `true` if a user with the given [role] can send invitations.
 * Only OWNER may manage participants; CONTRIBUTOR and READER may not.
 */
internal fun canSendInvitation(role: EventRole): Boolean =
    PermissionEngine.hasPermission(role, EventAction.ManageParticipants)
