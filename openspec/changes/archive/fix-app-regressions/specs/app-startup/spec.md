# Delta for App Startup

## ADDED Requirements

### Requirement: Staggered Sync Start (R001)

The system MUST provide a `startSyncStaggered(scope: CoroutineScope)` method in RepositoryProvider that starts each repository's sync with a 500ms delay between them.

#### Scenario: Staggered sync launches repos sequentially

- GIVEN `startSyncStaggered(scope)` is called
- WHEN the method executes
- THEN `eventRepository.startSync()` is called first
- AND after 500ms delay, `debtRepository.startSync()` is called
- AND after another 500ms delay, `expenseRepository.startSync()` is called
- AND after another 500ms delay, `profileRepository.startSync()` is called

#### Scenario: No sync before staggered start

- GIVEN RepositoryProvider is constructed
- WHEN no `startSyncStaggered()` has been called
- THEN no repository has an active sync job
- AND all `syncJob` fields are null

### Requirement: MainActivity Triggers Sync After First Render (R002)

The system MUST trigger `startSyncStaggered()` from MainActivity via a `LaunchedEffect(Unit)` after the first Compose render completes.

#### Scenario: Sync triggered after first render

- GIVEN MainActivity has called `setContent { MainAppContent }`
- WHEN the first composition completes
- THEN `LaunchedEffect(Unit)` fires
- AND `repositoryProvider.startSyncStaggered(scope)` is invoked

## MODIFIED Requirements

### Requirement: App Startup Flow

The app startup MUST NOT block on synchronous data loading. RepositoryProvider construction creates repositories WITHOUT starting sync. MainActivity renders the UI shell first, then triggers staggered sync via `LaunchedEffect`. DashboardScreen shows loading skeleton during initial data fetch. User can navigate while loading.

(Previously: All 4 repositories started sync immediately in their `init {}` blocks, racing on app launch and potentially blocking the first render)

#### Scenario: UI renders before sync starts

- GIVEN the app process starts
- WHEN MainActivity.onCreate() completes
- THEN the UI shell is visible (Scaffold with bottom nav)
- AND repositories exist but have not started syncing

#### Scenario: Dashboard shows skeleton during initial load

- GIVEN the app has just launched and sync is in progress
- WHEN DashboardScreen is rendered
- THEN a loading skeleton is displayed
- AND the skeleton is replaced with content when `isLoading` becomes false

#### Scenario: User can navigate while loading

- GIVEN the dashboard is showing a loading skeleton
- WHEN user taps "Eventos" in the bottom nav
- THEN the EventsScreen is displayed
- AND the sync continues in the background
