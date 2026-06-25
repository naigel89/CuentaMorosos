# SPR0008B1: Sprint 08B.1 - Usabilidad Básica

> **Código:** SPR0008B1
> **Versión:** A
> **Revisión:** 3
> **Fecha:** 2026-05-14

## Objetivo del sprint
Mejorar la experiencia de usuario inmediata eliminando restricciones innecesarias para el creador y reduciendo la carga cognitiva en la pantalla de cálculo.

## Estado
Hecho — implementado e integrado. `REAL_CONSUMPTION` es el modo de reparto por defecto, con selector desplegable para modos alternativos. Creador opcional funcional desde detalle de evento.

## Requisitos e historias incluidas
| ID | Tipo | Nombre | Prioridad | Estado | Dependencias |
|---|---|---|---|---|---|
| FR0007A1 | FR | Creador de evento opcional en el reparto | Media | Hecho | SPR0008A1 |
| UI0006A1 | UI | Simplificación de la interfaz de modos de cálculo | Media | Hecho | UI0005A1 |

## Tareas técnicas

### T-UX-01 — Creador opcional en la lista de participantes ✓ (en shared/)
- `EventDetailScreen` permite que el propietario se elimine de `memberIds` con acción "No participar".
- Diálogo de confirmación antes de salir del reparto.
- `ownerId` se mantiene intacto para conservar permisos de edición.
- `FirestoreEventRepository.observeEvents()` observa por `ownerId` y por `memberIds`.

### T-UX-02 — Simplificación de QuickSplitDialog ✓ (en shared/)
- Flujo centrado en modo predeterminado `REAL_CONSUMPTION`.
- Botón "Cambiar modo de reparto" con panel desplegable para modos alternativos.
- Vista previa recalculada en tiempo real al cambiar de modo.

## Definition of Done
- [x] Código de creador opcional implementado en shared/
- [x] QuickSplitDialog simplificado con selector desplegable en shared/
- [x] MainActivity wireada: CalculatorSheet usa REAL_CONSUMPTION por defecto con modo selector
- [x] Flujo validado en app real

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-05-01 | A | A.1 | Alta | Creación del sprint 08B1 enfocado en usabilidad básica. |
| 2026-05-01 | A | A.2 | Actualización | Sprint completado en shared/: creador opcional y simplificación de QuickSplitDialog. |
| 2026-05-14 | A | A.3 | Corrección | Estado cambiado a Parcial: código en shared/ pero MainActivity no integrada. |
| 2026-06-25 | A | A.4 | Actualización | Estado cambiado a Hecho: CalculatorSheet con REAL_CONSUMPTION por defecto, creador opcional funcional. Integración completa validada. |
