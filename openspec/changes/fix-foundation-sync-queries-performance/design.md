# Design: Fix Foundation Sync & Query Performance

## Technical Approach

Fix 9 bugs in dependency order (A1→A2→A3→A4→A5→H1→H2→H3→B1). Each fix is surgical — modify only the broken code paths, preserve existing public APIs. The sync layer becomes: drain queue → single snapshot subscription → upsert to SQLDelight. Dashboard reads exclusively from SQLDelight.

## Architecture Decisions

### A1: drain() Wiring

| Aspect | Decision | Rationale |
|--------|----------|-----------|
| Where to call | Top of each sync loop iteration + on network reconnection (before `startSyncLoop()`) | Spec requires drain BEFORE remote fetch; reconnection is the key moment |
| Who implements RemoteOperations | Each OfflineFirst repo passes a `RemoteOperations` adapter to `drain()` that delegates to its own `remoteRepository` methods | Avoids circular deps; each repo knows its own remote calls |
| Drain order vs fetch | drain() FIRST, then fetch/collect | Pending writes must reach Firestore before we pull remote state |
| Failure handling | Existing `markFailed()` + `maxRetries=5` in PendingOperationQueue; no changes needed | Already handles retry with backoff correctly |
| Max retry exhaustion | After `maxRetries`, op stays in DB but `selectPending` skips it (retryCount >= maxRetries filter in SQL) | Prevents infinite retry loops; ops are visible for debugging |

### A2: Immediate Sync Startup

| Aspect | Decision | Rationale |
|--------|----------|-----------|
| Pattern | Replicate `OfflineFirstEventRepository.startSync()` exactly: `startSyncLoop()` then `networkMonitor.isOnline.drop(1).onEach{...}.launchIn(syncScope)` | `.drop(1)` skips the initial emission that would double-start the loop |
| Files to change | `OfflineFirstProfileRepository.kt`, `OfflineFirstDebtRepository.kt`, `OfflineFirstExpenseRepository.kt` | These 3 wait for network; Event already does it right |

### A3: Snapshot Listeners for Debts/Expenses

| Aspect | Decision | Rationale |
|--------|----------|-----------|
| FirestoreDebtRepository.observeAllDebts() | Replace `flow { emit(allDebts) }` with `flow { ... combine(eventId -> db.collection("events/$eventId/debts").snapshots) }` | Turns one-shot into continuous snapshot listeners |
| Event ID resolution | Keep 3 initial queries (owner/member/participant) to resolve event IDs — this is O(1) constant | Firestore has no cross-collection query without collection groups |
| N+1 mitigation | Each event gets ONE snapshot listener (long-lived), not repeated `.get()` calls | Eliminates polling; N listeners but zero repeated reads |
| Same pattern for | `FirestoreExpenseRepository.observeAllExpenses()` | Identical bug, identical fix |

### A4: Single Subscription Per Collection

| Aspect | Decision | Rationale |
|--------|----------|-----------|
| Fix pattern | Replace `withTimeoutOrNull { .first() } + .collect` with single `.onEach { upsert(it) }.collect()` | One snapshot listener per collection, not two |
| Remove withTimeoutOrNull | Yes, remove entirely | Timeout was a workaround for the double-subscription pattern |
| Files | Profile, Debt, Expense OfflineFirst repos (sync loop methods) | All 3 have the same double-subscription bug |

### A5: Complete Member ID Replacement

| Aspect | Decision | Rationale |
|--------|----------|-----------|
| Fields to update | `memberIds`, `ownerId`, `participantIds`, AND `participants[].profileId` | All 4 fields reference member identity; queries use all 3 list fields |
| Atomicity | Use `db.batch()` (already used for chunking) — add `participantIds` and `participants` updates to same batch | Firestore batch = atomic up to 500 ops; existing code already chunks |
| participants array | Read `participants` as `List<Map>`, map `profileId` replacements, write back | Firestore stores as array of maps; batch update replaces the whole array |
| Missing participants | If `participants` is null/empty, derive from `memberIds` (same heuristic as `loadParticipantsFromFirestore`) | Handles old events that predate the participants field |

### H1: Efficient Dashboard Queries

| Aspect | Decision | Rationale |
|--------|----------|-----------|
| Approach | Per-event snapshot listeners combined via `combine()` in `observeAllDebts()`/`observeAllExpenses()` | Collection group queries need security rule changes; per-event snapshots fix polling NOW |
| Event ID source | Same 3 initial queries (constant cost) | Cannot avoid — Firestore requires parent path for subcollection queries |
| Tradeoff | N long-lived listeners vs N polling reads per cycle | Listeners cost 0 reads when data doesn't change; polling burns reads every cycle |
| Future optimization | Collection group queries (`db.collectionGroup("debts")`) — requires Firestore rule changes, noted as follow-up | Out of scope for this change |

### H2: Non-Blocking Auth

| Aspect | Decision | Rationale |
|--------|----------|-----------|
| Mechanism | Replace `runBlocking { ... }` with `LaunchedEffect(user.uid) { ... }` inside Composable | `LaunchedEffect` runs in composition coroutine scope, not main thread |
| Where | `MainActivity.kt` lines 82-87 (onCreate), 257-261 (login), 288-291 (register) | All 3 `runBlocking` call sites |
| Loading state | Show existing `LoginScreen` loading indicator; auth succeeds immediately, sync runs in background | User sees app right away; profile sync is non-blocking |
| Failure handling | `runCatching { syncCurrentUser(); ensureOwnProfile() }` — log error, proceed to app | Auth already succeeded; profile sync failure shouldn't block app entry |

### H3: Consolidated Derived State

| Aspect | Decision | Rationale |
|--------|----------|-----------|
| Structure | Single `data class DashboardAggregates` holding all 7 computed values | Type-safe; single source of truth |
| Computation | One `derivedStateOf` block that iterates `allDebts` + `allExpenses` once, computing all aggregates | Eliminates 7 independent iterations over same lists |
| Keys | `remember(allDebts, allExpenses, events, currentUserUid)` | These are the only inputs |
| Downstream | Destructure `DashboardAggregates` into local vals before passing to screens | Minimal change to screen call sites |

### B1: Direct Event Observation

| Aspect | Decision | Rationale |
|--------|----------|-----------|
| Implementation | `collection.document(eventId).snapshots.map { it.toEventItem() }` | Single document listener, no collection-wide queries |
| Null handling | `.map { if (!it.exists()) null else it.toEventItem() }` | Emit null when document is deleted |
| Deduplication | Keep separate from main `observeEvents()` flow — no dedup needed | Direct listener is independent; main flow serves the events list |
| Files | `FirestoreEventRepository.kt` only | `OfflineFirstEventRepository.observeEvent()` delegates to `observeEvents()` which reads SQLDelight — that path stays |

## Data Flow

### Before (broken):
```
User writes offline → enqueue() → [nothing drains] → data lost on sign-out
Network reconnects → wait for monitor emission → .first() creates listener #1
                         → .collect creates listener #2 → double subscription
Dashboard → observeAllDebts() → 3+N one-shot .get() queries → emits once → loop restarts → polling
```

### After (fixed):
```
User writes offline → enqueue() → drain() on next sync cycle → Firestore updated
Network reconnects → drain() immediately → startSyncLoop() → single .collect
Dashboard → SQLDelight (local cache) ← sync loop ← single snapshot listener per event
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `shared/.../data/PendingOperationQueue.kt` | Modify | Add `drainAll(remoteOps)` that loops until queue empty; add `RemoteOperations` adapter factory |
| `shared/.../repository/OfflineFirstProfileRepository.kt` | Modify | Fix `startSync()` (immediate start + `.drop(1)`); fix sync loop (single subscription); add drain call |
| `shared/.../repository/OfflineFirstDebtRepository.kt` | Modify | Same 3 fixes as Profile; pass RemoteOperations to drain |
| `shared/.../repository/OfflineFirstExpenseRepository.kt` | Modify | Same 3 fixes as Profile; pass RemoteOperations to drain |
| `shared/.../repository/OfflineFirstEventRepository.kt` | Modify | Add drain call at top of sync loop; fix `observeEvent()` delegation |
| `shared/.../repository/FirestoreEventRepository.kt` | Modify | Fix `replaceMemberId()` (add participantIds + participants); fix `observeEvent()` (direct doc snapshot) |
| `shared/.../repository/FirestoreDebtRepository.kt` | Modify | Replace `observeAllDebts()` with per-event snapshot listeners via `combine()` |
| `shared/.../repository/FirestoreExpenseRepository.kt` | Modify | Replace `observeAllExpenses()` with per-event snapshot listeners via `combine()` |
| `shared/.../ui/CuentaMorososApp.kt` | Modify | Replace 7 `derivedStateOf` blocks with single `DashboardAggregates` computation |
| `app/.../MainActivity.kt` | Modify | Replace 3 `runBlocking` calls with `LaunchedEffect` + `runCatching` |

## Interfaces / Contracts

```kotlin
// New data class for consolidated dashboard state
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

## Testing Strategy

| Layer | What | Approach |
|-------|------|----------|
| Unit | `drain()` called before fetch; single subscription pattern; `replaceMemberId` updates all 4 fields | Fake RemoteOperations; verify call order and field updates |
| Unit | `DashboardAggregates` single-pass computation | Input lists → verify all 7 output maps match expected |
| Unit | `observeEvent()` direct doc snapshot | Mock Firestore doc snapshot; verify single listener created |
| Integration | Sync loop end-to-end: enqueue → drain → fetch → upsert | Use fake Firestore + real PendingOperationQueue + SQLDelight in-memory |
| Manual | Login without freeze; dashboard loads <3s; offline→online data sync | Device testing with network throttling |

## Migration / Rollout

No migration required. All fixes are in-memory logic. Existing SQLDelight schema and Firestore data structures are unchanged.

## Open Questions

- [ ] Collection group queries for debts/expenses (H1) — deferred to follow-up; requires Firestore security rule changes
- [ ] `selectPending` SQL query — verify it filters by `retryCount < maxRetries` (assumed but not confirmed in code review)
