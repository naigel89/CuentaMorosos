# Spec: dashboard-debt-calculations

## Purpose
Define how dashboard aggregates (`totalOwedToYou`, `youAreOwedByEvent`, `computeProfileBreakdown`) classify debts between the current user and other participants.

## Requirements

### Requirement: totalOwedToYou excludes own debts
`totalOwedToYou` MUST sum ONLY unpaid debts where `profileId != currentUserUid`. Own debts (where `profileId == currentUserUid`) MUST be excluded, as they belong to `totalYouOwe`.

**Rationale**: The previous implementation summed all unpaid debts without checking whether the debt belonged to the current user, causing `totalOwedToYou` to incorrectly double-count the user's own debts.

#### Scenario: totalOwedToYou excludes current user own unpaid debts
- **GIVEN** a user with 3 debts: 2 from other profiles (10€, 20€) and 1 own debt (5€)
- **WHEN** `totalOwedToYou` is computed
- **THEN** the result MUST be 30€ (excluding the 5€ own debt)

#### Scenario: totalOwedToYou sums only other profiles unpaid debts
- **GIVEN** a user with debts from 3 different profiles (10€, 15€, 20€), none belonging to the current user
- **WHEN** `totalOwedToYou` is computed
- **THEN** the result MUST be 45€

#### Scenario: Mixed scenario — excludes own + paid
- **GIVEN** a user with unpaid debts from others (10€, 20€), an unpaid own debt (5€), and a paid debt from another profile (15€)
- **WHEN** `totalOwedToYou` is computed
- **THEN** the result MUST be 30€ (own debt excluded, paid debt excluded)

### Requirement: youAreOwedByEvent aggregation
`DashboardAggregates.youAreOwedByEvent` SHALL classify debts where `profileId != currentUserUid` as owed TO the user. Own debts (`profileId == currentUserUid`) SHALL go to `yourShareByEvent`, not `youAreOwedByEvent`.

#### Scenario: Current user own debts go to yourShareByEvent
- **GIVEN** an event with 2 debts: one from the current user (20€) and one from another profile (30€)
- **WHEN** `DashboardAggregates` is computed
- **THEN** the 20€ debt MUST appear in `yourShareByEvent` and NOT in `youAreOwedByEvent`

#### Scenario: Other profiles debts go to youAreOwedByEvent
- **GIVEN** an event with 2 debts: one from profile A (20€) and one from profile B (30€), neither being the current user
- **WHEN** `DashboardAggregates` is computed
- **THEN** both debts MUST appear in `youAreOwedByEvent` and NOT in `yourShareByEvent`

#### Scenario: Paid debts excluded from both
- **GIVEN** an event with 1 unpaid debt from another profile (20€) and 1 paid debt from another profile (30€)
- **WHEN** `DashboardAggregates` is computed
- **THEN** only the 20€ unpaid debt MUST appear in `youAreOwedByEvent`; the 30€ paid debt MUST NOT appear in either `yourShareByEvent` or `youAreOwedByEvent`

### Requirement: computeProfileBreakdown filter unchanged
`computeProfileBreakdown()` in `DashboardViewModel` SHALL continue to use `it.profileId != currentUserUid` filter, which was already correct. No change required.

#### Scenario: Separates owed-to-you from you-owe correctly
- **GIVEN** a user with multiple debts across different profiles
- **WHEN** `computeProfileBreakdown` is called
- **THEN** debts where `profileId != currentUserUid` appear as "owed to you" and debts where `profileId == currentUserUid` appear as "you owe"
