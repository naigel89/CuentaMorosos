# Proposal: fix-participants-and-calculation-flow

## Intent

Fix four UI/data-flow bugs in the event participation and calculation flow: paid checkboxes visible in OPEN state, missing auto-scroll + animation coordination, no batch profile addition, and null calculation mode in receipts.

## Scope

### In Scope
1. **Paid checkbox visibility**: Only show payment checkboxes in CALCULATED/CLOSED states
2. **Auto-scroll + animation**: Scroll to SettlementPanel, then 0.1s delay → MoneyExplosionAnimation
3. **Batch profile addition**: Multi-select checkboxes + single "Aceptar" button in AddProfileToEventDialog
4. **Calculation mode persistence**: Wire `selectedModeId` through `onApply` and store in `EventItem.lastCalculationMode`

### Out of Scope
- Redesign of SettlementPanel layout or structure
- New animation types or celebration effects
- Multi-event batch operations
- Profile creation or editing flow changes

## Capabilities

### New Capabilities
None — all changes modify existing specs.

### Modified Capabilities
- `event-lifecycle`: Add requirement that payment checkboxes only render in CALCULATED/CLOSED states. Add batch-add profiles via multi-select.
- `calculation-result-display`: Add post-calculation flow — scroll-to-animation coordination and `lastCalculationMode` persistence.

## Approach

Four independent fixes, one TDD change:

1. **Paid checkbox**: In `SettlementPanel`, gate `hasDebt` with `eventState != EventState.OPEN` before rendering Checkbox.
2. **Scroll + animation**: Hoist celebration trigger to `CuentaMorososApp`. On calculation apply → dismiss sheet, scroll to `SettlementPanel`, launch animation after 100ms delay. Use `LazyListState.animateScrollToItem()`.
3. **Batch add**: Replace `OutlinedButton` per profile with `Checkbox` list in `AddProfileToEventDialog`. Single "Aceptar" calls `onAddProfileToEvent` in batch.
4. **Calc mode**: Pass `selectedModeId` in `onApply` callback from `CalculatorSheet`. Store in `EventItem.lastCalculationMode` before saving.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `ui/SettlementPanel.kt` | Modified | Gate checkbox visibility by event state |
| `ui/CuentaMorososApp.kt` | Modified | Scroll + animation coordination, calc mode wiring |
| `ui/CalculatorSheet.kt` | Modified | Expose `selectedModeId` in `onApply` |
| `ui/EventDetailScreen.kt` | Modified | Batch multi-select in AddProfileToEventDialog |
| `ui/MoneyExplosionAnimation.kt` | Modified | Accept programmatic trigger (not sheet-only) |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Scroll target item index changes with data | Low | Use stable profile ID-based scroll, not hardcoded index |
| Animation overlaps with sheet dismiss | Low | 100ms delay between scroll and animation start |
| Batch add doesn't handle partially-selected then cancelled | Low | Clear selection state when dialog dismisses |

## Rollback Plan

Revert commit. Each fix is in a separate atomic commit, so individual issues can be rolled back independently.

## Dependencies

None — pure UI/data-flow changes within the existing module.

## Success Criteria

- [ ] Paid checkboxes absent in OPEN events, present in CALCULATED/CLOSED
- [ ] Applying calculation scrolls to settlement, animation plays 100ms later
- [ ] AddProfileToEventDialog supports multi-select checkboxes + single add
- [ ] Receipt modal shows the selected split mode (not null/—)
