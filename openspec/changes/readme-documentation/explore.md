# Exploration: README Documentation Update

## 1. Project Identity

- **Name**: CuentaMorosos
- **Type**: Kotlin Multiplatform (KMP) Android application with conditional iOS support
- **Purpose**: Event-based shared expense management — splits bills, calculates who owes whom with minimum transfers, supports roles/permissions, invites via email, and syncs offline-first with Firebase
- **License**: MIT (naigel89, 2026)
- **Package**: `com.cuentamorosos`

## 2. Complete Tech Stack

### Core

| Component | Version |
|-----------|---------|
| Kotlin | 1.9.24 |
| AGP (Android Gradle Plugin) | 8.5.2 |
| Compose Multiplatform | 1.6.11 |
| Kotlin Compiler Extension (Compose) | 1.5.14 |
| JVM Target | 17 |
| compileSdk / targetSdk | 35 |
| minSdk | 24 |

### UI

| Library | Version |
|---------|---------|
| Jetpack Compose BOM | 2024.06.00 |
| Material 3 | via BOM |
| Coil (KMP image loading) | 3.0.4 |
| Coil Compose | 3.0.4 |
| Coil Network (OkHttp) | 3.0.4 (androidMain) |
| Lifecycle ViewModel KMP | 2.8.0 |
| Lifecycle Runtime Compose KMP | 2.8.0 |
| Activity Compose | 1.9.1 |
| Core KTX | 1.13.1 |
| Lifecycle Runtime KTX | 2.8.4 |

### Data / Backend

| Library | Version |
|---------|---------|
| Firebase BOM | 33.6.0 (app) |
| Firebase Auth KTX | via BOM (app) |
| Firebase Firestore KTX | via BOM (app) |
| Firebase Storage KTX | via BOM (app) |
| Firebase Messaging KTX | via BOM (app) |
| dev.gitlive:firebase-auth | 1.13.0 (shared/KMP) |
| dev.gitlive:firebase-firestore | 1.13.0 (shared/KMP) |
| Google Services Plugin | 4.4.2 |

### Local Storage

| Library | Version |
|---------|---------|
| SQLDelight | 2.0.2 |
| SQLDelight Coroutines Extensions | 2.0.2 |
| SQLDelight Android Driver | 2.0.2 |
| SQLDelight SQLite Driver (JVM) | 2.0.2 |

### Async / Concurrency

| Library | Version |
|---------|---------|
| Kotlinx Coroutines Core | 1.8.1 |
| Kotlinx Coroutines Android | 1.8.1 |
| Kotlinx DateTime | 0.6.1 |
| WorkManager Runtime KTX | 2.9.1 |

### Testing

| Framework | Version | Scope |
|-----------|---------|-------|
| JUnit 4 | 4.13.2 | app unit tests |
| Kotlin Test | via kotlin("test") | shared commonTest |
| Kotlinx Coroutines Test | 1.8.1 | both |
| Robolectric | 4.13 | app unit tests |
| AndroidX Test Core | 1.6.1 | app unit tests |
| AndroidX Test Ext JUnit | 1.2.1 | both |
| Espresso | 3.6.1 | androidTest |
| Compose UI Test JUnit4 | via BOM | androidTest |
| JSON (org.json) | 20231013 | app unit tests |
| SQLDelight SQLite Driver | 2.0.2 | shared commonTest |

### No configured

- No DI framework (manual factory pattern: `AppViewModelFactory`)
- No linter (detekt/ktlint)
- No formatter (ktfmt)
- No coverage tooling (JaCoCo)
- No CI/PR checks visible in repo

## 3. Setup Requirements & Steps

### Prerequisites

- **JDK 17** (configured in `gradle.properties`: `/usr/lib/jvm/java-17-openjdk-amd64`)
- **Android SDK** (compileSdk 35, minSdk 24)
- **Android Studio** (Hedgehog+ recommended for Compose tooling)
- **Firebase project** with:
  - `google-services.json` in `app/` directory
  - Firebase Auth (email/password provider enabled)
  - Firestore Database (with security rules configured)
  - Firebase Storage (for profile photos)
  - Firebase Cloud Messaging (for push notifications, optional)
- **macOS** (for iOS targets — iOS is conditionally excluded on Linux/Windows)
- `local.properties` with `sdk.dir` pointing to Android SDK (if not auto-detected)
- Gradle wrapper already provided (`gradlew`/`gradlew.bat`)

### Environment Config

- `gradle.properties`: VM heap 4GB, Kotlin daemon 2GB, parallel build, caching, configure-on-demand enabled
- signing config (release): keystore at `keystore/CuentaMorosos.jks` (password in build file)
- `KOTLIN_NATIVE_DISTRIBUTION_DOWNLOAD_FROM_MAVEN=true` for Kotlin/Native

### Build & Run

```bash
# Build debug APK
./gradlew assembleDebug

# Run tests
./gradlew test                                     # app unit tests (JUnit/Robolectric)
./gradlew :shared:allTests                         # shared module tests (Kotlin test)
./gradlew connectedAndroidTest                     # instrumentation tests (Espresso)

# Install on device/emulator
./gradlew installDebug
```

### Firebase Setup Summary

1. Create Firebase project in Firebase Console
2. Enable **Authentication** → Email/Password sign-in
3. Create **Firestore Database** in production mode, configure rules
4. Create **Storage** bucket for profile photos
5. Download `google-services.json` and place it in `app/`
6. Enable **Cloud Messaging** API for push notifications (optional)

## 4. Full Directory Structure

```
CuentaMorosos/
├── app/                                    # Android application module
│   ├── build.gradle.kts                    # AGP config, dependencies, signing
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── google-services.json        # Firebase config (gitignored)
│       │   └── java/com/cuentamorosos/
│       │       ├── CuentaMorososApp.kt     # Application class, singleton access
│       │       ├── MainActivity.kt         # Entry point: auth flow, photo picker, deep links
│       │       ├── data/
│       │       │   ├── CuentaMorososLocalStore.kt      # SharedPreferences persistence layer
│       │       │   ├── FirebaseUserSyncManager.kt      # Firebase Auth+Firestore user sync
│       │       │   ├── MigrationManager.kt             # Legacy data migration
│       │       │   ├── NotificationScheduler.kt        # Local notification posting + dedup
│       │       │   ├── ReminderWorker.kt               # WorkManager periodic reminder worker
│       │       │   └── CuentaMorososFirebaseMessagingService.kt  # FCM push handler
│       │       ├── notifications/
│       │       │   └── NotificationDispatcher.kt       # Android notification dispatch
│       │       └── ui/
│       │           └── auth/
│       │               └── MigrationScreen.kt          # Legacy data migration UI
│       └── test/                                       # 6 test files (LocalStore, FCM, Dedup)
│
├── shared/                                  # KMP shared module (business logic + UI)
│   ├── build.gradle.kts                     # KMP plugin, Compose, SQLDelight config
│   └── src/
│       ├── commonMain/                      # Cross-platform code
│       │   ├── kotlin/com/cuentamorosos/
│       │   │   ├── Platform.kt             # expect declarations
│       │   │   ├── SystemBackHandler.kt    # Cross-platform back handler
│       │   │   ├── NotificationCallbacks.kt # Callback interface for notifications
│       │   │   ├── model/                  # Pure business logic (ZERO Android deps)
│       │   │   │   ├── Models.kt           # All data classes: EventItem, ProfileItem, EventExpenseItem, etc.
│       │   │   │   ├── StateMachine.kt     # Event lifecycle: OPEN→CALCULATED→CLOSED transitions
│       │   │   │   ├── SplitCalculator.kt   # 4 split modes: Equal, Exact, Percentage, Parts
│       │   │   │   ├── CalculatorEngine.kt  # 6 calculation modes + greedy settlement
│       │   │   │   ├── SettlementEngine.kt  # 8-step settlement algorithm with edge cases
│       │   │   │   ├── IntegrityGuard.kt    # State-based integrity rules
│       │   │   │   ├── PermissionEngine.kt  # Role-based access: OWNER, CONTRIBUTOR, READER
│       │   │   │   └── validation/          # Input validators (Event, Profile, Item)
│       │   │   ├── ui/                     # Compose screens & ViewModels (shared across platforms)
│       │   │   │   ├── CuentaMorososApp.kt  # Root composable: 5-tab pager, navigation
│       │   │   │   ├── CuentaMorososTheme.kt # Neo-fintech dark theme
│       │   │   │   ├── DashboardScreen.kt   # Financial summary panel
│       │   │   │   ├── EventsScreen.kt      # Event list with reminders
│       │   │   │   ├── EventDetailScreen.kt # Event view: expenses, debts, settlement
│       │   │   │   ├── ProfilesScreen.kt    # Profile management
│       │   │   │   ├── InvitationsScreen.kt # Pending invitations
│       │   │   │   ├── SettingsScreen.kt    # User preferences
│       │   │   │   ├── CalendarScreen.kt    # Calendar view of events
│       │   │   │   ├── AccountScreen.kt     # Account settings (password, photo, username)
│       │   │   │   ├── CalculatorSheet.kt   # Split calculator bottom sheet
│       │   │   │   ├── SettlementPanel.kt   # Transfer list display
│       │   │   │   ├── TransferListPanel.kt # Transfer management
│       │   │   │   ├── ReceiptPanel.kt      # Expense receipt view
│       │   │   │   ├── MoneyExplosionAnimation.kt # Celebration animation
│       │   │   │   ├── NeoFintech*.kt       # Design system: colors, typography, shapes, spacing, elevation
│       │   │   │   ├── *_ViewModel.kt       # ViewModels: Dashboard, Events, EventDetail, Profiles, Invitations, Account
│       │   │   │   └── auth/               # Auth screens: Login, Register, ForgotPassword, UserProfile
│       │   │   ├── data/                   # Shared data layer
│       │   │   │   ├── NetworkMonitor.kt    # expect interface for connectivity
│       │   │   │   ├── ReminderService.kt   # Builds reminder messages from events/debts
│       │   │   │   ├── PendingOperationQueue.kt # Offline operation queue with retry
│       │   │   │   └── repository/          # Repository pattern implementations
│       │   │   │       ├── EventRepository.kt, DebtRepository.kt, etc. # Interfaces
│       │   │   │       ├── Firestore*Repository.kt   # Firebase Firestore implementations
│       │   │   │       ├── OfflineFirst*Repository.kt # SQLDelight cache + Firestore sync
│       │   │   │       └── ProfileRepository.kt, etc. # Additional repos
│       │   │   ├── db/                     # SQLDelight driver factory (expect)
│       │   │   │   └── DriverFactory.kt
│       │   │   └── notifications/          # Notification models
│       │   │       ├── NotificationEvent.kt
│       │   │       └── DeepLinkTarget.kt
│       │   └── sqldelight/                 # SQLDelight schema files
│       │       └── com/cuentamorosos/db/
│       │           ├── CachedEvent.sq
│       │           ├── CachedProfile.sq
│       │           ├── CachedDebt.sq
│       │           ├── CachedExpense.sq
│       │           ├── CachedCalculationVersion.sq
│       │           ├── CachedAuditEntry.sq
│       │           ├── CachedAdjustmentEntry.sq
│       │           └── PendingOperation.sq
│       ├── commonTest/                     # 39 test files (engines, ViewModels, repos, UI)
│       ├── androidMain/                    # Android-specific KMP code
│       │   └── kotlin/com/cuentamorosos/
│       │       ├── RepositoryProvider.kt   # Wires all repositories
│       │       ├── AppViewModelFactory.kt  # Manual DI: ViewModel factory
│       │       ├── NetworkMonitorFactory.kt # Android connectivity monitor
│       │       └── db/DriverFactory.kt     # Android SQLDelight driver
│       ├── jvmMain/                        # JVM (desktop) target
│       │   └── kotlin/com/cuentamorosos/
│       │       └── db/DriverFactory.kt     # JVM SQLite driver
│       └── iosMain/                        # iOS target (conditional on macOS)
│
├── iosApp/                                 # iOS Xcode project (wrapper)
├── keystore/                               # Release signing key
├── documentation/                          # Project docs (CHANGELOG, FR, NFR, sprint docs, UI docs)
├── openspec/                               # SDD artifacts
│   ├── config.yaml
│   ├── specs/
│   └── changes/                            # 15 completed/active changes
├── gradle/
│   └── libs.versions.toml                  # Version catalog (may not exist — checked)
├── build.gradle.kts                        # Root: plugin declarations
├── settings.gradle.kts                     # Module includes, repository config
├── gradle.properties                       # Build properties, JVM args
├── gradlew / gradlew.bat                   # Gradle wrapper
├── README.md                               # Current (outdated) README
├── AGENTS.md                               # Agent guidelines for AI assistants
├── LICENSE                                 # MIT
└── logo.png                                # App logo
```

## 5. Feature Inventory

### 5.1 Events

- **CRUD**: Create, edit, delete events with name, dates (start/end), base currency
- **Lifecycle**: Strict state machine — OPEN → CALCULATED → CLOSED
  - OPEN: expenses can be added/removed
  - CALCULATED: debts computed, paid/unpaid tracking
  - CLOSED: read-only, all payments settled
- **Roles per participant**:
  - **OWNER**: full control — calculate, close, reopen, delete event, manage roles
  - **CONTRIBUTOR**: create/edit own expenses, view all
  - **READER**: view-only
- **Invitations**: Email-based invitations to join events; pending/accept/reject flow
- **Calendar view**: Visual timeline of events

### 5.2 Expenses (Gastos)

- **6 split modes** (`CalculatorEngine.kt`):
  1. **Real Consumption** (`real_consumption`) — each item split among assigned profiles
  2. **Simple Average** (`simple_avg`) — equal split among all participants
  3. **By Category** (`by_category`) — shared items split among all, others among assigned
  4. **Custom Percentage** (`custom_percentage`) — per-profile percentages (must sum 100%)
  5. **Exact Amount** (`exact`) — per-profile exact amounts (must sum to total)
  6. **By Parts** (`parts`) — integer parts (1-100), split proportionally
- **11 categories** with icons/colors: Shared, Flight, Accommodation, Food, Transport, Entertainment, Shopping, Health, Education, Services, Other
- **Multi-currency**: stub ready for FX conversion (`exchangeRate`, `itemCurrency` fields)
- **Audit trail**: immutable audit entries for every expense CRUD operation (`AuditRepository`)

### 5.3 Settlement (Liquidación)

- **Greedy algorithm**: minimises number of transfers to settle all debts (`SettlementEngine.kt`)
- **Edge case detection**: zero balances, self-netting, deleted creditors, internal compensation
- **Calculation versioning**: each calculation run creates an immutable `CalculationVersion`; previous versions preserved
- **Adjustments**: `AdjustmentEntry` for correcting paid transfers without modifying original debt
- **Currency**: EUR-only for this release (`SUPPORTED_CURRENCY = "EUR"`)
- **Integrity guard**: prevents recalculation if prior participants are missing; validates expense data before calculation

### 5.4 Profiles

- **Profile management**: Create, edit, delete profiles with name, icon emoji, email link
- **Ghost profiles**: temporary profiles for one-off participants
- **Custom display names**: per-viewer custom names (`customNames` map)
- **Photo upload**: via Firebase Storage → Firebase Auth profile photo update
- **Balance summary**: net balances per profile (positive = they owe you, negative = you owe them)
- **Account settings**: password change, username, display name, photo management

### 5.5 Dashboard

- **Financial summary**: total spent, your share vs. what you're owed per event
- **Profile balances**: who owes whom, net positions
- **Pending events by profile**: drill-down into who owes what
- **Calendar access**: quick navigation to calendar view

### 5.6 Notifications

- **Local reminders**: `ReminderWorker` (WorkManager, daily) for pending debts, incomplete events, upcoming events
- **FCM push**: `CuentaMorososFirebaseMessagingService` for invitation and calculation notifications
- **Dedup**: fingerprint-based deduplication (`CuentaMorososLocalStore` notification registry)
- **Deep links**: notifications navigate directly to events/invitations

### 5.7 Offline Support

- **Offline-first repositories**: all data cached in SQLDelight, synced with Firestore
- **Pending operation queue**: failed remote operations queued and retried on reconnect (`PendingOperationQueue`)
- **Staggered sync**: events → debts → expenses → profiles with 500ms delays
- **Offline banner**: visual indicator when offline
- **Network monitor**: `NetworkMonitor` interface with platform-specific connectivity detection

## 6. Architecture Overview

### Pattern: Offline-First Repository

```
┌─────────────────────────────────────────────────────────────┐
│                     App Module (Android)                     │
│  MainActivity → CuentaMorososApp (singleton)                │
│  Notification services, Firebase native SDK, WorkManager     │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│                 Shared Module (KMP)                          │
│                                                              │
│  ┌─────────┐    ┌──────────────┐    ┌────────────────────┐  │
│  │   UI    │───▶│  ViewModels   │───▶│    Repositories    │  │
│  │ Compose │    │ StateFlow +   │    │  OfflineFirst wrap │  │
│  │ Screens │    │ derivedStateOf│    │  Firestore remotes │  │
│  └─────────┘    └──────┬───────┘    └────────┬───────────┘  │
│                        │                      │              │
│                        ▼                      ▼              │
│              ┌─────────────────┐    ┌──────────────────┐    │
│              │  Model Engines  │    │  Local Cache      │    │
│              │  SplitCalculator│    │  SQLDelight (SQL) │    │
│              │  SettlementEng. │    │  8 tables         │    │
│              │  PermissionEng. │    └──────────────────┘    │
│              │  IntegrityGuard │                            │
│              │  StateMachine   │    ┌──────────────────┐    │
│              └─────────────────┘    │  Pending Queue    │    │
│                                     │  Offline retry    │    │
│                                     └──────────────────┘    │
│                                                              │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              Design System: NeoFintech                │  │
│  │  Dark theme, neon accents, custom typography/spacing  │  │
│  │  MoneyExplosionAnimation, animated transitions        │  │
│  └───────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

### Key Design Decisions

1. **KMP shared module**: Business logic and Compose UI live in `shared/` for code reuse. The `app/` module is a thin Android shell handling Firebase native SDK, notifications, and lifecycle.
2. **Repository pattern with OfflineFirst**: Each entity has an interface, a Firestore implementation, and an OfflineFirst wrapper caching locally in SQLDelight.
3. **Dual persistence**: SQLDelight (primary, reactive via `Flow`) + `CuentaMorososLocalStore` (SharedPreferences, for legacy data and notification dedup).
4. **Pure model engines**: `SplitCalculator`, `SettlementEngine`, `PermissionEngine`, `IntegrityGuard`, `StateMachine` are all pure Kotlin with no framework dependencies — testable as unit tests.
5. **Manual DI**: No Hilt/Koin. `AppViewModelFactory` creates ViewModels with repository dependencies. `RepositoryProvider` wires all repositories.
6. **derivedStateOf for computed properties**: Dashboard aggregates, net balances, reminder messages are computed reactively.
7. **Staggered sync startup**: 500ms delays between repository syncs to avoid overwhelming Firestore.
8. **PendingOperationQueue**: Failed remote operations are persisted in SQLDelight and retried on reconnect with exponential backoff.

### Data Flow (Example: Create Expense)

```
User fills form in EventDetailScreen
  → EventDetailViewModel.saveExpense(expense)
    → OfflineFirstExpenseRepository.saveExpense(expense)
      → SQLDelight INSERT (local cache — immediate UI update)
      → FirestoreExpenseRepository.saveExpense(expense) (remote — try)
        → on failure: PendingOperationQueue.enqueue(...)
```

## 7. Current README Status

### What Exists

The current `README.md` (80 lines) contains:
- Logo and tagline
- Brief description in Spanish
- **Outdated** functionality list:
  - Mentions "Borrador" state (removed from code — actual states: OPEN, CALCULATED, CLOSED)
  - Lists 4 split modes (6 exist)
  - Simplified feature descriptions
- A simplified tech stack table (only 6 rows)
- Outdated file structure showing only `shared/src/commonMain/`
- Brief "Next steps" (open in Android Studio, sync Gradle, run)

### What's Missing

1. **No setup instructions** — Firebase project setup, `google-services.json`, environment variables, prerequisites
2. **No architecture documentation** — offline-first pattern, repository layer, state management
3. **No complete tech stack** — missing versions, KMP details, SQLDelight, Coil, test frameworks, pending queue
4. **No build/run commands** — how to build, test, install
5. **No project structure** — full directory tree with descriptions
6. **No testing information** — how to run tests, coverage status
7. **No contributing guide** — no mention of AGENTS.md, SDD workflow, or development conventions
8. **No feature details** — event lifecycle states wrong, split modes incomplete, no mention of roles, notifications, offline support, audit trail, integrity guards
9. **No iOS information** — conditional iOS support, how to build for iOS
10. **No license mention** — MIT license file exists but not referenced
11. **No Firebase architecture** — Firestore collections, security rules, auth flow
12. **Outdated file structure** — only shows `shared/src/commonMain/`, misses `app/`, `iosApp/`, `androidMain/`, `iosMain/`, `jvmMain/`, `sqldelight/`, `documentation/`, `openspec/`
13. **No design system mention** — Neo-fintech dark theme, custom typography, animations
14. **No acknowledgments or credits** — Firebase, open source libraries used

### Documentation Folder (`/documentation/`)

Contains additional docs not linked from README:
- `CHANGELOG.md` — version history
- `README.md` — documentation index
- `api/` — API documentation
- `dd/` — design documents
- `fr/` — functional requirements
- `nfr/` — non-functional requirements
- `r-neg/` — business rules
- `sprints/` — sprint planning
- `ui/` — UI/UX docs
- `GITHUB_ACTIONS_SETUP.md` — CI setup

## Recommendations for README Update

### Must Include
1. English and Spanish versions (or at minimum, clear Spanish with English technical terms)
2. Complete tech stack with versions in a table
3. Prerequisites and setup steps (JDK 17, Android SDK, Firebase)
4. Build, test, and run commands
5. Full directory structure with descriptions
6. Architecture diagram/description (offline-first repository pattern)
7. Feature inventory with all 6 split modes, correct event lifecycle, roles, settlement
8. Link to LICENSE
9. Link to `documentation/` folder for detailed specs
10. Reference to `AGENTS.md` for development conventions

### Nice to Have
1. Screenshots of main screens (Dashboard, Events, Calculator)
2. Quick-start video or GIF
3. Badges (Kotlin version, minSdk, license)
4. Firebase setup walkthrough (or link to Firebase Console)
5. Architecture decision records (ADR) link

### Risks
- **Outdated Spanish README**: Current README references "Borrador" state which no longer exists in code. Must verify all states/features against actual `Models.kt` and engine code.
- **README language drift**: AGENTS.md and code comments are mixed English/Spanish. Decide consistent language for README. The current README is in Spanish — verify this is the intended language.
- **Scope creep**: The `/documentation/` folder contains extensive FR/NFR/sprint docs. The README should link to these, not duplicate them.
- **Sensitive info in build files**: `app/build.gradle.kts` contains signing keystore passwords. Do NOT reference these in README.
