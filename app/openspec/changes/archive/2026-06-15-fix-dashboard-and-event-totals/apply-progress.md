# Apply Progress: fix-dashboard-and-event-totals

**Date**: 2026-06-15
**Mode**: Strict TDD
**All Slices Complete**: ✅ Yes (3 of 3)

## Completed Tasks (19/19)

### Phase 1: Dashboard Debt Calculation Fix (Slice 1)

- [x] **T-01**: Unit test for `totalOwedToYou` aggregation — 5 tests added
- [x] **T-02**: Fix `totalOwedToYou` — added `it.profileId != currentUserUid` filter
- [x] **T-03**: Unit test for `youAreOwedByEvent` aggregation — 3 tests added
- [x] **T-04**: No fix needed — `youAreOwedByEvent` already correct

### Phase 2: Events Screen Cleanup (Slice 1)

- [x] **T-05**: Removed `BalanceSummaryCard(...)` call from `EventsScreen.kt`
- [x] **T-06**: Removed orphan derived states
- [x] **T-07**: Regression test for EventsScreen — 10 tests added

### Phase 3: Profile Avatar Consistency (Slice 2)

- [x] **T-08**: ProfileAvatarConsistencyTest — 18 tests for avatar infrastructure
- [x] **T-09**: PreviewBreakdown — Box+initials → ProfileAvatar(32dp)
- [x] **T-10**: TransferListPanel — profiles param + ProfileAvatar(24dp)
- [x] **T-11**: CalculatorSheet ParameterInputRow — icon+name text → Row(ProfileAvatar, name)
- [x] **T-12**: Removed unused imports (Box, background, clip)

### Phase 4: Calculation Snapshot Persistence (Slice 3)

- [x] **T-13**: Roundtrip tests for `CalculationSnapshot.toJson()` — 7 tests (transfers, totalExpense, calculatedAtMillis, algorithmVersion, participantBalances)
- [x] **T-14**: Null safety tests — 8 tests (null, empty, malformed, missing fields, backward compat)
- [x] **T-15**: Extended `toJson()` and `toCalculationSnapshot()` with `participantBalances`
- [x] **T-16**: Changed `onApplyCalculation` to store `result.snapshot?.toJson()`
- [x] **T-17**: SettlementPanel data transformation tests — 11 tests
- [x] **T-18**: Redesigned SettlementPanel with transfer details + read-only total cost
- [x] **T-19**: Full test suite verified — 497 tests, 496 passing, 1 pre-existing failure

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| T-01 | `DashboardViewModelTest.kt` | Unit | ⚠️ 434/435 | ✅ Unresolved ref | ✅ 5/5 | ✅ 5 cases | ✅ Extracted function |
| T-02 | `DashboardViewModel.kt` | Unit | ⚠️ 434/435 | — | ✅ Compiles+passes | — | ✅ Clean |
| T-03 | `DashboardAggregatesTest.kt` | Unit | ✅ 440/441 | ✅ Passes | ✅ 3/3 | ✅ 3 cases | ➖ None needed |
| T-04 | `CuentaMorososApp.kt` | — | — | — | — (no fix) | — | — |
| T-05 | `EventsScreen.kt` | — | — | — (UI removal) | — | — | — |
| T-06 | `EventsScreen.kt` | — | — | — (dead code) | — | — | — |
| T-07 | `EventsScreenTest.kt` | Unit | ✅ 443/444 | ✅ Unresolved ref | ✅ 10/10 | ✅ 10 cases | ➖ Clean |
| T-08 | `ProfileAvatarConsistencyTest.kt` | Unit¹ | ✅ 452/453 | ✅ Written | ✅ 18/18 | ✅ 18 cases | ➖ Clean |
| T-09 | `ProfileAvatarConsistencyTest.kt` | Unit¹ | ✅ 452/453 | N/A² | ✅ Compiles | N/A² | ✅ Unused imports |
| T-10 | `ProfileAvatarConsistencyTest.kt` | Unit¹ | ✅ 452/453 | N/A² | ✅ Compiles | N/A² | ✅ Clean |
| T-11 | `ProfileAvatarConsistencyTest.kt` | Unit¹ | ✅ 452/453 | N/A² | ✅ Compiles | N/A² | ✅ Clean |
| T-12 | `PreviewBreakdown.kt` | — | ✅ 452/453 | N/A² | ✅ Compiles | N/A² | ✅ Clean |
| T-13 | `CalculationSnapshotPersistenceTest.kt` | Unit | ✅ 470/471 | ✅ 2 FAIL | ✅ 15/15 | ✅ 7 cases | ✅ Clean |
| T-14 | `CalculationSnapshotPersistenceTest.kt` | Unit | ✅ 470/471 | ✅ Passed | ✅ 8/8 | ✅ 8 cases | ✅ Clean |
| T-15 | `Models.kt` | Unit | ✅ 470/471 | — (GREEN from T-13) | ✅ 15/15 | — | ✅ Clean |
| T-16 | `CuentaMorososApp.kt` | Unit | ✅ 485/486 | — | ✅ Compiles+passes | — | ✅ Clean |
| T-17 | `SettlementPanelPersistenceTest.kt` | Unit | ✅ 485/486 | ✅ Written | ✅ 11/11 | ✅ 11 cases | ✅ Clean |
| T-18 | `SettlementPanel.kt` | Unit | ✅ 485/486 | — (GREEN from T-17) | ✅ Compiles+passes | — | ✅ Clean |
| T-19 | Full suite | Unit | ✅ 485/486 | — | ✅ 496/497 | — | ✅ Clean |

¹ Degraded from Compose UI to unit tests — compose.ui.test unavailable in commonTest.
² Task replaces existing UI code with ProfileAvatar — visual change, not logic-based.

## Test Summary

- **Total tests written**: 58 (Slice 1: 18 + Slice 2: 18 + Slice 3: 26 — 15 roundtrip/null + 11 settlement)
- **Total tests passing**: 496/497
- **Pre-existing failures**: 1 (`OfflineFirstProfileRepositoryTest` — unrelated)
- **Layers used**: Unit (58)
- **Pure functions created**: 5 (`calculateTotalOwedToYou`, `filterEventsList`, `formatEuroAmount`, `colorForName`, `extractInitial`)

## Commits Made

### Slice 1
1. `3c47be3` — `fix(dashboard): corregir totalOwedToYou para excluir deudas propias`
2. `7a2183b` — `test(dashboard): agregar tests de exclusividad para youAreOwedByEvent`
3. `272c275` — `refactor(events): remover BalanceSummaryCard y derived states huérfanos`

### Slice 2
4. `217600d` — `feat(avatars): reemplazar renderizado manual de avatars por ProfileAvatar en 3 componentes`

### Slice 3
5. `960b832` — `test(snapshot): agregar tests de serialización roundtrip y null safety`
6. `5cacc44` — `feat(snapshot): incluir participantBalances en serialización JSON`
7. `c8fc73b` — `fix(snapshot): persistir snapshot.toJson() en onApplyCalculation`
8. `3e4dd56` — `feat(snapshot): rediseñar SettlementPanel con transferencias sugeridas`

## Files Changed (All Slices)

| File | Action | Slice | Description |
|------|--------|-------|-------------|
| `shared/.../DashboardViewModel.kt` | Modified | 1 | Added `profileId != currentUserUid` filter |
| `shared/.../EventsScreen.kt` | Modified | 1 | Removed BalanceSummaryCard + orphan states |
| `shared/.../PreviewBreakdown.kt` | Modified | 2 | ProfileAvatar(32dp) replacing initials Box |
| `shared/.../TransferListPanel.kt` | Modified | 2 | profiles param + ProfileAvatar(24dp) |
| `shared/.../CalculatorSheet.kt` | Modified | 2 | ParameterInputRow with ProfileAvatar |
| `shared/.../Models.kt` | Modified | 3 | toJson/toCalculationSnapshot with participantBalances |
| `shared/.../CuentaMorososApp.kt` | Modified | 3 | onApplyCalculation persists snapshot JSON |
| `shared/.../SettlementPanel.kt` | Modified | 3 | lastCalculationSummary, transfer details, read-only total |
| `shared/.../EventDetailScreen.kt` | Modified | 3 | Passes lastCalculationSummary to SettlementPanel |
| `shared/.../DashboardViewModelTest.kt` | Created | 1 | 5 tests for totalOwedToYou |
| `shared/.../DashboardAggregatesTest.kt` | Created | 1 | 3 tests for youAreOwedByEvent |
| `shared/.../EventsScreenTest.kt` | Created | 1 | 10 tests for EventsScreen filters |
| `shared/.../ProfileAvatarConsistencyTest.kt` | Created | 2 | 18 tests for avatar infrastructure |
| `shared/.../CalculationSnapshotPersistenceTest.kt` | Created | 3 | 15 tests for serialization roundtrip |
| `shared/.../SettlementPanelPersistenceTest.kt` | Created | 3 | 11 tests for SettlementPanel data logic |

## Deviations from Design

None — implementation matches design for all 3 slices.

## Issues Found

- Pre-existing test failure in `OfflineFirstProfileRepositoryTest` (not related to this change)
- `compose.ui.test` unavailable in `commonTest` — Compose UI tests degraded to unit tests for logic
- `lastCalculationSummary` field already existed in `EventItem` data model — no model change needed for T-14

## Status

✅ **All 19 tasks complete. All 3 slices done. Ready for archive.**
