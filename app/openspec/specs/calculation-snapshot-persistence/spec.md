# Spec: calculation-snapshot-persistence

## Purpose
Persist `CalculationSnapshot` data (including `participantBalances` and transfer details) as JSON in `EventItem.lastCalculationSummary`, and render transfer suggestions in `SettlementPanel`.

## Requirements

### Requirement: participantBalances in serialized JSON
`CalculationSnapshot.toJson()` MUST include `participantBalances` in its JSON output. `toCalculationSnapshot()` MUST deserialize `participantBalances` with an `emptyMap()` fallback for backward compatibility.

#### Scenario: Roundtrip preserves participantBalances
- **GIVEN** a `CalculationSnapshot` with `transfers`, `totalExpense`, `calculatedAtMillis`, `algorithmVersion`, and `participantBalances`
- **WHEN** the snapshot is serialized via `toJson()` and deserialized via `toCalculationSnapshot()`
- **THEN** all fields including `participantBalances` MUST match the original values

#### Scenario: Null safety — null, empty, malformed, missing fields
- **GIVEN** a null, empty string, malformed JSON, or JSON missing `participantBalances`
- **WHEN** `toCalculationSnapshot()` is called
- **THEN** it MUST return `null` for null/empty/malformed input and an empty map for `participantBalances` when the field is absent

#### Scenario: Backward compat — old summary with no participantBalances
- **GIVEN** an old `lastCalculationSummary` JSON that does not contain `participantBalances`
- **WHEN** `toCalculationSnapshot()` deserializes it
- **THEN** `participantBalances` MUST default to an empty map

### Requirement: SettlementPanel shows transfers from lastCalculationSummary
`SettlementPanel` MUST accept a `lastCalculationSummary: String?` parameter. When non-null and valid JSON, it SHALL deserialize the snapshot and render transfer details below the participant list. When null or invalid, it SHALL fall back to per-profile balances only (no transfer details).

#### Scenario: groupTransfersByDebtor aggregates transfers per debtor
- **GIVEN** a valid `lastCalculationSummary` with transfers where debtor A owes creditor B 10€ and creditor C 5€
- **WHEN** `SettlementPanel` processes the snapshot
- **THEN** debtor A MUST be shown with a total of 15€ across creditors B and C

#### Scenario: Null summary produces empty transfer list
- **GIVEN** `lastCalculationSummary` is null
- **WHEN** `SettlementPanel` renders
- **THEN** no transfer rows MUST be shown; only per-profile balances are displayed

### Requirement: Total cost read-only (Text, not TextField)
When `SettlementPanel` renders the total expense from a snapshot, it MUST display it as read-only `Text` ("Coste total: ..."), not as an editable `TextField`.

#### Scenario: totalExpense from snapshot is accessible for read-only display
- **GIVEN** a valid `lastCalculationSummary` with `totalExpense = 150.0`
- **WHEN** `SettlementPanel` renders
- **THEN** "Coste total: 150,00 €" MUST be displayed as non-editable text

### Requirement: Transfer format — "{debtor} debe {total}€ ({amount} a {creditor})"
Transfer details in `SettlementPanel` MUST use the format: `"{debtor} debe {total}€ ({amount_to_c1} a {c1}, {amount_to_c2} a {c2})"` with comma as decimal separator.

#### Scenario: formatDebtorTransfers produces correct Spanish text for multi-creditor
- **GIVEN** debtor "Alberto" with transfers: 10€ to "Luis", 5€ to "María"
- **WHEN** transfer text is formatted
- **THEN** the text MUST be "Alberto debe 15,00€ (10,00€ a Luis, 5,00€ a María)"
