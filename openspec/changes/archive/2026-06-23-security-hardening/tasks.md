# Tasks: Security Hardening

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~475 across 22 files |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Delivery strategy | ask-on-risk |
| Chain strategy | stacked-to-main |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: pending
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Build secrets + Manifest + Network config + ProGuard | PR 1 | Foundation; ~109 lines |
| 2 | EncryptedSharedPreferences + migration | PR 2 (→ PR 1) | Needs security-crypto dep; ~55 lines |
| 3 | Data leak prevention (LogSanitizer, PII, passwords, validators) | PR 3 (→ PR 1) | Independent; ~196 lines |
| 4 | Firestore rules + email verification | PR 4 (→ PR 1) | Independent; ~80 lines |

## Phase 1: Build & Network Foundation

- [x] 1.1 `app/build.gradle.kts`: extract keystore passwords from `local.properties`/env vars; fail with descriptive message when missing; remove hardcoded store/key passwords (spec secrets R001)
- [x] 1.2 `app/build.gradle.kts`: enable `isMinifyEnabled = true` release; add `androidx.security:security-crypto:1.1.0-alpha06`
- [x] 1.3 `app/src/main/AndroidManifest.xml`: `android:allowBackup="false"`; add `networkSecurityConfig="@xml/network_security_config"`
- [x] 1.4 `app/src/main/res/xml/network_security_config.xml` (NEW): `<base-config cleartextTrafficPermitted="false">`; pin-sets for `*.googleapis.com`, `*.firebaseio.com` with backup pins (spec network R001–R003)
- [x] 1.5 `app/proguard-rules.pro`: add `-keep` rules for Firebase Auth/Firestore/Storage/Messaging, Kotlin coroutines/serialization, Compose, SQLDelight, gitlive-firebase (spec obfuscation R002)

## Phase 2: Storage Encryption

- [x] 2.1 `app/.../CuentaMorososLocalStore.kt`: `EncryptedSharedPreferences.create` (`MasterKeys.AES256_GCM_SPEC`); rename `PREFS_NAME` to `cuenta_morosos_store_encrypted`; one-time migration: read all keys from old plain store → write encrypted → clear old (spec data-at-rest R001)

## Phase 3: Data Leak Prevention

- [x] 3.1 `shared/.../data/LogSanitizer.kt` (NEW): object with `log(tag, msg)` gated on `Platform.isDebug`, redacts emails (`s/^(.{3}).*(@.*)/$1***$2`), UIDs (last 6 chars only), names (first char + count). No-op on release (spec data-leak R001)
- [x] 3.2 `shared/.../Platform.kt`: add `expect val isDebug: Boolean`; `shared/.../Platform.android.kt`: add `actual val isDebug: Boolean` reading `BuildConfig.DEBUG`; `shared/build.gradle.kts`: add `buildConfig = true`
- [x] 3.3 Replace `println()` → `LogSanitizer.log()` in: `AccountViewModel.kt`, `FirestoreProfileRepository.kt`, `FirestoreEventRepository.kt`, `FirestoreDebtRepository.kt`, `FirestoreExpenseRepository.kt`, `FirestoreInvitationRepository.kt`, `OfflineFirst*Repository.kt` (5 files), `PendingOperationQueue.kt`, `EventsViewModel.kt`, `MainActivity.kt` (+ additionally found: `ProfilesViewModel.kt`, `ProfileAvatar.kt`, `CuentaMorososApp.kt`, `EventDetailViewModel.kt`, `SettlementEngine.kt`, `FirebaseUserSyncManager.kt`)
- [x] 3.4 `shared/.../ui/auth/SplashAuthScreen.kt`: set `password = ""` before `onLoginSuccess()`; `shared/.../ui/auth/RegisterScreen.kt`: set `password = ""` and `confirmPassword = ""` before `onRegisterSuccess()` (spec data-leak R002)
- [x] 3.5 `shared/.../model/validation/sanitize.kt` (NEW): shared `sanitize()` function stripping Unicode control chars, normalizing tabs/LF/CR to spaces, collapsing internal spaces, trimming. Used in `EventValidator.kt` (name), `ProfileValidator.kt` (name/email), `ItemValidator.kt` (name) (spec data-leak R003)
- [x] 3.6 Write tests: `LogSanitizerTest` (20 tests: redaction patterns, release no-op); `ValidatorSanitizeTest` (21 tests: RTL override, ZWJ, whitespace); `PasswordClearingTest` (4 tests: state empty after callbacks)

## Phase 4: Firestore Authorization

- [x] 4.1 `firestore.rules` (NEW): `rules_version='2'`; deny unauthenticated; `/users/{userId}` self-access; `/profiles/{profileId}` auth-read + self-write; `/events/{eventId}` participant-read, owner-write; subcollections auth-only (spec firestore R001–R004)
- [x] 4.2 `firebase.json` (NEW): firestore rules reference (spec firestore R001)
- [x] 4.3 `app/.../MainActivity.kt`: post-registration, check `user.isEmailVerified`; if false, show verification screen instead of `MainAppContent` (spec firestore R005)
- [x] 4.4 Tests for email verification gate (6 unit tests covering requiresVerification/resolveVerificationState)
