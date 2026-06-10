package com.cuentamorosos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuentamorosos.data.repository.DebtRepository
import com.cuentamorosos.data.repository.EventRepository
import com.cuentamorosos.model.EventAction
import com.cuentamorosos.model.EventDebtItem
import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.EventState
import com.cuentamorosos.model.PermissionEngine
import com.cuentamorosos.model.StateTransitionResult
import com.cuentamorosos.model.TransitionContext
import com.cuentamorosos.model.canTransitionTo
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EventsViewModel(
    private val eventRepository: EventRepository,
    private val debtRepository: DebtRepository,
    private val currentProfileId: String = "",
) : ViewModel() {

    private val _events = MutableStateFlow<List<EventItem>>(emptyList())
    val events: StateFlow<List<EventItem>> = _events.asStateFlow()

    private val _transitionWarning = MutableStateFlow<StateTransitionResult.AllowedWithWarning?>(null)
    val transitionWarning: StateFlow<StateTransitionResult.AllowedWithWarning?> = _transitionWarning.asStateFlow()

    private val _validationErrors = MutableStateFlow<List<String>>(emptyList())
    val validationErrors: StateFlow<List<String>> = _validationErrors.asStateFlow()

    private val _permissionError = MutableStateFlow<String?>(null)
    val permissionError: StateFlow<String?> = _permissionError.asStateFlow()

    init {
        observeEvents()
    }

    private fun observeEvents() {
        viewModelScope.launch {
            eventRepository.observeEvents()
                .collect { updatedEvents ->
                    _events.value = updatedEvents
                }
        }
    }

    fun saveEvent(event: EventItem) {
        val isNew = _events.value.none { it.id == event.id }
        viewModelScope.launch {
            eventRepository.saveEvent(event)
            if (isNew && event.ownerId.isNotBlank()) {
                runCatching {
                    debtRepository.saveDebt(
                        EventDebtItem(
                            eventId = event.id,
                            profileId = event.ownerId
                        )
                    )
                }
            }
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            eventRepository.deleteEvent(eventId)
        }
    }

    fun removeMember(eventId: String, memberUid: String) {
        viewModelScope.launch {
            runCatching {
                eventRepository.removeMember(eventId, memberUid)
            }.onFailure { e ->
                println("[EventsViewModel] Failed to remove member: ${e.message}")
            }
        }
    }

    suspend fun findUidByEmail(email: String): String? =
        eventRepository.findUidByEmail(email)

    fun openEvent(eventId: String, context: TransitionContext) {
        viewModelScope.launch {
            val event = _events.value.find { it.id == eventId } ?: return@launch
            val result = event.canTransitionTo(EventState.OPEN, context)
            when (result) {
                is StateTransitionResult.Allowed -> {
                    saveEvent(event.copy(state = result.newState))
                }
                is StateTransitionResult.AllowedWithWarning -> {
                    _transitionWarning.value = result
                }
                is StateTransitionResult.Blocked -> {
                    _validationErrors.value = result.reasons
                }
            }
        }
    }

    fun openEventConfirmed(eventId: String) {
        viewModelScope.launch {
            val event = _events.value.find { it.id == eventId } ?: return@launch
            saveEvent(event.copy(state = EventState.OPEN))
            _transitionWarning.value = null
        }
    }

    fun dismissTransitionWarning() {
        _transitionWarning.value = null
    }

    fun clearValidationErrors() {
        _validationErrors.value = emptyList()
    }

    fun dismissPermissionError() {
        _permissionError.value = null
    }

    /**
     * Checks whether the current user can perform an action on a given event.
     */
    fun canDo(event: EventItem, action: EventAction): Boolean =
        PermissionEngine.canDo(currentProfileId, event, action)

    /**
     * Returns the role of the current user in the given event.
     */
    fun getRole(event: EventItem) = PermissionEngine.getRole(currentProfileId, event)
}
