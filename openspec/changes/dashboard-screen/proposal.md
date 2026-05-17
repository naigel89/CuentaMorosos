# Proposal: Dashboard Screen as App Entry Point

## Intent

Replace the Events list as the default app entry point with a Dashboard (Panel de Control) that provides an at-a-glance financial overview: aggregated balances, smart alerts for incomplete events, and a recent activity feed. This aligns with the Neo-Fintech redesign (Phase C, PR2) and spec UI0007B1.

## Scope

### In Scope
- NEW: `DashboardViewModel.kt` — aggregates events, debts, expenses, profiles; computes totals, alerts, activity feed
- NEW: `DashboardScreen.kt` — composable with top indicators, Smart Alerts, Recent Activity
- MODIFIED: `CuentaMorososApp.kt` — add DASHBOARD as default `MainSection`, reduce bottom nav to 3 items (Events, Profiles, Settings)
- MODIFIED: `AppViewModelFactory.kt` — wire `DashboardViewModel` with required repositories
- MODIFIED: `EventsScreen.kt` — no longer default; accessible via "Events" nav item

### Out of Scope
- Full Events screen Bento Grid redesign (separate PR)
- Full Settings redesign (separate PR)
- Animation system (count-up, staggered lists)
- SharedAxis transitions
- Calendar/Invitations screen removal (screens stay, accessed via secondary navigation)

## Capabilities

### New Capabilities
- `dashboard-screen`: Dashboard as app entry point with aggregated indicators, smart alerts, and recent activity feed

### Modified Capabilities
- `navigation-structure`: Bottom nav reduced from 5 to 3 items; Dashboard becomes default section; Calendar and Invitations removed from primary nav

## Approach

1. **DashboardViewModel** — consumes `EventRepository`, `DebtRepository`, `ExpenseRepository`, `ProfileRepository` directly (same pattern as existing ViewModels). Aggregates:
   - `totalOwedToYou`: sum of unpaid debts across all events
   - `totalYouOwe`: sum of debts owed by current user across all events
   - `smartAlerts`: scan events for missing participants (0 debts), missing items (0 expenses), pending calculations (expenses exist but no `lastCalculationTimestamp`)
   - `recentActivity`: sorted list of recent events by `dateMillis` and `lastCalculationTimestamp`, limited to last 20 entries

2. **DashboardScreen** — single composable following UI0007B1 design spec:
   - Top row: two indicator cards ("Total Te Deben" / "Total Debés") with semantic color borders (green/red)
   - Smart Alerts section: actionable warnings with severity icons (group_off, receipt_long, calculate) and navigation to relevant events
   - Recent Activity feed: lazy column with event name, relative timestamp, amount, status badge (Active/Settling/Closed)

3. **CuentaMorososApp** — add `DASHBOARD` enum entry as first/default `MainSection`. Bottom nav shows only Events (Dashboard default), Profiles, Settings. Calendar and Invitations remain as screens but removed from primary nav (accessible via Events screen or Settings).

4. **AppViewModelFactory** — add `DashboardViewModel` case with all four repositories.

**Estimated effort**: Medium-High
**Estimated lines**: ~400-500 (DashboardViewModel ~120, DashboardScreen ~280, nav changes ~50, factory wiring ~30)

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `shared/src/commonMain/kotlin/com/cuentamorosos/ui/DashboardViewModel.kt` | New | Aggregated balance, alerts, activity computation |
| `shared/src/commonMain/kotlin/com/cuentamorosos/ui/DashboardScreen.kt` | New | Dashboard composable UI |
| `shared/src/commonMain/kotlin/com/cuentamorosos/ui/CuentaMorososApp.kt` | Modified | Add DASHBOARD section, reduce nav to 3 items, set default |
| `shared/src/androidMain/kotlin/com/cuentamorosos/AppViewModelFactory.kt` | Modified | Wire DashboardViewModel |
| `shared/src/commonMain/kotlin/com/cuentamorosos/ui/EventsScreen.kt` | Modified | No longer default; title/context unchanged |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Data aggregation slow with many events | Medium | Use `derivedStateOf` for computed values; limit recent activity to last 20 entries |
| Smart Alerts edge cases (no events, no debts) | Medium | Guard with empty-state checks; show "All clear" message when no alerts |
| User confusion from removed Calendar/Invitations nav | Medium | Keep screens accessible; add migration hint or tooltip in first launch |
| DashboardViewModel duplicates logic from EventsViewModel | Low | Dashboard aggregates across ALL events; EventsViewModel manages per-event CRUD — concerns are distinct |

## Rollback Plan

1. Revert `currentSection` default back to `MainSection.EVENTS` in `CuentaMorososApp.kt`
2. Restore 5-item bottom nav (add CALENDAR, INVITATIONS back)
3. Delete `DashboardScreen.kt` and `DashboardViewModel.kt`
4. Remove `DashboardViewModel` from `AppViewModelFactory.kt`

All changes are additive except the nav reduction, which is a single enum + conditional change — trivial to revert.

## Dependencies

- Existing repositories: `EventRepository`, `DebtRepository`, `ExpenseRepository`, `ProfileRepository`
- Existing models: `EventItem`, `EventDebtItem`, `EventExpenseItem`, `ProfileItem`
- No new external dependencies

## Success Criteria

- [ ] Dashboard screen renders as default on app launch
- [ ] Top indicators show correct aggregated totals (owed to you / you owe)
- [ ] Smart Alerts correctly identify events with 0 participants, 0 expenses, or no calculations
- [ ] Recent Activity feed shows last 20 events sorted by timestamp
- [ ] Bottom nav has exactly 3 items: Events (Dashboard), Profiles, Settings
- [ ] Calendar and Invitations screens remain functional (accessible via Events submenu or direct navigation)
- [ ] No regression in existing Events, Profiles, Settings screens
