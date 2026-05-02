package com.cuentamorosos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuentamorosos.data.repository.DebtRepository
import com.cuentamorosos.data.repository.EventRepository
import com.cuentamorosos.model.EventDebtItem
import com.cuentamorosos.model.EventItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EventsViewModel(
    private val eventRepository: EventRepository,
    private val debtRepository: DebtRepository,
) : ViewModel() {

    private val _events = MutableStateFlow<List<EventItem>>(emptyList())
    val events: StateFlow<List<EventItem>> = _events.asStateFlow()

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
            eventRepository.removeMember(eventId, memberUid)
        }
    }

    suspend fun findUidByEmail(email: String): String? =
        eventRepository.findUidByEmail(email)
}
