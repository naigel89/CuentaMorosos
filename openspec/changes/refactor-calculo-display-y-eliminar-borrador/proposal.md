# Proposal: Refactor Calculation Display & Remove DRAFT State

## Intent

Two problems need fixing:

1. **Wrong data in result display**: `PreviewBreakdown` in `CalculatorSheet` zips profiles with balance amounts using mismatched ordering (`LinkedHashMap` iteration ≠ profile list order), showing wrong amounts next to wrong names. `TransferListPanel` already shows correct data using profile-ID-keyed maps.
2. **Unnecessary DRAFT state**: The `DRAFT → OPEN` transition adds friction — users must press "Abrir evento" before operating on a newly created event. Events should be usable immediately upon creation.

## Scope

### In Scope
- Remove `PreviewBreakdown` from `CalculatorSheet`; `TransferListPanel` becomes the sole result display
- Enhance `TransferListPanel` visual design (typography hierarchy, card styling) to match quality of removed block
- Remove `EventState.DRAFT` from enum; default new events to `OPEN`
- Remove "Abrir evento" button, `openEvent()` ViewModel methods, and all `onOpenEvent` wiring
- Update persistence fallbacks in all 3 layers (LocalStore, SQLDelight, Firestore) from `DRAFT` to `OPEN`
- Fix pre-existing bug: add `state` field to `MigrationManager.toMigrationMap()`
- Update all affected tests (StateMachine, IntegrityGuard, EventValidator, EventsScreen, Dashboard)

### Out of Scope
- Changes to the calculation algorithm itself (`SettlementEngine`)
- New event states or state machine features
- Migration of existing Firestore documents via batch update (implicit migration via `valueOf` fallback is sufficient)
- UI redesign of screens beyond `CalculatorSheet` result area

## Capabilities

### New Capabilities
- `calculation-result-display`: How calculation results (total, transfers, balances) are displayed after computing — single unified display via TransferListPanel
- `event-lifecycle`: Event state machine (OPEN → CALCULATED → CLOSED), creation defaults, and state transitions

### Modified Capabilities
None — existing specs (profile-username, profile-custom-names, profile-security, profile-photo-upload, account-settings-ui) are unaffected.

## Approach

**Display refactor**: Delete `PreviewBreakdown` call from `CalculatorSheet` (line ~365). Keep `TransferListPanel` as-is for correctness; polish its visual design. Delete `PreviewBreakdown.kt` if no other call site exists (confirmed: only one usage).

**DRAFT removal**: Remove `DRAFT` from `EventState` enum. Change all defaults and fallbacks to `OPEN`. Remove `openEvent()` methods and "Abrir evento" button. Migration is implicit — `runCatching { EventState.valueOf(str) }.getOrDefault(OPEN)` in all persistence layers auto-converts stored `"DRAFT"` strings on load.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `shared/.../ui/CalculatorSheet.kt` | Modified | Remove PreviewBreakdown call |
| `shared/.../ui/PreviewBreakdown.kt` | Removed | Delete dead code |
| `shared/.../ui/TransferListPanel.kt` | Modified | Visual polish |
| `shared/.../model/Models.kt` | Modified | Remove DRAFT enum, isDraft(), update stateLabel() |
| `shared/.../model/StateMachine.kt` | Modified | Remove DRAFT transitions |
| `shared/.../ui/EventDetailScreen.kt` | Modified | Remove "Abrir evento" button |
| `shared/.../ui/EventDetailViewModel.kt` | Modified | Remove openEvent() |
| `shared/.../ui/EventsViewModel.kt` | Modified | Remove openEvent/openEventConfirmed |
| `shared/.../ui/CuentaMorososApp.kt` | Modified | Remove onOpenEvent wiring |
| `shared/.../ui/EventStateColors.kt` | Modified | Remove DRAFT color/label |
| `shared/.../ui/StateBadge.kt` | Modified | Remove DRAFT case |
| `shared/.../model/validation/EventValidator.kt` | Modified | Remove isDraft logic |
| `shared/.../ui/SettlementPanel.kt` | Modified | Default param → OPEN |
| `app/.../data/CuentaMorososLocalStore.kt` | Modified | Fallback → OPEN |
| `shared/.../data/repository/OfflineFirstEventRepository.kt` | Modified | Fallback → OPEN |
| `shared/.../data/repository/FirestoreEventRepository.kt` | Modified | Fallback → OPEN |
| `app/.../data/MigrationManager.kt` | Modified | Add state to migration map |
| Test files (6+) | Modified | Remove DRAFT tests, add OPEN-default tests |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Stored "DRAFT" strings in Firestore/SQLite after enum removal | High (expected) | `valueOf` + `getOrDefault(OPEN)` handles this implicitly; verified in all 3 persistence layers |
| Ordinal-based serialization of EventState | Low | Grep confirmed no ordinal usage; enum uses `name` everywhere |
| User confusion seeing DRAFT events as OPEN | Low | Desired behavior per user request; no action needed |
| MigrationManager missing state field causes state loss on sync | Med (pre-existing) | Fix as part of this change |

## Rollback Plan

1. Revert `EventState` enum to include `DRAFT` — all persistence fallbacks revert to `DRAFT`
2. Restore `PreviewBreakdown.kt` and its call in `CalculatorSheet`
3. Restore "Abrir evento" button and `openEvent()` methods
4. No data migration rollback needed (implicit migration is reversible by re-adding the enum value)

## Dependencies

None — all changes are internal to the app.

## Success Criteria

- [ ] `CalculatorSheet` shows only `TransferListPanel` after calculation (no `PreviewBreakdown`)
- [ ] Displayed amounts are correct (profile-to-amount mapping verified)
- [ ] `EventState.DRAFT` does not exist in the codebase
- [ ] New events are created with `state = OPEN` by default
- [ ] No "Abrir evento" button exists in `EventDetailScreen`
- [ ] Loading a persisted `"DRAFT"` string results in `OPEN` state
- [ ] `MigrationManager.toMigrationMap()` includes `state` field
- [ ] All existing tests pass; DRAFT-specific tests removed or updated
- [ ] New tests cover: OPEN default on creation, DRAFT→OPEN fallback on load
