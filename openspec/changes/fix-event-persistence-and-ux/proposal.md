# Proposal: Fix Event Persistence & UX Improvements

## Intent

Four issues are degrading the user experience: (1) events vanish from "Mis Eventos" after app restart despite data remaining in Firestore, (2) date selection requires manual text input which is error-prone, (3) action buttons are illegible (light text on neon green), and (4) there is no UI to remove participants from events.

## Scope

### In Scope
- Fix local cache being wiped on every startup, causing events to disappear until Firestore sync completes
- Replace manual date text fields with Material 3 `DatePicker` dialog
- Fix button color contrast (neon green background with light text in dark mode)
- Add participant removal UI with confirmation dialog in event detail screen

### Out of Scope
- Offline-first architecture redesign (only fixing the clear-data-on-start bug)
- Date picker for non-event screens (calendar, reminders)
- Global button theming refactor (only fixing the three illegible action buttons)
- Participant role management (only add/remove, not role changes)

## Capabilities

### New Capabilities
- `event-persistence-fix`: Ensure local SQLDelight cache survives app restarts; only clear on explicit sign-out
- `event-date-picker`: Material 3 DatePicker dialog replacing manual dd/MM/yyyy text input in event editor
- `event-participant-removal`: UI to remove participants from event detail screen with confirmation

### Modified Capabilities
None (no existing specs cover event management)

## Approach

1. **Persistence fix**: Remove `clearLocalData()` call from `startSyncStaggered()`. Move it exclusively to sign-out flow. This ensures the SQLDelight cache persists across restarts and events are visible immediately, even offline.
2. **Date picker**: Replace `OutlinedTextField` date inputs in `EventEditorDialog` with a clickable field that opens `DatePickerDialog`. Support both single date and date range modes using `DatePickerState` with optional end date.
3. **Button contrast**: Change `contentColor` from `colors.onSurface` to a fixed dark color (`Color(0xFF191C1D)`) for neon green buttons, ensuring legibility in both light and dark themes.
4. **Remove participant**: Wire the existing `_onRemoveMember` callback to a trash/remove icon button next to each participant in `SettlementPanel` and participant list. Add confirmation dialog. Guard with `canDo(EventAction.ManageParticipants)`.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `shared/.../RepositoryProvider.kt` | Modified | Remove `clearLocalData()` from startup; keep for sign-out only |
| `shared/.../ui/EventsScreen.kt` | Modified | Replace date text fields with DatePicker dialog |
| `shared/.../ui/EventDetailScreen.kt` | Modified | Fix button colors; wire remove participant UI |
| `shared/.../ui/SettlementPanel.kt` | Modified | Fix button colors; add remove button per participant |
| `shared/.../ui/NeoFintechColors.kt` | Modified | Add `onPrimaryContainer` token for consistent contrast |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Stale cache shows wrong data after user switch | Medium | Keep `clearLocalData()` on sign-out; verify auth state before sync |
| DatePicker API availability in Compose Multiplatform | Low | Material 3 DatePicker is available in Compose 1.6.x; verify iOS target |
| Removing owner breaks event integrity | Low | Reuse existing `removeMember` logic that promotes oldest contributor |

## Rollback Plan

Revert the change commit. The persistence fix is isolated to `RepositoryProvider.kt`. UI changes are purely additive (date picker, remove button) and safe to revert.

## Dependencies

- Material 3 `DatePicker` (already available via Compose BOM)
- Existing `removeMember` repository method (already implemented)

## Success Criteria

- [ ] Events persist in "Mis Eventos" after app restart without requiring Firestore sync
- [ ] Date selection uses a visual calendar picker instead of manual text input
- [ ] All action buttons have legible text (WCAG AA contrast ratio) on neon green background
- [ ] Users can remove participants from event detail with a confirmation step
- [ ] Unit tests cover: persistence behavior, date conversion logic, participant removal flow
