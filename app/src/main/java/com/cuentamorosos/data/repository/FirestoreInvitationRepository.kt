package com.cuentamorosos.data.repository

import com.cuentamorosos.model.EventInvitation
import com.cuentamorosos.model.InvitationStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreInvitationRepository : InvitationRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val invitationsCollection = db.collection("invitations")

    override fun observePendingInvitations(): Flow<List<EventInvitation>> = callbackFlow {
        val email = auth.currentUser?.email?.trim()?.lowercase() ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = invitationsCollection
            .whereEqualTo("invitedEmail", email)
            .whereEqualTo("status", InvitationStatus.PENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val invitations = snapshot?.documents?.mapNotNull { it.toInvitation() } ?: emptyList()
                trySend(invitations)
            }

        awaitClose { registration.remove() }
    }

    override suspend fun sendInvitation(invitation: EventInvitation) {
        invitationsCollection.document(invitation.id)
            .set(invitation.copy(invitedEmail = invitation.invitedEmail.trim().lowercase()).toMap())
            .await()
    }

    override suspend fun acceptInvitation(invitation: EventInvitation) {
        val uid = auth.currentUser?.uid ?: return

        // Añadir uid a memberIds del evento
        db.collection("events").document(invitation.eventId)
            .update("memberIds", FieldValue.arrayUnion(uid))
            .await()

        // Marcar invitación como aceptada
        invitationsCollection.document(invitation.id)
            .update("status", InvitationStatus.ACCEPTED)
            .await()
    }

    override suspend fun rejectInvitation(invitationId: String) {
        invitationsCollection.document(invitationId)
            .update("status", InvitationStatus.REJECTED)
            .await()
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

    private fun com.google.firebase.firestore.DocumentSnapshot.toInvitation(): EventInvitation? {
        val data = this.data ?: return null
        return EventInvitation(
            id = data["id"] as? String ?: return null,
            eventId = data["eventId"] as? String ?: return null,
            eventName = data["eventName"] as? String ?: "",
            invitedByUid = data["invitedByUid"] as? String ?: return null,
            invitedByEmail = data["invitedByEmail"] as? String ?: "",
            invitedEmail = data["invitedEmail"] as? String ?: return null,
            status = data["status"] as? String ?: InvitationStatus.PENDING,
            createdAtMillis = (data["createdAtMillis"] as? Long) ?: System.currentTimeMillis(),
            expiresAtMillis = (data["expiresAtMillis"] as? Long)
                ?: (System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000),
        )
    }
}
