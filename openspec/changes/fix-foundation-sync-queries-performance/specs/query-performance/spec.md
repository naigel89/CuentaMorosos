# Query Performance Specification

## Purpose

Minimize Firestore queries: eliminate N+1 patterns in dashboard loading and replace collection-wide queries with direct document snapshots for single-event observation.

## Requirements

### Requirement: Efficient Dashboard Queries

The system SHALL load dashboard debt and expense data without N+1 query patterns. The total number of Firestore queries MUST NOT scale linearly with the number of events.

#### Bug Context

**Síntoma**: El Dashboard tarda 5-15 segundos en cargar con 10 eventos. Firestore cobra cada query y se queman cuotas rápidamente.

**Causa raíz**: `FirestoreDebtRepository.observeAllDebts()` hace 3 queries para encontrar event IDs (owner, member, participant), luego 1 query POR EVENTO para traer deudas:
```kotlin
val ownerSnapshot = db.collection("events").where { "ownerId" equalTo uid }.get()
val memberSnapshot = db.collection("events").where { "memberIds" contains uid }.get()
val participantSnapshot = db.collection("events").where { "participantIds" contains uid }.get()
for (eventId in eventIds) {
    val debtsSnapshot = db.collection("events").document(eventId).collection("debts").get()
}
```
Con 10 eventos: 3 + 10 = 13 queries secuenciales. Mismo patrón en `FirestoreExpenseRepository.observeAllExpenses()`. Total en Dashboard: 10+ queries iniciales + listeners continuos.

**Por qué el fix lo resuelve**: Usar snapshot listeners por evento (como `observeDebts(eventId)` que ya usa `.snapshots`) en vez de `.get()` one-shot. O restructurar a collection group queries si Firestore lo permite. El número de queries debe ser constante, no escalar con N eventos.

#### Scenario: Dashboard loads with multiple events

- GIVEN the user has 10 events with associated debts and expenses
- WHEN the dashboard screen loads
- THEN debts and expenses are loaded via snapshot listeners (not per-event polling)
- AND total Firestore read operations are constant (not proportional to event count)

#### Scenario: Snapshot listeners replace per-event queries

- GIVEN the dashboard previously queried debts/expenses per event
- WHEN the fix is applied
- THEN a single `observeAllDebts()` and `observeAllExpenses()` call provides data for all events

### Requirement: Direct Document Observation

`observeEvent(eventId)` SHALL query the specific Firestore document directly, not derive from a full collection query.

#### Bug Context

**Síntoma**: Al abrir el detalle de un evento, se crean 3 listeners de Firestore (owner, member, participant), se combinan, deduplican y ordenan — solo para extraer UN evento. Esto causa latencia innecesaria y tráfico de red.

**Causa raíz**: `FirestoreEventRepository.observeEvent(eventId)` líneas 46-49:
```kotlin
override fun observeEvent(eventId: String): Flow<EventItem?> =
    observeEvents().map { events ->
        events.find { it.id == eventId }
    }
```
`observeEvents()` crea 3 snapshot listeners (ownerFlow, memberFlow, participantFlow), los combina con `combine()`, deduplica, y ordena — todo para hacer `.find { it.id == eventId }`.

**Por qué el fix lo resuelve**: Query directa al documento:
```kotlin
override fun observeEvent(eventId: String): Flow<EventItem?> =
    collection.document(eventId).snapshots.map { it.toEventItem() }
```
Un solo listener, datos directos, sin combinación ni filtrado.

#### Scenario: Single event detail opens

- GIVEN the user navigates to an event detail screen for event "abc-123"
- WHEN `observeEvent("abc-123")` is called
- THEN a single document listener is created for document "abc-123"
- AND no collection-wide listeners are started

#### Scenario: Event updates via direct listener

- GIVEN a direct document listener is active for event "abc-123"
- WHEN the event is modified in Firestore
- THEN the observer emits the updated event data

#### Scenario: Event deleted

- GIVEN a direct document listener is active for event "abc-123"
- WHEN the event is deleted from Firestore
- THEN the observer emits `null`
