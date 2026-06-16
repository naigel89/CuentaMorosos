# Proposal: Fix Foundation Sync & Query Performance

## Intent

Data sync layer is broken: offline writes never reach Firestore, sync loops don't start, Firestore quota burns from polling, app freezes on login. Blocking all feature work.

## Scope

### In Scope
- Wire `PendingOperationQueue.drain()` into sync loops and network reconnection
- Fix sync startup in Profile/Debt/Expense repos (start immediately, then monitor)
- Replace one-shot flows with snapshot listeners in Firestore Debt/Expense repos
- Eliminate double Firestore subscriptions (`.first()` + `.collect`)
- Fix `replaceMemberId()` to update `participantIds` and `participants`
- Remove `runBlocking` from main thread in `MainActivity`
- Consolidate 7 `derivedStateOf` blocks into single-pass computation
- Replace N+1 query in `observeEvent()` with direct document snapshot

### Out of Scope
- Notifications, adjustments, participant removal, calculation bugs (Fase 2+)

## Capabilities

### New Capabilities
- `offline-first-sync`: Correct offline-first sync — drain queue, snapshot listeners, immediate startup, single subscriptions

### Modified Capabilities
None — existing specs unaffected at requirement level.

## Approach

Fix order by severity: A1 (data loss) → A2 (sync startup) → A3 (quota burn) → A4 (double sub) → A5 (ghost IDs) → H1 (N+1) → H2 (main thread) → H3 (recomputation) → B1 (observeEvent).

## Affected Areas

| Area | Impact |
|------|--------|
| `shared/.../data/PendingOperationQueue.kt` | Modified — add `drain()` call sites |
| `shared/.../repository/OfflineFirst{Profile,Debt,Expense}Repository.kt` | Modified — fix startup, remove double sub |
| `shared/.../repository/Firestore{Debt,Expense}Repository.kt` | Modified — snapshot listeners |
| `shared/.../repository/FirestoreEventRepository.kt` | Modified — `replaceMemberId()`, `observeEvent()` |
| `shared/.../ui/CuentaMorososApp.kt` | Modified — consolidate derived state |
| `app/.../MainActivity.kt` | Modified — remove `runBlocking` |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Data loss during drain wiring | Med | Tests before implementation; verify with existing 3265-line suite |
| Snapshot listener lifecycle leaks | Med | Cancel in `CoroutineScope` tied to repo lifecycle |
| Regression in sync behavior | Med | Full test suite after each fix; ordered dependency chain |

## Rollback Plan

Revert commit(s). No schema/data migrations — all fixes are in-memory logic.

## Success Criteria

- [ ] `drain()` called on sync loop + network reconnection
- [ ] Profile/Debt/Expense sync start immediately
- [ ] `observeAllDebts()`/`observeAllExpenses()` use snapshot listeners
- [ ] No double Firestore subscriptions
- [ ] `replaceMemberId()` updates `participantIds` + `participants`
- [ ] No `runBlocking` on main thread
- [ ] Single `derivedStateOf` for all Dashboard aggregates
- [ ] `observeEvent()` uses direct document snapshot
- [ ] All tests pass; new tests for drain + snapshot behavior
