# Offline-First Sync Specification

## Purpose

Correct offline-first sync: drain pending operations on reconnect, start sync immediately, use snapshot listeners, maintain single subscriptions per collection.

## Requirements

### Requirement: Pending Operation Drain on Sync

The system SHALL call `PendingOperationQueue.drain()` on every sync loop start and on every network reconnection event. Pending operations MUST be replayed to Firestore before new remote data is fetched.

#### Bug Context

**Síntoma**: El usuario crea datos offline (deudas, gastos, eventos). Al reconectar, los datos aparecen en la UI local pero NUNCA llegan a Firestore. Al cerrar sesión, `clearLocalData()` borra la cola de operaciones pendientes y los datos se pierden permanentemente.

**Causa raíz**: `PendingOperationQueue.drain()` está definido en `shared/src/commonMain/kotlin/com/cuentamorosos/data/PendingOperationQueue.kt` líneas 57-91, pero **ningún archivo del codebase lo llama**. Los repositorios OfflineFirst hacen `pendingQueue.enqueue()` cuando falla un write (ej: `OfflineFirstEventRepository.kt` línea 158), pero nadie ejecuta `drain()` para replayar las operaciones.

**Por qué el fix lo resuelve**: Al llamar `drain(remoteOps)` en cada sync loop start y en cada reconexión de red, las operaciones encoladas se replayan a Firestore antes de hacer fetch de datos remotos.

#### Scenario: Drain on network reconnection

- GIVEN the app was offline and the user created 3 debts (queued locally)
- WHEN the network becomes available and sync loop starts
- THEN `drain()` is called before remote fetch
- AND all 3 debts are pushed to Firestore
- AND the pending queue is empty after successful drain

#### Scenario: Drain on cold start with pending ops

- GIVEN the app was closed with 2 pending operations in the local queue
- WHEN the app reopens with network available
- THEN `drain()` replays both operations to Firestore on startup

#### Scenario: Drain failure with retry

- GIVEN a pending operation fails to push (e.g., document deleted remotely)
- WHEN `drain()` processes that operation
- THEN the operation is marked failed with incremented retry count
- AND remaining operations continue to drain
- AND the failed operation is retried on next sync cycle with backoff

#### Scenario: Drain on sign-out

- GIVEN pending operations exist in the queue
- WHEN the user signs out
- THEN the system attempts one final drain
- AND the queue is cleared regardless of outcome

### Requirement: Immediate Sync Startup

The system SHALL initiate sync immediately when `startSync()` is called, without waiting for the first network monitor emission. Profile, Debt, and Expense repositories MUST attempt sync on startup.

#### Bug Context

**Síntoma**: Tras hacer login, el usuario ve los eventos pero NO ve perfiles, deudas ni gastos. Estos aparecen solo después de esperar varios segundos o reiniciar la app.

**Causa raíz**: `OfflineFirstProfileRepository.kt` líneas 33-39, `OfflineFirstDebtRepository.kt` líneas 31-39, y `OfflineFirstExpenseRepository.kt` líneas 31-38 solo suscriben a `networkMonitor.isOnline` y esperan a que emita `true`. En contraste, `OfflineFirstEventRepository.kt` líneas 37-48 llama `startSyncLoop()` inmediatamente y luego monitorea la red con `.drop(1)`.

**Por qué el fix lo resuelve**: Replicar el patrón de EventRepository: llamar `startSyncLoop()` inmediatamente al invocar `startSync()`, luego monitorear cambios de red con `.drop(1)` para evitar doble arranque.

#### Scenario: App starts with network available

- GIVEN the device has network connectivity at startup
- WHEN `startSync()` is called on each repository
- THEN all three repositories (Profile, Debt, Expense) begin syncing immediately
- AND no delay is introduced by waiting for network monitor

#### Scenario: App starts offline

- GIVEN the device has no network at startup
- WHEN `startSync()` is called
- THEN repositories attempt sync, fail gracefully, and retry when network returns

#### Scenario: Network toggles rapidly

- GIVEN sync is running and network goes offline then online
- WHEN the network monitor emits the online state
- THEN sync restarts without delay

### Requirement: Snapshot Listeners for Real-Time Sync

The system SHALL use Firestore snapshot listeners (`.snapshots`) for observing debts and expenses. One-shot `.get()` flows MUST NOT be used for sync data.

#### Bug Context

**Síntoma**: Los cambios de deudas y gastos hechos por otros usuarios NO aparecen en tiempo real. El Dashboard tarda 3-10 segundos en cargar y quema cuotas de Firestore con queries repetitivas.

**Causa raíz**: `FirestoreDebtRepository.observeAllDebts()` líneas 26-52 y `FirestoreExpenseRepository.observeAllExpenses()` líneas 26-52 usan `flow { emit(allDebts) }` — emiten una vez y completan. El sync loop en `OfflineFirstDebtRepository.kt` líneas 66-80 hace `.first()` (crea listener, obtiene datos, cancela), luego `.collect` (crea OTRO listener). Como el flow completa inmediatamente, el `while(isActive)` loop itera de nuevo sin delay, creando un polling loop continuo.

**Por qué el fix lo resuelve**: Reemplazar `.get()` con `.snapshots` (snapshot listeners) para que los flows sean continuos y reactivos. Los cambios de Firestore se propagan en tiempo real sin polling.

#### Scenario: Real-time debt propagation

- GIVEN User A and User B both observe the same event's debts
- WHEN User A adds a debt on device 1
- THEN User B sees the debt appear within seconds via snapshot listener

#### Scenario: Snapshot listener reconnection

- GIVEN a snapshot listener is active for debts
- WHEN the connection drops temporarily
- THEN the listener reconnects automatically and emits updated data

#### Scenario: No polling loop

- GIVEN snapshot listeners are active
- WHEN no data changes occur
- THEN no Firestore read requests are made (no polling)

### Requirement: Single Firestore Subscription Per Collection

Each sync loop SHALL maintain exactly one Firestore snapshot subscription per collection. The system MUST NOT create duplicate subscriptions via `.first()` followed by `.collect`.

#### Bug Context

**Síntoma**: Cada sync loop crea DOS listeners de Firestore por colección (uno con `.first()`, otro con `.collect`), duplicando el tráfico de red y causando upserts duplicados en SQLDelight.

**Causa raíz**: Patrón en `OfflineFirstProfileRepository.kt` líneas 62-75, `OfflineFirstDebtRepository.kt` líneas 66-80, `OfflineFirstExpenseRepository.kt` líneas 63-76:
```kotlin
val firstEmission = withTimeoutOrNull(15_000) {
    remoteRepository.observeProfiles().first()  // suscripción #1
}
if (firstEmission != null) { upsertProfiles(firstEmission) }
remoteRepository.observeProfiles().collect { ... }  // suscripción #2
```
Cada llamada a `observeProfiles()` crea un NUEVO snapshot listener.

**Por qué el fix lo resuelve**: Usar una sola suscripción con `onEach` para manejar todas las emisiones:
```kotlin
remoteRepository.observeProfiles()
    .onEach { upsertProfiles(it) }
    .collect()
```

#### Scenario: Single subscription on sync start

- GIVEN a repository starts its sync loop
- WHEN the snapshot listener is created
- THEN exactly one subscription exists for that collection
- AND the same listener handles all subsequent emissions

#### Scenario: No duplicate upserts

- GIVEN a single snapshot listener is active
- WHEN Firestore emits an update
- THEN the local database receives exactly one upsert per changed document
