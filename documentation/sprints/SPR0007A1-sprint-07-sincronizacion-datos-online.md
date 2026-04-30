# SPR0007A1: Sprint 07 - Sincronización de datos online

> **Código:** SPR0007A1
> **Versión:** A
> **Revisión:** 1
> **Fecha:** 2026-04-30

## Objetivo del sprint
Reemplazar el almacenamiento local en `SharedPreferences` por Firestore como fuente de verdad, implementando el patrón de repositorio y la migración automática de datos existentes.

## Estado
Pendiente

## Requisitos e historias incluidas
| ID | Tipo | Nombre | Prioridad | Estado | Dependencias |
|---|---|---|---|---|---|
| US-05 | US | Crear un evento online | Alta | Pendiente | SPR0006 |
| US-06 | US | Ver eventos en tiempo real | Alta | Pendiente | US-05 |

## Tareas técnicas

### T2-01 — Interfaz de repositorio
- Crear interfaz `EventRepository` con operaciones `observeEvents(): Flow`, `saveEvent`, `deleteEvent`.
- Crear interfaces equivalentes para `DebtRepository` y `ExpenseRepository`.

### T2-02 — FirestoreEventRepository
- Implementar `FirestoreEventRepository` que lee y escribe en la colección `events`.
- Añadir `ownerId` y `memberIds` al modelo y al documento Firestore.
- Los eventos se filtran por `memberIds` contiene el UID del usuario actual.

### T2-03 — FirestoreDebtRepository
- Implementar `FirestoreDebtRepository` usando la sub-colección `events/{eventId}/debts`.

### T2-04 — FirestoreExpenseRepository
- Implementar `FirestoreExpenseRepository` usando la sub-colección `events/{eventId}/expenses`.

### T2-05 — Refactorización de ViewModels
- Actualizar los ViewModels existentes para inyectar y usar los nuevos repositorios.
- Eliminar las referencias directas a `CuentaMorososLocalStore`.

### T2-06 — Migración automática de datos locales
- Implementar `MigrationManager` siguiendo la guía `03-guia-migracion-datos.md`.
- Detectar datos en `SharedPreferences` y subirlos a Firestore en el primer login.
- Marcar el flag `migrated: true` en `users/{uid}` al completar.
- Mostrar pantalla de carga con mensaje durante la migración.

### T2-07 — Reglas de seguridad de Firestore
- Configurar las reglas de seguridad según las especificadas en `01-especificaciones-tecnicas.md`.
- Verificar que un usuario sin acceso no puede leer documentos ajenos.

### T2-08 — Índices de Firestore
- Crear los índices compuestos necesarios para las consultas por `memberIds` y `ownerId`.

### T2-09 — Pruebas de sincronización en tiempo real
- Verificar que los cambios se reflejan en menos de 5 segundos desde otro dispositivo con la misma cuenta.

## Riesgos o bloqueos
- La refactorización de ViewModels puede introducir regresiones en las pantallas existentes; ejecutar pruebas de regresión tras cada cambio.

## Definition of Done
- [ ] Los datos se guardan en Firestore en lugar de SharedPreferences
- [ ] Los datos migrados del almacenamiento local aparecen correctamente tras el primer login
- [ ] Los cambios se reflejan en tiempo real en el mismo dispositivo
- [ ] Un usuario sin acceso no puede leer eventos ajenos

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-30 | A | 1 | Alta | Creación del sprint 07 con sincronización online y migración de datos. |
