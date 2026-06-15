# Proposal: UI Redesign — Colors, Dashboard & Animations

## Intent

El verde neón actual (#39FF14) tiene contraste 1.4:1 — falla WCAG AA. El dashboard tiene secciones redundantes (Alertas Inteligentes, Todos Mis Eventos) que diluyen el resumen financiero. Falta un sistema de animaciones reutilizable para dar sensación moderna. Este cambio resuelve los tres problemas en una sola pasada.

## Scope

### In Scope
- Reemplazar valores de color en `NeoFintechColors.kt`: verde #00A651 (light) / #00C853 (dark), teal #00897B (light) / #26A69A (dark)
- Crear sistema de animaciones reutilizable: `AnimatedCounter`, `fadeInStaggered` modifier, `slideUp` modifier
- Rediseñar Dashboard: eliminar Alertas Inteligentes + Todos Mis Eventos, agregar resumen financiero (fila Debes|Te deben + Balance neto)
- Aplicar animaciones en Dashboard, Events, Profiles, Settings, Account

### Out of Scope
- Lógica de negocio o estructura de datos
- Nuevas features
- Cambios en `DashboardViewModel` / `DashboardState` (solo UI)

## Capabilities

### New Capabilities
- `animation-system`: Modificadores y composables de animación reutilizables (fade-in escalonado, slide-up, contador animado)

### Modified Capabilities
- `neo-fintech-tokens`: Valores de color primario y secundario actualizados para cumplir WCAG AA
- `dashboard-screen`: Layout reestructurado — secciones eliminadas, resumen financiero agregado

## Approach

1. **Colores** — Solo cambiar valores en `NeoFintechColors.light()` y `.dark()`. Agregar token `teal` al `NeoFintechColorSet`. Estructura intacta.
2. **Animaciones** — Extender `NeoFintechAnimations.kt` con: `AnimatedCounter` composable (wrapper sobre `rememberAnimatedDouble` + format), `Modifier.fadeInStaggered(index, total)`, `Modifier.slideUp()`. Todos usan tokens existentes (`DURATION_MS`, `STAGGER_DELAY_MS`).
3. **Dashboard** — Eliminar bloques de Alertas Inteligentes y Todos Mis Eventos en `DashboardScreen.kt`. Insertar `FinancialSummaryCard` arriba (Row con Debes/Te deben + Balance neto). Aplicar `fadeInStaggered` a DebtAccordionCards.
4. **Pantallas restantes** — Aplicar `slideUp` a secciones y `fadeInStaggered` a listas en Events, Profiles, Settings, Account.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `shared/.../ui/NeoFintechColors.kt` | Modified | Nuevos valores primary/teal, agregar token teal al ColorSet |
| `shared/.../ui/NeoFintechAnimations.kt` | Modified | AnimatedCounter, fadeInStaggered, slideUp |
| `shared/.../ui/DashboardScreen.kt` | Modified | Eliminar 2 secciones, agregar FinancialSummaryCard, aplicar animaciones |
| `shared/.../ui/EventsScreen.kt` | Modified | Aplicar animaciones fade-in/slide-up |
| `shared/.../ui/ProfilesScreen.kt` | Modified | Aplicar animaciones fade-in/slide-up |
| `shared/.../ui/SettingsScreen.kt` | Modified | Aplicar animaciones fade-in/slide-up |
| `shared/.../ui/AccountScreen.kt` | Modified | Aplicar animaciones fade-in/slide-up |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Performance en dispositivos antiguos con múltiples animaciones | Medium | Usar `animateFloatAsState` (GPU-accelerated), evitar animaciones en LazyColumn items visibles |
| Color teal no usado en todos los contextos necesarios | Low | Revisar todos los call sites de `primaryContainer` post-cambio |
| Layout del resumen financiero requiere iteraciones | Low | Prototipar con Spacer/Box primero, ajustar padding |

## Rollback Plan

1. `git revert` del cambio completo — todos los archivos son modifications, no hay migraciones de datos
2. Los colores viejos se recuperan del git history de `NeoFintechColors.kt`
3. Las animaciones son aditivas — eliminar las funciones nuevas no rompe nada
4. El dashboard puede revertirse independientemente restaurando los bloques eliminados

## Dependencies

- Design system Neo-Fintech ya implementado (tokens, typography, spacing, shapes)
- Dashboard ya implementado (DashboardScreen, DashboardViewModel, DashboardState, DebtAccordionCard)
- Ninguna dependencia externa nueva

## Success Criteria

- [ ] Contraste WCAG AA (≥4.5:1) verificado para texto sobre fondos claro/oscuro
- [ ] Dashboard muestra FinancialSummaryCard + DebtAccordionCards únicamente
- [ ] `AnimatedCounter`, `fadeInStaggered`, `slideUp` existen y son reutilizables
- [ ] Animaciones fluidas (sin jank) en Dashboard, Events, Profiles, Settings, Account
- [ ] No regresión en funcionalidad existente (deudas, eventos, perfiles, settings)
- [ ] `./gradlew compileDebugKotlin` pasa sin errores
