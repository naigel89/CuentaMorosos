# Design: Security Hardening

## Technical Approach

Defense-in-depth: secrets → env vars, storage → AndroidX Security Crypto, network → cert pinning, code → R8, data handling → PII redaction + input sanitization, server → Firestore rules. Each capability is independently revertible per the proposal's rollback plan.

## Architecture Decisions

| Decision | Option | Chosen | Why |
|----------|--------|--------|-----|
| **EncryptedSP migration** | a) Dual-read migration b) Clear-first c) Key-prefix | **a) Dual-read** | First launch reads old `cuenta_morosos_store` plain SP, writes all data to new `cuenta_morosos_store_encrypted`, then discards old keys. No data loss. |
| **SQLCipher vs SQLDelight** | a) SQLCipher driver b) net.zetetic c) Defer | **c) Defer** | SQLCipher 4.5.6 has no `AndroidSqliteDriver`-compatible bridge for SQLDelight 2.x. Plain `SupportFactory` would bypass SQLDelight's schema management. Deferred to Phase 2 with tracked `design-debt` tag. Mitigation: `allowBackup=false` + EncryptedSP already protect cached data; SQLDelight data is derived from Firestore. |
| **Secrets extraction** | a) Gradle properties b) env vars c) local.properties | **c) local.properties** | `local.properties` is gitignored by Android convention. Read via `gradleLocalProperties(rootDir)` in build script. Fallback: `System.getenv("CM_KEYSTORE_PASS")` for CI/CD. |
| **ProGuard keep rules** | a) Full rules b) Conservative deps | **a) Full rules** | Firebase Auth/Firestore/Storage/Messaging, Kotlin serialization/coroutines, Compose runtime, SQLDelight generated code, gitlive-firebase. Rules from official Firebase/Compose/SQLDelight ProGuard docs. |
| **Cert pinning** | a) `network_security_config.xml` b) OkHttp interceptor | **a) XML config** | Android-native, declarative, no code dependency. Pins `*.googleapis.com`, `*.firebaseio.com` with backup pins and 90-day expiry. |
| **Logging wrapper** | a) Kotlin object b) expect/actual | **a) Kotlin object in `shared/src/commonMain`** | Single `LogSanitizer.log(tag, msg)` gate on `Platform.isDebug`. Redacts emails (s/^(.{3}).*(@.*)/$1***$2), UIDs (last 6 chars only), names (first char + count). |
| **Password clearing** | a) LaunchedEffect on navigate b) explicit `=""` before callback | **b) explicit clear** | `SplashAuthScreen`: set `password=""` before calling `onLoginSuccess`. `RegisterScreen`: set both `password=""` and `confirmPassword=""` before `onRegisterSuccess`. `onSignOut` in `MainAppContent` already triggers `localStore.clearAll()` + `FirebaseAuth.signOut()`. Password as `CharArray` rejected: Firebase Auth requires `String`, and Compose `mutableStateOf` with `CharArray` causes recomposition issues. |

## Data Flow — Encrypted Storage Migration

```
First launch after upgrade
    │
    ├── oldStore = getSharedPreferences("cuenta_morosos_store", MODE_PRIVATE)
    ├── encryptedStore = EncryptedSharedPreferences.create("cuenta_morosos_store_encrypted", ...)
    │
    ├── For each KEY (events/profiles/debts/expenses/preferences):
    │       raw = oldStore.getString(key, null)
    │       if raw != null → encryptedStore.edit().putString(key, raw).apply()
    │
    ├── Migrate dedup StringSet: same KEY_SENT_FINGERPRINTS
    │
    └── oldStore.edit().clear().apply()  // discard plain store
    │
    └── All subsequent reads/writes → encryptedStore ONLY
```

```
CuentaMorososLocalStore(context)
    │
    └── private val prefs: SharedPreferences
            = EncryptedSharedPreferences.create(
                "cuenta_morosos_store_encrypted",
                MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
```

No interface changes — `prefs` type remains `SharedPreferences`, same `getString/getStringSet/putString` API.

## Component Diagram

```
┌─────────────────────────────────────────────────────────────┐
│  AndroidManifest.xml                                        │
│  allowBackup=false, networkSecurityConfig=@xml/nsc          │
└─────────────────────────────────────────────────────────────┘
         │
    ┌────┴────────────────────────────────────────┐
    │  app/build.gradle.kts                       │
    │  signingConfig → local.properties           │
    │  isMinifyEnabled=true, proguard-rules.pro   │
    │  deps: +security-crypto                     │
    └─────────────────────────────────────────────┘
         │
    ┌────┴──────┐  ┌──────────────┐  ┌───────────────────┐
    │ nsc.xml   │  │ Firebase     │  │ CuentaMorosos-    │
    │ cert pins │  │ rules        │  │ LocalStore         │
    │ googleapis│  │ firestore.   │  │ → EncryptedSP     │
    │ firebaseio│  │ rules        │  │ (lib: security-   │
    └───────────┘  └──────────────┘  │  crypto)           │
                                     └───────────────────┘
    ┌──────────────────────────────────────────────────┐
    │  LogSanitizer (shared/commonMain)                 │
    │  log(tag, msg) → if(Platform.isDebug) println()  │
    │  Redacts: emails, UIDs, names                    │
    └──────────────────────────────────────────────────┘
         │
    ┌────┴──────────────────────────────────────────┐
    │  Validators (+Unicode control char stripping) │
    │  EventValidator, ProfileValidator, ItemValidator│
    └───────────────────────────────────────────────┘
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `app/build.gradle.kts` | Modify | Load keystore passwords from `local.properties`; enable `isMinifyEnabled=true`; add `security-crypto` dependency |
| `app/src/main/AndroidManifest.xml` | Modify | `allowBackup=false`; add `networkSecurityConfig` attribute |
| `app/src/main/res/xml/network_security_config.xml` | **Create** | Certificate pinning for `*.googleapis.com` and `*.firebaseio.com` |
| `app/proguard-rules.pro` | Modify | Add `-keep` rules for Firebase, Kotlin/Compose, SQLDelight, gitlive-firebase |
| `firestore.rules` | **Create** | Auth-gated read/write; profile visibility: authenticated users only; write: own document only |
| `firebase.json` | **Create** | Firebase project config referencing `firestore.rules` |
| `shared/src/commonMain/kotlin/.../util/LogSanitizer.kt` | **Create** | Debug-only logging wrapper with PII redaction |
| `shared/src/androidMain/kotlin/.../Platform.android.kt` | Modify | Add `actual val isDebug: Boolean` (reads `BuildConfig.DEBUG`) |
| `shared/src/commonMain/kotlin/.../Platform.kt` | Modify | Add `expect val isDebug: Boolean` |
| `app/src/main/java/.../CuentaMorososLocalStore.kt` | Modify | `PREFS_NAME` → `cuenta_morosos_store_encrypted`; constructor uses `EncryptedSharedPreferences`; migration from old store on first launch |
| `shared/src/commonMain/kotlin/.../ui/auth/SplashAuthScreen.kt` | Modify | Clear `password=""` before calling `onLoginSuccess` |
| `shared/src/commonMain/kotlin/.../ui/auth/RegisterScreen.kt` | Modify | Clear `password=""` and `confirmPassword=""` before `onRegisterSuccess` |
| `app/src/main/java/.../MainActivity.kt` | Modify | Clear pending password context on sign-out (already handled by `clearAll()` + `signOut()`) |
| `shared/src/commonMain/kotlin/.../AccountViewModel.kt` | Modify | Replace `println()` with `LogSanitizer.log()` (6 calls) |
| `shared/src/commonMain/kotlin/.../FirestoreProfileRepository.kt` | Modify | Replace all `println()` with `LogSanitizer.log()` (PII-bearing: uid, name, username, linkedEmail, email) |
| `shared/src/commonMain/kotlin/.../FirestoreEventRepository.kt` | Modify | Replace `println()` with `LogSanitizer.log()` |
| `shared/src/commonMain/kotlin/.../OfflineFirst*Repository.kt` (5 files) | Modify | Replace `println()` with `LogSanitizer.log()` |
| `shared/src/commonMain/kotlin/.../EventValidator.kt` | Modify | Add `sanitize()` call stripping Unicode control chars `[\u0000-\u001F\u007F-\u009F\u200B-\u200F\u2028-\u202F\uFEFF]` before `validateName()` |
| `shared/src/commonMain/kotlin/.../ProfileValidator.kt` | Modify | Same sanitization before name/email checks |
| `shared/src/commonMain/kotlin/.../ItemValidator.kt` | Modify | Sanitize `item.name` and `item.category` |

## Firestore Rules Architecture

Collection-level access patterns mirroring `PermissionEngine`:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users: own document only
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    // Profiles: authenticated read; write own only
    match /profiles/{profileId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == profileId;
      // customNames subcollection
      match /customNames/{viewerId} {
        allow read, write: if request.auth != null && request.auth.uid == viewerId;
      }
    }
    // Events: read if participant; write OWNER/CONTRIBUTOR per PermissionEngine
    match /events/{eventId} {
      allow read: if request.auth != null &&
        request.auth.uid in resource.data.participantIds;
      allow create: if request.auth != null &&
        request.auth.uid == request.resource.data.ownerId;
      allow update, delete: if request.auth != null &&
        request.auth.uid == resource.data.ownerId;
      // Subcollections inherit event-level participant check
      match /debts/{debtId} { allow read, write: if request.auth != null; }
      match /expenses/{expenseId} { allow read, write: if request.auth != null; }
    }
  }
}
```

**Note**: Full `PermissionEngine` mirroring (CONTRIBUTOR write scoping, Calculate/Close/DeleteEvent role checks, expense ownership) is deferred per proposal scope. Collection-level rules above are the Phase 1 baseline. Deferred rules tracked as `design-debt: firestore-rules-full-mirror`.

## Migration / Rollout

1. **EncryptedSP migration**: Automatic on first launch, one-time, no user interaction
2. **Secrets extraction**: Developers add `local.properties` lines (documented in README); CI/CD uses env vars
3. **R8**: Enabled for release builds only; debug builds unaffected
4. **Firestore rules**: Deployed via `firebase deploy --only firestore:rules`; test with emulator before prod
5. **Rollback**: Each capability independently revertible per proposal

## Open Questions

- [ ] Confirm SQLCipher 4.6.x release timeline for SQLDelight 2.x bridge (tracked as `design-debt: sqlcipher`)
- [ ] Firebase API key restriction in Google Cloud Console (out of code scope, but documented in README)
- [ ] `firestore.rules` full PermissionEngine mirror: when to schedule Phase 2?
