# Agent Guidelines - CuentaMorosos

## Project Overview
Android application built with Kotlin and Jetpack Compose for managing event-based debts and expenses.

## Architecture
- `shared/src/commonMain/kotlin/com/cuentamorosos/ui`: UI components and screens.
- `shared/src/commonMain/kotlin/com/cuentamorosos/model`: Data models and calculation engines.
- `shared/src/commonMain/kotlin/com/cuentamorosos/data`: Local storage and notification services.

## Development Notes
- **State Management**: Uses `derivedStateOf` for computed properties. When using the `by` delegate, do NOT use `.value` to access the state.
- **Kotlin Smart Casts**: Be aware that properties with open or custom getters (like those delegated by `remember`) cannot be smart-cast. Assign them to a local variable first.
- **Persistence**: Data is persisted via `CuentaMorososLocalStore`. Always call `saveEvents()` / `saveProfiles()` after modifying state lists.

## Commands
- **Build/Run**: Standard Android Studio / Gradle build.
- **Lint/Check**: Use Android Studio's built-in linting and Kotlin compiler checks.
