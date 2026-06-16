# Archive Report: fix-dashboard-and-event-totals

**Archived**: 2026-06-15
**Verification grade**: PASS (0 CRITICAL, 2 WARNING, 3 SUGGESTION)
**Tests**: 58 new, 496/497 total passing (1 pre-existing failure unrelated)

---

## Final State

| Phase | Artifact | Status |
|-------|----------|--------|
| Explore | `explore.md` | ✅ Complete |
| Design | `design.md` | ✅ Complete |
| Specs | 4 domains, 13 requirements | ✅ All compliant (verify-report.md) |
| Tasks | 19 tasks (apply-progress.md) | ✅ 19/19 complete |
| Implement | 3 slices, 9 commits | ✅ All merged |
| Verify | `verify-report.md` | ✅ PASS |

> **Note**: `proposal.md`, `tasks.md`, and `specs/` delta files were not persisted to disk in the change folder. Requirements are fully documented in `verify-report.md`. Main specs have been reconstructed at `openspec/specs/` from the verified requirements.

---

## Implementation Summary Per Area

### Area 1: Dashboard debt calculation fix
**Problem**: `totalOwedToYou` in `DashboardViewModel.kt` summed ALL unpaid debts without filtering out the current user's own debts.

**Fix**: Added `it.profileId != currentUserUid` filter extracted as pure function `calculateTotalOwedToYou()`.

**Files**:
- `DashboardViewModel.kt` — added `profileId != currentUserUid` filter in `calculateTotalOwedToYou()`
- `DashboardViewModelTest.kt` — 5 tests (own debts, others' debts, mixed scenario)
- `DashboardAggregatesTest.kt` — 3 tests for `youAreOwedByEvent` (confirmed already correct)

**Key decision**: Used best-effort filter rather than adding `creditorId` field to model. Multi-person event edge case acknowledged but out of scope.

### Area 2: Profile avatar consistency
**Problem**: 3 components rendered avatars manually (initials-only boxes, text-only names) instead of using `ProfileAvatar` with `photoUrl` support.

**Fix**: Replaced manual rendering with `ProfileAvatar` composable:
- `PreviewBreakdown.kt` — initials Box → `ProfileAvatar`(32dp)
- `TransferListPanel.kt` — added `profiles: List<ProfileItem>` param; `ProfileAvatar`(24dp) in `TransferRow`/`BalanceRow`
- `CalculatorSheet.kt` — `"${icon} ${name}"` text → `Row(ProfileAvatar(...), name)`

**Files**:
- `PreviewBreakdown.kt` — ProfileAvatar(32dp)
- `TransferListPanel.kt` — profiles param + ProfileAvatar(24dp)
- `CalculatorSheet.kt` — ParameterInputRow with ProfileAvatar
- `ProfileAvatarConsistencyTest.kt` — 18 tests for avatar infrastructure

### Area 3: Calculation snapshot persistence
**Problem**: Transfer details (`CalculationSnapshot.transfers`) were lost after dismissing CalculatorSheet. Only per-profile net balances persisted.

**Fix**: 
- Extended `CalculationSnapshot.toJson()`/`toCalculationSnapshot()` to include `participantBalances`
- Changed `onApplyCalculation` to store `result.snapshot?.toJson()` in `EventItem.lastCalculationSummary`
- Redesigned `SettlementPanel` to accept `lastCalculationSummary: String?`, deserialize and render transfer details below participant list
- Total cost displayed as read-only `Text` (not `TextField`)

**Files**:
- `Models.kt` — extended JSON serialization with `participantBalances`
- `CuentaMorososApp.kt` — `onApplyCalculation` persists snapshot JSON
- `SettlementPanel.kt` — `lastCalculationSummary` param, transfer rendering, read-only total
- `EventDetailScreen.kt` — passes `lastCalculationSummary` to `SettlementPanel`
- `CalculationSnapshotPersistenceTest.kt` — 15 tests (roundtrip + null safety)
- `SettlementPanelPersistenceTest.kt` — 11 tests (data transformation logic)

### Area 4: Events screen cleanup
**Problem**: `BalanceSummaryCard` rendered on events list screen — user considered it unnecessary. Orphan derived states remained after removal.

**Fix**: Removed `BalanceSummaryCard(...)` call and 3 orphan `derivedStateOf` values. Kept `BalanceSummaryCard.kt` file (0 consumers).

**Files**:
- `EventsScreen.kt` — removed BalanceSummaryCard call + `totalPending`/`activeEventCount`/`owedEventCount` derived states
- `EventsScreenTest.kt` — 10 tests for EventsScreen filters (open events, con deuda filter)

---

## Files Changed (Final List)

### Production Code (9 files)
| File | Action | Area |
|------|--------|------|
| `shared/.../ui/DashboardViewModel.kt` | Modified | 1 |
| `shared/.../ui/PreviewBreakdown.kt` | Modified | 2 |
| `shared/.../ui/TransferListPanel.kt` | Modified | 2 |
| `shared/.../ui/CalculatorSheet.kt` | Modified | 2 |
| `shared/.../model/Models.kt` | Modified | 3 |
| `shared/.../ui/CuentaMorososApp.kt` | Modified | 3 |
| `shared/.../ui/SettlementPanel.kt` | Modified | 3 |
| `shared/.../ui/EventDetailScreen.kt` | Modified | 3 |
| `shared/.../ui/EventsScreen.kt` | Modified | 4 |

### Test Code (5 files)
| File | Action | Tests |
|------|--------|-------|
| `shared/.../DashboardViewModelTest.kt` | Created | 5 |
| `shared/.../DashboardAggregatesTest.kt` | Created | 3 |
| `shared/.../EventsScreenTest.kt` | Created | 10 |
| `shared/.../ProfileAvatarConsistencyTest.kt` | Created | 18 |
| `shared/.../CalculationSnapshotPersistenceTest.kt` | Created | 15 |
| `shared/.../SettlementPanelPersistenceTest.kt` | Created | 11 |

---

## Commits

| # | Hash | Message | Slice |
|---|------|---------|-------|
| 1 | `3c47be3` | fix(dashboard): corregir totalOwedToYou para excluir deudas propias | 1 |
| 2 | `7a2183b` | test(dashboard): agregar tests de exclusividad para youAreOwedByEvent | 1 |
| 3 | `272c275` | refactor(events): remover BalanceSummaryCard y derived states huérfanos | 1 |
| 4 | `217600d` | feat(avatars): reemplazar renderizado manual de avatars por ProfileAvatar en 3 componentes | 2 |
| 5 | `960b832` | test(snapshot): agregar tests de serialización roundtrip y null safety | 3 |
| 6 | `5cacc44` | feat(snapshot): incluir participantBalances en serialización JSON | 3 |
| 7 | `c8fc73b` | fix(snapshot): persistir snapshot.toJson() en onApplyCalculation | 3 |
| 8 | `3e4dd56` | feat(snapshot): rediseñar SettlementPanel con transferencias sugeridas | 3 |
| 9 | `71da444` | docs(sdd): actualizar apply-progress y tasks.md con Slice 3 completado | 3 |

---

## Known Caveats (from Verify Report)

### W001 — UI tests degraded to unit tests
`compose.ui.test` unavailable in KMP `commonTest`. Design specified Compose UI tests for PreviewBreakdown, SettlementPanel, and EventsScreen. All replaced with pure data transformation unit tests. UI composition and rendering behavior (Coil AsyncImage loading, layout correctness) cannot be verified in current test environment.

### W002 — Dead code: BalanceSummaryCard.kt
File retained at `shared/.../BalanceSummaryCard.kt` with zero consumers. Open question from design ("Should it be deleted entirely?") remains unresolved.

### S001 — Extracted helper cleaner than design
`calculateTotalOwedToYou` extracted as pure function (instead of inline filter). Better than design — independently testable and reusable.

### S002 — Shared predicate opportunity
Both `calculateTotalOwedToYou` and `computeProfileBreakdown` use same filter `!it.paid && it.profileId != currentUserUid`. Consider extracting shared predicate.

### S003 — BalanceSummaryCard.kt cleanup
Consider deleting the 92-line dead file in a future cleanup pass.

---

## Specs Synced

| Domain | Action | Requirements |
|--------|--------|-------------|
| `dashboard-debt-calculations` | Created | R001–R003 |
| `profile-avatar-consistency` | Created | R004–R006 |
| `calculation-snapshot-persistence` | Created | R007–R010 |
| `events-screen-cleanup` | Created | R011–R012 |

Plus R013 (no regression) — cross-domain, verified via full test suite.

---

## Archive Artifacts

| Artifact | Path | Status |
|----------|------|--------|
| Exploration | `openspec/changes/archive/2026-06-15-fix-dashboard-and-event-totals/explore.md` | ✅ |
| Design | `openspec/changes/archive/2026-06-15-fix-dashboard-and-event-totals/design.md` | ✅ |
| Apply Progress | `openspec/changes/archive/2026-06-15-fix-dashboard-and-event-totals/apply-progress.md` | ✅ |
| Verify Report | `openspec/changes/archive/2026-06-15-fix-dashboard-and-event-totals/verify-report.md` | ✅ |
| Archive Report | `openspec/changes/archive/2026-06-15-fix-dashboard-and-event-totals/archive.md` | ✅ |
| Main Specs | `openspec/specs/{4-domains}/spec.md` | ✅ |
