# Design: Fix Event Persistence & UX Improvements

## Technical Approach

Four isolated changes across the repository: (1) remove `clearLocalData()` from startup, move exclusively to sign-out; (2) replace manual date text fields with Material 3 `DatePickerDialog`; (3) fix neon-green button contrast using a dedicated `onPrimaryContainer` color token; (4) wire the existing `_onRemoveMember` callback into a red trash-icon button per participant row with confirmation dialog.

## Architecture Decisions

### Decision: Remove `clearLocalData()` from `startSyncStaggered()`

**Choice**: Delete line 92 in `RepositoryProvider.kt`. Keep `clearLocalData()` only in `MainActivity.kt:230` (sign-out).
**Alternatives considered**: Sync first then clear, or track auth UID changes. Simpler: the cache is already upsert-based — stale data gets overwritten on sync.
**Rationale**: Every app start clears the local SQLDelight cache. If Firestore sync fails (network, timeout), the user sees zero events. The cache is already keyed by Firestore document IDs — no risk of data mixing between users as long as we clear on sign-out.

### Decision: Grant `ManageParticipants` to CONTRIBUTOR

**Choice**: Change `PermissionEngine.kt:46-47` so `ManageParticipants` returns `true` for both OWNER and CONTRIBUTOR.
**Alternatives considered**: Add a new `RemoveParticipant` action. Unnecessary — the user wants contributors to add/remove participants, which is exactly what `ManageParticipants` covers.
**Rationale**: Existing test at `RoleUiGatingTest.kt:74` asserts CONTRIBUTOR cannot manage participants — this was a deliberate but overly restrictive choice. The user explicitly wants OWNER + CONTRIBUTOR to have this capability.

### Decision: Use fixed dark text on neon green buttons

**Choice**: Add `onPrimaryContainer: Color` token to `NeoFintechColorSet` — set to `#191C1D` (dark) in both light and dark themes.
**Alternatives considered**: `Color.White` (fails on light theme), `colors.onButton` (not designed for primary container), or conditional logic per theme (adds complexity).
**Rationale**: `primaryContainer` is `#39FF14` in both themes. This neon green requires dark text for WCAG AA compliance regardless of light/dark mode. The same token applies to all three affected buttons.

### Decision: Add remove button inside `SettlementPanel.DebtRow`

**Choice**: Add an `onRemoveProfile` lambda to `DebtRow`. Show a red square button with trash icon next to each participant row. Gate visibility with `canRemoveParticipant` (OWNER/CONTRIBUTOR + not involved in expenses).
**Alternatives considered**: Separate composable outside SettlementPanel (breaks the existing layout, adds coupling).
**Rationale**: `DebtRow` already renders each participant. Adding the remove button there keeps the UI cohesive and reuses the existing `canManageParticipants` gate. The confirmation dialog lives at the `EventDetailScreen` level.

### Decision: Use `DatePickerDialog` for date selection

**Choice**: Replace both `OutlinedTextField` date inputs in `EventEditorDialog` with `DatePickerDialog` triggered by a read-only clickable field.
**Alternatives considered**: Inline `DatePicker` (takes too much space), custom calendar (maintenance burden).
**Rationale**: Material 3 `DatePickerDialog` is cross-platform in Compose 1.6.11, supports single and range modes, and provides the visual calendar UX the user wants.

## Data Flow

```
1. PERSISTENCE FIX:
   Before:  App start → clearLocalData() → startSync() → Firestore timeout → empty UI
   After:   App start → startSync() → upsert from Firestore → cached events visible

   Sign-out: Auth.signOut() → clearLocalData() → new user sees only their data

2. REMOVE PARTICIPANT:
   User taps trash icon
     → canManageParticipants? (OWNER/CONTRIBUTOR)
     → canRemoveParticipant? (not payer/debtor in expenses)
     → Confirmation dialog
     → eventsViewModel.removeMember(eventId, uid)
       → FirestoreEventRepository.removeMember() [updates remote]
       → observeEvents() flow triggers re-render with updated participants

3. DATE PICKER:
   User taps date field
     → DatePickerDialog opens with last selected date (or today)
     → User picks date → DatePickerDialog closes
     → startDateText updated in dd/MM/yyyy
     → Same flow for endDate if range mode

4. BUTTON COLORS:
   Before: containerColor=primaryContainer(#39FF14) + contentColor=onSurface(#E5E2E1 dark)
   After:  containerColor=primaryContainer(#39FF14) + contentColor=onPrimaryContainer(#191C1D)
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `shared/.../RepositoryProvider.kt:92` | Modify | Delete `clearLocalData()` from startSyncStaggered() |
| `shared/.../ui/NeoFintechColors.kt` | Modify | Add `onPrimaryContainer: Color` token (dark in both themes) |
| `shared/.../ui/EventsScreen.kt` | Modify | Replace date text fields with DatePickerDialog |
| `shared/.../ui/EventDetailScreen.kt` | Modify | Add confirmation dialog for participant removal; fix button colors |
| `shared/.../ui/SettlementPanel.kt` | Modify | Add `onRemoveProfile` param; render red trash button in DebtRow |
| `shared/.../model/PermissionEngine.kt:46-47` | Modify | Grant `ManageParticipants` to CONTRIBUTOR as well |
| `shared/.../ui/CuentaMorososApp.kt:443` | No change | `_onRemoveMember` already wired correctly — now actually invoked |

## Interfaces / Contracts

### PermissionEngine change
```kotlin
// Before: ManageParticipants -> role == EventRole.OWNER
// After:
EventAction.ManageParticipants -> role == EventRole.OWNER || role == EventRole.CONTRIBUTOR
EventAction.AssignRoles,
EventAction.Calculate,
EventAction.Close,
EventAction.DeleteEvent,
EventAction.Reopen -> role == EventRole.OWNER
```

### SettlementPanel new signature
```kotlin
fun SettlementPanel(
    // ... existing params ...
    canManageParticipants: Boolean = true,
    onRemoveMember: ((String) -> Unit)? = null,  // NEW
)
```

### NeoFintechColorSet new token
```kotlin
data class NeoFintechColorSet(
    // ... existing ...
    val onPrimaryContainer: Color,  // NEW
)
// Light: onPrimaryContainer = Color(0xFF191C1D)
// Dark:  onPrimaryContainer = Color(0xFF191C1D)  // same—neon green needs dark text always
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | `PermissionEngine.hasPermission(CONTRIBUTOR, ManageParticipants)` = true | Update `PermissionEngineTest.kt` |
| Unit | `startSyncStaggered` doesn't call `clearLocalData` | Update `RepositoryProvider` test if exists |
| Unit | `canRemoveParticipant` blocks removal when profile is payer/debtor | Already exists in `PermissionEngineTest.kt` |
| UI | Remove button visible for OWNER/CONTRIBUTOR, hidden for READER | Update `RoleUiGatingTest.kt` |
| UI | DatePickerDialog opens on field tap, yields correct date | Compose UI test |

## Migration / Rollout

No migration required. The persistence fix is backward-compatible — existing cached data is simply not cleared on restart. The permission change affects only new authorization checks (no data migration).

## Open Questions

- `DatePickerDialog` is available in Compose Multiplatform 1.6.11 for both Android and iOS targets
