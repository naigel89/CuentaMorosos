package com.cuentamorosos.data.repository

import com.cuentamorosos.currentTimeMillis
import com.cuentamorosos.data.NetworkMonitor
import com.cuentamorosos.data.PendingOperationQueue
import com.cuentamorosos.data.RemoteOperations
import com.cuentamorosos.db.CuentaMorososDatabase
import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.EventParticipant
import com.cuentamorosos.model.EventRole
import com.cuentamorosos.model.EventState
import com.cuentamorosos.model.SUPPORTED_CURRENCY
import com.cuentamorosos.model.deserializeParticipants
import com.cuentamorosos.model.migrateMemberIdsToParticipants
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

    private val eventRemoteOps = object : RemoteOperations {
        override suspend fun saveEvent(entityId: String) {
            // Read from LOCAL cache (SQLDelight), not Firestore
            val local = queries.selectById(entityId).executeAsOneOrNull()?.toEventItem()
            if (local != null) {
                remoteRepository.saveEvent(local)
            } else {
                println("[OfflineFirstEventRepo] saveEvent pending: event $entityId not in local cache, skipping")
            }
        }
        override suspend fun deleteEvent(entityId: String) = remoteRepository.deleteEvent(entityId)
        override suspend fun saveDebt(entityId: String) = throw UnsupportedOperationException()
        override suspend fun deleteDebt(entityId: String) = throw UnsupportedOperationException()
        override suspend fun saveExpense(entityId: String) = throw UnsupportedOperationException()
        override suspend fun deleteExpense(entityId: String) = throw UnsupportedOperationException()
        override suspend fun saveProfile(entityId: String) = throw UnsupportedOperationException()
        override suspend fun deleteProfile(entityId: String) = throw UnsupportedOperationException()
        override suspend fun updateProfilePhoto(profileId: String, photoUrl: String) = throw UnsupportedOperationException()
        override suspend fun updateProfileUsername(profileId: String, username: String) = throw UnsupportedOperationException()
        override suspend fun updateProfileDisplayName(profileId: String, displayName: String) = throw UnsupportedOperationException()
        override suspend fun deleteProfilePhoto(profileId: String) = throw UnsupportedOperationException()
        override suspend fun linkGhostProfile(email: String, realUid: String) = throw UnsupportedOperationException()
    }

    fun startSync() {
        stopSyncLoop()
        // Start sync loop IMMEDIATELY — don't wait for network monitor
        startSyncLoop()
        // Also subscribe to reconnection events
        networkMonitor.isOnline
            .drop(1) // Skip initial emission (already handled above)
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
                val items = cachedEvents.map { it.toEventItem() }
                println("[OfflineFirstEventRepo] observeEvents: SQLDelight emitted ${items.size} events")
                items
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
                    // 0. Drain pending operations FIRST
                    pendingQueue.drainAll(eventRemoteOps)

                    // 1. One-shot fetch — loads data immediately via get(), not snapshot listeners
                    val initialEvents = withTimeoutOrNull(15_000) {
                        remoteRepository.fetchEvents()
                    }
                    if (initialEvents != null) {
                        upsertEvents(initialEvents)
                        println("[OfflineFirstEventRepo] Initial fetch: ${initialEvents.size} events")
                    } else {
                        println("[OfflineFirstEventRepo] Initial fetch timed out after 15s")
                    }
                    // 2. Then watch for realtime changes via snapshot listeners
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
        if (events.isEmpty()) {
            println("[OfflineFirstEventRepo] upsertEvents: called with 0 events, skipping")
            return
        }
        println("[OfflineFirstEventRepo] upsertEvents: writing ${events.size} events to SQLDelight")
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
        println("[OfflineFirstEventRepo] upsertEvents: transaction complete, ${events.size} events written")
    }

    override suspend fun fetchEvents(): List<EventItem> =
        remoteRepository.fetchEvents()

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
            println("[OfflineFirstEventRepo] saveEvent remote FAILED for ${event.id}: ${e.message}")
            e.printStackTrace()
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
            println("[OfflineFirstEventRepo] deleteEvent remote FAILED for $eventId: ${e.message}")
            e.printStackTrace()
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
        // Update local cache immediately so the UI reflects the change
        val cachedEvent = queries.selectById(eventId).executeAsOneOrNull()
        if (cachedEvent != null) {
            val parsedParticipants = deserializeParticipants(cachedEvent.participants)
            val updatedParticipants = parsedParticipants.filter { it.profileId != memberUid }
            val updatedMemberIds = (if (cachedEvent.memberIds.isBlank()) emptyList()
            else cachedEvent.memberIds.split(",")).filter { it != memberUid }
            queries.upsert(
                id = cachedEvent.id,
                name = cachedEvent.name,
                dateMillis = cachedEvent.dateMillis,
                ownerId = cachedEvent.ownerId,
                memberIds = updatedMemberIds.joinToString(","),
                participants = updatedParticipants.serializeParticipants(),
                base_currency = cachedEvent.base_currency,
                lastCalculationMode = cachedEvent.lastCalculationMode,
                lastCalculationTotal = cachedEvent.lastCalculationTotal,
                lastCalculationTimestamp = cachedEvent.lastCalculationTimestamp,
                lastCalculationSummary = cachedEvent.lastCalculationSummary,
                updatedAt = currentTimeMillis(),
                state = cachedEvent.state,
                startDateMillis = cachedEvent.startDateMillis,
                endDateMillis = cachedEvent.endDateMillis,
            )
        }
        // Then try remote — local is already updated, so on reconnect
        // the sync loop will converge with Firestore state.
        runCatching {
            remoteRepository.removeMember(eventId, memberUid)
        }.onFailure { e ->
            println("[OfflineFirstEventRepo] removeMember remote failed: ${e.message}")
        }
    }

    override suspend fun replaceMemberId(oldId: String, newId: String) {
        remoteRepository.replaceMemberId(oldId, newId)
        // Local cache updated via observeEvents() synchronization
    }

    override suspend fun findUidByEmail(email: String): String? {
        return remoteRepository.findUidByEmail(email)
    }

    private fun com.cuentamorosos.db.CachedEvent.toEventItem(): EventItem = migrateMemberIdsToParticipants(EventItem(
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
        state = runCatching { EventState.valueOf(state) }.getOrDefault(EventState.OPEN)
    ))
}
