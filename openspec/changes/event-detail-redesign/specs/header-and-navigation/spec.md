# Header and Navigation Specification

## Purpose

The header area provides navigation back to the Events screen, displays event identity, and offers quick actions for adding expenses and profiles.

## Requirements

### Requirement: Back Navigation

The back button MUST invoke the onBack callback to return to the Events screen.

#### Scenario: Back button returns to Events screen

- GIVEN the user is on EventDetailScreen
- WHEN the user taps the back button ("Volver")
- THEN onBack callback is invoked

### Requirement: Event Identity Display

The header MUST display the event title in headlineMedium typography and the event date in bodySmall typography.

#### Scenario: Event title and date are displayed

- GIVEN an event named "Viaje Madrid" with date 2025-08-15
- WHEN the header renders
- THEN "Viaje Madrid" appears in headlineMedium and the formatted date in bodySmall

### Requirement: Add Expense Button

An "Add Expense" button MUST be present below the header, creating a new blank expense item when tapped.

#### Scenario: Add Expense opens editor

- GIVEN the user is on EventDetailScreen
- WHEN the user taps "Add Expense"
- THEN ExpenseEditDialog opens with a blank expense (empty name, 0.00 amount)

### Requirement: Add Profile Button

An "Add Profile" button MUST be present, opening the AddProfileDialog to add existing profiles to the event.

#### Scenario: Add Profile opens selector

- GIVEN the user is on EventDetailScreen
- WHEN the user taps "Add Profile"
- THEN AddProfileDialog opens showing profiles not yet in the event

### Requirement: Action Button Placement

On mobile, action buttons (Add Expense, Add Profile) MUST appear below the header row. On desktop, they MUST appear in the expense column below the header.

#### Scenario: Buttons below header on mobile

- GIVEN mobile layout (< 600dp)
- WHEN the screen renders
- THEN Add Expense and Add Profile buttons appear below the header and date

#### Scenario: Buttons in expense column on desktop

- GIVEN desktop layout (>= 600dp)
- WHEN the screen renders
- THEN action buttons appear in the left (expense) column below the full-width header
