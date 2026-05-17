# SPR0005A1: Sprint 05 - Calendario y búsqueda

> **Código:** SPR0005A1
> **Versión:** A
> **Revisión:** 2
> **Fecha:** 2026-04-30

## Objetivo del sprint
Mejorar la navegación y la localización de eventos: añadir una vista de calendario mensual y una barra de búsqueda con filtros de estado en `EventsScreen`.

## Estado
Hecho

## Requisitos e historias incluidas
| ID | Tipo | Nombre | Prioridad | Estado | Dependencias |
|---|---|---|---|---|---|
| FR0001B1 | FR | Vista de calendario de eventos | Baja | Hecho | FR0001A1 |
| UX0001A1 | UX | Búsqueda y filtrado de eventos | Media | Hecho | FR0001A1 |

## Tareas técnicas

### FR0001B1 — Vista de calendario de eventos ✓
- Nuevo valor `CALENDAR` en enum `MainSection`.
- `CalendarScreen` composable con cuadrícula mensual, navegación entre meses.
- Día hoy resaltado con `primaryContainer`; días con eventos muestran punto indicador.
- Al seleccionar un día con eventos, lista de eventos debajo del calendario.

### UX0001A1 — Búsqueda y filtrado de eventos ✓
- `OutlinedTextField` de búsqueda en `EventsScreen` con trailing icon para limpiar.
- `FilterChip`: "Todos" / "Con deuda" / "Sin deuda".
- `filteredEvents` con `derivedStateOf` aplicando búsqueda + filtro.
- Estado vacío diferenciado para búsqueda sin resultados.

## Definition of Done
- [x] Existe una pestaña "Calendario" en la barra de navegación inferior
- [x] El calendario mensual muestra correctamente todos los eventos en su fecha
- [x] Se puede navegar entre meses
- [x] Al pulsar un día con eventos se ven los eventos de ese día
- [x] `EventsScreen` tiene barra de búsqueda funcional por nombre
- [x] Los chips "Todos / Con deuda / Sin deuda" filtran la lista correctamente

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-30 | A | A.1 | Alta | Creación del sprint 05 con calendario y búsqueda de eventos. |
| 2026-04-30 | A | A.2 | Actualización | Sprint completado: CalendarScreen y búsqueda+filtros en EventsScreen implementados. |
