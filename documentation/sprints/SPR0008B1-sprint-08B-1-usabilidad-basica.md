# SPR0008B1: Sprint 08B.1 - Usabilidad Básica

> **Código:** SPR0008B1
> **Versión:** A
> **Revisión:** A.2
> **Fecha:** 2026-05-01

## Objetivo del sprint
Mejorar la experiencia de usuario inmediata eliminando restricciones innecesarias para el creador y reduciendo la carga cognitiva en la pantalla de cálculo.

## Estado
Hecho

## Requisitos e historias incluidas
| ID | Tipo | Nombre | Prioridad | Estado | Dependencias |
|---|---|---|---|---|---|
| FR0007A1 | FR | Creador de evento opcional en el reparto | Media | Hecho | SPR0008A1 |
| UI0006A1 | UI | Simplificación de la interfaz de modos de cálculo | Media | Hecho | UI0005A1 |

## Tareas técnicas

### T-UX-01 — Creador opcional en la lista de participantes ✓
- `EventDetailScreen` permite que el propietario se elimine de `memberIds` usando la acción `No participar`.
- Se añade diálogo de confirmación: "¿Seguro? No participarás en el reparto de este evento".
- Se mantiene `ownerId` intacto en `EventItem` para conservar permisos de edición del evento.
- `FirestoreEventRepository.observeEvents()` se actualiza para observar por `ownerId` y por `memberIds`, evitando que el creador pierda visibilidad del evento al salir del reparto.

### T-UX-02 — Simplificación de QuickSplitDialog ✓
- Flujo centrado en modo predeterminado `REAL_CONSUMPTION`.
- Botón principal renombrado a `Cambiar modo de reparto`, con panel desplegable para `SIMPLE_AVG` y `BY_CATEGORY`.
- `CUSTOM_PERCENTAGE` queda en sección `Avanzado` dentro del desplegable.
- La vista previa se recalcula en tiempo real al cambiar de modo.

## Definition of Done
- [x] El propietario puede eliminarse a sí mismo de la lista de participantes del evento sin perder la propiedad del evento.
- [x] La pantalla de cálculo no obliga a elegir un modo al inicio; usa Consumo Real por defecto.
- [x] Los modos alternativos son accesibles mediante un menú desplegable/acordeón.

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-05-01 | A | A.1 | Alta | Creación del sprint 08B1 enfocado en usabilidad básica. |
| 2026-05-01 | A | A.2 | Actualización | Sprint completado: creador opcional en membresía del evento y simplificación de QuickSplitDialog con selector avanzado desplegable. |
