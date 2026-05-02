# SPR0007A1: Sprint 07 - Sincronización de datos online

> **Código:** SPR0007A1
> **Versión:** A
> **Revisión:** 2
> **Fecha:** 2026-05-01

## Objetivo del sprint
Reemplazar el almacenamiento local en `SharedPreferences` por Firestore como fuente de verdad, implementando el patrón de repositorio y la migración automática de datos existentes.

## Estado
Hecho

## Requisitos e historias incluidas
| ID | Tipo | Nombre | Prioridad | Estado | Dependencias |
|---|---|---|---|---|---|
| US-05 | US | Crear un evento online | Alta | Hecho | SPR0006 |
| US-06 | US | Ver eventos en tiempo real | Alta | Hecho | US-05 |

## Tareas técnicas

### T2-01 — Interfaz de repositorio ✓
- Interfaz `EventRepository` con `observeEvents(): Flow`, `saveEvent`, `deleteEvent` — ya existía desde SPR0006.
- Creada interfaz `ProfileRepository` con `observeProfiles(): Flow`, `saveProfile`, `deleteProfile`.
- Añadido método `deleteDebtsForProfile(profileId)` a `DebtRepository` para borrado en cascada al eliminar un perfil.

### T2-02 — FirestoreProfileRepository ✓
- Nuevo archivo `data/repository/ProfileRepository.kt` con la interfaz.
- Nuevo archivo `data/repository/FirestoreProfileRepository.kt`:
  - Filtra perfiles por `ownerId == currentUser.uid` mediante query Firestore.
  - `saveProfile` añade el `ownerId` del usuario autenticado al documento.
  - `deleteProfile` elimina el documento de la colección `profiles`.
- `RepositoryProvider` actualizado con `profileRepository: ProfileRepository`.

### T2-03 — FirestoreDebtRepository: deleteDebtsForProfile ✓
- Implementado usando `collectionGroup("debts").whereEqualTo("profileId", profileId)`.
- Las eliminaciones se ejecutan en batches de 499 para respetar el límite de Firestore.

### T2-04 — Refactorización de ProfilesViewModel ✓
- `ProfilesViewModel` ahora recibe `ProfileRepository` y `DebtRepository`.
- Observa `profileRepository.observeProfiles()` y expone `StateFlow<List<ProfileItem>>`.
- `deleteProfile(profile)` ejecuta primero `deleteDebtsForProfile` y luego `deleteProfile` en cascada.
- `AppViewModelFactory` actualizado para instanciar `ProfilesViewModel` con ambos repositorios.

### T2-05 — Migración de CuentaMorososApp ✓
- `profiles` en `CuentaMorososApp` ya no proviene de `LocalStore` (`mutableStateListOf`), sino del `StateFlow` del `ProfilesViewModel`.
- Callbacks `onSaveProfile` y `onDeleteProfile` delegan al ViewModel.
- `persistData()` simplificado: ya solo persiste `UserPreferences` (no perfiles).
- Función helper `upsertProfile` eliminada (ya no necesaria).

### T2-06 — MigrationManager y MigrationScreen ✓
- Ya implementados en SPR0006: `MigrationManager.migrate()` sube eventos, perfiles, deudas y gastos a Firestore en batch.
- `MigrationScreen` muestra spinner durante la migración y ofrece reintento ante error.
- `AppNavHost` en `MainActivity` redirige a `MigrationScreen` tras login si hay datos locales no migrados.

### T2-07 — Reglas de seguridad de Firestore
- Especificadas en `DD0003A1-especificaciones-tecnicas-online.md`.
- Deben aplicarse manualmente en Firebase Console antes de producción.

## Riesgos o bloqueos
- La query `collectionGroup("debts")` requiere un índice compuesto en Firestore para el campo `profileId`; debe crearse en Firebase Console.
- La refactorización de ViewModels puede introducir regresiones en las pantallas existentes; ejecutar pruebas de regresión tras cada cambio.

## Definition of Done
- [x] Los perfiles se guardan y observan en tiempo real desde Firestore
- [x] Al eliminar un perfil, sus deudas en todos los eventos se borran en cascada
- [x] Los datos migrados del almacenamiento local aparecen correctamente tras el primer login
- [x] Los cambios se reflejan en tiempo real en el mismo dispositivo
- [ ] Un usuario sin acceso no puede leer eventos ajenos (pendiente de configurar reglas en Firebase Console)

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-30 | A | A.1 | Alta | Creación del sprint 07 con sincronización online y migración de datos. |
| 2026-05-01 | A | A.2 | Actualización | Sprint completado: ProfileRepository + FirestoreProfileRepository, deleteDebtsForProfile en cascada, ProfilesViewModel refactorizado con Firestore, CuentaMorososApp migrado a ViewModel para perfiles. MigrationManager y MigrationScreen ya estaban implementados en SPR0006. |
| 2026-05-02 | A | A.3 | Actualización | Hardening de migración relacionado con SPR0008A1: marcado de `migrated` reforzado con `set(..., merge)` para tolerar ausencia previa de documento en `users/{uid}`. |
