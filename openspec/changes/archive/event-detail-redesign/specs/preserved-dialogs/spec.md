# Preserved Dialogs Specification

## Purpose

All existing dialogs from the current EventDetailScreen MUST remain functional with unchanged behavior after the layout redesign.

## Requirements

### Requirement: Dialog Preservation

The following dialogs MUST be preserved with identical signatures, behavior, and validation logic:

| Dialog | Trigger | Purpose |
|--------|---------|---------|
| DebtEditDialog | Edit debt item | Edit amount, notes, calculation mode |
| ExpenseEditDialog | Add/edit expense | Create/edit with name, amount, category, assigned profiles |
| AddProfileDialog | Add Profile button | Add existing profile to event |
| QuickSplitDialog | Calculate Totals button | Calculation preview and apply |
| InviteMemberDialog | Invite member button | Invite by email |
| RemoveOwnerConfirm | Remove owner debt | Confirm removing owner from event |
| RemoveOwnerFromMembersConfirm | Owner removes self | Confirm removing owner from members list |

#### Scenario: DebtEditDialog opens with correct data

- GIVEN a debt item exists for participant "Ana" with amount 50.00 €
- WHEN the user taps edit on Ana's debt
- THEN DebtEditDialog opens with amount "50.00" and existing notes pre-filled

#### Scenario: ExpenseEditDialog validates input

- GIVEN the ExpenseEditDialog is open
- WHEN user saves with blank name
- THEN validation error "Indica un nombre para el ítem" is shown

#### Scenario: QuickSplitDialog calculates preview

- GIVEN 3 participants and total 90.00 €
- WHEN QuickSplitDialog opens in SIMPLE_AVG mode
- THEN preview shows 30.00 € per participant

#### Scenario: AddProfileDialog shows available profiles

- GIVEN 2 profiles exist that are not in the event
- WHEN AddProfileDialog opens
- THEN both profiles are listed as selectable options

#### Scenario: InviteMemberDialog validates email

- GIVEN InviteMemberDialog is open
- WHEN user enters "invalid-email" and taps invite
- THEN validation error "Introduce un email válido" is shown

#### Scenario: RemoveOwnerConfirm shows confirmation

- GIVEN the owner's debt is being removed
- WHEN the remove action is triggered
- THEN RemoveOwnerConfirm dialog appears with warning text

### Requirement: Dialog State Management

Dialog open/close state MUST be managed locally within EventDetailScreen using remember mutableStateOf, unchanged from current implementation.

#### Scenario: Dismissing dialog clears state

- GIVEN ExpenseEditDialog is open
- WHEN user taps cancel or dismisses
- THEN the editableExpense state is set to null and dialog closes
