package com.cuentamorosos.data.repository

import com.cuentamorosos.model.EventInvitation
import kotlinx.coroutines.flow.Flow

interface InvitationRepository {
    /** Invitaciones pendientes recibidas por el usuario actual (por su email). */
    fun observePendingInvitations(): Flow<List<EventInvitation>>
    /** Crea una nueva invitación en Firestore. */
    suspend fun sendInvitation(invitation: EventInvitation)
    /** Acepta la invitación: añade el uid del usuario a memberIds del evento y actualiza el estado. */
    suspend fun acceptInvitation(invitation: EventInvitation)
    /** Rechaza la invitación: actualiza el estado a rejected. */
    suspend fun rejectInvitation(invitationId: String)
}
