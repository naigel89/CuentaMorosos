package com.cuentamorosos.data.repository

import com.cuentamorosos.model.EventInvitation
import com.cuentamorosos.model.InvitationStatus
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class FirestoreInvitationRepository : InvitationRepository {

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
                snapshot.documents.mapNotNull { it.toInvitation() }
            }
    }

    override suspend fun sendInvitation(invitation: EventInvitation) {
        invitationsCollection.document(invitation.id)
            .set(invitation.copy(invitedEmail = invitation.invitedEmail.trim().lowercase()).toMap())
    }

    override suspend fun acceptInvitation(invitation: EventInvitation) {
        val uid = auth.currentUser?.uid ?: return

        val eventsCollection = db.collection("events")
        val doc = eventsCollection.document(invitation.eventId).get()
        val current = (doc.get("memberIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        if (!current.contains(uid)) {
            eventsCollection.document(invitation.eventId)
                .update("memberIds" to (current + uid))
        }

        invitationsCollection.document(invitation.id)
            .update("status" to InvitationStatus.ACCEPTED)
    }

    override suspend fun rejectInvitation(invitationId: String) {
        invitationsCollection.document(invitationId)
            .update("status" to InvitationStatus.REJECTED)
    }

    private fun EventInvitation.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "eventId" to eventId,
        "eventName" to eventName,
        "invitedByUid" to invitedByUid,
        "invitedByEmail" to invitedByEmail,
        "invitedEmail" to invitedEmail,
        "status" to status,
        "createdAtMillis" to createdAtMillis,
        "expiresAtMillis" to expiresAtMillis,
    )

    private fun dev.gitlive.firebase.firestore.DocumentSnapshot.toInvitation(): EventInvitation? {
        return try {
            val data = data<Map<String, Any?>>()
            EventInvitation(
                id = data["id"] as? String ?: return null,
                eventId = data["eventId"] as? String ?: return null,
                eventName = data["eventName"] as? String ?: "",
                invitedByUid = data["invitedByUid"] as? String ?: return null,
                invitedByEmail = data["invitedByEmail"] as? String ?: "",
                invitedEmail = data["invitedEmail"] as? String ?: return null,
                status = data["status"] as? String ?: InvitationStatus.PENDING,
                createdAtMillis = (data["createdAtMillis"] as? Number)?.toLong() ?: com.cuentamorosos.currentTimeMillis(),
                expiresAtMillis = (data["expiresAtMillis"] as? Number)?.toLong()
                    ?: (com.cuentamorosos.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000),
            )
        } catch (e: Exception) {
            null
        }
    }
}
