# UI Responsiveness Specification

## Purpose

Ensure the UI remains responsive during authentication and data computation by eliminating main-thread blocking and redundant state recomputation.

## Requirements

### Requirement: Non-Blocking Authentication

Login and register operations SHALL NOT block the main thread. All Firebase sync operations during auth MUST run in a coroutine scope, not via `runBlocking`.

#### Bug Context

**Síntoma**: Tras hacer login o registro, la app se congela 2-5 segundos. El usuario ve la pantalla de login pero no puede interactuar. En redes lentas, puede aparecer ANR (Application Not Responding).

**Causa raíz**: `MainActivity.kt` líneas 82-87, 257-261, 288-291 usan `runBlocking`:
```kotlin
FirebaseAuth.getInstance().currentUser?.let {
    runBlocking {
        FirebaseUserSyncManager.syncCurrentUser()
        FirebaseUserSyncManager.ensureOwnProfile()
    }
}
```
`runBlocking` bloquea el thread actual (main thread) hasta que las corutinas completen. `syncCurrentUser()` hace 2-3 llamadas de red a Firestore.

**Por qué el fix lo resuelve**: Reemplazar `runBlocking` con `LaunchedEffect` o `viewModelScope.launch`:
```kotlin
LaunchedEffect(user) {
    FirebaseUserSyncManager.syncCurrentUser()
    FirebaseUserSyncManager.ensureOwnProfile()
}
```
Las operaciones corren en un coroutine scope sin bloquear el main thread.

#### Scenario: Login with network sync

- GIVEN the user taps "Login" with valid credentials
- WHEN Firebase authentication succeeds and profile sync begins
- THEN the UI remains responsive (no ANR/freeze)
- AND a loading indicator is shown during sync

#### Scenario: Registration with profile creation

- GIVEN the user completes registration
- WHEN `ensureOwnProfile()` runs to create the user's profile
- THEN the operation runs in a coroutine, not `runBlocking`
- AND the UI does not freeze during profile creation

#### Scenario: Slow network during auth

- GIVEN the user logs in with a slow network connection
- WHEN Firebase sync takes several seconds
- THEN the UI shows a loading state
- AND the user can interact with loading UI (not frozen)

### Requirement: Consolidated Derived State

Multiple `derivedStateOf` blocks depending on the same source data SHALL be consolidated into a single-pass computation. The system MUST NOT perform redundant `groupBy`/`filter`/`sumOf` operations on the same collections.

#### Bug Context

**Síntoma**: Cuando se actualiza una deuda o gasto, la UI tiene micro-stutters. Con muchos eventos, el lag es noticeable.

**Causa raíz**: `CuentaMorososApp.kt` líneas 179-240 tiene 7 bloques `derivedStateOf` que dependen de `allDebts` y/o `allExpenses`:
1. `activeTotalsByProfile` (línea 179)
2. `pendingTotalsByEvent` (línea 188)
3. `totalSpent` (línea 197)
4. `participantCountByEvent` (línea 201)
5. `yourShareByEvent` (línea 207)
6. `youAreOwedByEvent` (línea 217)
7. `pendingEventsByProfile` (línea 227)

Cada uno hace `groupBy`, `filter`, `mapValues`, `sumOf` sobre las mismas listas. Cuando `allDebts` cambia, los 7 bloques recalculan independientemente.

**Por qué el fix lo resuelve**: Consolidar en un solo `derivedStateOf` que compute todos los agregados en una pasada:
```kotlin
val dashboardAggregates by derivedStateOf {
    val debts = allDebts.value
    val expenses = allExpenses.value
    // Calcular todos los valores en una pasada
    mapOf(
        "activeTotals" to ...,
        "pendingTotals" to ...,
        // etc
    )
}
```
Una sola iteración sobre las listas, todos los valores calculados juntos.

#### Scenario: Debts update triggers single computation

- GIVEN the dashboard has 7 `derivedStateOf` blocks all depending on `allDebts`
- WHEN `allDebts` changes (new debt added)
- THEN a single derivation computes all aggregates (activeTotals, pendingTotals, participantCount, yourShare, youAreOwed, pendingByProfile) in one pass
- AND no redundant iterations over `allDebts` occur

#### Scenario: UI recomposes correctly

- GIVEN consolidated derived state produces all dashboard aggregates
- WHEN any aggregate value changes
- THEN all dependent UI elements recompose with correct values
- AND no stale values are displayed
