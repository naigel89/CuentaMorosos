package com.cuentamorosos.data.repository

import com.cuentamorosos.model.EventInvitation
import com.cuentamorosos.notifications.NotificationEvent
import kotlinx.coroutines.flow.Flow

interface InvitationRepository {
    /** Invitaciones pendientes recibidas por el usuario actual (por su email). */
    fun observePendingInvitations(): Flow<List<EventInvitation>>
    /** Crea una nueva invitación en Firestore. */
    suspend fun sendInvitation(invitation: EventInvitation)
    /** Acepta la invitación: añade el uid del usuario a memberIds del evento y actualiza el estado.
     * @param inviteeName Display name of the user accepting the invitation (for notifying the inviter). */
    suspend fun acceptInvitation(invitation: EventInvitation, inviteeName: String = "")
    /** Rechaza la invitación: actualiza el estado a rejected. */
    suspend fun rejectInvitation(invitationId: String)
    /** Listen on notifications/{uid}/invitation-accepted for acceptance confirmations. */
    fun observeInvitationAccepted(): Flow<NotificationEvent.InvitationAccepted>
    /** Write a calculation-completed doc per participant so they receive a cross-user notification.
     *  @param participantUid The Firebase Auth UID of the participant to notify.
     *  @param amountOwed The total unpaid debt for this participant in the event. */
    suspend fun sendCalculationNotification(eventId: String, eventName: String, participantUid: String, amountOwed: Double)
    /** Listen on notifications/{uid}/calculation-completed for incoming calculation notifications. */
    fun observeCalculationCompleted(): Flow<NotificationEvent.CalculationCompleted>
}
