package com.cuentamorosos.data.repository

import com.cuentamorosos.currentTimeMillis
import com.cuentamorosos.data.NetworkMonitor
import com.cuentamorosos.data.PendingOperationQueue
import com.cuentamorosos.db.CuentaMorososDatabase
import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.EventParticipant
import com.cuentamorosos.model.EventRole
import com.cuentamorosos.model.EventState
import com.cuentamorosos.model.SUPPORTED_CURRENCY
import com.cuentamorosos.model.deserializeParticipants
import com.cuentamorosos.model.serializeParticipants
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class OfflineFirstEventRepository(
    private val remoteRepository: EventRepository,
    private val database: CuentaMorososDatabase,
    private val networkMonitor: NetworkMonitor,
    private val syncScope: CoroutineScope,
    private val pendingQueue: PendingOperationQueue
) : EventRepository {

    private val queries = database.cachedEventQueries
    private var syncJob: Job? = null
    private var started = false

    fun startSync() {
        if (started) return
        started = true
        networkMonitor.isOnline
            .onEach { isOnline ->
                if (isOnline) startSyncLoop() else stopSyncLoop()
            }
            .launchIn(syncScope)
    }

    override fun observeEvents(): Flow<List<EventItem>> {
        return queries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { cachedEvents ->
                cachedEvents.map { it.toEventItem() }
            }
    }

    override fun observeEvent(eventId: String): Flow<EventItem?> =
        observeEvents().map { events ->
            events.find { it.id == eventId }
        }

    private fun startSyncLoop() {
        syncJob?.cancel()
        syncJob = syncScope.launch(Dispatchers.Default) {
            var backoffMs = 1000L
            val maxBackoffMs = 30000L
            while (isActive) {
                try {
                    val firstEmission = withTimeoutOrNull(15_000) {
                        remoteRepository.observeEvents().first()
                    }
                    if (firstEmission != null) {
                        upsertEvents(firstEmission)
                    } else {
                        println("[OfflineFirstEventRepo] Sync timeout after 15s, retrying with backoff")
                    }
                    remoteRepository.observeEvents().collect { remoteEvents ->
                        upsertEvents(remoteEvents)
                    }
                    backoffMs = 1000L
                } catch (e: Exception) {
                    println("[OfflineFirstEventRepo] Sync error: ${e.message}")
                    delay(backoffMs)
                    backoffMs = minOf(backoffMs * 2, maxBackoffMs)
                }
            }
        }
    }

    private fun stopSyncLoop() {
        syncJob?.cancel()
        syncJob = null
    }

    private fun upsertEvents(events: List<com.cuentamorosos.model.EventItem>) {
        queries.transaction {
            events.forEach { event ->
                queries.upsert(
                    id = event.id,
                    name = event.name,
                    dateMillis = event.dateMillis,
                    ownerId = event.ownerId,
                    memberIds = event.memberIds.joinToString(","),
                    participants = event.participants.serializeParticipants(),
                    base_currency = event.baseCurrency,
                    lastCalculationMode = event.lastCalculationMode,
                    lastCalculationTotal = event.lastCalculationTotal,
                    lastCalculationTimestamp = event.lastCalculationTimestamp,
                    lastCalculationSummary = event.lastCalculationSummary,
                    updatedAt = currentTimeMillis(),
                    state = event.state.name,
                    startDateMillis = event.startDateMillis,
                    endDateMillis = event.endDateMillis,
                )
            }
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
            participants = event.participants.serializeParticipants(),
            base_currency = event.baseCurrency,
            lastCalculationMode = event.lastCalculationMode,
            lastCalculationTotal = event.lastCalculationTotal,
            lastCalculationTimestamp = event.lastCalculationTimestamp,
            lastCalculationSummary = event.lastCalculationSummary,
            updatedAt = currentTimeMillis(),
            state = event.state.name,
            startDateMillis = event.startDateMillis,
            endDateMillis = event.endDateMillis,
        )
        // Try remote, enqueue on failure
        try {
            remoteRepository.saveEvent(event)
        } catch (e: Exception) {
            pendingQueue.enqueue(
                id = "event_${event.id}_${currentTimeMillis()}",
                entityType = "event",
                entityId = event.id,
                operation = "save",
                payload = ""
            )
        }
    }

    override suspend fun deleteEvent(eventId: String) {
        queries.deleteById(eventId)
        try {
            remoteRepository.deleteEvent(eventId)
        } catch (e: Exception) {
            pendingQueue.enqueue(
                id = "event_${eventId}_${currentTimeMillis()}",
                entityType = "event",
                entityId = eventId,
                operation = "delete",
                payload = ""
            )
        }
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
        participants = deserializeParticipants(participants),
        baseCurrency = base_currency,
        lastCalculationMode = lastCalculationMode,
        lastCalculationTotal = lastCalculationTotal,
        lastCalculationTimestamp = lastCalculationTimestamp,
        lastCalculationSummary = lastCalculationSummary,
        startDateMillis = if (startDateMillis == 0L) dateMillis else startDateMillis,
        endDateMillis = if (endDateMillis == 0L) dateMillis else endDateMillis,
        state = runCatching { EventState.valueOf(state) }.getOrDefault(EventState.DRAFT)
    )
}
