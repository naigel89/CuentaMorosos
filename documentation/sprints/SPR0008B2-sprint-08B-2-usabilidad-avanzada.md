# SPR0008B2: Sprint 08B.2 - Usabilidad Avanzada y Flexibilidad

> **Código:** SPR0008B2
> **Versión:** A
> **Revisión:** A.2
> **Fecha:** 2026-05-02

## Objetivo del sprint
Reducir la fricción de adopción mediante perfiles locales (fantasmas) y permitir repartos complejos basados en pesos individuales por ítem.

## Estado
Hecho

## Requisitos e historias incluidas
| ID | Tipo | Nombre | Prioridad | Estado | Dependencias |
|---|---|---|---|---|---|
| FR0006A1 | FR | Perfiles fantasma y vinculación manual | Alta | Hecho | SPR0008A1 |
| FR0008A1 | FR | Divisiones porcentuales por ítem individual | Alta | Hecho | FR0005A1 |

## Tareas técnicas

### T-DATA-01 — Modelo de Perfiles Fantasma ✓
- `ProfileItem` ampliado con `isGhost: Boolean` y `linkedEmail: String?`.
- `CuentaMorososLocalStore` actualizado para persistir `isGhost` y `linkedEmail` en local.
- `ProfileEditorDialog` permite crear/editar perfiles locales (fantasma) con email de vinculación opcional.

### T-DATA-02 — Lógica de vinculación y reemplazo ✓
- En login y registro se invoca `linkGhostProfile(email, uid)` para vincular automáticamente perfiles fantasma por email.
- `CompositeProfileRepository` realiza el reemplazo de ID fantasma por UID real en eventos, deudas y gastos.
- Los perfiles fantasma se mantienen en local; los perfiles reales se sincronizan en Firestore.

### T-MATH-01 — Motor de cálculo con pesos por ítem ✓
- `EventExpenseItem` soporta `profileWeights: Map<String, Double>`.
- `CalculatorEngine.buildExpenseDrivenPreview` reparte proporcionalmente cuando hay pesos válidos; si no, aplica reparto equitativo.
- Hardening del cálculo: los pesos solo afectan a receptores válidos del ítem para evitar asignaciones fuera del conjunto seleccionado.

### T-UI-01 — Interfaz de reparto personalizado por ítem ✓
- `ExpenseEditorDialog` añade una sección opcional/expandible de "Reparto personalizado" para categoría `SELECTED`.
- Inputs numéricos por perfil seleccionado para definir % por gasto.
- Validación antes de guardar: porcentajes numéricos, no negativos y suma exacta de 100% cuando el reparto personalizado está activo.
- Si la sección está desactivada, se guarda reparto estándar (sin pesos).

## Definition of Done
- [x] Se pueden crear perfiles sin cuenta de Firebase y añadirlos a eventos.
- [x] Al registrarse, un usuario puede "heredar" los datos de un perfil fantasma creado previamente con su email.
- [x] Un ítem puede repartirse en proporciones distintas (ej. 60% y 40%) entre sus participantes.
- [x] La interfaz de creación de gastos permite definir estos porcentajes de forma intuitiva.

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-05-01 | A | A.1 | Alta | Creación del sprint 08B2 enfocado en perfiles locales y repartos complejos. |
| 2026-05-02 | A | A.2 | Actualización | Sprint completado: creación/edición de perfiles fantasma en UI, vinculación automática en registro/login, reparto porcentual por ítem con validaciones UI y hardening del cálculo en CalculatorEngine. |
