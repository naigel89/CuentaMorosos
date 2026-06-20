# Tasks: fix-participants-and-calculation-flow

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~180–220 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | ask-on-risk |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: Low

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | All four fixes in one PR | Single PR | Independent but touch same call hierarchy; smaller to do together |

## Phase 1: Callback Signatures (Foundation)

- [x] 1.1 **CalculatorSheet**: Change `onApply: (CalculationResult) -> Unit` to `onApply: (modeId: String, result: CalculationResult) -> Unit` (line 78). Wire `selectedModeId` at call site (line 462).
- [x] 1.2 **EventDetailScreen**: Update `onApplyCalculation` param from `(CalculationResult) -> Unit` to `(String, CalculationResult) -> Unit` (line 86). Update lambda at line 344 to pass `selectedModeId`.
- [x] 1.3 **EventDetailScreen**: Update `onAddProfileToEvent` param from `(ProfileItem) -> Unit` to `(List<ProfileItem>) -> Unit` (line 80).

## Phase 2: Core Implementation

- [x] 2.1 **SettlementPanel**: Gate checkbox + row clickable with `eventState != EventState.OPEN` (lines 235, 246). Already has `eventState` param (line 68).
- [x] 2.2 **AddProfileToEventDialog** (EventDetailScreen, lines 636–679): Replace `OutlinedButton` list with `Checkbox` list + `selectedProfileIds: SnapshotStateList<String>` + single "Aceptar" calling `onAddProfileToEvent(selected)`.
- [x] 2.3 **CalculatorSheet**: Remove `showCelebration` / `hasCalculatedOnce` state (lines 100–102, 117, 155–158) and `MoneyExplosionAnimation` overlay (lines 499–504).
- [x] 2.4 **CuentaMorososApp**: Add `showCelebration: Boolean`, `scrollTrigger: Boolean`, `lastCalculationMode: String?` states. Wire `onApplyCalculation` to accept `(modeId, result)`, store mode in `EventItem.lastCalculationMode`, toggle scroll + celebration.
- [x] 2.5 **EventDetailScreen**: Add `scrollToSettlement: Boolean = false` param. Add `LaunchedEffect(scrollToSettlement)` to `scrollState.animateScrollTo(maxValue)`, then `delay(100ms)`, then signal celebration visible.
- [x] 2.6 **CuentaMorososApp**: Add `MoneyExplosionAnimation` overlay gated by `showCelebration` state (at app level, not sheet).
- [x] 2.7 **CuentaMorososApp**: Update `onAddProfileToEvent` callback to iterate `List<ProfileItem>` and batch-save each (lines 409–429).

## Phase 3: Testing

- [x] 3.1 **R007 — OPEN event hides checkboxes**: Write tests in new/existing test file for `EventState.OPEN` → no Checkbox rendered; `EventState.CALCULATED` → Checkbox rendered.
- [x] 3.2 **R008 batch-add**: Write test for `AddProfileToEventDialog`: 3 profiles, toggle 2, tap "Aceptar", assert `List(2)` emitted. Assert disabled button when 0 selected.
- [x] 3.3 **R008 scroll+animation**: Write test verifying scroll trigger toggles `LaunchedEffect` and `showCelebration` state transitions.
- [x] 3.4 **R009 mode persistence**: Write test asserting `onApply` passes `selectedModeId` and `saveEvent` stores it in `lastCalculationMode`.
