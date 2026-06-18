# Enriched Expense Items Specification

## Purpose

Expense items display enriched visual information including category icons with colored circles, split type badges, monospaced amounts, and edit/delete interactions.

## Requirements

### Requirement: Category Icon Display

Each expense item MUST display a category icon inside a colored circle. Color mapping by category:

| Category | Color Role |
|----------|-----------|
| vuelo (flight) | tertiary |
| alojamiento (accommodation) | secondary |
| comida (food) | error |
| other/shared | outline |

#### Scenario: Flight expense shows tertiary-colored icon

- GIVEN an expense with category "vuelo"
- WHEN the expense card renders
- THEN a circle with tertiary color contains the flight icon

#### Scenario: Food expense shows error-colored icon

- GIVEN an expense with category "comida"
- WHEN the expense card renders
- THEN a circle with error color contains the food icon

#### Scenario: Unknown category shows outline icon

- GIVEN an expense with category "" or unrecognized value
- WHEN the expense card renders
- THEN an outlined circle displays a default icon

### Requirement: Expense Item Content

Each expense item MUST display: expense title, paid-by profile name, split type badge, amount (right-aligned, JetBrains Mono font), and date below amount.

#### Scenario: Equal split badge displays participant count

- GIVEN an expense split equally among 4 participants
- WHEN the expense card renders
- THEN a pill badge with surface-container background shows "Split equally (4)"

#### Scenario: Unequal split badge displays generic label

- GIVEN an expense with custom percentage weights
- WHEN the expense card renders
- THEN a pill badge with surface-container background shows "Unequal split"

#### Scenario: Amount uses monospaced font

- GIVEN an expense of 42.50 €
- WHEN the expense card renders
- THEN the amount "42.50 €" is displayed in JetBrains Mono font, right-aligned

#### Scenario: Paid-by profile name is shown

- GIVEN an expense paid by profile "Ana"
- WHEN the expense card renders
- THEN "Paid by Ana" (or equivalent) appears below the expense title

### Requirement: Edit and Delete Interactions

Tapping an expense item MUST open the edit dialog. A swipe gesture or menu action MUST trigger delete with confirmation.

#### Scenario: Tap opens edit dialog

- GIVEN an expense card is displayed
- WHEN the user taps the card
- THEN ExpenseEditDialog opens with the expense data pre-filled

#### Scenario: Swipe or menu triggers delete

- GIVEN an expense card is displayed
- WHEN the user performs a swipe-delete or selects delete from a menu
- THEN a confirmation prompt appears before removing the expense
