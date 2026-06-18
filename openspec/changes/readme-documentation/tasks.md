# Tasks: Actualización Integral del README.md

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~300-400 (single-file markdown rewrite) |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | ask-on-risk |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Low

## Phase 1: Verificación de Fuentes

- [x] 1.1 Verificar estados de evento (OPEN, CALCULATED, CLOSED) en `shared/.../model/` — contrastar contra `EventState` enum
- [x] 1.2 Verificar 6 modos de reparto (`SplitMode`) con sus IDs en el motor de cálculo del módulo `shared`
- [x] 1.3 Verificar 11 categorías (`ExpenseCategory`) en el modelo del módulo `shared`
- [x] 1.4 Verificar estructura real de directorios (`app/`, `shared/`, `documentation/`, `openspec/`, `keystore/`) contra el árbol del diseño
- [x] 1.5 Extraer versiones exactas de dependencias desde `app/build.gradle.kts` y `shared/build.gradle.kts`
- [x] 1.6 Contar archivos de test reales (`app/src/test/` y `shared/src/commonTest/`)

## Phase 2: Escritura del README

- [x] 2.1 Escribir Sección 1 — Encabezado: logo, tagline en tuteo, 3 párrafos (qué es, diferenciales, llamado a la acción)
- [x] 2.2 Escribir Sección 2 — Stack Tecnológico: 6 subsecciones con tablas Componente|Versión|Capa (36 componentes) + nota de ausencias
- [x] 2.3 Escribir Sección 3 — Instalación: prerrequisitos, 6 pasos Firebase, archivos de config, 5 comandos gradle
- [x] 2.4 Escribir Sección 4 — Estructura del Proyecto: árbol ASCII con ~45 entradas + tabla de módulos (app, shared, iosApp)
- [x] 2.5 Escribir Sección 5 — 7 subsecciones de funcionalidades: Eventos, Gastos, Liquidación, Perfiles, Panel, Notificaciones, Offline
- [x] 2.6 Escribir Sección 6 — Arquitectura: diagrama ASCII offline-first, 8 decisiones clave, ejemplo de flujo de datos
- [x] 2.7 Escribir Sección 7 — Testing: tabla de frameworks con comandos, 4 comandos bash, nota de cobertura
- [x] 2.8 Escribir Sección 8 — Recursos Adicionales: 4 enlaces relativos (`documentation/`, `AGENTS.md`, `LICENSE`, `openspec/`)
- [x] 2.9 Escribir Sección 9 — Capturas de Pantalla: aviso «en construcción» + 6 placeholders `<!-- SCREENSHOT: ... -->`

## Phase 3: Verificación y Revisión

- [x] 3.1 Verificar precisión factual: cruzar cada afirmación contra código real (`EventState`, `SplitMode`, `ExpenseCategory`, `SettlementEngine`)
- [x] 3.2 Verificar enlaces: confirmar existencia en disco de `documentation/README.md`, `AGENTS.md`, `LICENSE`, `openspec/`
- [x] 3.3 Auditoría de seguridad: confirmar ausencia de rutas de keystore, contraseñas o valores de `google-services.json`
- [x] 3.4 Revisión final: tuteo consistente, máximo 1 emoji por sección, backticks en términos técnicos, success criteria del proposal
