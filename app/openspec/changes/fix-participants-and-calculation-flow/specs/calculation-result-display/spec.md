# Delta for Calculation Result Display

## ADDED Requirements

### Requirement: Auto-scroll to SettlementPanel on calculation apply (R008)

When calculation is applied in `CalculatorSheet`, after the sheet is dismissed, the main screen MUST auto-scroll to the `SettlementPanel` where calculation results are displayed. A `MoneyExplosionAnimation` MUST play 100ms after the scroll completes (or 100ms after sheet dismiss if scroll is instant).

#### Scenario: Calculation applied — screen scrolls to settlement panel

- GIVEN an event in OPEN state
- WHEN calculation is applied in CalculatorSheet
- THEN the sheet is dismissed
- AND the main screen scrolls to SettlementPanel

#### Scenario: Animation plays after scroll with 100ms delay

- GIVEN calculation was just applied
- WHEN scroll to SettlementPanel completes
- THEN 100ms later MoneyExplosionAnimation MUST play

#### Scenario: Event still in OPEN — no auto-scroll triggered

- GIVEN an event in OPEN state with no calculation applied
- WHEN the user navigates through the screen
- THEN no auto-scroll to SettlementPanel MUST occur

### Requirement: Calculation mode persistence (R009)

`EventItem.lastCalculationMode` MUST be set to the `SplitMode.id` string selected in the CalculatorSheet when calculation is applied. The `onApply` callback MUST carry the selected mode ID so the caller can save it. The `ReceiptPanel` MUST display this value (not null/"—").

#### Scenario: REAL_CONSUMPTION mode selected — stored and displayed in receipt

- GIVEN the user selects REAL_CONSUMPTION mode in CalculatorSheet
- WHEN calculation is applied
- THEN `EventItem.lastCalculationMode` equals "REAL_CONSUMPTION"
- AND ReceiptPanel displays "REAL_CONSUMPTION"

#### Scenario: CUSTOM_PERCENTAGE mode selected — stored and displayed in receipt

- GIVEN the user selects CUSTOM_PERCENTAGE mode in CalculatorSheet
- WHEN calculation is applied
- THEN `EventItem.lastCalculationMode` equals "CUSTOM_PERCENTAGE"
- AND ReceiptPanel displays "CUSTOM_PERCENTAGE"

#### Scenario: Receipt panel shows "—" only when no calculation has been done

- GIVEN a newly created event with no calculation applied
- WHEN ReceiptPanel renders
- THEN it MUST display "—" for the calculation mode field
