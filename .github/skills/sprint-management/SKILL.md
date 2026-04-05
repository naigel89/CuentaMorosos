---
name: sprint-management
description: 'Organiza requisitos, backlog y tareas pendientes por sprints. Úsalo para saber qué queda por hacer, priorizar el siguiente desarrollo, planificar entregas y consultar `documentation/sprints/` antes de implementar.'
argument-hint: 'Ej.: "organiza el backlog en sprints" o "qué queda pendiente del sprint actual"'
user-invocable: true
---

# Gestión de sprints del proyecto

## Objetivo
Organizar todos los requisitos y tareas pendientes en diferentes sprints dentro de `documentation/sprints/`, para que Copilot pueda consultar qué queda pendiente y cómo debe continuar el desarrollo del proyecto.

## Estructura obligatoria
Trabajar con esta base documental:

```text
documentation/sprints/
├── README.md
├── BKL0001A1-product-backlog.md
├── SPR0001A1-sprint-01.md
└── SPR0002A1-sprint-02.md
```

- `README.md`: guía del sistema de sprints.
- `BKL*.md`: backlog general priorizado.
- `SPR*.md`: detalle de cada sprint con alcance, tareas y estado.

## Cuándo usar esta skill
Usa esta skill cuando necesites:
- organizar requisitos en sprints
- saber qué queda pendiente
- decidir el siguiente paso de desarrollo
- priorizar historias o tareas técnicas
- replanificar el proyecto tras cambios o bloqueos

## Reglas obligatorias
1. **Revisar primero la documentación base**: `documentation/fr/`, `documentation/nfr/`, `documentation/r-neg/`, `documentation/dd/`, `documentation/api/` y el código relacionado.
2. **No inventar trabajo**: si falta contexto, registrarlo como pendiente, supuesto o bloqueo.
3. **Mantener trazabilidad** enlazando cada tarea con sus IDs documentales (`FR0001A1`, `RN0002A1`, etc.).
4. **Usar estos estados**: `Pendiente`, `En progreso`, `Bloqueado`, `Hecho`, `Descartado`.
5. **Priorizar por valor, dependencia y riesgo** antes que por orden arbitrario.
6. **Consultar siempre el backlog y el sprint actual** antes de responder qué queda por hacer o cómo seguir desarrollando el proyecto.
7. **Usar nomenclatura versionada** también en estos archivos:
   - `BKL0001A1-product-backlog.md`
   - `SPR0001A1-sprint-01.md`
8. **Todo nuevo documento de sprint** empieza por defecto en versión `A` y revisión `1`, e incluye `## Changelog`.

## Procedimiento

### 1. Analizar el estado actual
Revisar:
- requisitos existentes
- reglas de negocio
- dependencias técnicas
- estado real del código
- backlog y sprint actual si ya existen

### 2. Extraer el trabajo pendiente
Crear una lista única con:
- historias de usuario
- tareas técnicas
- deuda técnica
- bloqueos
- mejoras no funcionales

### 3. Agrupar por sprint
Asignar cada ítem al sprint más adecuado según:
- prioridad de negocio
- orden de dependencias
- complejidad estimada
- riesgo técnico

### 4. Documentar backlog y sprint
Actualizar:
- `BKL0001A1-product-backlog.md`
- `SPRxxxx...` del sprint activo o siguiente

### 5. Guiar el desarrollo
Cuando se pida continuar el proyecto o saber qué falta:
1. consultar el backlog
2. revisar el sprint activo
3. identificar el siguiente ítem no bloqueado
4. proponer la implementación respetando el orden planificado

## Plantilla de backlog

```md
# BKL0001A1: Product Backlog

> **Código:** BKL0001A1
> **Versión:** A
> **Revisión:** 1
> **Fecha:** YYYY-MM-DD

## Resumen
Vista global de requisitos, tareas y pendientes del proyecto.

| ID | Tipo | Descripción | Prioridad | Sprint objetivo | Estado | Dependencias |
|---|---|---|---|---|---|---|

## Notas de priorización
- criterio 1
- criterio 2

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| YYYY-MM-DD | A | 1 | Alta | Creación inicial del backlog. |
```

## Plantilla de sprint

```md
# SPR0001A1: Sprint 01 - <nombre>

> **Código:** SPR0001A1
> **Versión:** A
> **Revisión:** 1
> **Fecha:** YYYY-MM-DD

## Objetivo del sprint
Resultado principal esperado.

## Estado
Pendiente / En progreso / Bloqueado / Hecho

## Requisitos e historias incluidas
| ID | Tipo | Nombre | Prioridad | Estado | Dependencias |
|---|---|---|---|---|---|

## Tareas técnicas
- tarea 1
- tarea 2

## Riesgos o bloqueos
- riesgo o bloqueo 1

## Definition of Done
- [ ] criterio 1
- [ ] criterio 2

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| YYYY-MM-DD | A | 1 | Alta | Creación inicial del sprint. |
```

## Resultado esperado
Mantener una visión clara del trabajo pendiente y del orden recomendado de implementación, de forma que Copilot pueda consultar `documentation/sprints/` y continuar el desarrollo con contexto y prioridad.