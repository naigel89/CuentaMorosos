package com.cuentamorosos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuentamorosos.data.repository.InvitationRepository
import com.cuentamorosos.model.EventInvitation
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InvitationsViewModel(
    private val invitationRepository: InvitationRepository,
    /** Called when a new invitation arrives so the platform can post a notification. */
    private val onNewInvitation: ((eventName: String, invitedByEmail: String) -> Unit)? = null,
) : ViewModel() {

    // IDs de invitaciones ya notificadas en esta sesión para no repetir la notificación
    private val notifiedIds = mutableSetOf<String>()

    val pendingInvitations: StateFlow<List<EventInvitation>> =
        invitationRepository.observePendingInvitations()
            .onEach { invitations ->
                invitations.forEach { invitation ->
                    if (invitation.id !in notifiedIds) {
                        notifiedIds.add(invitation.id)
                        onNewInvitation?.invoke(invitation.eventName, invitation.invitedByEmail)
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun sendInvitation(invitation: EventInvitation) {
        viewModelScope.launch {
            invitationRepository.sendInvitation(invitation)
        }
    }

    fun acceptInvitation(invitation: EventInvitation) {
        viewModelScope.launch {
            invitationRepository.acceptInvitation(invitation)
        }
    }

    fun rejectInvitation(invitationId: String) {
        viewModelScope.launch {
            invitationRepository.rejectInvitation(invitationId)
        }
    }
}
