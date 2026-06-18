# Design: Netear balances por perfil

## Technical Approach

Agrupar por `profileId` en `buildUnifiedBreakdown()`: si un perfil aparece en ambas listas (`owedToYou` y `youOwe`), netear amounts y fusionar eventos (eventos "you owe" con signo negativo). El `direction` de la fila se asigna según signo del neto. Si neto = 0, omitir.

En `EventBreakdownDialog`, renderizar cada evento con color según signo de `event.amount` (> 0 verde, < 0 rojo). Usar `kotlin.math.abs()` para display.

No se modifican los modelos (`UnifiedDebtItem`, `EventDebt`).

## Architecture Decisions

| Decisión | Opción elegida | Alternativa rechazada | Rationale |
|----------|---------------|----------------------|-----------|
| Dirección del neto | `direction` según signo (>0 OWED_TO_YOU, <0 YOU_OWE) | Nueva prop `isNetted` | `UnifiedDebtRow` ya deriva color/subtitle de `direction` |
| Eventos youOwe | Negar amount al fusionar (amount * -1) | `direction` por-evento en modelo | `EventDebt.amount` no tiene restricción de signo |
| Neto cero | Filtrar (no agregar a unified list) | Fila con amount=0 | Ídem a debts paid: sin deuda activa, no se muestra |

## Implementation Plan

### 1. `buildUnifiedBreakdown()` — groupBy profileId + neteo

```kotlin
val owedProfileIds = owedToYou.map { it.profileId }.toSet()
val byProfile = (owedToYou + youOwe)
    .filter { it.amount > 0.0 }
    .groupBy { it.profileId }

return byProfile.map { (profileId, items) ->
    val netAmount = items.sumOf { item ->
        item.amount * if (item.profileId in owedProfileIds) 1.0 else -1.0
    }
    if (netAmount == 0.0) return@map null
    val direction = if (netAmount > 0) DebtDirection.OWED_TO_YOU else DebtDirection.YOU_OWE
    val allEvents = items.flatMap { item ->
        val sign = if (item.profileId in owedProfileIds) 1.0 else -1.0
        item.events.map { it.copy(amount = it.amount * sign) }
    }
    UnifiedDebtItem(profileId, items.first().profileName, abs(netAmount), direction, allEvents)
}.filterNotNull().sortedByDescending { it.amount }
```

Usar `Set<String>` de profileIds en owedToYou para determinar signo — más predecible que comparar referencias.

### 2. `EventBreakdownDialog` — color por signo de evento

Reemplazar un solo `accentColor`/`prefix` por lógica por-evento:

```kotlin
val isPos = event.amount >= 0
val color = if (isPos) OwedToYouAvatarFg else YouOweAvatarFg
val prefix = if (isPos) "+" else "-"
// display: "$prefix${formatEuros(abs(event.amount))}"
```

Total del dialog sin cambios: usa `item.amount` (absoluto) y `item.direction`.

### 3. `UnifiedDebtRow` — sin cambios

## Data Flow

```
debts → computeProfileBreakdown()
  → owedToYou + youOwe (DebtBreakdownItem[])
    → buildUnifiedBreakdown() [NUEVO: groupBy, net, merge events con signo]
      → UnifiedDebtItem[] (1 por perfil, neto 0 filtrado)
        → UnifiedDebtsCard → UnifiedDebtRow (color por direction)
          → EventBreakdownDialog (color por signo de event.amount)
```

## Edge Cases

| Caso | Comportamiento |
|------|---------------|
| Neto cero (50-50) | Perfil no aparece |
| Solo una dirección | Misma fila que antes |
| Eventos cross-dirección | Todos en dialog con signo; suma algebraica da neto |
| Redondeo (10.03 - 5.01) | Usar `roundTo2()` en netAmount |

## Testing Strategy

| Capa | Qué probar | Enfoque |
|------|-----------|---------|
| Unit | buildUnifiedBreakdown: perfil en ambos lados | 1 item, neto correcto, events con signo |
| Unit | Neto cero filtrado | Perfil ausente del resultado |
| Unit | Solo owedToYou / solo youOwe | Mismo comportamiento que antes |
| Unit | Eventos mixtos → signos correctos | Assert event.amount tiene signo esperado |

Tests existentes en `DashboardViewModelTest.kt` no se modifican. `buildUnifiedBreakdown()` se extrae a `internal` para test directo (como `calculateTotalOwedToYou`).

## Migration / Rollout

No requiere migración de datos. Rollback: revertir cambios en `DashboardViewModel.kt` y `DebtAccordionCard.kt`.

## Open Questions

- [ ] Extraer `buildUnifiedBreakdown()` a `internal` o testear indirectamente vía ViewModel. Recomendación: internal.
