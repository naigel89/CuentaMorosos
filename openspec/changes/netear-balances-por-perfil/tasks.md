# Tasks: Netear balances por perfil

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~140–160 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | single-pr |
| Chain strategy | pending |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Low

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | All tasks | PR 1 | Single PR, ~140–160 lines, no chaining needed |

## Phase 1: RED — Failing tests for netting logic

- [ ] 1.1 Extract `buildUnifiedBreakdown()` to `internal` in `DebtBreakdownCalculator.kt` so it's directly testable (same pattern as `computeProfileBreakdown`)
- [ ] 1.2 Write failing test: dual-direction profile net positive → 1 unified item, net = owed - youOwe, direction OWED_TO_YOU, events from both sides with youOwe events negated
- [ ] 1.3 Write failing test: dual-direction profile net negative → direction YOU_OWE, amount = abs(owed - youOwe)
- [ ] 1.4 Write failing test: zero net (owed == youOwe) → profile excluded from result
- [ ] 1.5 Write failing test: single-direction only (owedToYou only) → unchanged behavior, 1 item
- [ ] 1.6 Write failing test: multiple events same direction → all events preserved, amounts summed

## Phase 2: GREEN — Implement netting in `buildUnifiedBreakdown()`

- [ ] 2.1 Implement `buildUnifiedBreakdown()`: group by `profileId`, compute net = sum(amount * sign), filter zero-net, assign direction by sign, merge events with youOwe amounts negated
- [ ] 2.2 Wire extracted function into ViewModel, remove old private impl
- [ ] 2.3 Verify all Phase 1 tests pass (GREEN)

## Phase 3: GREEN — Update `EventBreakdownDialog()` for mixed events

- [ ] 3.1 Replace single `accentColor`/`prefix` with per-event logic: `event.amount >= 0` → green `+` prefix (te debe), `event.amount < 0` → red `-` prefix (debes)
- [ ] 3.2 Use `kotlin.math.abs(event.amount)` for display value
- [ ] 3.3 Total row unchanged — uses `item.direction` and `item.amount` (already absolute)

## Phase 4: VERIFY — Manual UI check & regression

- [ ] 4.1 Run existing tests — all pass without modification
- [ ] 4.2 Manual visual: open dialog for a dual-direction profile → both green and red events, correct signs and totals

## Acceptance Criteria per Task

| ID | Criterion |
|----|-----------|
| T-01 | `buildUnifiedBreakdown()` produces 1 item per profile with net amount, zero-net profiles excluded |
| T-02 | Per-event sign/color correct for mixed-direction events; total row unchanged |
| T-03 | All 5 test scenarios pass (dual pos/neg, zero net, single-dir, multi-event) |
| T-04 | Dialog renders correct colors and signs; existing tests green |
