# Calculation Result Display Specification

## Purpose

Define the calculation result display in `CalculatorSheet`: remove the buggy `PreviewBreakdown` component and establish `TransferListPanel` as the sole result display, with a defined section order, correct data binding, and NeoFintech visual polish.

---

## ADDED Requirements

### Requirement: PreviewBreakdown component removed (R001)

The system MUST NOT render `PreviewBreakdown` inside `CalculatorSheet`. The `PreviewBreakdown.kt` file MUST be deleted. All imports and call sites referencing `PreviewBreakdown` MUST be removed from every source file.

**Rationale (the bug)**: `PreviewBreakdown` calls `profiles.zip(amounts)` where `amounts = snapshot.participantBalances.values.toList()`. `participantBalances` is a `Map<String, Double>`. Map `.values` iteration order depends on the internal implementation (e.g., `LinkedHashMap` preserves insertion order, but that insertion order is NOT guaranteed to match the `profiles` list order). The `zip` then pairs profile A with profile B's amount — silently showing wrong data to the user. This is a data-integrity bug, not a cosmetic one.

#### Scenario: CalculatorSheet shows no PreviewBreakdown after calculation

- GIVEN an event with profiles `[Ana (id=1), Bruno (id=2), Caro (id=3)]` and expenses totalling 150 €
- WHEN the user taps "Calcular" and the calculation succeeds
- THEN `PreviewBreakdown` MUST NOT appear anywhere in the result area
- AND `TransferListPanel` MUST be the only result component rendered

#### Scenario: PreviewBreakdown source file deleted and all references removed

- GIVEN the codebase after this change is applied
- WHEN the `ui/` package is inspected and the full project is searched
- THEN `PreviewBreakdown.kt` MUST NOT exist
- AND no import of `PreviewBreakdown` MUST remain in any source file
- AND `CalculatorSheet.kt` MUST NOT contain any reference to `PreviewBreakdown`

---

### Requirement: TransferListPanel as sole result display (R002)

`CalculatorSheet` MUST render `TransferListPanel` as the only calculation result component when `calculationResult.snapshot` is non-null. `TransferListPanel` SHALL receive:

| Parameter | Type | Purpose |
|-----------|------|---------|
| `snapshot` | `CalculationSnapshot` | Transfers, totalExpense, participantBalances |
| `status` | `CalculationStatus` | Success / ZeroBalance / EdgeCaseWarning / Error |
| `profileNameResolver` | `(String) -> String` | Resolves profileId → display name |
| `profiles` | `List<Profile>` | Full profile list for avatar/lookup |
| `paidTransferIndices` | `Set<Int>` | Indices of transfers marked as paid |
| `onTogglePaid` | `(Int) -> Unit` | Callback to toggle paid state on a transfer |

#### Scenario: Result area renders TransferListPanel on successful calculation

- GIVEN a calculation that produces a valid snapshot with 2 transfers
- WHEN the result is displayed in `CalculatorSheet`
- THEN `TransferListPanel` MUST be rendered with the snapshot data
- AND no other result component MUST appear alongside it

#### Scenario: Null-snapshot error card still displayed

- GIVEN a calculation that returns `snapshot == null` with error messages
- WHEN the result is displayed
- THEN the error `Card` MUST be rendered with error text
- AND `TransferListPanel` MUST NOT be rendered

---

### Requirement: TransferListPanel section order and structure (R003)

`TransferListPanel` MUST display its sections in this exact top-to-bottom order:

1. **Status banner** — color-coded banner with icon + message
2. **Total expense** — "Total del evento" label + formatted amount
3. **Transfer rows** — "Transferencias sugeridas" header + list of transfer rows
4. **Per-profile balances** — "Saldos por perfil" header + list of balance rows

#### Scenario: Sections appear in defined order with 3 profiles and 2 transfers

- GIVEN profiles `[Ana (id=1), Bruno (id=2), Caro (id=3)]`
- AND `totalExpense = 150.00 €`
- AND transfers: `[Bruno → Ana: 30,00 €]`, `[Caro → Ana: 20,00 €]`
- AND balances: `{1: +50.00, 2: -30.00, 3: -20.00}`
- WHEN `TransferListPanel` renders
- THEN section 1 MUST show a green Success banner
- AND section 2 MUST show "Total del evento" with "150,00 €" in monospace
- AND section 3 MUST show header "Transferencias sugeridas" followed by 2 transfer rows:
  - Row 1: `[avatar] Bruno → [avatar] Ana  30,00 €`
  - Row 2: `[avatar] Caro → [avatar] Ana  20,00 €`
- AND section 4 MUST show header "Saldos por perfil" followed by 3 balance rows:
  - `[avatar] Ana (Acreedor)  50,00 €`
  - `[avatar] Bruno (Deudor)  30,00 €`
  - `[avatar] Caro (Deudor)  20,00 €`

#### Scenario: All balances zero — no transfers needed

- GIVEN profiles `[Ana (id=1), Bruno (id=2)]`
- AND `totalExpense = 0.00 €`
- AND `transfers = []` (empty list)
- AND `participantBalances = {1: 0.00, 2: 0.00}`
- WHEN `TransferListPanel` renders
- THEN status banner MUST show ZeroBalance state
- AND total expense MUST show "0,00 €"
- AND transfer section MUST display fallback text "No hay transferencias pendientes"
- AND per-profile balances MUST still render:
  - `[avatar] Ana (Saldado)  0,00 €`
  - `[avatar] Bruno (Saldado)  0,00 €`

#### Scenario: One creditor, multiple debtors

- GIVEN profiles `[Ana (id=1), Bruno (id=2), Caro (id=3), Diego (id=4)]`
- AND `totalExpense = 200.00 €`
- AND transfers: `[Bruno → Ana: 40,00 €]`, `[Caro → Ana: 35,00 €]`, `[Diego → Ana: 25,00 €]`
- AND balances: `{1: +100.00, 2: -40.00, 3: -35.00, 4: -25.00}`
- WHEN `TransferListPanel` renders
- THEN 3 transfer rows MUST appear under "Transferencias sugeridas"
- AND Ana MUST be labeled "Acreedor", all others "Deudor"

---

### Requirement: Correct data binding via profileId (R004)

All data binding in `TransferListPanel` MUST use `profileId` as the key to resolve profile information. The system MUST NOT rely on list index or map iteration order to associate profiles with amounts.

**This is the correct pattern** (used in balance rows and transfer rows):
- For balances: iterate `participantBalances` entries, use each entry's `profileId` key to look up the profile name via `profileNameResolver(profileId)` and the profile object via `profiles.find { it.id == profileId }`.
- For transfers: use `transfer.fromProfileId` and `transfer.toProfileId` to resolve names and avatars.

#### Scenario: Profile order in list differs from map insertion order

- GIVEN profiles list ordered `[Caro (id=3), Ana (id=1), Bruno (id=2)]`
- AND `participantBalances = {1: +50.00, 2: -30.00, 3: -20.00}` (insertion order: Ana, Bruno, Caro)
- WHEN balance rows render
- THEN Caro's row MUST show `-20,00 €` (her own balance, not Ana's +50.00)
- AND Ana's row MUST show `+50,00 €`
- AND Bruno's row MUST show `-30,00 €`

#### Scenario: ProfileId not found in profiles list

- GIVEN a `participantBalances` entry with `profileId = "unknown-id"` that does not match any profile
- WHEN the balance row for that entry renders
- THEN `profileNameResolver("unknown-id")` MUST be called and its return value used as the display name
- AND the avatar MUST fall back to a default/placeholder avatar
- AND the amount MUST still display correctly

---

### Requirement: TransferRow visual specification (R005)

Each transfer row MUST display:

| Element | Specification |
|---------|--------------|
| From avatar | 24dp, resolved from `profiles.find { it.id == transfer.fromProfileId }` |
| From name | Body text, resolved via `profileNameResolver(transfer.fromProfileId)` |
| Arrow | "→" separator between from and to |
| To avatar | 24dp, resolved from `profiles.find { it.id == transfer.toProfileId }` |
| To name | Body text, resolved via `profileNameResolver(transfer.toProfileId)` |
| Amount | `JetBrainsMonoFontFamily`, `MaterialTheme.colorScheme.primary`, formatted as "XX,XX €" |

**Paid state**: When a transfer's index is in `paidTransferIndices`:
- Entire row alpha: `0.5f`
- Checkmark icon displayed
- Amount color: `MaterialTheme.colorScheme.tertiary` (instead of primary)
- Name colors: muted

#### Scenario: Unpaid transfer row renders correctly

- GIVEN transfer `Bruno (id=2) → Ana (id=1): 30,00 €` at index 0, not in `paidTransferIndices`
- WHEN the transfer row renders
- THEN it MUST show `[avatar 24dp] Bruno → [avatar 24dp] Ana  30,00 €`
- AND amount MUST be in `JetBrainsMonoFontFamily` with `primary` color
- AND row alpha MUST be `1.0f`
- AND no checkmark icon MUST be visible

#### Scenario: Paid transfer row renders with visual feedback

- GIVEN the same transfer at index 0, now in `paidTransferIndices`
- WHEN the transfer row renders
- THEN row alpha MUST be `0.5f`
- AND a checkmark icon MUST be visible
- AND amount color MUST be `tertiary` instead of `primary`

#### Scenario: Toggle paid state on transfer row

- GIVEN an unpaid transfer row at index 0
- WHEN the user taps the row
- THEN `onTogglePaid(0)` MUST be called
- AND the row MUST re-render in paid state on next composition

---

### Requirement: BalanceRow visual specification (R006)

Each balance row MUST display:

| Element | Specification |
|---------|--------------|
| Avatar | 24dp, resolved from `profiles.find { it.id == profileId }` |
| Name | Body text, resolved via `profileNameResolver(profileId)` |
| Label | Classification tag in parentheses |
| Amount | `JetBrainsMonoFontFamily`, absolute value formatted as "XX,XX €" |

**Balance classification** (threshold: ±0.01):
- `balance > 0.01` → Label: "Acreedor", color: `MaterialTheme.colorScheme.tertiary` (green)
- `balance < -0.01` → Label: "Deudor", color: `MaterialTheme.colorScheme.error` (red)
- `-0.01 ≤ balance ≤ 0.01` → Label: "Saldado", color: neutral/secondary

Amount color MUST match the label color.

#### Scenario: Creditor balance row

- GIVEN profile Ana (id=1) with balance `+50.00`
- WHEN the balance row renders
- THEN label MUST be "(Acreedor)" in `tertiary` color
- AND amount MUST show "50,00 €" in `tertiary` color with `JetBrainsMonoFontFamily`

#### Scenario: Debtor balance row

- GIVEN profile Bruno (id=2) with balance `-30.00`
- WHEN the balance row renders
- THEN label MUST be "(Deudor)" in `error` color
- AND amount MUST show "30,00 €" in `error` color with `JetBrainsMonoFontFamily`

#### Scenario: Zero balance (within threshold)

- GIVEN profile Caro (id=3) with balance `0.005` (within ±0.01 threshold)
- WHEN the balance row renders
- THEN label MUST be "(Saldado)" in neutral color
- AND amount MUST show "0,01 €" or "0,00 €" in neutral color

#### Scenario: Balance exactly at threshold boundary

- GIVEN a profile with balance exactly `0.01`
- WHEN the balance row renders
- THEN label MUST be "(Saldado)" (since `0.01` is NOT `> 0.01`)

- GIVEN a profile with balance exactly `0.011`
- WHEN the balance row renders
- THEN label MUST be "(Acreedor)" (since `0.011 > 0.01`)

---

### Requirement: TransferListPanel NeoFintech visual polish (R007)

`TransferListPanel` SHOULD use NeoFintech design tokens consistently:

| Element | Token |
|---------|-------|
| Card shape | `NeoFintechShapes.lg` |
| Card elevation | `NeoFintechElevation.cardShadowElevation` |
| Monetary amounts font | `JetBrainsMonoFontFamily` |
| Creditor color | `MaterialTheme.colorScheme.tertiary` |
| Debtor color | `MaterialTheme.colorScheme.error` |
| Transfer amount color | `MaterialTheme.colorScheme.primary` |

#### Scenario: Card uses NeoFintech shape and shadow

- GIVEN `TransferListPanel` is rendered
- WHEN the card is inspected
- THEN the card shape MUST be `NeoFintechShapes.lg`
- AND the card MUST have a shadow using `NeoFintechElevation.cardShadowElevation`

#### Scenario: All monetary amounts use monospace font

- GIVEN any monetary value displayed in `TransferListPanel` (total, transfer amounts, balance amounts)
- WHEN the text style is inspected
- THEN the font family MUST be `JetBrainsMonoFontFamily`

---

## Data Model Reference

```kotlin
data class CalculationSnapshot(
    val transfers: List<SettlementTransfer>,
    val totalExpense: Double,
    val calculatedAtMillis: Long,
    val algorithmVersion: String = "v1-greedy",
    val participantBalances: Map<String, Double> = emptyMap(),
)

data class SettlementTransfer(
    val fromProfileId: String,
    val toProfileId: String,
    val amount: Double,
)
```
