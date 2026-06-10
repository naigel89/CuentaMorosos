# Tasks: Fix Foundation Sync & Query Performance

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 350–450 (code only), 550–700 (with tests) |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 (sync fixes) → PR 2 (data integrity + queries) → PR 3 (UI perf + tests) |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: pending
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Sync layer: drain + immediate start + single subscription | PR 1 | 4 OfflineFirst repos + PendingOperationQueue; tests included |
| 2 | Data integrity + query optimization | PR 2 | replaceMemberId, per-event snapshots, direct doc observation |
| 3 | UI performance + integration tests | PR 3 | DashboardAggregates, LaunchedEffect, manual verification |

## Phase 1: Core Sync Fixes (A1, A2, A3, A4)

- [x] 1.1 Add `drainAll()` loop to `PendingOperationQueue.kt` — wrap existing `drain()` in `while (getAllPending() > 0)` loop; no new interface needed, `RemoteOperations` already exists (~15 lines, **Low** risk)
- [x] 1.2 Fix `OfflineFirstProfileRepository.startSync()` — replicate `OfflineFirstEventRepository` pattern: call `startSyncLoop()` immediately, add `.drop(1)` to network monitor flow; fix `startSyncLoop()` to single `.onEach { upsertProfiles(it) }.collect()` removing `withTimeoutOrNull` + `.first()` double subscription; add `pendingQueue.drainAll(remoteOps)` at top of loop (~30 lines, **Medium** risk — must build `RemoteOperations` adapter)
- [x] 1.3 Fix `OfflineFirstDebtRepository.startSync()` + `startSyncAll()` — same 3 fixes as 1.2: immediate start, single subscription, drain call (~25 lines, **Medium** risk)
- [x] 1.4 Fix `OfflineFirstExpenseRepository.startSync()` + `startSyncAll()` — same 3 fixes as 1.2 (~25 lines, **Medium** risk)
- [x] 1.5 Add drain call to `OfflineFirstEventRepository.startSyncLoop()` — add `pendingQueue.drainAll(remoteOps)` before `fetchEvents()`; `startSync()` already correct (~10 lines, **Low** risk)
- [x] 1.6 Unit tests for sync fixes — verify drain ordering (drain before fetch), single subscription (no `.first()` + `.collect` pattern), immediate start (no network wait) using fake `RemoteOperations` + in-memory SQLDelight (~80 lines, **Low** risk)

## Phase 2: Data Integrity (A5)

- [x] 2.1 Fix `FirestoreEventRepository.replaceMemberId()` — add `participantIds` and `participants[].profileId` updates to existing batch; read `participants` as `List<Map>`, map replacements, write back; handle null/empty participants by deriving from `memberIds` (~35 lines, **High** risk — Firestore batch atomicity, array manipulation)
- [x] 2.2 Unit tests for `replaceMemberId()` — verify all 4 fields updated (`memberIds`, `ownerId`, `participantIds`, `participants[].profileId`); test null participants fallback (~50 lines, **Low** risk)

## Phase 3: Query Optimization (H1, B1)

- [x] 3.1 Rewrite `FirestoreDebtRepository.observeAllDebts()` — replace one-shot `flow { emit(allDebts) }` with per-event snapshot listeners via `combine(eventId -> db.collection("events/$eventId/debts").snapshots)`; keep 3 initial event-ID queries (~30 lines, **Medium** risk — N listeners lifecycle)
- [x] 3.2 Rewrite `FirestoreExpenseRepository.observeAllExpenses()` — same pattern as 3.1 for expenses subcollection (~30 lines, **Medium** risk)
- [x] 3.3 Fix `FirestoreEventRepository.observeEvent()` — replace `observeEvents().map { find }` with `collection.document(eventId).snapshots.map { it.toEventItem() }`; handle non-existent docs with null emission (~10 lines, **Low** risk)
- [x] 3.4 Unit tests for query optimization — verify per-event snapshot listeners created (not one-shot), `observeEvent()` creates single doc listener, null handling for deleted docs (~60 lines, **Low** risk)

## Phase 4: UI Performance (H2, H3)

- [x] 4.1 Replace 3 `runBlocking` calls in `MainActivity.kt` — lines 82-87 (onCreate), 257-261 (login), 288-291 (register) → wrap in `LaunchedEffect(user.uid)` + `runCatching { syncCurrentUser(); ensureOwnProfile() }`; show app immediately, sync in background (~15 lines, **Medium** risk — auth flow timing)
- [x] 4.2 Create `DashboardAggregates` data class + single-pass computation in `CuentaMorososApp.kt` — replace 7 `derivedStateOf` blocks (lines 179-240) with one `remember(allDebts, allExpenses, events, currentUserUid) { derivedStateOf { DashboardAggregates(...) } }`; destructure into local vals at call sites (~50 lines, **Medium** risk — must preserve all 7 output maps exactly)
- [x] 4.3 Unit tests for `DashboardAggregates` — input lists → verify all 7 output maps; test empty inputs, single-event, multi-profile scenarios (~50 lines, **Low** risk)

## Phase 5: Integration & Verification

- [ ] 5.1 Integration test: sync loop end-to-end — fake Firestore + real `PendingOperationQueue` + SQLDelight in-memory; verify enqueue → drain → fetch → upsert pipeline (~80 lines, **Medium** risk)
- [ ] 5.2 Manual verification checklist — login without freeze (<1s to app), dashboard loads <3s, offline→online sync completes, `replaceMemberId` updates visible in Firestore console
