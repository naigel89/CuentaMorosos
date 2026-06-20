# Design: fix-participants-and-calculation-flow

## Technical Approach

Four independent fixes, each targeting a specific data-flow gap in the event participation and calculation flow. No model changes, no migrations — pure UI state wiring and callback signature adjustments.

## Architecture Decisions

### Decision: Gate checkbox by event state, not debt existence

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Filter debt list by state upstream | Couples data filtering to UI state concerns | ❌ |
| Gate Checkbox render with `eventState != EventState.OPEN` | Minimal change, local to composable, easy to test | ✅ |

`SettlementPanel.kt` line 246: change `if (hasDebt)` → `if (hasDebt && eventState != EventState.OPEN)`. Also gate the Row's `clickable` modifier (line 234) with the same condition. The Checkbox is never rendered in OPEN state because profiles have `amountEuros = 0.0` debts that shouldn't be togglable.

### Decision: Hoist celebration + scroll to CuentaMorososApp

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Keep animation inside CalculatorSheet | Animation plays inside sheet before it dismisses — invisible on main screen | ❌ |
| Hoist `showCelebration` + `scrollToSettlement` to CuentaMorososApp, pass down to EventDetailScreen | Animation visible on main screen, scroll targets SettlementPanel, clean separation | ✅ |

Flow: `onApplyCalculation` → set `showCelebration = true` → `LaunchedEffect` in EventDetailScreen calls `scrollState.animateScrollTo(scrollState.maxValue)` → `delay(100ms)` → MoneyExplosionAnimation renders as overlay at CuentaMorososApp level.

### Decision: Multi-select via `selectedProfileIds: Set<String>` in dialog state

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Keep individual OutlinedButtons | One tap per profile, no batch UX | ❌ |
| Checkbox list + single "Aceptar" | Familiar UX pattern, minimal dialog state | ✅ |

Change `onAddProfileToEvent` callback from `(ProfileItem) -> Unit` to `(List<ProfileItem>) -> Unit`. Internalize iteration in the existing callback body.

### Decision: Pass `selectedModeId` through `onApply` callback

| Option | Tradeoff | Decision |
|--------|----------|----------|
| Extract modeId from CalculationResult | CalculationResult has no mode field; adding it couples domain to UI | ❌ |
| Change `onApply` to `(String, CalculationResult) -> Unit` | Explicit, minimal interface change | ✅ |

The `selectedModeId` (string like `"real_consumption"`) is already available in CalculatorSheet. Thread it through to `CuentaMorososApp` where `lastCalculationMode` is finally persisted.

## Data Flow

```
CalculatorSheet                         CuentaMorososApp
  │                                          │
  │ onApply(modeId, result)                  │
  └──────────────────────────────────────►   │
                                             │ showCelebration = true
                                             │ events.saveEvent(lastCalculationMode=modeId)
                                             │
                              ┌──────────────┴──────────────┐
                              │ EventDetailScreen           │
                              │  LaunchedEffect: scroll max │
                              │  delay(100)                 │
                              │  → celebrationVisible = true│
                              └──────────────┬──────────────┘
                                             │
                              MoneyExplosionAnimation (overlay)
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `shared/.../ui/SettlementPanel.kt` | Modify | Gate checkbox + row clickable with `eventState != EventState.OPEN` (lines 234, 246) |
| `shared/.../ui/CalculatorSheet.kt` | Modify | Change `onApply` to `(String, CalculationResult) -> Unit`; remove MoneyExplosionAnimation overlay (lines 78, 459-464, 499-504) |
| `shared/.../ui/EventDetailScreen.kt` | Modify | Batch multi-select in AddProfileToEventDialog (lines 638-679); thread scroll trigger + modeId through callbacks; accept `scrollToEndTrigger: Boolean` |
| `shared/.../ui/CuentaMorososApp.kt` | Modify | Add `showCelebration` state, wire `lastCalculationMode` using passed modeId, add scroll trigger. Add MoneyExplosionAnimation overlay (lines 409-498) |

## Interfaces / Contracts

```kotlin
// Issue 3: callback change (EventDetailScreen + CuentaMorososApp)
// Before:
onAddProfileToEvent: (ProfileItem) -> Unit
// After:
onAddProfileToEvent: (List<ProfileItem>) -> Unit

// Issue 4: callback change (CalculatorSheet → EventDetailScreen → CuentaMorososApp)
// Before:
onApply: (CalculationResult) -> Unit
// After:
onApply: (modeId: String, result: CalculationResult) -> Unit

// Issue 2: new scroll trigger param for EventDetailScreen
scrollToSettlement: Boolean = false  // toggled in CuentaMorososApp after calculation
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | SettlementPanel — checkbox hidden in OPEN state | Pass `eventState = EventState.OPEN`, assert Checkbox not rendered |
| Unit | AddProfileToEventDialog — multi-select tracks selected profiles | Render with 3 profiles, toggle 2, tap "Aceptar", assert `List(2)` emitted |
| Unit | CuentaMorososApp — `lastCalculationMode` persisted after apply | Mock callback, assert `events.saveEvent` called with correct mode |
| Compose | CalculatorSheet — `onApply` passes `selectedModeId` | Call `onApply` with mode, verify second arg equals `selectedModeId` |

## Migration / Rollout

No migration required. Each fix is independently revertible.

## Open Questions

None.
