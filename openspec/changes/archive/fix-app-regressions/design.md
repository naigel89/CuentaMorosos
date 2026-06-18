# Design: Fix App Regressions

## Technical Approach

Four targeted fixes restore baseline UX after CalendarScreen addition: (1) demote Calendar from nav tab to modal overlay, (2) restore per-alert expand/collapse with `animateContentSize` instead of `AnimatedVisibility` removal, (3) formalize schema migration in `DriverFactory.android.kt` with version tracking, (4) defer sync start and add loading state to Dashboard.

## Architecture Decisions

| Decision | Option A | Option B | Choice | Rationale |
|----------|----------|----------|--------|-----------|
| Calendar visibility | `MainSection` enum entry | `mutableState<Boolean>` overlay | **B** | Keeps nav at 4 tabs; modal avoids pager index shifts; matches proposal approach |
| Alert collapse | `AnimatedVisibility` (removes from composition) | `animateContentSize` (keeps in composition) | **B** | `AnimatedVisibility` inside `LazyColumn items {}` removes items entirely — breaks scroll state and tap targets |
| Schema migration | `PRAGMA user_version` + ALTER TABLE | DROP + recreate on mismatch | **A** | Existing `migrateDatabase()` in `DriverFactory.android.kt` already does column-level ALTER; formalize with version constant |
| Sync start timing | `init {}` block in repos | `startSync()` called from `MainActivity` | **B** | Stops 4 repos from racing on app launch; enables staggered 500ms delays |
| Loading state | `MutableStateFlow<Boolean>` in ViewModel | `Flow<List<T>>.map` derived | **A** | Explicit flag survives empty-list ambiguity; UI distinguishes "loading" from "no data" |

## Data Flow

```
MainActivity.onCreate()
  └─ create DriverFactory → migrateDatabase() (if needed)
  └─ create RepositoryProvider (NO sync started yet)
  └─ setContent { MainAppContent }
       └─ LaunchedEffect(Unit) { startSyncStaggered() }  ← 500ms between repos

CuentaMorososApp
  └─ var showCalendar by remember { mutableStateOf(false) }
  └─ DashboardScreen(onOpenCalendar = { showCalendar = true })
  └─ if (showCalendar) Dialog { CalendarScreen(onClose = { showCalendar = false }) }

DashboardViewModel
  └─ init: combine(flows) → computeState() → _state
  └─ isLoading: true until first combine emission
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `shared/.../ui/CuentaMorososApp.kt` | Modify | Remove `CALENDAR` from enum (4 entries). `pageCount = { 4 }`. Add `showCalendar` state. Render `CalendarScreen` in `Dialog` when true. Pass `onClose` lambda. |
| `shared/.../ui/CalendarScreen.kt` | Modify | Add `onClose: () -> Unit = {}` parameter. Render close button (back arrow) in month header row. |
| `shared/.../ui/DashboardScreen.kt` | Modify | Replace `AnimatedVisibility` + `items` pattern with `AlertAccordionCard` per alert. Each card owns `var expanded`. Section header toggles all. Add loading skeleton when `state.isLoading`. |
| `shared/.../ui/DashboardState.kt` | Modify | Add `isLoading: Boolean = false` field. |
| `shared/.../ui/DashboardViewModel.kt` | Modify | Set `isLoading = true` in init. Set `isLoading = false` after first `combine` emission. |
| `shared/.../ui/AlertAccordionCard.kt` | **Create** | New composable: header row (icon + message + chevron) + collapsible `AlertCard` body via `animateContentSize`. Follows `DebtAccordionCard` pattern. |
| `shared/.../data/repository/OfflineFirstEventRepository.kt` | Modify | Extract `init {}` network listener to `startSync()` method. Add `withTimeout(15000)` around `remoteRepository.observeEvents().first()`. Log errors with `println("[Repo] ...")`. |
| `shared/.../data/repository/OfflineFirstDebtRepository.kt` | Modify | Same pattern as Event repo. |
| `shared/.../data/repository/OfflineFirstExpenseRepository.kt` | Modify | Same pattern as Event repo. |
| `shared/.../data/repository/OfflineFirstProfileRepository.kt` | Modify | Same pattern as Event repo. |
| `shared/.../androidMain/kotlin/.../RepositoryProvider.kt` | Modify | Add `startSyncStaggered(scope: CoroutineScope)` method. Delays 500ms between each repo's sync init. Remove auto-start from repo `init`. |
| `app/.../MainActivity.kt` | Modify | In `MainAppContent`, add `LaunchedEffect(Unit) { repositoryProvider.startSyncStaggered(scope) }` after first render. |
| `shared/.../db/DriverFactory.android.kt` | Modify | Add `const val SCHEMA_VERSION = 2`. Check `PRAGMA user_version` before `migrateDatabase()`. If version < 2, run migration then `PRAGMA user_version = 2`. Add catastrophic fallback: delete DB if migration throws. |

## Interfaces / Contracts

### AlertAccordionCard

```kotlin
@Composable
fun AlertAccordionCard(
    alert: SmartAlert,
    onTap: () -> Unit,
    initiallyExpanded: Boolean = false,
)
```

- Header: icon circle (40dp) + alert message + chevron (▶/▼)
- Body: full `AlertCard` with clickable area
- Animation: `Modifier.animateContentSize()` on body column
- Each instance owns its `expanded` state via `remember { mutableStateOf(false) }`

### DashboardState

```kotlin
data class DashboardState(
    val isLoading: Boolean = false,       // NEW
    val totalOwedToYou: Double = 0.0,
    val totalYouOwe: Double = 0.0,
    val smartAlerts: List<SmartAlert> = emptyList(),
    val allEvents: List<DashboardEventRow> = emptyList(),
    val owedToYouBreakdown: List<DebtBreakdownItem> = emptyList(),
    val youOweBreakdown: List<DebtBreakdownItem> = emptyList(),
)
```

### RepositoryProvider.startSyncStaggered

```kotlin
fun startSyncStaggered(scope: CoroutineScope) {
    scope.launch {
        listOf(
            { eventRepository.startSync() },
            { debtRepository.startSync() },
            { expenseRepository.startSync() },
            { profileRepository.startSync() },
        ).forEachIndexed { i, start ->
            if (i > 0) delay(500)
            start()
        }
    }
}
```

### OfflineFirst repo contract change

Each repo gains:
- `fun startSync()` — public, starts the network listener + sync loop
- `init {}` no longer launches sync

Sync timeout pattern:
```kotlin
withTimeout(15_000) {
    remoteRepository.observeEvents().first()
}
// on timeout: log warning, continue with cached data
```

### CalendarScreen modal

```kotlin
// In CuentaMorososApp:
if (showCalendar) {
    Dialog(onDismissRequest = { showCalendar = false }) {
        Surface(modifier = Modifier.fillMaxSize()) {
            CalendarScreen(
                events = events,
                pendingTotalsByEvent = pendingTotalsByEvent,
                onOpenEvent = { eventDetailViewModel.setEventId(it.id) },
                onClose = { showCalendar = false },
            )
        }
    }
}
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | `DashboardViewModel` loading state transitions | Verify `isLoading` starts true, becomes false after first emission |
| Unit | `AlertAccordionCard` expand/collapse | Compose test: tap chevron, verify body visible/hidden |
| Unit | `DatabaseSchemaManager` migration logic | In-memory SQLite: create old schema, run migration, verify columns exist |
| Integration | Calendar modal open/close | Compose test: tap calendar button → dialog appears → tap close → dialog dismissed |
| Integration | Staggered sync start | Verify repos don't start sync until `startSyncStaggered()` called |

## Migration / Rollout

No migration needed. Schema version check runs on every app launch. If `user_version` < 2, existing `migrateDatabase()` logic executes (already handles column additions and backfills). After migration, `user_version` is set to 2.

Rollback: revert git commit. If schema issues arise, user can clear app data.

## Open Questions

- [ ] Should sync timeout be configurable via UserPreferences (for users on slow networks)?
- [ ] Should `AlertAccordionCard` persist expanded state across recompositions (e.g., navigate away and back)?
