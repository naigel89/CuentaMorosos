# Design: fix-dashboard-and-event-totals

## Technical Approach

Four independent fixes. Area 1 corrects `totalOwedToYou` filter in `DashboardViewModel.kt` (adds missing `profileId != currentUserUid`). Area 2 replaces manual avatar rendering with `ProfileAvatar` composable across 3 components. Area 3 persists `CalculationSnapshot` JSON into `EventItem.lastCalculationSummary` and renders transfers in `SettlementPanel`. Area 4 removes `BalanceSummaryCard` + orphan derived states from `EventsScreen`.

## Architecture Decisions

| Decision | Choice | Rejected | Rationale |
|----------|--------|----------|-----------|
| `totalOwedToYou` fix scope | Add filter only; no `creditorId` field | Model change with `creditorId` | Out of scope; filter is best-effort for multi-person events |
| Snapshot persistence format | Reuse existing `CalculationSnapshot.toJson()` extended with `participantBalances` | New entity/table | `lastCalculationSummary` is already `String?`; JSON is flexible and backward-compatible |
| `TransferListPanel` avatar data | Add `profiles: List<ProfileItem>` param | Change resolver to `(String)->ProfileItem?` | Simpler; avoids breaking existing `profileNameResolver` API |
| `BalanceSummaryCard` removal | Remove call + derived states; keep file | Delete entire file | Low risk; file is 92 lines, no dependency cost to keep |

## Data Flow

### Area 1: Dashboard totals
```
allDebts ──→ DashboardViewModel.computeState()
               ├── totalOwedToYou: filter(!paid && profileId != uid).sumOf(amountEuros)  ← FIXED
               └── totalYouOwe:   filter(!paid && profileId == uid).sumOf(amountEuros)   ← unchanged

allDebts ──→ CuentaMorososApp derivedStateOf { DashboardAggregates }
               └── youAreOwedByEvent: profileId != uid branch  ← already correct
```

### Area 3: Snapshot persistence
```
CalculatorSheet ──onApply──→ CuentaMorososApp.onApplyCalculation
                                ├── update debts from participantBalances
                                └── save: EventItem(lastCalculationSummary = snapshot.toJson())
                                                                    │
SettlementPanel ←── deserialize: lastCalculationSummary?.toCalculationSnapshot()
  ├── per-profile balances (existing)
  └── transfer details: "{debtor} debe {total}€ ({a}→{c1}, {b}→{c2})"  ← NEW
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `shared/.../ui/DashboardViewModel.kt:89` | Modify | Add `it.profileId != currentUserUid` to `totalOwedToYou` filter |
| `shared/.../ui/PreviewBreakdown.kt:84-105` | Modify | Replace initials Box with `ProfileAvatar(name, icon, photoUrl, 32.dp)` |
| `shared/.../ui/TransferListPanel.kt` | Modify | Add `profiles: List<ProfileItem>` param; render `ProfileAvatar`(24dp) in `TransferRow` and `BalanceRow` |
| `shared/.../ui/CalculatorSheet.kt:502` | Modify | Replace `"${profile.icon} ${profile.name}"` with `Row(ProfileAvatar(...), name)` |
| `shared/.../model/Models.kt:363-410` | Modify | Extend `CalculationSnapshot.toJson()`/`toCalculationSnapshot()` to include `participantBalances` |
| `shared/.../ui/CuentaMorososApp.kt:449` | Modify | Store `result.snapshot?.toJson()` instead of `result.status?.message` |
| `shared/.../ui/SettlementPanel.kt` | Modify | Accept `lastCalculationSummary: String?`; deserialize and render transfers below participant list |
| `shared/.../ui/EventsScreen.kt:113-121,163-168` | Modify | Remove `BalanceSummaryCard(...)` call + 3 derived states |

## Interfaces / Contracts

### `CalculationSnapshot.toJson()` — extended format
```kotlin
// ADD participantBalances serialization:
append("\"participantBalances\":{${participantBalances.entries.joinToString { "\"${it.key}\":${it.value}" }}}")
```

### `SettlementPanel` new signature
```kotlin
fun SettlementPanel(
    // ...existing params...
    lastCalculationSummary: String? = null,  // NEW: persisted snapshot JSON
)
```

### `TransferListPanel` new signature
```kotlin
fun TransferListPanel(
    // ...existing params...
    profiles: List<ProfileItem> = emptyList(),  // NEW: for ProfileAvatar resolution
)
```

## State Management

- `totalOwedToYou`: Computed inside `DashboardViewModel.computeState()` via reactive `combine()` flow. No `derivedStateOf` — just a local variable. Fix is a one-line filter addition.
- `youAreOwedByEvent`: Already inside `derivedStateOf` in `CuentaMorososApp.kt` with correct `profileId != uid` branch. No change.
- `SettlementPanel`: reads `lastCalculationSummary` from event (already in Compose state). Deserialization happens in `@Composable` via `remember(lastCalculationSummary)`. No additional state needed.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | `totalOwedToYou` excludes own debts | Extend `DashboardViewModelTest`: add multi-person scenario where user's own debts NOT in `totalOwedToYou` |
| Unit | `DashboardAggregates.youAreOwedByEvent` consistency | Extend `DashboardAggregatesTest`: verify user's own debts go to `yourShareByEvent`, not `youAreOwedByEvent` |
| Unit | `CalculationSnapshot.toJson()` roundtrip | New test: serialize → deserialize → assert transfers + participantBalances match |
| Unit | `toCalculationSnapshot()` null safety | Null/empty/malformed JSON → returns null |
| Compose | `ProfileAvatar` renders in `PreviewBreakdown` | Screenshot/golden test: verify avatar with/without `photoUrl` |
| Compose | `SettlementPanel` shows transfers from JSON | Test with mock `lastCalculationSummary`; verify transfer rows rendered |
| Compose | `EventsScreen` without `BalanceSummaryCard` | Verify card not present; grid/search/filter/dialogs still work |

## Migration & Backward Compatibility

- **Old events (null `lastCalculationSummary`)**: `SettlementPanel` falls back to per-profile balances only (no transfer detail). Natural `null` check — no migration needed.
- **Data model**: No new fields added. `lastCalculationSummary` is existing `String?`. Old status messages are not JSON — `toCalculationSnapshot()` returns `null` gracefully.
- **`BalanceSummaryCard.kt`**: File kept. Can be restored or deleted later.

## Open Questions

- [ ] Should `BalanceSummaryCard.kt` be deleted entirely (zero consumers after EventsScreen removal)?
