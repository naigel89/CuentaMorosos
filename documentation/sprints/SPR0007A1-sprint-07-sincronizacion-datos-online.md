# SPR0007A1: Sprint 07 - Sincronización de datos online

> **Código:** SPR0007A1
> **Versión:** A
> **Revisión:** 3
> **Fecha:** 2026-05-14

## Objetivo del sprint
Reemplazar el almacenamiento local en `SharedPreferences` por Firestore como fuente de verdad, implementando el patrón de repositorio y la migración automática de datos existentes.

## Estado
Hecho — repositorios OfflineFirst envueltos alrededor de Firestore, con cola de operaciones pendientes (`PendingOperationQueue`) y sincronización escalonada. Todo integrado en `MainActivity` vía `RepositoryProvider`.

## Requisitos e historias incluidas
| ID | Tipo | Nombre | Prioridad | Estado | Dependencias |
|---|---|---|---|---|---|
| US-05 | US | Crear un evento online | Alta | Hecho | SPR0006 |
| US-06 | US | Ver eventos en tiempo real | Alta | Hecho | US-05 |

## Tareas técnicas

### T2-01 — Interfaz de repositorio ✓
- Interfaces `EventRepository`, `ProfileRepository`, `DebtRepository`, `ExpenseRepository` en `commonMain`.
- `RepositoryProvider` con acceso a todas las implementaciones.

### T2-02 a T2-05 — Firestore repositories y ViewModels ✓ (en shared/)
- `FirestoreEventRepository`, `FirestoreProfileRepository`, `FirestoreDebtRepository`, `FirestoreExpenseRepository` en `commonMain` usando `dev.gitlive:firebase-firestore`.
- `EventsViewModel`, `EventDetailViewModel`, `ProfilesViewModel` refactorizados para usar repositorios.
- `deleteDebtsForProfile` con `collectionGroup` y batches de 499.

### T2-06 — MigrationManager ✓ (en shared/)
- `MigrationManager.migrate()` sube eventos, perfiles, deudas y gastos a Firestore.
- `MigrationScreen` con spinner y reintento ante error.

### T2-07 — Reglas de seguridad de Firestore ⏳
- Especificadas en `DD0003A1`. Pendientes de aplicar en Firebase Console.

## Riesgos o bloqueos
- La query `collectionGroup("debts")` requiere índice compuesto en Firestore.

## Definition of Done
- [x] Repositorios Firestore implementados en commonMain con dev.gitlive
- [x] ViewModels refactorizados para usar repositorios online
- [x] MigrationManager y MigrationScreen implementados
- [x] MainActivity wireada con RepositoryProvider: OfflineFirst* repos + PendingOperationQueue + sync escalonada
- [ ] Reglas de seguridad aplicadas en Firebase Console
- [x] Datos migrados del almacenamiento local aparecen correctamente tras login

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-30 | A | A.1 | Alta | Creación del sprint 07 con sincronización online. |
| 2026-05-01 | A | A.2 | Actualización | Sprint completado en shared/: ProfileRepository, deleteDebtsForProfile, ProfilesViewModel refactorizado, CuentaMorososApp migrado. |
| 2026-05-14 | A | A.3 | Corrección | Estado cambiado a Parcial: código en shared/ pero MainActivity no integrada. |
| 2026-06-25 | A | A.4 | Actualización | Estado cambiado a Hecho: RepositoryProvider integrado en MainActivity con OfflineFirstEventRepository, OfflineFirstDebtRepository, OfflineFirstExpenseRepository, OfflineFirstProfileRepository + PendingOperationQueue + sync escalonada. |
