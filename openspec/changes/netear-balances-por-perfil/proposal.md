# Proposal: Netear balances por perfil

## Intent

Netear las deudas por perfil en el dashboard: cuando un mismo perfil aparece como deudor y acreedor, mostrar una sola fila con el saldo neto en lugar de dos filas separadas.

## Scope

### In Scope
- Netear perfiles duplicados en `buildUnifiedBreakdown()` (DashboardViewModel)
- Soportar eventos con montos negativos en `EventDebt` para indicar dirección "debes"
- Renderizar colores por signo en `EventBreakdownDialog`
- Tests actualizados para el nuevo comportamiento de neteo

### Out of Scope
- Cambios en la UI del dashboard más allá del breakdown dialog
- Neteo entre múltiples perfiles (solo pairwise, perfil a perfil)
- Cambios en `DashboardState.kt` (los modelos existentes son flexibles)

## Capabilities

### Modified Capabilities
- `dashboard-debt-calculations`: la función `computeProfileBreakdown` o su equivalente `buildUnifiedBreakdown` cambia para netear perfiles que aparecen en ambos lados (owedToYou y youOwe). Se agrega soporte para montos negativos en eventos del breakdown.

## Approach

1. En `DashboardViewModel.buildUnifiedBreakdown()`, agrupar los `UnifiedDebtItem` por `profileId`. Si un perfil aparece en ambos owedToYou y youOwe, computar neto = owedAmount - youOweAmount. Dirección según signo. Fusionar eventos de ambos lados, marcando los "youOwe" con monto negativo.
2. En `EventBreakdownDialog`, inspeccionar `event.amount`: si es negativo → rojo "Debes X€", si es positivo → verde "Te debe X€". Usar `kotlin.math.abs()` para display.
3. La fila total del dialog suma algebraicamente los montos (neto = suma con signo).

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `DashboardViewModel.kt` | Modified | `buildUnifiedBreakdown()`: netear perfiles duplicados |
| `DebtAccordionCard.kt` | Modified | `EventBreakdownDialog`: colorear por signo de `amount` |
| `DashboardViewModelTest.kt` | Modified | Tests para neteo, signos, y casos borde |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Perfiles con neto cero muestran fila vacía | Low | Si neto = 0, no renderizar la fila (colapsar ambos lados) |
| Mismo evento aporta a ambos lados (imposible por modelo) | None | Cada evento tiene un solo deudor; no puede aparecer en ambos lados |
| Redondeo por montos impares (ej: 10,03 - 5,01 = 5,019999) | Low | Usar `roundTo2()` existente en el modelo de display |

## Rollback Plan

Revertir cambios en `DashboardViewModel.kt` y `DebtAccordionCard.kt`. Los modelos de estado (`UnifiedDebtItem`, `EventDebt`) no cambian, no hay migración. Los tests existentes recuperan su comportamiento anterior.

## Dependencies

- Ninguna. El cambio es puramente lógico dentro del módulo shared.

## Success Criteria

- [ ] Perfil con deudas en ambos sentidos muestra UNA fila con neto y dirección correcta
- [ ] El breakdown dialog muestra eventos de ambos lados con colores por signo
- [ ] Perfiles con neto = 0 no aparecen en la lista unificada
- [ ] Perfiles con deuda solo en una dirección se muestran igual que antes
- [ ] Tests existentes pasan sin modificaciones
