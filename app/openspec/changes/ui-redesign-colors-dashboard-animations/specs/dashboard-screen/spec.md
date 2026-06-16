# dashboard-screen (MODIFIED)

## Requirement R1: Remove Redundant Sections

**Description**: Eliminar secciones que no aportan valor al usuario.

**Changes**:
- Remove "Alertas Inteligentes" section (AlertAccordionCard)
- Remove "Todos Mis Eventos" section (lista de eventos)
- Remove related composables and state management

**Scenarios**:
- Given the dashboard, When rendered, Then "Alertas Inteligentes" is not visible
- Given the dashboard, When rendered, Then "Todos Mis Eventos" is not visible
- Given the dashboard, When rendered, Then only financial summary and debt breakdown are shown

**Acceptance Criteria**:
- No visual trace of removed sections
- No unused code or imports
- Clean removal without breaking other functionality

---

## Requirement R2: Financial Summary Layout

**Description**: Agregar una NUEVA sección de resumen financiero ARRIBA de los DebtAccordionCard existentes. NO reemplaza los accordion — los complementa.

**Layout final del Dashboard** (de arriba a abajo):
```
1. Header ("Panel" + botón calendario)
2. Financial Summary Section ← NUEVO
   ├── FinancialSummaryRow (Debes | Te deben)
   └── NetBalanceCard (Balance neto)
3. Debt Breakdown ← EXISTENTE (DebtAccordionCard)
   ├── "TE DEBEN" section
   └── "DEBES" section
```

**FinancialSummaryRow**:
```
┌─────────────────────────────────────────┐
│  DEBES          │  TE DEBEN             │
│  150,00€        │  230,00€              │
│  2 personas     │  3 personas           │
└─────────────────────────────────────────┘
```
- Dos cards en una Row horizontal (Modifier.weight(1f) cada una)
- Cada card muestra: label, AnimatedCounter(monto), conteo de personas
- Colores: "Debes" usa secondary, "Te deben" usa primaryContainer
- FadeInStaggered: indices 0 y 1 (aparecen con 100ms de diferencia)

**NetBalanceCard**:
```
┌─────────────────────────────────────────┐
│         BALANCE NETO                    │
│           +80,00€                       │
│         2 de 3 personas te deben más    │
└─────────────────────────────────────────┘
```
- Card de ancho completo debajo de FinancialSummaryRow
- AnimatedCounter con color: verde si positivo, red si negativo
- FadeInStaggered: índice 2 (aparece 200ms después del primer card)

**Scenarios**:
- Given the dashboard, When rendered, Then financial summary appears at the top
- Given the dashboard, When data loads, Then numbers animate from 0 to actual value
- Given the dashboard, When values change, Then numbers animate smoothly

**Acceptance Criteria**:
- Layout responsive (adapts to screen width)
- Numbers use AnimatedCounter (800ms duration)
- Cards fade in with staggered delay (100ms between cards)
- Total animation time <1s

---

## Requirement R3: Debt Breakdown Section

**Description**: Mantener DebtAccordionCard para desglose detallado de deudas.

**Structure**:
- "TE DEBEN" section con lista de personas que te deben
- "DEBES" section con lista de personas a las que debes
- Cada persona expandible para ver desglose por evento

**Scenarios**:
- Given the dashboard, When rendered, Then debt breakdown appears below financial summary
- Given a person row, When tapped, Then expands to show per-event breakdown
- Given multiple people, When rendered, Then each has independent expand/collapse state

**Acceptance Criteria**:
- DebtAccordionCard functionality preserved
- Smooth expand/collapse animations
- Clear visual hierarchy (person > event)

---

## Requirement R4: Animation Integration

**Description**: Aplicar animaciones del sistema animation-system al dashboard.

**Animations**:
- Financial summary cards: FadeInStaggered (100ms delay between cards)
- Numbers: AnimatedCounter (800ms duration)
- Debt sections: SlideUp (400ms duration, 24dp distance)
- Expand/collapse: smooth height animation

**Performance**:
- Dashboard loads in <500ms
- 60fps during animations
- Max 4 simultaneous animations
- Graceful degradation on slow devices

**Scenarios**:
- Given the dashboard, When first rendered, Then cards fade in with stagger
- Given the dashboard, When numbers appear, Then they animate from 0
- Given the dashboard, When scrolling, Then animations don't cause jank
- Given a slow device, When animations are disabled, Then content appears instantly

**Acceptance Criteria**:
- All animations complete in <1s total
- No frame drops (60fps maintained)
- Animations respect system accessibility settings
- Fallback for low-end devices

---

## Requirement R5: Color Palette Application

**Description**: Aplicar nueva paleta de colores neo-fintech-tokens al dashboard.

**Changes**:
- Use new primaryContainer (#00A651 light / #00C853 dark)
- Use new secondary (#00897B light / #26A69A dark)
- Update all color references to use new tokens

**Scenarios**:
- Given light mode, When dashboard renders, Then uses #00A651 for primary elements
- Given dark mode, When dashboard renders, Then uses #00C853 for primary elements
- Given any mode, When dashboard renders, Then all text meets WCAG AA contrast

**Acceptance Criteria**:
- New colors applied consistently
- No hardcoded color values
- All contrast ratios ≥4.5:1

---

## Requirement R6: Performance Optimization

**Description**: Garantizar performance óptima del dashboard.

**Targets**:
- Initial render: <500ms
- Scroll performance: 60fps
- Animation performance: 60fps
- Memory usage: no leaks

**Optimizations**:
- Lazy load debt breakdown (only render visible items)
- Memoize expensive calculations
- Use remember for animated values
- Avoid unnecessary recompositions

**Scenarios**:
- Given the dashboard, When first opened, Then renders in <500ms
- Given the dashboard, When scrolling, Then maintains 60fps
- Given the dashboard, When animations run, Then no frame drops
- Given the dashboard, When left open for 10 minutes, Then no memory leaks

**Acceptance Criteria**:
- Initial render <500ms (measured on mid-range device)
- 60fps during scroll and animations
- No memory leaks (verified with profiler)
- Graceful degradation on low-end devices

---

## Requirement R7: Dead Code Cleanup

**Description**: Eliminar todo el código huérfano al quitar las secciones "Alertas Inteligentes" y "Todos Mis Eventos".

### DashboardScreen.kt — código a eliminar
| Componente | Acción |
|---|---|
| `AlertCard` composable privado | ELIMINAR (ya era dead code) |
| `DashboardEventRow` composable privado | ELIMINAR |
| `LoadingSkeleton` composable privado | MANTENER (reutilizable) |
| Sección "Alertas Inteligentes" (AlertAccordionCard) | ELIMINAR |
| Sección "Todos Mis Eventos" (event list) | ELIMINAR |
| Parámetro `onAlertTap` en firma | ELIMINAR |
| Parámetro `onEventTap` en firma | ELIMINAR |
| Parámetro `smartAlerts` en firma | ELIMINAR |
| Parámetro `allEvents` en firma | ELIMINAR |

### Archivos huérfanos
| Archivo | Acción |
|---|---|
| `AlertAccordionCard.kt` | ELIMINAR (solo usado en Dashboard) |
| `SmartAlert` data class | ELIMINAR del modelo |
| `AlertType` enum/sealed | ELIMINAR del modelo |

### CuentaMorososApp.kt — cleanup
| Sección | Acción |
|---|---|
| Construcción de `smartAlerts` | ELIMINAR |
| Construcción de `allEvents` para dashboard | ELIMINAR |
| Callbacks `onAlertTap` y `onEventTap` | ELIMINAR |

### Validación post-limpieza
- `git grep AlertCard` → 0 resultados
- `git grep DashboardEventRow` → 0 resultados
- `git grep SmartAlert` → 0 resultados
- Build exitoso

---

## Migration Notes

**Files to Modify**:
- `shared/src/commonMain/kotlin/com/cuentamorosos/ui/DashboardScreen.kt`
  - Remove AlertasInteligentes section
  - Remove TodosMisEventos section
  - Add FinancialSummaryRow composable
  - Add NetBalanceCard composable
  - Integrate AnimatedCounter
  - Apply FadeInStaggered and SlideUp modifiers
  - Update color references

**Dependencies**:
- animation-system (R001-R005)
- neo-fintech-tokens (R1-R5)

**Validation Steps**:
1. Remove redundant sections
2. Add financial summary layout
3. Integrate animations
4. Apply new colors
5. Test performance (render time, FPS, memory)
6. Verify on multiple devices
