package com.cuentamorosos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuentamorosos.data.repository.InvitationRepository
import com.cuentamorosos.model.EventInvitation
import com.cuentamorosos.model.InvitationStatus
import com.cuentamorosos.notifications.NotificationEvent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InvitationsViewModel(
    private val invitationRepository: InvitationRepository,
    /** Called when a new invitation arrives so the platform can post a notification. */
    private val onNewInvitation: ((NotificationEvent.InvitationReceived) -> Unit)? = null,
) : ViewModel() {

    val pendingInvitations: StateFlow<List<EventInvitation>> =
        invitationRepository.observePendingInvitations()
            .onEach { invitations ->
                invitations.forEach { invitation ->
                    if (invitation.status == InvitationStatus.PENDING) {
                        onNewInvitation?.invoke(
                            NotificationEvent.InvitationReceived(
                                invitationId = invitation.id,
                                eventId = invitation.eventId,
                                inviterName = invitation.invitedByEmail,
                                eventName = invitation.eventName,
                            )
                        )
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
