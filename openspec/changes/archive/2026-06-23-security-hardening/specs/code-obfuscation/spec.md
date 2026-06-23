# Code Obfuscation Specification

## Purpose

Release APKs MUST have R8/ProGuard minification and obfuscation enabled to prevent reverse engineering of business logic, data models, and credential extraction.

## Requirements

### R001: R8 Minification Enabled in Release

`app/build.gradle.kts` MUST set `isMinifyEnabled = true` for the `release` build type. Debug builds MUST remain unminified.

**Acceptance Criteria**: `./gradlew assembleRelease` completes. Release APK class names are obfuscated (e.g., `a`, `b`, `c`). Debug APK class names remain readable.

#### Scenario: Release build produces obfuscated APK

- **GIVEN** `isMinifyEnabled = true` in release build type
- **WHEN** `./gradlew assembleRelease` is executed
- **THEN** the resulting APK contains obfuscated class and method names
- **AND** `strings` utility on the APK reveals no PII or secret strings

#### Scenario: Debug build remains readable

- **GIVEN** `isMinifyEnabled = false` for debug build type
- **WHEN** `./gradlew assembleDebug` is executed
- **THEN** class names and method names remain human-readable
- **AND** stack traces in Logcat reference original names

### R002: ProGuard Keep Rules Preserve Critical Runtime Classes

`app/proguard-rules.pro` MUST contain `-keep` rules for: Firebase Auth/Firestore/Messaging, Kotlin coroutines/serialization, Jetpack Compose, SQLDelight generated code, and `gitlive-firebase`.

**Acceptance Criteria**: Release build runs without `ClassNotFoundException`, `NoSuchMethodError`, or Firestore serialization failures.

#### Scenario: Firebase SDKs work after obfuscation

- **GIVEN** ProGuard rules preserve Firebase SDK entry points
- **WHEN** the release APK performs Firebase Auth login and Firestore queries
- **THEN** authentication succeeds
- **AND** Firestore data reads and writes function correctly

#### Scenario: Compose UI renders in release build

- **GIVEN** ProGuard rules preserve `@Composable` functions and Compose runtime
- **WHEN** the release APK launches and navigates through screens
- **THEN** all screens render without crashes
- **AND** recomposition and state management work correctly

#### Scenario: SQLDelight queries execute in release build

- **GIVEN** ProGuard rules preserve SQLDelight generated query classes
- **WHEN** the app queries the local database
- **THEN** results are returned correctly
- **AND** no serialization errors occur
