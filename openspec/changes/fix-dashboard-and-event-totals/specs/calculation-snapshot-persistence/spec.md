# Calculation Snapshot Persistence

## Purpose
When a calculation is applied to an event, the `CalculationSnapshot` (transfers + balances) MUST be persisted as JSON in `EventItem.lastCalculationSummary`, enabling SettlementPanel to display transfer details permanently.

## Requirements

### R007 — Persist CalculationSnapshot on apply
When the user applies a calculation result, the system MUST serialize `CalculationSnapshot.toJson()` into `EventItem.lastCalculationSummary`. The current behavior of storing only the status message MUST be replaced.

#### Scenario: Apply successful calculation
- GIVEN CalculatorSheet produces `CalculationResult` with snapshot containing transfers [{Ana→Carlos: €25}]
- WHEN user taps "Aplicar cálculo"
- THEN `EventItem.lastCalculationSummary` contains JSON with `"transfers":[{"from":"ana1","to":"carlos1","amount":25.0}]`

#### Scenario: Apply with no transfers (zero balance)
- GIVEN calculation produces empty transfers and totalExpense = 0
- WHEN user applies calculation
- THEN `lastCalculationSummary` stores valid JSON with empty transfers array AND totalExpense = 0

### R008 — SettlementPanel shows persisted transfers
After a calculation is applied, `SettlementPanel` MUST read `EventItem.lastCalculationSummary`, deserialize via `String.toCalculationSnapshot()`, and render transfer details (who→whom + amounts) BELOW the per-profile balance section. The display MUST survive navigation away and back.

#### Scenario: Viewing settlement after applying calculation
- GIVEN event has `lastCalculationSummary` with transfers [{Pepe→Luis: €13}, {Pepe→Ana: €10}]
- WHEN user opens the event detail
- THEN SettlementPanel shows "Pepe → Luis: 13,00 €" and "Pepe → Ana: 10,00 €"

#### Scenario: Event without prior calculation (null summary)
- GIVEN event has `lastCalculationSummary = null`
- WHEN user opens event detail
- THEN SettlementPanel only shows per-profile balances (no transfer list)

### R009 — Total event cost display is read-only
The total cost in the settlement display MUST be read-only — not editable. It MUST reflect the value from the persisted snapshot's `totalExpense`.

#### Scenario: Total cost from snapshot
- GIVEN snapshot totalExpense = €150.00
- WHEN SettlementPanel renders
- THEN total cost shows "150,00 €" as a non-editable text label

### R010 — Transfer details format
Transfer details in SettlementPanel MUST follow the format: `"{debtorName} debe {total}€ ({amount1} a {creditor1}, {amount2} a {creditor2})"`.

#### Scenario: Multi-creditor display
- GIVEN Pepe owes €23 total (€13 to Luis, €10 to Ana)
- WHEN SettlementPanel renders Pepe's row
- THEN text reads "Pepe debe 23€ (13 a Luis, 10 a Ana)" or equivalent localized format

## Acceptance Criteria
- [ ] `onApplyCalculation` serializes `snapshot.toJson()` into `lastCalculationSummary`
- [ ] `SettlementPanel` deserializes `lastCalculationSummary` and renders transfers
- [ ] Transfer list survives navigation away and back (reads from persisted data)
- [ ] Total cost label is read-only Text (not OutlinedTextField)
- [ ] Transfer format matches specified pattern
- [ ] Backward compatible: null `lastCalculationSummary` shows current behavior only
- [ ] Unit test: persisted snapshot round-trips correctly (serialize + deserialize)
