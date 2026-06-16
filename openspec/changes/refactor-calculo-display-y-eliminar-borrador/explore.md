## Exploration: refactor-calculo-display-y-eliminar-borrador

### Current State

#### Area 1: Calculation Result Display

When the user presses "Calcular Totales" in the `SettlementPanel`, the `CalculatorSheet` modal bottom sheet opens. After pressing "Calcular" (the button inside the sheet), two result blocks appear sequentially:

1. **`PreviewBreakdown`** (top block) — "Vista previa por perfil"
   - File: `shared/.../ui/PreviewBreakdown.kt`
   - Receives `profiles` (the full list) and `amounts = snapshot.participantBalances.values.toList()`
   - **BUG**: It zips `profiles` (ordered by event participant order) with `snapshot.participantBalances.values` (ordered by Map iteration order, which is `LinkedHashMap` insertion order — based on balance computation order, NOT profile order). This mismatch causes wrong amounts to appear next to wrong profile names.
   - Shows only a per-profile amount + total, no transfer information.

2. **`TransferListPanel`** (bottom block) — Correct data
   - File: `shared/.../ui/TransferListPanel.kt`
   - Receives the full `CalculationSnapshot` and resolves profile IDs to names via `profileNameResolver`.
   - Shows: total expense, transfer rows (debtor → creditor: amount), and per-profile net balances with color-coded labels (Acreedor/Deudor/Saldado).
   - **Data is correct** because it uses profile IDs as keys, not positional zip.

**User's desired result order**:
1. TOTAL first
2. What each profile owes to each person (transfers, ordered)
3. Each person's balance, well classified

This matches exactly what `TransferListPanel` already shows (sections 2, 3, 4), but the user wants `PreviewBreakdown` removed and `TransferListPanel` to be the sole result display with improved visual design.

**Data flow**:
```
CalculatorSheet.runCalculation()
  → SettlementEngine.calculateWithEdgeCases(event, expenses, profileNameResolver)
    → SettlementEngine.calculate()
      → computes balances (payer contributions - debtor amounts per expense)
      → greedy minimum transfers algorithm
      → builds CalculationSnapshot(transfers, totalExpense, participantBalances)
    → post-checks (zero balance, zero transfers, deleted creditors, self-netting)
  → returns CalculationResult(snapshot, errors, status)

CalculatorSheet displays:
  → PreviewBreakdown(profiles, snapshot.participantBalances.values.toList(), total)  ← WRONG zip
  → TransferListPanel(snapshot, status, profileNameResolver, profiles, ...)  ← CORRECT
```

**Snapshot persistence** (on "Aplicar cálculo"):
- `CuentaMorososApp.kt` line 413-460: `onApplyCalculation` callback
  - Updates debts from `snapshot.participantBalances` (negative balance → debt amount)
  - Persists snapshot JSON via `event.lastCalculationSummary = result.snapshot?.toJson()`
  - Calls `eventDetailViewModel.calculateEvent(ctx)` to transition state to CALCULATED

#### Area 2: Event State — DRAFT Removal

**Current state machine** (4 states):
```
DRAFT → OPEN → CALCULATED → CLOSED
                ↓
              OPEN (recalculate)
```

**Where DRAFT appears**:

| File | Line(s) | Usage |
|------|---------|-------|
| `Models.kt` | 21-30 | `EventState.DRAFT` enum value + `isDraft()`, `stateLabel()` |
| `StateMachine.kt` | 39, 43, 53-69 | `DRAFT → OPEN` transition guard + `CLOSED → DRAFT` blocked |
| `EventItem` | 87 | Default: `state: EventState = EventState.DRAFT` |
| `EventsScreen.kt` | 152 | Event creation: `EventItem(...)` uses default DRAFT |
| `EventDetailScreen.kt` | 488-504 | "Abrir evento" button visible for DRAFT events |
| `EventDetailViewModel.kt` | 154-170 | `openEvent()` method — DRAFT → OPEN transition |
| `EventsViewModel.kt` | 92-116 | `openEvent()` + `openEventConfirmed()` |
| `CuentaMorososApp.kt` | 482-491, 583-601 | Wiring `onOpenEvent` callbacks |
| `EventStateColors.kt` | 7, 16 | Color + label for DRAFT |
| `StateBadge.kt` | 21, 32 | Badge styling for DRAFT |
| `EventValidator.kt` | 22, 59, 71 | `isDraft` flag for relaxed validation |
| `IntegrityGuard.kt` | 30 | `canDeleteExpense` only allows OPEN state |
| `PermissionEngine.kt` | 157 | Exception message references state |
| `CuentaMorososLocalStore.kt` | 46-47 | Default state when deserializing: DRAFT |
| `OfflineFirstEventRepository.kt` | 264 | Default state from SQLDelight: DRAFT |
| `FirestoreEventRepository.kt` | 260, 278 | Default + heuristic fallback: DRAFT |
| `MigrationManager.kt` | 90-114 | `toMigrationMap` does NOT include `state` field |
| `SettlementPanel.kt` | 64 | Default parameter: `eventState = EventState.DRAFT` |
| `CalendarScreen.kt` | 389 | Checks `event.state != EventState.CLOSED` |
| `DashboardViewModel.kt` | 62 | Checks `EventState.CALCULATED` for debt display |

**"Abrir evento" button**: `EventDetailScreen.kt` lines 488-504, inside `HeaderSection`, visible only when `event.state == EventState.DRAFT` and user is OWNER or CONTRIBUTOR.

**Event creation path**: `EventsScreen.kt` line 152 creates `EventItem(...)` with default `state = EventState.DRAFT`. No explicit state is set.

### Affected Areas

#### Area 1: Calculation Display
- `shared/.../ui/CalculatorSheet.kt` — Lines 357-389: Remove `PreviewBreakdown` call, keep only `TransferListPanel`
- `shared/.../ui/PreviewBreakdown.kt` — Entire file becomes dead code (can be deleted or kept if used elsewhere)
- `shared/.../ui/TransferListPanel.kt` — May need visual polish to match the "nice design" of the removed PreviewBreakdown

#### Area 2: DRAFT Removal
- `shared/.../model/Models.kt` — Remove `DRAFT` from `EventState` enum, change default to `OPEN`, remove `isDraft()`, update `stateLabel()`
- `shared/.../model/StateMachine.kt` — Remove `DRAFT → OPEN` guard, remove `CLOSED → DRAFT` case, update `attemptTransition`
- `shared/.../ui/EventsScreen.kt` — Set `state = EventState.OPEN` on event creation (line 152)
- `shared/.../ui/EventDetailScreen.kt` — Remove "Abrir evento" button (lines 488-504), remove `onOpenEvent` parameter
- `shared/.../ui/EventDetailViewModel.kt` — Remove `openEvent()` method (lines 154-170)
- `shared/.../ui/EventsViewModel.kt` — Remove `openEvent()` and `openEventConfirmed()` methods
- `shared/.../ui/CuentaMorososApp.kt` — Remove `onOpenEvent` wiring (lines 482-491, 583-601)
- `shared/.../ui/EventStateColors.kt` — Remove DRAFT color/label entries
- `shared/.../ui/StateBadge.kt` — Remove DRAFT case
- `shared/.../model/validation/EventValidator.kt` — Remove `isDraft` conditional logic (lines 22, 59, 71)
- `shared/.../ui/SettlementPanel.kt` — Change default parameter from `EventState.DRAFT` to `EventState.OPEN`
- `app/.../data/CuentaMorososLocalStore.kt` — Change default state fallback from DRAFT to OPEN
- `shared/.../data/repository/OfflineFirstEventRepository.kt` — Change default state fallback
- `shared/.../data/repository/FirestoreEventRepository.kt` — Change heuristic fallback from DRAFT to OPEN
- `app/.../data/MigrationManager.kt` — Add `state` field to migration map (currently missing!)

### Approaches

#### Area 1: Calculation Display

1. **Remove PreviewBreakdown, enhance TransferListPanel styling**
   - Pros: Single source of truth for display, correct data, simpler code
   - Cons: Need to improve TransferListPanel visual design to match user expectations
   - Effort: Low

2. **Fix PreviewBreakdown zip bug and keep both blocks**
   - Pros: Preserves current visual layout
   - Cons: Two blocks showing similar data is confusing, user explicitly wants one unified display
   - Effort: Low — but goes against user's request

**Recommendation**: Approach 1. Remove `PreviewBreakdown` from `CalculatorSheet`, keep `TransferListPanel` as the sole result display. Optionally enhance its visual design (card shadow, borders, typography hierarchy) to match the quality of the removed block.

#### Area 2: DRAFT Removal

1. **Remove DRAFT enum value entirely**
   - Pros: Clean state machine (3 states: OPEN → CALCULATED → CLOSED), no dead code
   - Cons: Breaking change for persisted data, requires migration of all DRAFT events
   - Effort: Medium

2. **Keep DRAFT enum but never use it (soft deprecation)**
   - Pros: No migration needed, backward compatible
   - Cons: Dead enum value, confusing for future developers, user explicitly wants it removed
   - Effort: Low — but doesn't fulfill the requirement

**Recommendation**: Approach 1. Remove `DRAFT` from the enum, change all defaults to `OPEN`, add a one-time migration to move existing DRAFT events to OPEN.

### Migration Strategy

**Local (SharedPreferences — CuentaMorososLocalStore)**:
- Change the default fallback in `loadEvents()` from `EventState.DRAFT` to `EventState.OPEN` (line 47)
- Events already persisted with `"state":"DRAFT"` will fail `EventState.valueOf("DRAFT")` after enum removal → `getOrDefault(EventState.OPEN)` handles this automatically

**SQLDelight (OfflineFirstEventRepository)**:
- Change default fallback from `EventState.DRAFT` to `EventState.OPEN` (line 264)
- Existing rows with `state = "DRAFT"` will fail `valueOf` → fallback to OPEN handles it

**Firestore (FirestoreEventRepository)**:
- Change heuristic fallback from `EventState.DRAFT` to `EventState.OPEN` (line 278)
- Change `getOrDefault(EventState.DRAFT)` to `getOrDefault(EventState.OPEN)` (line 260)
- Old documents with `state: "DRAFT"` string will fail `valueOf` → fallback to OPEN

**MigrationManager**:
- Currently does NOT include `state` in `toMigrationMap()` (line 90-114). This is a pre-existing bug — migrated events lose their state. Should add `"state" to state.name` to the map.

**No explicit batch migration needed**: The `valueOf` + `getOrDefault(OPEN)` pattern in all three persistence layers acts as an implicit migration. When a DRAFT event is loaded after the enum change, it automatically becomes OPEN.

### Test Coverage

#### Existing tests:
| Test File | Coverage |
|-----------|----------|
| `SettlementEngineTest.kt` | 20+ tests for calculation algorithm, edge cases, locking |
| `StateMachineTest.kt` | 20+ tests for all state transitions including DRAFT → OPEN |
| `SplitCalculatorTest.kt` | Tests for all split modes (EQUAL, PERCENTAGE, EXACT, PARTS) |
| `CalculationSnapshotPersistenceTest.kt` | Snapshot JSON serialization/deserialization |
| `EventMigrationTest.kt` | memberIds → participants migration |
| `IntegrityGuardTest.kt` | Includes DRAFT state in `canDeleteExpense` test |
| `EventValidatorTest.kt` | DRAFT-specific validation (EV-05, EV-06) |
| `SettlementPanelPersistenceTest.kt` | TransferListPanel data transformation |
| `EventsScreenTest.kt` | Event filtering by state (OPEN, CLOSED) |
| `DashboardAggregatesTest.kt` | Dashboard calculations with various states |
| `RoleUiGatingTest.kt` | SettlementPanel role gating |

#### Tests that need updating:
- `StateMachineTest.kt` — Remove all DRAFT → OPEN tests, update CLOSED → DRAFT tests, update `isDraft()` test, update `stateLabel()` test
- `IntegrityGuardTest.kt` — Remove `canDeleteExpense blocks when DRAFT` test
- `EventValidatorTest.kt` — Remove DRAFT-specific validation tests (EV-05 draft warning, EV-06 draft skip)
- `EventsScreenTest.kt` — Update any test using DRAFT state
- `DashboardAggregatesTest.kt` — Update if any test creates DRAFT events
- `DashboardViewModelNotificationTest.kt` — Update if any test uses DRAFT

#### Tests to add:
- Test that event creation defaults to OPEN
- Test that loading a persisted "DRAFT" string falls back to OPEN
- Test that TransferListPanel is the only result display in CalculatorSheet (UI test)

### Risks

1. **Enum removal is a binary-incompatible change**: If any module caches `EventState.DRAFT` as an ordinal, it will break. Mitigation: Grep for ordinal usage (none found in codebase).

2. **Firestore documents with `state: "DRAFT"`**: After enum removal, `EventState.valueOf("DRAFT")` throws → caught by `runCatching` → falls back to OPEN. This is safe but implicit. Mitigation: Add a comment documenting this behavior.

3. **PreviewBreakdown used elsewhere**: Need to verify no other composable calls `PreviewBreakdown`. Grep shows only `CalculatorSheet.kt` line 365 uses it. Safe to remove from that call site; the file itself can be deleted or left as unused.

4. **User confusion during migration**: Users who had DRAFT events will see them as OPEN without explanation. Mitigation: This is the desired behavior per the user's request.

5. **MigrationManager missing `state` field**: Pre-existing bug. Events migrated to Firestore via `MigrationManager.migrate()` don't include the `state` field, so Firestore heuristic determines state. Should fix as part of this change.

6. **EventValidator relaxed logic**: Currently DRAFT events get warnings instead of errors for < 2 participants. After removing DRAFT, all OPEN events will get errors. This is correct behavior — events should have 2+ participants from creation.

### Ready for Proposal

**Yes**. The codebase is well-understood, the changes are well-scoped, and the migration strategy is safe (implicit fallback via `valueOf` + `getOrDefault`). The orchestrator should proceed to proposal with two capabilities:

1. **Refactor calculation display**: Remove `PreviewBreakdown` from `CalculatorSheet`, keep `TransferListPanel` as sole result display, optionally enhance its design.
2. **Remove DRAFT state**: Remove `EventState.DRAFT`, default to `OPEN`, remove "Abrir evento" button and associated ViewModel methods, update all persistence fallbacks.
