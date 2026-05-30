# Delta for Data Loading

## ADDED Requirements

### Requirement: Sync Timeout (R001)

The system MUST apply a 15-second timeout to each repository's initial remote data fetch. If no data is received within the timeout, a warning is logged and the sync continues with cached data.

#### Scenario: Sync completes within timeout

- GIVEN the network is available and remote data exists
- WHEN `startSync()` collects from `remoteRepository.observeEvents()`
- AND data arrives within 15 seconds
- THEN the data is upserted into the local cache
- AND no timeout warning is logged

#### Scenario: Sync exceeds timeout

- GIVEN the network is slow or remote service is unresponsive
- WHEN `startSync()` waits for `remoteRepository.observeEvents().first()`
- AND 15 seconds pass without data
- THEN a warning is logged with the repository name
- AND the sync loop continues (does not crash)
- AND cached data remains available to the UI

### Requirement: Sync Error Logging (R002)

The system MUST log sync errors with the repository name and exception message for debugging.

#### Scenario: Sync throws exception

- GIVEN a network error occurs during sync
- WHEN the catch block handles the exception
- THEN a log message is printed with format `[OfflineFirst{Repo}] Sync error: {message}`
- AND the backoff delay is applied before retry

## MODIFIED Requirements

### Requirement: Repository Sync Initialization

Each OfflineFirst repository MUST NOT start sync in its `init {}` block. Instead, sync is triggered by calling a public `startSync()` method. The `init {}` block only sets up the network monitor listener that calls `startSync()`/`stopSync()` on connectivity changes. Each repository exposes `fun startSync()` as a public method.

(Previously: `init {}` launched sync immediately via `networkMonitor.isOnline.onEach { ... }.launchIn(syncScope)`, causing all 4 repos to race on app startup)

#### Scenario: Sync not started on repository construction

- GIVEN an OfflineFirstEventRepository is instantiated
- WHEN the constructor completes
- THEN no sync job is running
- AND `syncJob` is null
- AND the network monitor listener is registered but does not trigger sync yet

#### Scenario: Sync started externally

- GIVEN the repository is constructed but sync has not started
- WHEN `startSync()` is called externally
- THEN a sync job is launched
- AND remote data begins flowing into the local cache

### Requirement: Dashboard Loading State

The system MUST show a loading skeleton in DashboardScreen while the first data emission from all combined flows is pending. `DashboardState` has an `isLoading: Boolean` field that is `true` until the first `combine` emission, then `false` thereafter.

(Previously: DashboardState had no `isLoading` field; UI showed empty state while data loaded)

#### Scenario: Initial loading state

- GIVEN DashboardViewModel is initialized
- WHEN `state` is observed before the first `combine` emission
- THEN `state.isLoading` is `true`
- AND DashboardScreen renders a loading skeleton

#### Scenario: Data loaded, loading state cleared

- GIVEN `isLoading` is `true`
- WHEN the first `combine` emission arrives with data
- THEN `isLoading` is set to `false`
- AND the skeleton is replaced with actual content

#### Scenario: Loading does not block interaction

- GIVEN the dashboard is showing a loading skeleton
- WHEN user taps a navigation item
- THEN the app navigates to the selected page
- AND the loading state does not prevent interaction
