# Apply Progress: fix-dashboard-and-event-totals

## Slice 1 (Completed) — Phases 1+2: Dashboard Debt Fix + Events Cleanup

- [x] T-01 **RED**: DashboardViewModelTest — user's own debts excluded
- [x] T-02 **GREEN**: Filter fix in DashboardViewModel.kt
- [x] T-03 **RED→GREEN**: DashboardAggregatesTest — youAreOwedByEvent exclusivity
- [x] T-04 **REFACTOR**: computeProfileBreakdown verification
- [x] T-05 **RED**: EventsScreenTest — regression tests (10 tests)
- [x] T-06 **GREEN**: Remove BalanceSummaryCard + orphan derived states
- [x] T-07 **REFACTOR**: Clean up unused imports

**Tests**: 18 new, all passing. 1 pre-existing failure (OfflineFirstProfileRepositoryTest).

## Slice 2 (Completed) — Phase 3: Profile Avatar Consistency

- [x] T-08 **RED**: ProfileAvatarConsistencyTest — 18 tests for avatar infrastructure (colorForName, data contracts)
- [x] T-09 **GREEN**: PreviewBreakdown — Box+initials → ProfileAvatar(32dp, photoUrl)
- [x] T-10 **GREEN**: TransferListPanel — profiles param + ProfileAvatar(24dp) in TransferRow, BalanceRow
- [x] T-11 **GREEN**: CalculatorSheet ParameterInputRow — icon+name text → Row(ProfileAvatar(24dp), name)
- [x] T-12 **REFACTOR**: Removed unused imports (Box, background, clip, size, sp from PreviewBreakdown)

**Tests**: 18 new in ProfileAvatarConsistencyTest, all passing. Total: 470 passing / 471 (1 pre-existing).

## Files Changed (Slice 2)

| File | Action | Description |
|------|--------|-------------|
| `shared/src/commonMain/.../ui/PreviewBreakdown.kt` | Modified | Replaced manual Box+initials with ProfileAvatar(32dp) |
| `shared/src/commonMain/.../ui/TransferListPanel.kt` | Modified | Added profiles param, ProfileAvatar(24dp) in TransferRow + BalanceRow |
| `shared/src/commonMain/.../ui/CalculatorSheet.kt` | Modified | ParameterInputRow: replaced icon+name text with Row(ProfileAvatar(24dp), name) |
| `shared/src/commonTest/.../ui/ProfileAvatarConsistencyTest.kt` | Created | 18 tests for colorForName, extractInitial, ProfileItem data contracts |

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| T-08 | `ProfileAvatarConsistencyTest.kt` | Unit¹ | ✅ 452/453 | ✅ Written | ✅ Passed | ✅ 18 cases | ➖ Clean |
| T-09 | `ProfileAvatarConsistencyTest.kt` | Unit¹ | ✅ 452/453 | N/A² | ✅ Compiles | N/A² | ✅ Unused imports removed |
| T-10 | `ProfileAvatarConsistencyTest.kt` | Unit¹ | ✅ 452/453 | N/A² | ✅ Compiles | N/A² | ✅ Clean |
| T-11 | `ProfileAvatarConsistencyTest.kt` | Unit¹ | ✅ 452/453 | N/A² | ✅ Compiles | N/A² | ✅ Clean |
| T-12 | `PreviewBreakdown.kt` | — | ✅ 452/453 | N/A² | ✅ Compiles | N/A² | ✅ All unused imports removed |

¹ Degraded from Compose UI tests to unit tests because `compose.ui.test` is not available in `commonTest` source set.
² Task involves replacing existing UI code with ProfileAvatar — behavior change is visual (internal composable replacement), not logic-based. Existing tests validate the avatar infrastructure.

## Remaining Tasks

- [ ] T-13 through T-19 (Phase 4: Calculation Snapshot Persistence) — Slice 3

## Status

12/19 tasks complete. Ready for Slice 3 (Phase 4).
