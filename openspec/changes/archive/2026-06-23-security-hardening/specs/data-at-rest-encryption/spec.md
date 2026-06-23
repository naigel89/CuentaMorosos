# Data-at-Rest Encryption Specification

## Purpose

All application data stored locally MUST be encrypted at rest. Android Auto Backup MUST be disabled to prevent unencrypted data extraction. Existing unencrypted data MUST be migrated transparently on upgrade.

## Requirements

### R001: Encrypted SharedPreferences

`CuentaMorososLocalStore` MUST use `EncryptedSharedPreferences` from `androidx.security:security-crypto`, backed by Android Keystore (AES-256-GCM). Plain `SharedPreferences` MUST NOT be used in release builds.

**Acceptance Criteria**: All `getSharedPreferences()` calls in `CuentaMorososLocalStore.kt` use `EncryptedSharedPreferences`. Reading/writing events, profiles, debts, expenses, and preferences works correctly.

#### Scenario: Clean install creates encrypted storage

- **GIVEN** the app is installed for the first time
- **WHEN** a user creates an event and a profile
- **THEN** data is persisted to encrypted SharedPreferences
- **AND** the data can be read back correctly after app restart

#### Scenario: Data migration from unencrypted upgrade

- **GIVEN** a previous version stored data in plain `SharedPreferences`
- **WHEN** the user upgrades to the hardened version
- **THEN** existing data is read from old store and written to `EncryptedSharedPreferences`
- **AND** no data loss occurs

### R002: Encrypted SQLite via SQLCipher

The SQLDelight database driver on Android MUST use SQLCipher for encryption. `DriverFactory.android.kt` MUST create the database with a `SQLCipherUtils.getDatabaseState()` check and `SQLiteDatabaseHook` for migration.

**Acceptance Criteria**: `cuentamorosos.db` cannot be opened by `sqlite3` CLI without the encryption key. SQLDelight queries return correct data.

#### Scenario: SQLCipher database created on clean install

- **GIVEN** the app is installed fresh
- **WHEN** the database is initialized
- **THEN** the database file is encrypted
- **AND** SQLDelight read/write operations function normally

### R003: Android Backup Disabled

`AndroidManifest.xml` MUST set `android:allowBackup="false"`. `android:fullBackupContent` MUST NOT be declared.

**Acceptance Criteria**: `adb backup com.cuentamorosos` returns empty/no backup. App data is NOT included in Google Drive backups.

#### Scenario: ADB backup returns no data

- **GIVEN** a device with the hardened APK installed and user data present
- **WHEN** `adb backup com.cuentamorosos` is executed
- **THEN** the backup file is empty or contains no application data
- **AND** `adb restore` does not recover any CuentaMorosos data
