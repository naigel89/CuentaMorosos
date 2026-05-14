package com.cuentamorosos.data.repository

import com.cuentamorosos.currentTimeMillis
import com.cuentamorosos.db.CuentaMorososDatabase
import com.cuentamorosos.model.EventItem
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class OfflineFirstEventRepository(
    private val remoteRepository: EventRepository,
    private val database: CuentaMorososDatabase,
    private val scope: CoroutineScope
) : EventRepository {

    private val queries = database.cachedEventQueries

    override fun observeEvents(): Flow<List<EventItem>> {
        // 1. Start observing remote data and sync to local in the background
        scope.launch(Dispatchers.Default) {
            remoteRepository.observeEvents()
                .collect { remoteEvents ->
                    queries.transaction {
                        remoteEvents.forEach { event ->
                            queries.upsert(
                                id = event.id,
                                name = event.name,
                                dateMillis = event.dateMillis,
                                ownerId = event.ownerId,
                                memberIds = event.memberIds.joinToString(","),
                                lastCalculationMode = event.lastCalculationMode,
                                lastCalculationTotal = event.lastCalculationTotal,
                                lastCalculationTimestamp = event.lastCalculationTimestamp,
                                lastCalculationSummary = event.lastCalculationSummary,
                                updatedAt = currentTimeMillis()
                            )
                        }
                    }
                }
        }

        // 2. Return the local cache as the Single Source of Truth
        return queries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { cachedEvents ->
                cachedEvents.map { it.toEventItem() }
            }
    }

    override suspend fun saveEvent(event: EventItem) {
        // Update local immediately
        queries.upsert(
            id = event.id,
            name = event.name,
            dateMillis = event.dateMillis,
            ownerId = event.ownerId,
            memberIds = event.memberIds.joinToString(","),
            lastCalculationMode = event.lastCalculationMode,
            lastCalculationTotal = event.lastCalculationTotal,
            lastCalculationTimestamp = event.lastCalculationTimestamp,
            lastCalculationSummary = event.lastCalculationSummary,
            updatedAt = currentTimeMillis()
        )
        // Update remote
        remoteRepository.saveEvent(event)
    }

    override suspend fun deleteEvent(eventId: String) {
        queries.deleteById(eventId)
        remoteRepository.deleteEvent(eventId)
    }

    override suspend fun removeMember(eventId: String, memberUid: String) {
        remoteRepository.removeMember(eventId, memberUid)
        // Local cache updated via observeEvents() synchronization
    }

    override suspend fun replaceMemberId(oldId: String, newId: String) {
        remoteRepository.replaceMemberId(oldId, newId)
        // Local cache updated via observeEvents() synchronization
    }

    override suspend fun findUidByEmail(email: String): String? {
        return remoteRepository.findUidByEmail(email)
    }

    private fun com.cuentamorosos.db.CachedEvent.toEventItem(): EventItem = EventItem(
        id = id,
        name = name,
        dateMillis = dateMillis,
        ownerId = ownerId,
        memberIds = if (memberIds.isBlank()) emptyList() else memberIds.split(","),
        lastCalculationMode = lastCalculationMode,
        lastCalculationTotal = lastCalculationTotal,
        lastCalculationTimestamp = lastCalculationTimestamp,
        lastCalculationSummary = lastCalculationSummary
    )
}
