# Event Persistence Fix Specification

## Purpose

Ensure the local SQLDelight cache survives app restarts so events remain visible immediately, while still preventing data leakage between user sessions.

## Requirements

### Requirement: Cache Persistence Across Restarts

The system MUST preserve the local SQLDelight cache across application restarts. Data MUST NOT be cleared during startup or sync initialization.

#### Scenario: Events visible immediately after restart

- GIVEN a user has created events and they are cached locally
- WHEN the user closes and reopens the application
- THEN all previously cached events MUST appear in "Mis Eventos" without waiting for Firestore sync

#### Scenario: Events visible offline after restart

- GIVEN a user has cached events and has no network connection
- WHEN the user reopens the application
- THEN all cached events MUST be visible from the local database

### Requirement: Cache Clearing on Sign-Out Only

The system SHALL clear all local cached data exclusively when the user explicitly signs out. Cache clearing MUST NOT occur during app startup, sync initialization, or any automatic process.

#### Scenario: Data isolation between sessions

- GIVEN User A is signed in with their events cached locally
- WHEN User A signs out
- THEN all local cached data (events, profiles, debts, expenses) MUST be deleted
- AND when User B signs in, they MUST NOT see User A's data

#### Scenario: Sync after sign-in populates new user's data

- GIVEN the local cache was cleared on sign-out
- WHEN a new user signs in and Firestore sync completes
- THEN only the new user's events MUST appear in the local cache

### Requirement: Sync Without Cache Wipe

The system MUST synchronize remote Firestore data into the local cache without deleting existing cached entries first. The sync process SHALL use upsert operations to merge remote data with local data.

#### Scenario: Sync updates existing events

- GIVEN the local cache has 3 events from a previous session
- WHEN Firestore sync returns updated data for those events
- THEN the local cache MUST reflect the latest remote state via upsert

#### Scenario: Sync removes deleted events

- GIVEN an event was deleted remotely (via another device)
- WHEN Firestore sync completes
- THEN the deleted event MUST be removed from the local cache
