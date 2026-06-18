# Delta for Schema Migration

## ADDED Requirements

### Requirement: Schema Version Tracking (R001)

The system MUST track the database schema version using `PRAGMA user_version` and compare it against a constant `SCHEMA_VERSION = 2` on every app launch.

#### Scenario: First launch after schema update

- GIVEN the app is launched after a code update that changes the schema
- AND the stored `user_version` is less than `SCHEMA_VERSION`
- WHEN `createDriver()` is called
- THEN `migrateDatabase()` is executed
- AND `PRAGMA user_version` is set to `SCHEMA_VERSION` after successful migration

#### Scenario: Launch with current schema version

- GIVEN the stored `user_version` equals `SCHEMA_VERSION`
- WHEN `createDriver()` is called
- THEN `migrateDatabase()` is NOT executed
- AND the database opens normally

### Requirement: Catastrophic Migration Fallback (R002)

The system MUST handle migration failures by deleting and recreating the database, preserving the ability to re-sync from remote.

#### Scenario: Migration throws exception

- GIVEN `migrateDatabase()` throws an exception during column additions
- WHEN the exception is caught
- THEN the database file is deleted via `context.deleteDatabase()`
- AND a new database is created with the current schema
- AND a warning is logged with the exception message

## MODIFIED Requirements

### Requirement: Database Migration Logic

The system MUST check `PRAGMA user_version` before running `migrateDatabase()`. If version < 2, the existing migration logic executes (ALTER TABLE for missing columns, backfill of startDateMillis/endDateMillis from dateMillis, state backfill for legacy events). After migration, `PRAGMA user_version = 2` is set. The entire migration is wrapped in a try-catch with database deletion as fallback. Missing tables trigger immediate database deletion and recreation.

(Previously: Migration ran unconditionally on every launch if the database file existed, with no version tracking)

#### Scenario: Missing columns are added via ALTER TABLE

- GIVEN CachedEvent table exists but lacks `state`, `startDateMillis`, or `endDateMillis` columns
- WHEN migration runs
- THEN each missing column is added via `ALTER TABLE ... ADD COLUMN`
- AND `state` defaults to 'DRAFT'
- AND `startDateMillis` and `endDateMillis` default to 0

#### Scenario: Date range backfill from dateMillis

- GIVEN CachedEvent rows have `startDateMillis = 0` or `endDateMillis = 0`
- WHEN migration runs the backfill step
- THEN `startDateMillis` is set to `dateMillis` where it was 0
- AND `endDateMillis` is set to `dateMillis` where it was 0

#### Scenario: State backfill for legacy events

- GIVEN CachedEvent rows have `state = 'DRAFT'` but have participants or calculations
- WHEN migration runs the state backfill
- THEN events with `lastCalculationMode` set are updated to 'CALCULATED'
- AND events with participants/memberIds are updated to 'OPEN'

#### Scenario: Missing tables trigger recreation

- GIVEN the database file exists but is missing one or more expected tables
- WHEN migration checks table existence
- THEN the database is deleted immediately
- AND a new database is created with all tables from the current schema

#### Scenario: Existing data is preserved during migration

- GIVEN the database has existing rows in CachedEvent, CachedProfile, etc.
- WHEN migration adds missing columns
- THEN all existing rows remain intact
- AND no data is lost during ALTER TABLE operations
