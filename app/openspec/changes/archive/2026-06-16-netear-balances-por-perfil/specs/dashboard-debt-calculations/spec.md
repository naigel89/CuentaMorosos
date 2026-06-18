# Delta for dashboard-debt-calculations

## ADDED Requirements

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
