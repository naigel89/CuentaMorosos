# Events Screen Cleanup

## Purpose
Remove the unnecessary `BalanceSummaryCard` from `EventsScreen` and delete orphaned derived state computations that have no remaining consumers.

## Requirements

### R011 — Remove BalanceSummaryCard
`EventsScreen` MUST NOT render `BalanceSummaryCard`. The card call at the top of the content column MUST be removed.

#### Scenario: Events screen renders without balance card
- GIVEN user navigates to the Events tab
- WHEN EventsScreen renders
- THEN the screen starts directly with the header row (title + create button), no balance summary card above it

### R012 — Remove orphan derived states
Derived states `totalPending`, `activeEventCount`, and `owedEventCount` in `EventsScreen` MUST be removed if they have no remaining consumers after `BalanceSummaryCard` removal.

#### Scenario: Derived states have no other consumers
- GIVEN `totalPending`, `activeEventCount`, `owedEventCount` are only consumed by `BalanceSummaryCard`
- WHEN `BalanceSummaryCard` is removed
- THEN all three `derivedStateOf` blocks are removed from EventsScreen

#### Scenario: Derived state has external consumer (defensive check)
- GIVEN any of the three derived states is also consumed outside `BalanceSummaryCard`
- WHEN evaluating removal
- THEN only `BalanceSummaryCard` and its exclusive derived states are removed; shared states are preserved

### R013 — No functional regression
Removing `BalanceSummaryCard` MUST NOT affect: event filtering, event grid rendering, search, create/edit/delete event dialogs, or transition warning dialogs.

#### Scenario: Full event lifecycle after cleanup
- GIVEN EventsScreen without BalanceSummaryCard
- WHEN user creates event, searches, filters, edits, and deletes events
- THEN all operations function identically to before the removal

## Acceptance Criteria
- [ ] `BalanceSummaryCard(...)` call removed from `EventsScreen.kt`
- [ ] `totalPending` derived state removed (no other consumers)
- [ ] `activeEventCount` derived state removed (no other consumers)
- [ ] `owedEventCount` derived state removed (no other consumers)
- [ ] `BalanceSummaryCard.kt` file may remain (no other consumers found) or be deleted
- [ ] All existing EventsScreen functionality preserves behavior
- [ ] Compilation succeeds without unused import warnings from removed code
