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

### Requirement: Profile debt netting in unified breakdown

`buildUnifiedBreakdown()` MUST net profiles that appear in both `owedToYou` and `youOwe` into a single `UnifiedDebtItem`. Net amount = sum(owedToYou) − sum(youOwe). Direction MUST be determined by net sign: positive → "te debe", negative → "le debes". Profiles with net = 0 MUST be excluded.

#### Scenario: Profile in both sides nets to single row
- GIVEN Profile A owes user 80€ AND user owes Profile A 20€
- WHEN `buildUnifiedBreakdown()` is computed
- THEN Profile A appears once with net 60€ and direction "te debe"

#### Scenario: Negative net reverses direction
- GIVEN Profile A owes user 20€ AND user owes Profile A 80€
- WHEN `buildUnifiedBreakdown()` is computed
- THEN Profile A appears once with net −60€ and direction "le debes"

#### Scenario: Zero net removes profile
- GIVEN Profile A owes user 50€ AND user owes Profile A 50€
- WHEN `buildUnifiedBreakdown()` is computed
- THEN Profile A does NOT appear in the unified list

#### Scenario: Single-direction profile unchanged
- GIVEN Profile A owes user 30€ AND has no debts owed by the user
- WHEN `buildUnifiedBreakdown()` is computed
- THEN Profile A appears as a single row with 30€ "te debe" (no change from current behavior)

#### Scenario: Multiple events in same direction
- GIVEN Profile A has two events in owedToYou (50€, 30€) and one event in youOwe (20€)
- WHEN `buildUnifiedBreakdown()` computes net
- THEN Profile A appears once with net 60€ and the events list contains [+50€, +30€, −20€]

### Requirement: Negative amounts for youOwe events in breakdown

Events originating from the `youOwe` side MUST carry a negative `amount` in the merged events list of the netted `UnifiedDebtItem`. Events from `owedToYou` MUST carry a positive amount. Non-netted profiles MUST retain their original sign (always positive).

#### Scenario: Merged events preserve direction via sign
- GIVEN Profile A has one 80€ event in owedToYou and one 20€ event in youOwe
- WHEN `buildUnifiedBreakdown()` merges events
- THEN the events list contains one event with +80€ and one with −20€

#### Scenario: Single-direction events keep positive sign
- GIVEN Profile A has two events in owedToYou (10€, 15€) and no youOwe events
- WHEN `buildUnifiedBreakdown()` merges events
- THEN both events remain positive (+10€, +15€) — no sign change

### Requirement: Event sign-based coloring in breakdown dialog

`EventBreakdownDialog` MUST render events with negative amount in red with label "Le debes {abs}€" and events with non-negative amount in green with label "Te debe {abs}€". The dialog total MUST show the algebraic sum. Display values SHALL use `kotlin.math.abs()` for the amount and `roundTo2()` for formatting.

#### Scenario: Mixed signs render correctly
- GIVEN a breakdown dialog with events of +80€ and −20€
- WHEN the dialog renders
- THEN +80€ shows green "Te debe 80,00€", −20€ shows red "Le debes 20,00€", and total shows 60,00€

#### Scenario: All-youOwe renders all red
- GIVEN a breakdown dialog with events of −30€ and −10€ (user owes in both)
- WHEN the dialog renders
- THEN both rows show red "Le debes" and total shows −40,00€
