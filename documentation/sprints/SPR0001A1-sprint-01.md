# SPR0001A1: Sprint 01 - Base inicial

> **Código:** SPR0001A1
> **Versión:** A
> **Revisión:** 4
> **Fecha:** 2026-04-06

## Objetivo del sprint
Construir la base funcional mínima de `CuentaMorosos`: perfiles globales, creación de eventos, persistencia local y navegación principal.

## Estado
Hecho

## Requisitos e historias incluidas
| ID | Tipo | Nombre | Prioridad | Estado | Dependencias |
|---|---|---|---|---|---|
| DD0001A1 | DD | Modelo funcional base | Alta | Hecho | - |
| FR0001A1 | FR | Gestión de eventos y calendario | Alta | Hecho | DD0001A1 |
| FR0002A1 | FR | Gestión global de perfiles | Alta | Hecho | DD0001A1 |
| NFR0002A1 | NFR | Persistencia y usabilidad operativa | Alta | Hecho | DD0001A1 |
| UI0001A1 | UI | Pantalla de eventos | Alta | Hecho | FR0001A1 |
| UI0003A1 | UI | Pantalla de perfiles | Alta | Hecho | FR0002A1 |

## Tareas técnicas
- diseñar entidades `Evento`, `Perfil` y relación de deuda
- implementar almacenamiento local persistente
- crear navegación base entre eventos y perfiles
- permitir alta y edición básica de eventos y perfiles

## Riesgos o bloqueos
- definición definitiva del modelo de datos
- selección del mecanismo de persistencia local

## Definition of Done
- [x] se pueden crear y listar eventos
- [x] se pueden crear y listar perfiles
- [x] los datos persisten entre sesiones
- [x] la navegación principal entre pantallas está operativa

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-05 | A | A.1 | Alta | Creación inicial del sprint 01. |
| 2026-04-05 | A | A.2 | Actualización | Se concreta el alcance del sprint 01 con requisitos y tareas base. |
| 2026-04-05 | A | A.3 | Actualización | Se inicia el desarrollo técnico del sprint 01 con la base funcional de eventos, perfiles y persistencia local. |
| 2026-04-06 | A | A.4 | Actualización | Se marca el sprint 01 como completado tras implementar eventos, perfiles, navegación base y persistencia local. |
