# Spec: events-screen-cleanup

## Purpose
Define the removal of the `BalanceSummaryCard` and associated orphan derived states from the Events screen.

## Requirements

### Requirement: BalanceSummaryCard removed from EventsScreen
The `EventsScreen` composable MUST NOT render `BalanceSummaryCard`. The card component file MAY be retained for potential future use.

#### Scenario: BalanceSummaryCard is not present in EventsScreen
- **GIVEN** the Events screen is rendered
- **WHEN** the screen composable tree is inspected
- **THEN** `BalanceSummaryCard` MUST NOT be present; the events list, search bar, filter chips, and dialogs MUST all function correctly

### Requirement: Orphan derived states removed
Any `derivedStateOf` values that were exclusively consumed by `BalanceSummaryCard` MUST be removed from `EventsScreen` to eliminate dead code.

#### Scenario: Derived states consumed only by BalanceSummaryCard are no longer present
- **GIVEN** the `EventsScreen.kt` source file
- **WHEN** the file is inspected
- **THEN** `totalPending`, `activeEventCount`, and `owedEventCount` derived states MUST NOT exist

### Requirement: No functional regression
All remaining EventsScreen functionality (event list rendering, search, filter by debt state, dialogs) MUST continue to work as before the removal.

#### Scenario: All open events returned with default filter
- **GIVEN** a list of events with various states
- **WHEN** the default filter (all open events) is applied
- **THEN** all open events MUST be returned

#### Scenario: Filter con deuda shows only events with pending total greater than zero
- **GIVEN** events with different pending debt totals
- **WHEN** the "con deuda" filter is applied
- **THEN** only events with `pendingTotal > 0` MUST be shown
