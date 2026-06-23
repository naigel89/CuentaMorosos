# Proposal: Security Hardening

## Intent

Address critical and high-severity vulnerabilities found in a full security audit of CuentaMorosos v1.1.0. The app has no encryption at rest, hardcoded secrets in source code, disabled obfuscation, no network security config, and client-only authorization — exposing user financial data to extraction via backup, reverse engineering, and MITM attacks.

## Scope

### In Scope
- Remove hardcoded keystore passwords from `build.gradle.kts` → environment variables
- Disable `allowBackup` + encrypt `SharedPreferences` and SQLite at rest (EncryptedSharedPreferences + SQLCipher)
- Enable R8 minification with Firebase/Kotlin/Compose/SQLDelight ProGuard rules
- Add `network_security_config.xml` with certificate pinning for `*.googleapis.com` / `*.firebaseio.com`
- Strip PII from all `println()` statements; clear passwords from Compose state on navigation
- Add `firestore.rules` to repo with auth-gated read/write; enforce email verification post-registration
- Harden validators: strip Unicode control characters, normalize whitespace

### Out of Scope
- Full PermissionEngine mirroring in Firestore rules (partial coverage only)
- FCM token access control (covered by basic firestore.rules per-user documents)
- Firebase API key restriction (Google Cloud Console change, not code)
- Debug certificate management, CI/CD secrets integration

## Capabilities

### New Capabilities
- `secrets-management`: Keystore credentials via env vars / `local.properties`, excluded from git
- `data-at-rest-encryption`: EncryptedSharedPreferences + SQLCipher, `allowBackup=false`
- `network-security`: Certificate pinning, explicit cleartext traffic policy
- `code-obfuscation`: R8 enabled with complete `-keep` rules for Firebase, Kotlin, Compose, SQLDelight, gitlive-firebase
- `data-leak-prevention`: PII redacted from logs, passwords cleared on all navigation paths, Unicode control chars stripped in validators
- `firestore-authorization`: `firestore.rules` checked in; email verification gating post-registration

### Modified Capabilities
None — existing specs unchanged.

## Approach

Defense-in-depth across layers: secrets → build config, storage → AndroidX Security Crypto, network → cert pinning, code → R8, data handling → PII redaction + input sanitization, server → Firestore rules. Each capability maps to a focused work unit: config changes, dependency additions, new files, and targeted code patches.

## Affected Areas

| Area | Impact | Files |
|------|--------|-------|
| Build config | Modified | `app/build.gradle.kts`, `shared/build.gradle.kts` |
| Manifest | Modified | `app/src/main/AndroidManifest.xml` |
| Network config | New | `app/src/main/res/xml/network_security_config.xml` |
| Firestore config | New | `firestore.rules`, `firebase.json` |
| ProGuard | Modified | `app/proguard-rules.pro` |
| Local store | Modified | `CuentaMorososLocalStore.kt` → EncryptedSharedPreferences |
| Database driver | Modified | `DriverFactory.android.kt` → SQLCipher |
| Auth screens | Modified | `SplashAuthScreen.kt`, `CuentaMorososApp.kt` (password clearing) |
| Repositories | Modified | 5 `*Repository.kt` files (PII log redaction) |
| Validators | Modified | `EventValidator.kt`, `ProfileValidator.kt`, `ItemValidator.kt` |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| R8 breaks Firebase/Kotlin/Compose runtime | Medium | Add official `-keep` rules; test release build |
| EncryptedSharedPreferences migration causes data loss | Low | Read-old-then-write-new migration with fallback |
| Cert pinning causes connectivity failure on rotation | Low | Pin backup pins; short pin lifetimes |
| firestore.rules too restrictive | Medium | Test with Firebase emulator; iterate |

## Rollback Plan

Each capability is independently revertible: set `isMinifyEnabled=false`, revert to plain SharedPreferences (old store retained for first migration), remove `networkSecurityConfig` from manifest, revert to open Firestore rules temporarily, restore hardcoded secrets as emergency fallback.

## Dependencies

- `androidx.security:security-crypto:1.1.0-alpha06`
- `net.zetetic:android-database-sqlcipher:4.5.6`
- Firebase CLI for `firestore.rules` deployment

## Success Criteria

- [ ] No secrets present in source code
- [ ] `adb backup` cannot extract application data
- [ ] Release APK strings inspection shows no PII or secrets
- [ ] Release APK code is obfuscated (class/method names minified)
- [ ] Cert pinning rejects non-pinned certificates in test
- [ ] All PII `println()` calls gated behind `BuildConfig.DEBUG`
- [ ] Passwords cleared from Compose state on all navigation paths
- [ ] `firestore.rules` denies unauthenticated reads
- [ ] Email verification enforced before app access post-registration
- [ ] All existing unit tests pass
