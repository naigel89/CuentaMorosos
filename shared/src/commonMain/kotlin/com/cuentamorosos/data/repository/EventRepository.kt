package com.cuentamorosos.data.repository

import com.cuentamorosos.model.EventItem
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun observeEvents(): Flow<List<EventItem>>
    fun observeEvent(eventId: String): Flow<EventItem?>
    suspend fun saveEvent(event: EventItem)
    suspend fun deleteEvent(eventId: String)
    suspend fun removeMember(eventId: String, memberUid: String)
    suspend fun findUidByEmail(email: String): String?
    suspend fun replaceMemberId(oldId: String, newId: String)
}
