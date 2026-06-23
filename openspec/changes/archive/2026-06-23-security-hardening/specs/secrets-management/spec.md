# Secrets Management Specification

## Purpose

Ensure keystore credentials and API secrets are never committed to version control. Credentials MUST be sourced from environment variables or `local.properties`, excluded from git via `.gitignore`.

## Requirements

### R001: Keystore Credentials from Environment

The build system MUST read release keystore passwords from `local.properties` or environment variables. Hardcoded passwords in `build.gradle.kts` MUST NOT exist.

**Acceptance Criteria**: `grep -r "llevalatarara" app/build.gradle.kts` returns no match. `./gradlew assembleRelease` succeeds with credentials in `local.properties`.

#### Scenario: Developer configures local.properties

- **GIVEN** a developer clones the repo and creates `local.properties` with `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`
- **WHEN** they run `./gradlew assembleRelease`
- **THEN** the release APK is signed with the configured keystore
- **AND** no credentials appear in `app/build.gradle.kts`

#### Scenario: Credentials missing in CI

- **GIVEN** `local.properties` does not exist and no environment variables are set
- **WHEN** the release build runs
- **THEN** the build MUST fail with a descriptive message: "Missing keystore credentials: set RELEASE_KEYSTORE_PASSWORD in local.properties or environment"
- **AND** no partial APK is produced

### R002: Secret Files Excluded from Git

Files containing credentials (`local.properties`, `keystore.properties`, `google-services.json` derivatives with secrets) MUST be listed in `.gitignore`.

**Acceptance Criteria**: `git ls-files` does not include `local.properties` or any file containing the release keystore password.

#### Scenario: Secret files are never committed

- **GIVEN** `local.properties` exists with keystore credentials
- **WHEN** `git add .` is executed
- **THEN** `local.properties` is NOT staged
- **AND** `git status` shows it as untracked
