# Detailed Task Breakdown: fix-foundation-sync-queries-performance

## Overview
This document provides detailed implementation guidance for each task in the task breakdown. Each task includes specific code changes, dependencies, testing requirements, and acceptance criteria.

---

## Phase 1: Core Sync Fixes (A1, A2, A3, A4)

### Task 1.1: Add `drainAll()` loop to PendingOperationQueue
**File**: `shared/src/commonMain/kotlin/com/cuentamorosos/data/PendingOperationQueue.kt`

**What to do**:
- Add new method `suspend fun drainAll(remoteOps: RemoteOperations)` that loops until queue is empty
- Implementation:
```kotlin
suspend fun drainAll(remoteOps: RemoteOperations) {
    while (getAllPending() > 0) {
        drain(remoteOps)
    }
}
```

**Dependencies**: None (foundational)
**Estimated lines**: +8 lines
**Risk**: Low — simple loop around existing `drain()`
**Testing**: Verify loop terminates when queue empty; verify multiple batches processed
**Acceptance criteria**:
- ✅ `drainAll()` processes all pending operations (not just first 10)
- ✅ Loop terminates when `getAllPending() == 0`
- ✅ Existing `drain()` behavior unchanged

---

### Task 1.2: Fix OfflineFirstProfileRepository sync pattern
**File**: `shared/src/commonMain/kotlin/com/cuentamorosos/data/repository/OfflineFirstProfileRepository.kt`

**What to do**:
1. **Fix `startSync()`** (lines 33-40):
   - Call `startSyncLoop()` immediately (don't wait for network)
   - Add `.drop(1)` to network monitor flow to skip initial emission
   ```kotlin
   fun startSync() {
       stopSyncLoop()
       startSyncLoop()  // Start immediately
       networkMonitor.isOnline
           .drop(1)  // Skip initial emission
           .onEach { isOnline ->
               if (isOnline) startSyncLoop() else stopSyncLoop()
           }
           .launchIn(syncScope)
   }
   ```

2. **Fix `startSyncLoop()`** (lines 56-84):
   - Remove `withTimeoutOrNull` + `.first()` double subscription
   - Add `pendingQueue.drainAll()` at top of loop
   - Single subscription pattern:
   ```kotlin
   private fun startSyncLoop() {
       syncJob?.cancel()
       syncJob = syncScope.launch(Dispatchers.Default) {
           var backoffMs = 1000L
           val maxBackoffMs = 30000L
           while (isActive) {
               try {
                   // 1. Drain pending operations FIRST
                   pendingQueue.drainAll(profileRemoteOps)
                   
                   // 2. Single subscription to remote
                   remoteRepository.observeProfiles()
                       .onEach { remoteProfiles ->
                           println("[OfflineFirstProfileRepo] Sync update: ${remoteProfiles.size} profiles")
                           upsertProfiles(remoteProfiles)
                       }
                       .collect()
                   
                   backoffMs = 1000L
               } catch (e: Exception) {
                   println("[OfflineFirstProfileRepo] Sync error: ${e.message}")
                   delay(backoffMs)
                   backoffMs = minOf(backoffMs * 2, maxBackoffMs)
               }
           }
       }
   }
   ```

3. **Create `RemoteOperations` adapter** (add as private property):
   ```kotlin
   private val profileRemoteOps = object : RemoteOperations {
       override suspend fun saveProfile(entityId: String) {
           val profile = remoteRepository.observeProfiles().first().find { it.id == entityId }
           if (profile != null) remoteRepository.saveProfile(profile)
       }
       override suspend fun deleteProfile(entityId: String) = remoteRepository.deleteProfile(entityId)
       // ... other methods throw UnsupportedOperationException
   }
   ```

**Dependencies**: Task 1.1 (drainAll)
**Estimated lines**: ~35 lines changed
**Risk**: Medium — must build RemoteOperations adapter correctly
**Testing**: 
- Verify immediate start (no network wait)
- Verify single subscription (no `.first()` + `.collect` pattern)
- Verify drain called before fetch
**Acceptance criteria**:
- ✅ `startSync()` calls `startSyncLoop()` immediately
- ✅ Network monitor flow has `.drop(1)`
- ✅ Sync loop calls `drainAll()` before `observeProfiles()`
- ✅ Only ONE subscription to `remoteRepository.observeProfiles()`
- ✅ No `withTimeoutOrNull` wrapper

---

### Task 1.3: Fix OfflineFirstDebtRepository sync pattern
**File**: `shared/src/commonMain/kotlin/com/cuentamorosos/data/repository/OfflineFirstDebtRepository.kt`

**What to do**: Same 3 fixes as Task 1.2:
1. Fix `startSync()` — immediate start + `.drop(1)`
2. Fix `startSyncAll()` — drain + single subscription
3. Create `debtRemoteOps` adapter

**Dependencies**: Task 1.1
**Estimated lines**: ~30 lines changed
**Risk**: Medium
**Testing**: Same as 1.2 but for debts
**Acceptance criteria**: Same pattern as 1.2

---

### Task 1.4: Fix OfflineFirstExpenseRepository sync pattern
**File**: `shared/src/commonMain/kotlin/com/cuentamorosos/data/repository/OfflineFirstExpenseRepository.kt`

**What to do**: Same 3 fixes as Task 1.2:
1. Fix `startSync()` — immediate start + `.drop(1)`
2. Fix `startSyncAll()` — drain + single subscription
3. Create `expenseRemoteOps` adapter

**Dependencies**: Task 1.1
**Estimated lines**: ~30 lines changed
**Risk**: Medium
**Testing**: Same as 1.2 but for expenses
**Acceptance criteria**: Same pattern as 1.2

---

### Task 1.5: Add drain to OfflineFirstEventRepository
**File**: `shared/src/commonMain/kotlin/com/cuentamorosos/data/repository/OfflineFirstEventRepository.kt`

**What to do**:
- Add `pendingQueue.drainAll(eventRemoteOps)` at top of `startSyncLoop()` (line 66)
- `startSync()` already correct (immediate start + `.drop(1)`)
- Create `eventRemoteOps` adapter

**Dependencies**: Task 1.1
**Estimated lines**: ~15 lines changed
**Risk**: Low — `startSync()` already correct
**Testing**: Verify drain called before `fetchEvents()`
**Acceptance criteria**:
- ✅ `startSyncLoop()` calls `drainAll()` before `fetchEvents()`
- ✅ Existing `startSync()` pattern preserved

---

### Task 1.6: Unit tests for sync fixes
**File**: `shared/src/test/kotlin/com/cuentamorosos/data/repository/OfflineFirstSyncTest.kt` (new)

**What to do**:
- Test drain ordering: verify `drainAll()` called before remote fetch
- Test single subscription: verify no `.first()` + `.collect` pattern
- Test immediate start: verify `startSyncLoop()` called without network wait
- Use fake `RemoteOperations` + in-memory SQLDelight

**Dependencies**: Tasks 1.1-1.5
**Estimated lines**: ~80 lines
**Risk**: Low
**Testing**: N/A (this IS the test)
**Acceptance criteria**:
- ✅ All tests pass
- ✅ Fake RemoteOperations verifies call order
- ✅ SQLDelight in-memory database verifies upsert

---

## Phase 2: Data Integrity (A5)

### Task 2.1: Fix FirestoreEventRepository.replaceMemberId()
**File**: `shared/src/commonMain/kotlin/com/cuentamorosos/data/repository/FirestoreEventRepository.kt`

**What to do**:
- Fix `replaceMemberId()` (lines 136-153) to update ALL 4 fields:
  1. `memberIds` (already done)
  2. `ownerId` (already done)
  3. `participantIds` (MISSING — add)
  4. `participants[].profileId` (MISSING — add)

```kotlin
override suspend fun replaceMemberId(oldId: String, newId: String) {
    val snapshot = collection.where { "memberIds" contains oldId }.get()
    snapshot.documents.chunked(499).forEach { chunk ->
        db.batch().apply {
            chunk.forEach { doc ->
                val data = doc.getRawData() ?: return@forEach
                
                // 1. Update memberIds
                val memberIds = (data["memberIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                val newMemberIds = memberIds.map { if (it == oldId) newId else it }
                
                // 2. Update ownerId
                val ownerId = data["ownerId"] as? String
                val newOwnerId = if (ownerId == oldId) newId else ownerId
                
                // 3. Update participants array
                @Suppress("UNCHECKED_CAST")
                val participants = (data["participants"] as? List<Map<String, Any?>>) ?: emptyList()
                val newParticipants = participants.map { p ->
                    if (p["profileId"] == oldId) p + ("profileId" to newId) else p
                }
                
                // 4. Update participantIds
                val newParticipantIds = newParticipants.map { it["profileId"] }
                
                // Batch update all fields
                val docRef = collection.document(doc.id)
                update(docRef, mapOf(
                    "memberIds" to newMemberIds,
                    "ownerId" to newOwnerId,
                    "participants" to newParticipants,
                    "participantIds" to newParticipantIds
                ))
            }
            commit()
        }
    }
}
```

**Dependencies**: None (standalone)
**Estimated lines**: ~35 lines changed
**Risk**: High — Firestore batch atomicity, array manipulation
**Testing**: Verify all 4 fields updated in single batch
**Acceptance criteria**:
- ✅ `memberIds` updated (existing)
- ✅ `ownerId` updated (existing)
- ✅ `participantIds` updated (NEW)
- ✅ `participants[].profileId` updated (NEW)
- ✅ All updates in single atomic batch
- ✅ Handles null/empty `participants` gracefully

---

### Task 2.2: Unit tests for replaceMemberId()
**File**: `shared/src/test/kotlin/com/cuentamorosos/data/repository/FirestoreEventRepositoryTest.kt` (new)

**What to do**:
- Mock Firestore document with all 4 fields
- Call `replaceMemberId(oldId, newId)`
- Verify all 4 fields updated correctly
- Test edge case: null/empty `participants` array

**Dependencies**: Task 2.1
**Estimated lines**: ~50 lines
**Risk**: Low
**Acceptance criteria**:
- ✅ All 4 fields verified in test
- ✅ Null participants test passes

---

## Phase 3: Query Optimization (H1, B1)

### Task 3.1: Rewrite FirestoreDebtRepository.observeAllDebts()
**File**: `shared/src/commonMain/kotlin/com/cuentamorosos/data/repository/FirestoreDebtRepository.kt`

**What to do**:
- Replace one-shot `flow { emit(allDebts) }` (lines 26-52) with per-event snapshot listeners
- Keep 3 initial event-ID queries (constant cost)
- Use `combine()` to merge per-event snapshot flows

```kotlin
override fun observeAllDebts(): Flow<List<EventDebtItem>> = flow {
    val uid = auth.currentUser?.uid ?: run {
        emit(emptyList())
        return@flow
    }

    // 1. Resolve event IDs (3 queries, constant cost)
    val ownerSnapshot = db.collection("events").where { "ownerId" equalTo uid }.get()
    val memberSnapshot = db.collection("events").where { "memberIds" contains uid }.get()
    val participantSnapshot = db.collection("events").where { "participantIds" contains uid }.get()

    val eventIds = (ownerSnapshot.documents + memberSnapshot.documents + participantSnapshot.documents)
        .map { it.id }
        .distinct()

    if (eventIds.isEmpty()) {
        emit(emptyList())
        return@flow
    }

    // 2. Create per-event snapshot listeners
    val debtFlows = eventIds.map { eventId ->
        db.collection("events")
            .document(eventId)
            .collection("debts")
            .snapshots
            .map { snapshot -> snapshot.documents.mapNotNull { it.toDebtItem() } }
    }

    // 3. Combine all flows into single emission
    combine(debtFlows) { debtLists ->
        debtLists.flatMap { it }
    }.collect { emit(it) }
}
```

**Dependencies**: None (standalone)
**Estimated lines**: ~30 lines changed
**Risk**: Medium — N listeners lifecycle management
**Testing**: Verify per-event snapshot listeners created (not one-shot)
**Acceptance criteria**:
- ✅ Per-event snapshot listeners (not `.get()`)
- ✅ `combine()` merges all flows
- ✅ Continuous updates (not one-shot)

---

### Task 3.2: Rewrite FirestoreExpenseRepository.observeAllExpenses()
**File**: `shared/src/commonMain/kotlin/com/cuentamorosos/data/repository/FirestoreExpenseRepository.kt`

**What to do**: Same pattern as Task 3.1 for expenses subcollection

**Dependencies**: None (standalone)
**Estimated lines**: ~30 lines changed
**Risk**: Medium
**Testing**: Same as 3.1
**Acceptance criteria**: Same pattern as 3.1

---

### Task 3.3: Fix FirestoreEventRepository.observeEvent()
**File**: `shared/src/commonMain/kotlin/com/cuentamorosos/data/repository/FirestoreEventRepository.kt`

**What to do**:
- Replace `observeEvents().map { find }` (lines 46-49) with direct document snapshot

```kotlin
override fun observeEvent(eventId: String): Flow<EventItem?> =
    collection.document(eventId).snapshots.map { snapshot ->
        if (!snapshot.exists()) null else snapshot.toEventItem()
    }
```

**Dependencies**: None (standalone)
**Estimated lines**: ~5 lines changed
**Risk**: Low
**Testing**: Verify single doc listener created
**Acceptance criteria**:
- ✅ Direct document snapshot (not collection query)
- ✅ Null emission when doc doesn't exist

---

### Task 3.4: Unit tests for query optimization
**File**: `shared/src/test/kotlin/com/cuentamorosos/data/repository/FirestoreQueryTest.kt` (new)

**What to do**:
- Mock Firestore collection/document snapshots
- Verify per-event listeners created (not one-shot)
- Verify `observeEvent()` creates single doc listener
- Test null handling for deleted docs

**Dependencies**: Tasks 3.1-3.3
**Estimated lines**: ~60 lines
**Risk**: Low
**Acceptance criteria**:
- ✅ All tests pass
- ✅ Mock verifies listener creation

---

## Phase 4: UI Performance (H2, H3)

### Task 4.1: Replace runBlocking in MainActivity
**File**: `app/src/main/java/com/cuentamorosos/MainActivity.kt`

**What to do**:
- Replace 3 `runBlocking` calls with `LaunchedEffect` + `runCatching`:

1. **onCreate** (lines 82-87):
```kotlin
// BEFORE:
FirebaseAuth.getInstance().currentUser?.let {
    runBlocking {
        FirebaseUserSyncManager.syncCurrentUser()
        FirebaseUserSyncManager.ensureOwnProfile()
    }
}

// AFTER: Remove entirely — handled by LaunchedEffect in setContent
```

2. **LoginScreen onLoginSuccess** (lines 257-261):
```kotlin
// BEFORE:
auth.currentUser?.let { user ->
    runBlocking {
        FirebaseUserSyncManager.syncCurrentUser()
        FirebaseUserSyncManager.ensureOwnProfile()
    }
    onAuthSuccess(user)
}

// AFTER:
auth.currentUser?.let { user ->
    onAuthSuccess(user)  // Auth succeeds immediately
    // Profile sync happens in MainAppContent LaunchedEffect
}
```

3. **RegisterScreen onRegisterSuccess** (lines 288-291):
```kotlin
// BEFORE:
auth.currentUser?.let { user ->
    runBlocking {
        FirebaseUserSyncManager.syncCurrentUser(defaultMigrated = true)
        FirebaseUserSyncManager.ensureOwnProfile()
    }
    onAuthSuccess(user)
}

// AFTER:
auth.currentUser?.let { user ->
    onAuthSuccess(user)  // Auth succeeds immediately
    // Profile sync happens in MainAppContent LaunchedEffect
}
```

4. **Add profile sync to MainAppContent** (after line 147):
```kotlin
LaunchedEffect(user.uid) {
    runCatching {
        FirebaseUserSyncManager.syncCurrentUser()
        FirebaseUserSyncManager.ensureOwnProfile()
    }.onFailure { e ->
        println("[MainActivity] Profile sync failed: ${e.message}")
    }
    repositoryProvider.startSyncStaggered(syncScope)
}
```

**Dependencies**: None (standalone)
**Estimated lines**: ~20 lines changed
**Risk**: Medium — auth flow timing
**Testing**: Manual: login without freeze
**Acceptance criteria**:
- ✅ No `runBlocking` in codebase
- ✅ App shows immediately after login
- ✅ Profile sync runs in background
- ✅ Login/register flow still works

---

### Task 4.2: Create DashboardAggregates + single-pass computation
**File**: `shared/src/commonMain/kotlin/com/cuentamorosos/ui/CuentaMorososApp.kt`

**What to do**:
1. **Create data class** (add at top of file):
```kotlin
data class DashboardAggregates(
    val activeTotalsByProfile: Map<String, Double>,
    val pendingTotalsByEvent: Map<String, Double>,
    val totalSpent: Double,
    val participantCountByEvent: Map<String, Int>,
    val yourShareByEvent: Map<String, Double>,
    val youAreOwedByEvent: Map<String, Double>,
    val pendingEventsByProfile: Map<String, List<String>>,
)
```

2. **Replace 7 derivedStateOf blocks** (lines 179-240) with single computation:
```kotlin
val aggregates by remember(allDebts, allExpenses, events, currentUserUid) {
    derivedStateOf {
        val uid = currentUserUid ?: ""
        
        // Single pass over allDebts
        val activeTotalsByProfile = mutableMapOf<String, Double>()
        val pendingTotalsByEvent = mutableMapOf<String, Double>()
        val participantCountByEvent = mutableMapOf<String, Int>()
        val yourShareByEvent = mutableMapOf<String, Double>()
        val youAreOwedByEvent = mutableMapOf<String, Double>()
        val pendingEventsByProfile = mutableMapOf<String, MutableList<String>>()
        
        allDebts.forEach { debt ->
            if (!debt.paid) {
                // activeTotalsByProfile
                activeTotalsByProfile[debt.profileId] = 
                    (activeTotalsByProfile[debt.profileId] ?: 0.0) + debt.amountEuros
                
                // pendingTotalsByEvent
                pendingTotalsByEvent[debt.eventId] = 
                    (pendingTotalsByEvent[debt.eventId] ?: 0.0) + debt.amountEuros
                
                // yourShareByEvent
                if (debt.profileId == uid) {
                    yourShareByEvent[debt.eventId] = 
                        (yourShareByEvent[debt.eventId] ?: 0.0) + debt.amountEuros
                } else {
                    youAreOwedByEvent[debt.eventId] = 
                        (youAreOwedByEvent[debt.eventId] ?: 0.0) + debt.amountEuros
                }
                
                // pendingEventsByProfile
                val event = events.firstOrNull { it.id == debt.eventId }
                if (event != null) {
                    pendingEventsByProfile.getOrPut(debt.profileId) { mutableListOf() }
                        .add("${event.name} · ${formatEuros(debt.amountEuros)}")
                }
            }
            
            // participantCountByEvent (includes paid)
            participantCountByEvent[debt.eventId] = 
                (participantCountByEvent[debt.eventId] ?: 0) + 1
        }
        
        DashboardAggregates(
            activeTotalsByProfile = activeTotalsByProfile,
            pendingTotalsByEvent = pendingTotalsByEvent,
            totalSpent = allExpenses.sumOf { it.amountEuros },
            participantCountByEvent = participantCountByEvent,
            yourShareByEvent = yourShareByEvent,
            youAreOwedByEvent = youAreOwedByEvent,
            pendingEventsByProfile = pendingEventsByProfile,
        )
    }
}

// Destructure for call sites
val activeTotalsByProfile = aggregates.activeTotalsByProfile
val pendingTotalsByEvent = aggregates.pendingTotalsByEvent
val totalSpent = aggregates.totalSpent
val participantCountByEvent = aggregates.participantCountByEvent
val yourShareByEvent = aggregates.yourShareByEvent
val youAreOwedByEvent = aggregates.youAreOwedByEvent
val pendingEventsByProfile = aggregates.pendingEventsByProfile
```

**Dependencies**: None (standalone)
**Estimated lines**: ~60 lines changed
**Risk**: Medium — must preserve all 7 output maps exactly
**Testing**: Unit test for computation
**Acceptance criteria**:
- ✅ Single `derivedStateOf` block
- ✅ Single pass over `allDebts` (not 7 iterations)
- ✅ All 7 output maps preserved
- ✅ Call sites unchanged (destructured into local vals)

---

### Task 4.3: Unit tests for DashboardAggregates
**File**: `shared/src/test/kotlin/com/cuentamorosos/ui/DashboardAggregatesTest.kt` (new)

**What to do**:
- Test single-pass computation with various inputs
- Test empty inputs
- Test single-event scenario
- Test multi-profile scenario
- Verify all 7 output maps correct

**Dependencies**: Task 4.2
**Estimated lines**: ~50 lines
**Risk**: Low
**Acceptance criteria**:
- ✅ All tests pass
- ✅ Edge cases covered

---

## Phase 5: Integration & Verification

### Task 5.1: Integration test for sync loop
**File**: `shared/src/test/kotlin/com/cuentamorosos/data/repository/SyncLoopIntegrationTest.kt` (new)

**What to do**:
- Fake Firestore + real `PendingOperationQueue` + SQLDelight in-memory
- Test: enqueue → drain → fetch → upsert pipeline
- Verify data flows correctly end-to-end

**Dependencies**: Tasks 1.1-1.5
**Estimated lines**: ~80 lines
**Risk**: Medium
**Acceptance criteria**:
- ✅ End-to-end test passes
- ✅ Pending operations drained before fetch
- ✅ Data upserted to SQLDelight

---

### Task 5.2: Manual verification checklist
**What to do**:
- [ ] Login without freeze (<1s to app)
- [ ] Dashboard loads <3s
- [ ] Offline→online sync completes
- [ ] `replaceMemberId` updates visible in Firestore console
- [ ] No double subscriptions in logs
- [ ] Per-event snapshot listeners active

**Dependencies**: All tasks
**Estimated lines**: 0 (manual testing)
**Risk**: N/A
**Acceptance criteria**:
- ✅ All checklist items pass

---

## Summary

**Total tasks**: 17
**Total estimated lines**: 550-700 (with tests)
**Files modified**: 10
**Risk level**: High (due to 400-line budget)

**Recommendation**: Split into 3 chained PRs:
- **PR 1**: Tasks 1.1-1.6 (sync fixes)
- **PR 2**: Tasks 2.1-3.4 (data integrity + queries)
- **PR 3**: Tasks 4.1-5.2 (UI perf + tests)

**Next step**: Ask user which chain strategy to use before implementation.
