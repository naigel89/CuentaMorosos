# SPR0008B2: Sprint 08B.2 - Usabilidad Avanzada y Flexibilidad

> **Código:** SPR0008B2
> **Versión:** A
> **Revisión:** 3
> **Fecha:** 2026-05-14

## Objetivo del sprint
Reducir la fricción de adopción mediante perfiles locales (fantasmas) y permitir repartos complejos basados en pesos individuales por ítem.

## Estado
Parcial — código implementado en `shared/src/commonMain/` pero **NO integrado** en `MainActivity`. La app actual no usa estas mejoras.

## Requisitos e historias incluidas
| ID | Tipo | Nombre | Prioridad | Estado | Dependencias |
|---|---|---|---|---|---|
| FR0006A1 | FR | Perfiles fantasma y vinculación manual | Alta | Parcial | SPR0008A1 |
| FR0008A1 | FR | Divisiones porcentuales por ítem individual | Alta | Parcial | FR0005A1 |

## Tareas técnicas

### T-DATA-01 — Modelo de Perfiles Fantasma ✓ (en shared/)
- `ProfileItem` ampliado con `isGhost: Boolean` y `linkedEmail: String?`.
- `ProfileEditorDialog` permite crear/editar perfiles locales con email de vinculación.

### T-DATA-02 — Lógica de vinculación y reemplazo ✓ (en shared/)
- `linkGhostProfile(email, uid)` en login/registro para vinculación automática.
- `CompositeProfileRepository` realiza reemplazo de ID fantasma por UID real.

### T-MATH-01 — Motor de cálculo con pesos por ítem ✓ (en shared/)
- `EventExpenseItem` soporta `profileWeights: Map<String, Double>`.
- `CalculatorEngine.buildExpenseDrivenPreview` reparte proporcionalmente con pesos válidos.

### T-UI-01 — Interfaz de reparto personalizado por ítem ✓ (en shared/)
- `ExpenseEditorDialog` con sección expandible de "Reparto personalizado" para categoría `SELECTED`.
- Validación: porcentajes numéricos, no negativos, suma exacta de 100%.

## Definition of Done
- [x] Modelo de perfiles fantasma con isGhost y linkedEmail implementado en shared/
- [x] Lógica de vinculación automática en login/registro implementada en shared/
- [x] Motor de cálculo con pesos por ítem implementado en shared/
- [x] UI de reparto personalizado por ítem implementada en shared/
- [ ] MainActivity wireada para usar versiones de shared/
- [ ] Flujo validado en app real

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-05-01 | A | A.1 | Alta | Creación del sprint 08B2 enfocado en perfiles locales y repartos complejos. |
| 2026-05-02 | A | A.2 | Actualización | Sprint completado en shared/: perfiles fantasma, vinculación, reparto porcentual por ítem. |
| 2026-05-14 | A | A.3 | Corrección | Estado cambiado a Parcial: código en shared/ pero MainActivity no integrada. |
