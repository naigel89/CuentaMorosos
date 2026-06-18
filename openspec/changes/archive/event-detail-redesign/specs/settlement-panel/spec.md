# Settlement Panel Specification

## Purpose

The settlement panel provides a dedicated view of total event cost, calculation access, and participant debt status with toggle-to-settle interactions.

## Requirements

### Requirement: Total Event Cost Card

The panel MUST display a "Total Event Cost" card with the sum of all event expenses shown prominently.

#### Scenario: Total reflects all expenses

- GIVEN an event with expenses totaling 150.00 €
- WHEN the settlement panel renders
- THEN the Total Event Cost card displays "150.00 €" prominently

#### Scenario: Total updates when expenses change

- GIVEN total is 150.00 €
- WHEN a new expense of 30.00 € is added
- THEN the Total Event Cost card updates to 180.00 €

### Requirement: Calculate Totals Button

The panel MUST include a "Calculate Totals" button that opens the QuickSplitDialog calculator.

#### Scenario: Button opens calculator

- GIVEN the settlement panel is visible
- WHEN the user taps "Calculate Totals"
- THEN QuickSplitDialog opens with current event expenses and participants

#### Scenario: Button hidden when no debts exist

- GIVEN an event with zero participants/debts
- WHEN the settlement panel renders
- THEN the "Calculate Totals" button is not displayed

### Requirement: Participant Debt List

The panel MUST list each participant with: checkbox, avatar (initials) + name, debt amount, and status label.

#### Scenario: Participant shows pending status

- GIVEN participant "Carlos" has debt.paid = false and amount 25.00 €
- WHEN the participant row renders
- THEN checkbox is unchecked, avatar shows "CA", name "Carlos", amount "25.00 €", status "Pending"

#### Scenario: Participant shows settled status

- GIVEN participant "Carlos" has debt.paid = true
- WHEN the participant row renders
- THEN checkbox is checked and status shows "Settled"

### Requirement: Debt Amount Color Coding

Debt amounts MUST display in neon green for normal debts and in red for significant debts exceeding a defined threshold.

#### Scenario: Normal debt shows green

- GIVEN a debt of 25.00 € (below threshold)
- WHEN the amount renders
- THEN the text color is neon green

#### Scenario: Significant debt shows red

- GIVEN a debt of 200.00 € (above threshold of 100.00 €)
- WHEN the amount renders
- THEN the text color is red (error color)

### Requirement: Toggle Paid State

Tapping a participant's checkbox MUST toggle the debt.paid field via the onTogglePaid callback.

#### Scenario: Check marks debt as paid

- GIVEN participant debt.paid = false
- WHEN user taps the checkbox
- THEN onTogglePaid is called with the debt item and debt.paid becomes true

#### Scenario: Uncheck marks debt as pending

- GIVEN participant debt.paid = true
- WHEN user taps the checkbox
- THEN onTogglePaid is called with the debt item and debt.paid becomes false
