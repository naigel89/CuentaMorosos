# Proposal: fix-dashboard-and-event-totals

## Intent
Fix three bugs: (1) Dashboard "Te deben" shows wrong total by summing ALL unpaid debts, (2) Event detail missing profile photos in calculation views and debt breakdown lost after calculation, (3) Events screen shows unnecessary "Balance total" row.

## Scope

### In Scope
- Fix `totalOwedToYou` filter in DashboardViewModel.kt to exclude current user's own debts
- Fix `youAreOwedByEvent` aggregation in CuentaMorososApp.kt
- Replace manual initials-only avatars with ProfileAvatar in PreviewBreakdown, TransferListPanel, CalculatorSheet
- Persist CalculationSnapshot.transfers as JSON on event apply (reuse existing serialization in Models.kt)
- Show transfer details (who→whom) in SettlementPanel
- Remove BalanceSummaryCard from EventsScreen + orphan derived states
- Unit tests for corrected debt calculations

### Out of Scope
- Adding `creditorId` to EventDebtItem (data migration risk, deferred)
- Event-level debt tracking
- Full redesign of debt aggregation logic

## Capabilities

### New Capabilities
- `dashboard-debt-calculations`: Accurate per-user debt totals on dashboard
- `calculation-snapshot-persistence`: Settlement transfers stored with event, visible post-calculation
- `profile-avatar-consistency`: ProfileAvatar used in all profile display contexts

### Modified Capabilities
- None (no existing specs govern dashboard calculations or settlement panel behavior)

## Approach

### Issue 1 — Dashboard "Te deben" filter
- **Immediate fix**: Add `it.profileId != currentUserUid` filter to `totalOwedToYou` (DashboardViewModel.kt line 88-90)
- **Mirror fix**: Same filter in `youAreOwedByEvent` aggregation (CuentaMorososApp.kt line 242-244)
- **Deferred**: Use CalculationSnapshot.transfers for accurate creditor attribution

### Issue 2a — Profile photos
Replace initials-on-circle patterns with `ProfileAvatar(name, emoji, photoUrl, size)`:
- **PreviewBreakdown.kt**: Add avatar before profile name in each row
- **TransferListPanel.kt**: Add avatar to TransferRow and BalanceRow
- **CalculatorSheet.kt**: Add avatar in ParameterInputRow

### Issue 2b — Persist settlement transfers
- Serialize `CalculationSnapshot` via existing `toJson()` (Models.kt L355-404)
- Store snapshot JSON in `EventItem.lastCalculationSummary` (existing nullable String field)
- SettlementPanel reads snapshot JSON, renders transfer list (who→whom + amounts)
- No migration needed — `lastCalculationSummary` is nullable, old events show current behavior

### Issue 3 — Remove balance row
- Delete `BalanceSummaryCard(...)` call from EventsScreen.kt line 163-168
- Delete orphan derived states (`totalPending`, `activeEventCount`, `owedEventCount`) if unused

## Affected Areas
| File | Impact | Description |
|------|--------|-------------|
| `shared/.../ui/DashboardViewModel.kt` | Modified | Fix `totalOwedToYou` + `computeProfileBreakdown` |
| `shared/.../ui/CuentaMorososApp.kt` | Modified | Fix `youAreOwedByEvent` + persist snapshot on apply |
| `shared/.../ui/PreviewBreakdown.kt` | Modified | ProfileAvatar instead of initials |
| `shared/.../ui/TransferListPanel.kt` | Modified | Avatars in TransferRow/BalanceRow |
| `shared/.../ui/CalculatorSheet.kt` | Modified | Avatars in ParameterInputRow |
| `shared/.../ui/SettlementPanel.kt` | Modified | Show transfer details from persisted snapshot |
| `shared/.../ui/EventsScreen.kt` | Modified | Remove BalanceSummaryCard + unused derived states |
| `shared/.../model/Models.kt` | Read | Reuse existing `toJson()`/`toCalculationSnapshot()` |

## Risks
| Risk | Likelihood | Mitigation |
|------|------------|------------|
| `!= uid` filter still imprecise for 3+ person events | Medium | Accept as best-effort immediate fix; defer creditor model |
| Coil AsyncImage fails in KMP context | Low | ProfileAvatar already used in dashboard and profile card |
| Snapshot JSON exceeds String column limit | Low | Transfer lists bounded by participant count |
| BalanceSummaryCard removal orphans derived states | Low | Verify no other usages before deleting |

## Rollback
All changes are additive or conditional removals — `git revert` per commit is sufficient.
- **Issue 1**: Remove the added `profileId != uid` filter condition
- **Issue 2a**: Revert to initials-only rendering in three components
- **Issue 2b**: Remove snapshot-reading code; SettlementPanel falls back to per-profile totals
- **Issue 3**: Restore `BalanceSummaryCard(...)` call and derived states

## Dependencies
None — all three issues are independent and parallelizable.

## Success Criteria
- [ ] Dashboard "Te deben" excludes current user's own debts
- [ ] Profile photos render in PreviewBreakdown, TransferListPanel, CalculatorSheet via ProfileAvatar
- [ ] SettlementPanel shows transfer details (who→whom + amount) after calculation applied
- [ ] Events screen no longer shows "Balance total"
- [ ] All existing + new unit tests pass
- [ ] No data migration required for existing events
