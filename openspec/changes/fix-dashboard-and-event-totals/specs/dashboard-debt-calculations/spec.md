# Dashboard Debt Calculations

## Purpose
Accurate per-user debt totals on the dashboard. Fixes the "Te deben" total incorrectly summing ALL unpaid debts instead of only debts where the current user is the creditor.

## Requirements

### R001 — totalOwedToYou excludes own debts
The system MUST compute `totalOwedToYou` by summing only unpaid debts where `profileId != currentUserUid`. The current user's own debts (`profileId == currentUserUid`) MUST NOT contribute to this total.

#### Scenario: User has both creditor and debtor roles
- GIVEN current user is "Ana" (uid=ana1), debts: [A1: B→Ana unpaid €30, A2: Ana→Carlos unpaid €20]
- WHEN dashboard state is computed
- THEN `totalOwedToYou` = €30 AND `totalYouOwe` = €20

#### Scenario: All unpaid debts belong to others
- GIVEN current user is "Ana", debts: [B→Ana unpaid €50, C→Ana unpaid €25]
- WHEN dashboard state is computed
- THEN `totalOwedToYou` = €75 AND `totalYouOwe` = €0

### R002 — DashboardAggregates.youAreOwedByEvent consistency
`youAreOwedByEvent` in `DashboardAggregates` MUST only aggregate amounts from debts where `profileId != currentUserUid`. The `yourShareByEvent` computation MUST remain unchanged.

#### Scenario: Event with mixed debts
- GIVEN current user is "Ana", event E1 has debts: [Ana→B €10, C→Ana €15, D→Ana €5]
- WHEN aggregates are computed
- THEN `youAreOwedByEvent[E1]` = €20 AND `yourShareByEvent[E1]` = €10

### R003 — computeProfileBreakdown already correct
The `computeProfileBreakdown` method's `owedToYou` filter (`it.profileId != currentUserUid`) already works correctly. It MUST NOT be changed — only the summary totals at R001 and R002 need alignment.

#### Scenario: Breakdown matches corrected totals
- GIVEN current user "Ana", debts as in R001 scenario
- WHEN both `totalOwedToYou` and `owedToYouBreakdown` are computed
- THEN the sum of `owedToYouBreakdown` amounts equals `totalOwedToYou`

## Acceptance Criteria
- [ ] `totalOwedToYou` equals `debts.filter { !it.paid && it.profileId != currentUserUid }.sumOf { it.amountEuros }`
- [ ] `totalYouOwe` unchanged: `debts.filter { !it.paid && it.profileId == currentUserUid }.sumOf { it.amountEuros }`
- [ ] `youAreOwedByEvent` only aggregates `profileId != uid` debts
- [ ] `yourShareByEvent` unchanged
- [ ] `computeProfileBreakdown` unchanged
- [ ] Unit test: user's own debt excluded from "Te deben"
- [ ] Unit test: debts from multiple profiles correctly aggregated
