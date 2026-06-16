# Design: Refactor Calculation Display & Remove DRAFT State

## Technical Approach

Two independent changes bundled in one change set. **Area 1** removes a buggy duplicate display (`PreviewBreakdown`) from `CalculatorSheet`, leaving `TransferListPanel` as the sole result view with visual polish. **Area 2** collapses the event state machine from 4 states to 3 (`OPEN → CALCULATED → CLOSED`) by removing `DRAFT`, defaulting creation to `OPEN`, and relying on implicit migration via `valueOf` + `getOrDefault(OPEN)`.

Both areas are decoupled — Area 1 touches only UI composables, Area 2 touches model + data + UI wiring. They can be implemented in either order but Area 1 is simpler and should go first.

## Architecture Decisions

| Decision | Option A | Option B | Choice | Rationale |
|----------|----------|----------|--------|-----------|
| Display strategy | Remove PreviewBreakdown, keep TransferListPanel | Fix zip bug, keep both | **A** | User wants single unified display; TransferListPanel already correct via ID-keyed maps |
| TransferListPanel polish | Add card shadow + border to match PreviewBreakdown quality | Leave as-is | **A** | PreviewBreakdown had shadow + border; TransferListPanel currently lacks them — visual regression if not added |
| DRAFT removal | Remove enum value entirely | Soft-deprecate (keep but unused) | **A** | Clean state machine, no dead code; implicit migration via `valueOf` fallback is safe |
| Migration approach | Implicit (`getOrDefault(OPEN)`) | Explicit batch migration script | **A** | All 3 persistence layers already use `runCatching { valueOf }.getOrDefault` — just change default. No batch needed |
| `onOpenEvent` naming | Remove only DRAFT-transition usage | Rename all `onOpenEvent` to `onNavigateToEvent` | **A** | CalendarScreen/EventsScreen `onOpenEvent` = navigation (keep). EventDetailScreen `onOpenEvent` = state transition (remove). Renaming is out of scope |

## Data Flow

### Area 1: Calculation Result Display (after change)

```
CalculatorSheet.runCalculation()
  → SettlementEngine.calculate() → CalculationSnapshot
  → TransferListPanel(snapshot, status, profileNameResolver, profiles, ...)
    ├── StatusBanner (status)
    ├── Total expense header
    ├── Transfer rows (ID-keyed → correct names)
    └── Per-profile balances (ID-keyed → correct names)
```

PreviewBreakdown call removed from line 365. File `PreviewBreakdown.kt` deleted (single call site confirmed).

### Area 2: Event Lifecycle (after change)

```
[Creation] → OPEN → CALCULATED → CLOSED
                ↑        │
                └────────┘ (recalculate)
```

Persistence load path (all 3 layers):
```
stored "DRAFT" string → EventState.valueOf("DRAFT") → throws
  → runCatching catches → getOrDefault(EventState.OPEN) → OPEN ✓
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `shared/.../ui/CalculatorSheet.kt` | Modify | Remove `PreviewBreakdown(...)` call (lines 363-369) |
| `shared/.../ui/PreviewBreakdown.kt` | **Delete** | Dead code — single call site removed |
| `shared/.../ui/TransferListPanel.kt` | Modify | Add card shadow + border to match removed PreviewBreakdown quality |
| `shared/.../model/Models.kt` | Modify | Remove `DRAFT` from enum, remove `isDraft()`, update `stateLabel()`, change default `state = OPEN` (line 87) |
| `shared/.../model/StateMachine.kt` | Modify | Remove `DRAFT→OPEN` guard + `CLOSED→DRAFT` case + `guardDraftToOpen()` function |
| `shared/.../model/validation/EventValidator.kt` | Modify | Remove `isDraft` flag (line 22), make EV-05 always error, make EV-06 always active |
| `shared/.../ui/EventDetailScreen.kt` | Modify | Remove "Abrir evento" button (lines 488-504), remove `onOpenEvent` param from `HeaderSection` and `EventDetailScreen` |
| `shared/.../ui/EventDetailViewModel.kt` | Modify | Remove `openEvent()` method (lines 154-170) |
| `shared/.../ui/EventsViewModel.kt` | Modify | Remove `openEvent()` + `openEventConfirmed()` methods (lines 92-116) |
| `shared/.../ui/CuentaMorososApp.kt` | Modify | Remove DRAFT-transition `onOpenEvent` wiring (lines 482-491). Keep navigation `onOpenEvent` at lines 583, 685 |
| `shared/.../ui/EventStateColors.kt` | Modify | Remove `DRAFT` entries (lines 7, 16) |
| `shared/.../ui/StateBadge.kt` | Modify | Remove `DRAFT` cases (lines 21, 32) |
| `shared/.../ui/SettlementPanel.kt` | Modify | Change default param from `DRAFT` to `OPEN` (line 64) |
| `app/.../data/CuentaMorososLocalStore.kt` | Modify | Change `getOrDefault(DRAFT)` → `getOrDefault(OPEN)` (line 47) |
| `shared/.../data/repository/OfflineFirstEventRepository.kt` | Modify | Change `getOrDefault(DRAFT)` → `getOrDefault(OPEN)` (line 264) |
| `shared/.../data/repository/FirestoreEventRepository.kt` | Modify | Change `getOrDefault(DRAFT)` → `getOrDefault(OPEN)` (lines 260, 278) |
| `app/.../data/MigrationManager.kt` | Modify | Add `"state" to state.name` to `toMigrationMap()` (pre-existing bug fix) |
| `shared/.../ui/EventsScreen.kt` | Modify | Change event creation default to `state = EventState.OPEN` (line 152) |
| Test: `StateMachineTest.kt` | Modify | Remove DRAFT→OPEN tests, CLOSED→DRAFT test, `isDraft` test, `stateLabel` DRAFT case. Add: OPEN default on creation |
| Test: `IntegrityGuardTest.kt` | Modify | Remove `canDeleteExpense blocks when DRAFT` test |
| Test: `EventValidatorTest.kt` | Modify | Remove DRAFT-specific tests (EV-05 draft warning, EV-06 draft skip) |
| Test: `EventsScreenTest.kt` | Modify | Update any test creating DRAFT events to use OPEN |
| Test: `DashboardAggregatesTest.kt` | Modify | Update if any test uses DRAFT state |

## Interfaces / Contracts

No new interfaces. One enum value removed:

```kotlin
// BEFORE
enum class EventState { DRAFT, OPEN, CALCULATED, CLOSED }

// AFTER
enum class EventState { OPEN, CALCULATED, CLOSED }
```

TransferListPanel visual enhancement — add to outer `Card`:
```kotlin
modifier = modifier
    .fillMaxWidth()
    .shadow(
        elevation = NeoFintechElevation.cardShadowElevation,
        shape = NeoFintechElevation.cardShadowShape,
        clip = false,
    ),
border = BorderStroke(1.dp, themeColors.outline),
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | `EventState.valueOf("DRAFT")` throws; `getOrDefault(OPEN)` returns OPEN | Add to StateMachineTest |
| Unit | Event creation defaults to `OPEN` | Add assertion in existing creation tests |
| Unit | StateMachine: all DRAFT transitions removed, 3-state machine works | Update existing StateMachineTest |
| Unit | EventValidator: EV-05 always error for <2 members, EV-06 always active | Update EventValidatorTest |
| Unit | MigrationManager includes `state` in migration map | Add assertion |
| Compile | No `EventState.DRAFT` references remain | `./gradlew compileDebugKotlin` |

## Migration / Rollout

**No explicit migration needed.** All 3 persistence layers use `runCatching { EventState.valueOf(str) }.getOrDefault(...)`. Changing the default from `DRAFT` to `OPEN` means:
- Persisted `"DRAFT"` strings → `valueOf` throws → caught → `OPEN` returned
- This is automatic on first load after deploy

**Rollback**: Re-add `DRAFT` to enum, revert defaults to `DRAFT`. Events that were auto-migrated to `OPEN` will remain `OPEN` (acceptable — they were DRAFT with no functional difference).

## Open Questions

- [ ] None — all technical questions resolved during exploration
