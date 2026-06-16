# Exploration: fix-dashboard-and-event-totals

## Issue 1: Dashboard "Te deben" indicator shows wrong total

### Root Cause
**File**: `shared/src/commonMain/kotlin/com/cuentamorosos/ui/DashboardViewModel.kt`, line 88-89

```kotlin
val totalOwedToYou = debts
    .filter { !it.paid }
    .sumOf { it.amountEuros }
```

This sums **ALL** unpaid debts — including both the current user's own debts (which belong in `totalYouOwe`) and debts between other participants that are NOT owed to the current user. The `totalYouOwe` on line 93 correctly filters by `it.profileId == currentUserUid`, but `totalOwedToYou` lacks the complementary filter `it.profileId != currentUserUid`.

**Secondary problem**: Even with the profile filter, the assumption that `profileId != uid` means "owed to you" is incorrect. In multi-person events, a debt between Alberto and Luis (neither being the current user Pepe) should NOT appear in Pepe's "Te deben". The `EventDebtItem` model doesn't store a `creditorId`, so we can't directly determine WHO a debt is owed to.

**Also affects**: `DashboardAggregates.youAreOwedByEvent` in `CuentaMorososApp.kt` (lines 238-244) — same assumption.

### Affected Files
- `shared/src/commonMain/kotlin/com/cuentamorosos/ui/DashboardViewModel.kt` — `computeState()`: `totalOwedToYou`, `computeProfileBreakdown()`
- `shared/src/commonMain/kotlin/com/cuentamorosos/ui/CuentaMorososApp.kt` — `DashboardAggregates.youAreOwedByEvent` (lines 238-244)
- `shared/src/commonMain/kotlin/com/cuentamorosos/model/Models.kt` — `EventDebtItem` (no creditor field)
- `shared/src/commonMain/kotlin/com/cuentamorosos/model/SettlementEngine.kt` — `SettlementTransfer` has `toProfileId` (the creditor)

### Proposed Approach
1. **Short-term fix**: Add `it.profileId != currentUserUid` filter to `totalOwedToYou`
2. **Proper fix**: Use `CalculationSnapshot.transfers` to determine which debts are actually owed TO the current user, or add a `creditorId` field to `EventDebtItem` when applying calculations

---

## Issue 2: Event detail — broken profile photos + debt breakdown

### 2a: Profile photos broken in calculation preview

**Root Cause**: `PreviewBreakdown.kt` (lines 84-105) renders avatars using only initials drawn on a colored circle. It does NOT use `ProfileAvatar` component nor pass `photoUrl`. Similarly:
- `TransferListPanel.kt` uses `profileNameResolver` to get names as strings — no photos at all
- `CalculatorSheet.kt` `ParameterInputRow` (line 502) uses `${profile.icon} ${profile.name}` as text, no photo

**Affected Files**:
- `shared/src/commonMain/kotlin/com/cuentamorosos/ui/PreviewBreakdown.kt` — replace initials-only avatar with `ProfileAvatar(photoUrl=)`
- `shared/src/commonMain/kotlin/com/cuentamorosos/ui/TransferListPanel.kt` — `TransferRow` and `BalanceRow` use only names, should add avatars
- `shared/src/commonMain/kotlin/com/cuentamorosos/ui/CalculatorSheet.kt` — `ParameterInputRow` uses text only

**Proposed Approach**: Replace initials/manual avatar rendering with `ProfileAvatar(name, emoji, photoUrl, size)` component which already supports Coil AsyncImage.

### 2b: Debt breakdown (who owes whom) not persisted after calculation

**Root Cause**: When calculation is applied (`onApplyCalculation` in `CuentaMorososApp.kt`, lines 412-460), only the net per-profile balance is stored as `EventDebtItem.amountEuros`. The detailed settlement transfers (`CalculationSnapshot.transfers`) showing "A → B: X€" are NOT persisted — only `totalExpense` and `status.message` are saved to the event.

After the CalculatorSheet is dismissed, the transfer details are gone. The SettlementPanel shows per-profile totals but not who owes whom.

**Affected Files**:
- `shared/src/commonMain/kotlin/com/cuentamorosos/ui/CuentaMorososApp.kt` — `onApplyCalculation` (lines 412-460)
- `shared/src/commonMain/kotlin/com/cuentamorosos/model/Models.kt` — `EventItem` has `lastCalculationSummary` but not the transfer list
- `shared/src/commonMain/kotlin/com/cuentamorosos/ui/SettlementPanel.kt` — only shows profile totals, not transfers

**Proposed Approach**: 
- Persist the `CalculationSnapshot` (transfers + participantBalances) on the event or as a separate entity
- Show transfer details in a post-calculation summary that's always visible (not just in the CalculatorSheet modal)
- The total event amount should remain display-only (currently it's in `TotalCostCard` which is read-only)

---

## Issue 3: Events screen — remove "Balance total" row

### Root Cause
`EventsScreen.kt` line 163-168 renders `BalanceSummaryCard` at the top of the events list, showing "Balance total" and "X activos". The user considers this unnecessary on the events list screen.

### Affected Files
- `shared/src/commonMain/kotlin/com/cuentamorosos/ui/EventsScreen.kt` — line 163-168, call to `BalanceSummaryCard`
- `shared/src/commonMain/kotlin/com/cuentamorosos/ui/BalanceSummaryCard.kt` — the component itself (may still be used elsewhere — check: only `EventsScreen` uses it)

### Proposed Approach
- Remove the `BalanceSummaryCard(...)` call from `EventsScreen.kt` lines 163-168
- Also remove unused derived states: `totalPending`, `activeEventCount`, `owedEventCount` (lines 113-121) if no longer used after removing the card
- Or keep them if they're still used by other UI elements on the screen

---

## Summary of Affected Files

| File | Issue # | Change |
|------|---------|--------|
| `DashboardViewModel.kt` | 1 | Fix `totalOwedToYou` calculation |
| `CuentaMorososApp.kt` | 1 | Fix `youAreOwedByEvent` aggregation |
| `PreviewBreakdown.kt` | 2a | Add ProfileAvatar with photoUrl |
| `TransferListPanel.kt` | 2a | Add avatars to transfer/balance rows |
| `CalculatorSheet.kt` | 2a | Add avatars to ParameterInputRow |
| `CuentaMorososApp.kt` | 2b | Persist snapshot transfers on apply |
| `SettlementPanel.kt` | 2b | Show transfer details post-calculation |
| `Models.kt` | 2b | Add transfer storage to EventItem (or new entity) |
| `EventsScreen.kt` | 3 | Remove BalanceSummaryCard call |

## Risks
- **Issue 1**: Adding `creditorId` to `EventDebtItem` is a data model change requiring migration
- **Issue 2b**: Persisting transfers as JSON in `EventItem` (similar to `CalculationVersion`) adds complexity but avoids schema changes
- **Issue 2a**: ProfileAvatar with AsyncImage may fail silently if Coil is not properly configured for the KMP target
- **Issue 3**: Low risk — pure UI removal
- All three issues are independent and can be implemented in parallel
