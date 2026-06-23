# Data Leak Prevention Specification

## Purpose

Personally identifiable information (PII) MUST NOT leak via logs, memory dumps, or clipboard. Password values MUST be cleared from memory on navigation. User input MUST be sanitized against Unicode spoofing attacks.

## Requirements

### R001: PII Redacted from Logcat in Release Builds

All `println()` and `Log.*()` calls that output PII (emails, usernames, profile names, UIDs) MUST be gated behind `if (BuildConfig.DEBUG)` or use redacted identifiers. In release builds, PII MUST NOT appear in logcat.

**Acceptance Criteria**: `adb logcat | grep "@"` shows no email addresses from release APK. `grep -rn "println" --include="*.kt"` returns no PII-printing calls unguarded by `BuildConfig.DEBUG`.

#### Scenario: Release build redacts PII from logs

- **GIVEN** the app is running in release mode (`BuildConfig.DEBUG == false`)
- **WHEN** a profile is loaded and logged
- **THEN** logcat output contains no email addresses, usernames, or full names
- **AND** only redacted identifiers (e.g., first 4 chars + "***") appear

#### Scenario: Debug build retains PII for development

- **GIVEN** the app is running in debug mode (`BuildConfig.DEBUG == true`)
- **WHEN** a profile is loaded and logged
- **THEN** logcat output includes full email, username, and name for debugging

### R002: Password Clearing on All Navigation Paths

Password `MutableState<String>` in `SplashAuthScreen`, `RegisterScreen`, and `CuentaMorososApp` MUST be set to `""` on: successful sign-in, successful sign-up, sign-out, back navigation, and app destruction.

**Acceptance Criteria**: Memory heap dump (via Android Profiler) after login shows no password string in the heap. `password.value` is `""` after any navigation away from auth.

#### Scenario: Password cleared after successful login

- **GIVEN** the user has typed a password in the login form
- **WHEN** Firebase Auth returns success and navigation to main screen occurs
- **THEN** `password.value` is set to `""`
- **AND** the password string is dereferenced

#### Scenario: Password cleared on sign-out

- **GIVEN** the app was used for a session after login
- **WHEN** the user signs out and returns to the splash/auth screen
- **THEN** `password.value` is `""` (not the previous session's password)
- **AND** the password field appears empty

### R003: Unicode Control Character Stripping in Validators

Event, profile, and item validators MUST strip Unicode control characters (categories Cc, Cf — including RTL/LTR override, ZWJ, ZWNJ) and normalize internal whitespace before validation.

**Acceptance Criteria**: Input "Na\u202Eme" (with RTL override) → validator strips to "Name". Internal multiple spaces → collapsed to single space. Leading/trailing whitespace → trimmed.

#### Scenario: RTL override character stripped from name

- **GIVEN** a user pastes or types a profile name containing `U+202E` (RIGHT-TO-LEFT OVERRIDE)
- **WHEN** `ProfileValidator.validateName()` processes the input
- **THEN** the control character is stripped before length/format validation
- **AND** the sanitized name passes validation

#### Scenario: Whitespace normalized in event name

- **GIVEN** a user types an event name with multiple internal spaces: "Fiesta   2026"
- **WHEN** `EventValidator.validateName()` processes the input
- **THEN** internal spaces are collapsed to a single space: "Fiesta 2026"
- **AND** leading/trailing whitespace is trimmed
