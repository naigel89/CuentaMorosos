# Tasks: Fix Event Persistence & UX Improvements

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~140 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | single-pr |

```
Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: Low
```

## Phase 1: Foundation

- [x] 1.1 **PermissionEngine**: Change `PermissionEngine.kt:46-47` so `ManageParticipants` returns `true` for both `OWNER` and `CONTRIBUTOR`
- [x] 1.2 **NeoFintechColors**: Add `onPrimaryContainer: Color` to `NeoFintechColorSet` — set to `#191C1D` in both light and dark palettes; expose via `toColorScheme()` mapping

## Phase 2: Core Implementation

- [x] 2.1 **Persistence fix**: Remove `clearLocalData()` call from `RepositoryProvider.kt:92` in `startSyncStaggered()`. Keep only in `MainActivity.kt:230` sign-out flow
- [x] 2.2 **Date picker**: In `EventsScreen.kt:499-541`, replace `OutlinedTextField` date inputs with `DatePickerDialog`. Use `rememberDatePickerState()` for single date, `rememberDateRangePickerState()` for range mode. Default to today on new events, existing date on edit
- [x] 2.3 **Remove participant button**: In `SettlementPanel.kt`, add `onRemoveMember: ((String) -> Unit)?` param. Add red square trash-icon button to each `DebtRow`. Gate visibility with `canManageParticipants` AND `canRemoveParticipant`. Use `onRemoveMember?.let { showButton }` pattern
- [x] 2.4 **Confirmation dialog**: In `EventDetailScreen.kt`, add confirmation `AlertDialog` when remove participant is tapped. Call `_onRemoveMember(profileId)` on confirm
- [x] 2.5 **Button color fix**: In `EventDetailScreen.kt:448-450,493-495` and `SettlementPanel.kt:90-92`, change `contentColor = colors.onSurface` to `contentColor = colors.onPrimaryContainer`

## Phase 3: Testing

- [x] 3.1 **PermissionEngine tests**: Update `PermissionEngineTest.kt:` — change CONTRIBUTOR `ManageParticipants` assertion from `assertFalse` to `assertTrue`
- [x] 3.2 **Role gating tests**: Update `RoleUiGatingTest.kt:74` — change CONTRIBUTOR `ManageParticipants` assertion to reflect new permission
- [x] 3.3 **Persistence test**: Add test verifying `observeEvents()` returns cached events after simulated restart (no `clearLocalData` call)
- [ ] 3.4 **Date picker test**: Add Compose UI test verifying `DatePickerDialog` opens on field tap and selected date populates the text field
