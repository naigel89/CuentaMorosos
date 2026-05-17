# SPR0011B1: Sprint 11 — Rediseño UI Neo-Fintech Precision

> **Código:** SPR0011B1
> **Versión:** B
> **Revisión:** 1
> **Fecha:** 2026-05-14

## Objetivo del sprint
Implementar el rediseño visual completo de la aplicación bajo el concepto "Neo-Fintech Precision": nuevo sistema de diseño con paleta dual claro/oscuro, tipografía Geist + JetBrains Mono, nueva pantalla principal de Panel de Control y animaciones de micro-interacción en toda la app.

## Estado
Pendiente

## Requisitos e historias incluidas
| ID | Tipo | Nombre | Prioridad | Estado | Dependencias |
|---|---|---|---|---|---|
| NFR0001B1 | NFR | Sistema de diseño Neo-Fintech Precision | Alta | Pendiente | NFR0001A1 |
| UI0007B1 | UI | Panel de Control (Dashboard) — nueva pantalla principal | Alta | Pendiente | NFR0001B1 |
| UI0001B1 | UI | Pantalla de eventos — rediseño Bento Grid | Alta | Pendiente | NFR0001B1, UI0007B1 |
| UI0002B1 | UI | Detalle de evento y pagos — rediseño dos columnas | Alta | Pendiente | NFR0001B1, UI0001B1 |
| UI0003B1 | UI | Pantalla de perfiles — rediseño con dashboard balance | Alta | Pendiente | NFR0001B1 |
| UI0004B1 | UI | Ajustes y apariencia — rediseño con toggles neón | Media | Pendiente | NFR0001B1 |
| UI0005B1 | UI | Calculadora automática — simplificación Consumo Real | Media | Pendiente | NFR0001B1, UI0002B1 |
| UI0006A1 | UI | Simplificación de interfaz de modos de cálculo | Media | Hecho | UI0005A1 |

## Tareas técnicas

### T11-01 — Configuración del sistema de diseño (NFR0001B1)
- Definir `ColorScheme` completo para light y dark en Compose Material 3
- Tokens de color: background, surface, primary-container (#39FF14), error, outline-variant, etc.
- Integrar fuentes Geist (headlines/body) y JetBrains Mono (datos/labels) como recursos
- Crear `Theme.kt` con tipografía Material 3 personalizada (Display, Headline, Body, Label)
- Crear `Spacing.kt` con escala 4px base (xs, sm, md, lg, xl)
- Crear `Shapes.kt` con border radius del sistema (4px, 8px, 12px, pill)

### T11-02 — Panel de Control / Dashboard (UI0007B1) — NUEVA pantalla principal
- Crear `DashboardScreen` como nueva pantalla de entrada de la app
- Implementar Top App Bar con avatar, toggle de tema y notificaciones
- Implementar Main Indicators: cards "Total Owed to You" y "Total You Owe"
- Implementar Smart Alerts: 3 tipos de alertas proactivas con iconos y navegación
- Implementar Recent Activity: feed con iconos contextuales, montos y badges de estado
- Animación count-up en montos grandes al entrar en pantalla
- Animación staggered en Smart Alerts (50ms delay entre items)
- Bottom navigation: Events, Profiles, Settings

### T11-03 — Pantalla de Eventos (UI0001B1)
- Refactorizar `EventsScreen` con layout Bento Grid
- Implementar Balance Summary: tarjeta total + quick stats (eventos activos, total gastado)
- Implementar tarjetas de evento con: icono, badge estado, título, fechas, gasto total, tu parte, avatares, saldo
- Implementar tarjeta "Crear Nuevo Evento" con borde discontinuo como empty state
- FAB móvil en bottom-right para crear evento rápido
- Animación hover con elevación de sombra en cards
- Animación staggered en entrada de tarjetas

### T11-04 — Detalle de Evento (UI0002B1)
- Refactorizar `EventDetailScreen` con layout dos columnas (desktop) / apilado (móvil)
- Header con "Back to Events", título del evento, fechas y Total Event Cost
- Lista de gastos con iconos contextuales por categoría, badges de tipo de reparto
- Panel lateral de Settlement con "Calculate Totals" y lista de participantes con checkboxes
- Color coding en deudas: verde neón (pequeñas), rojo (grandes), gris (saldadas)
- Animación en checkbox de participante (ripple + color transition)

### T11-05 — Pantalla de Perfiles (UI0003B1)
- Refactorizar `ProfilesScreen` con dashboard Bento Grid de balance
- Implementar tarjetas "Total Owed to You" y "Total You Owe"
- Implementar lista de perfiles con avatares (foto o iniciales), nombre, eventos compartidos, monto, badge
- Badges de estado: "Owes you" (verde), "You owe" (rojo), "Settled up" (gris)
- Perfiles saldados con opacidad 75%
- Animación count-up en montos del dashboard

### T11-06 — Ajustes y Apariencia (UI0004B1)
- Refactorizar `SettingsScreen` con layout dos columnas (desktop)
- Implementar segmented control para tema Light/Dark
- Implementar Accent Color picker con swatches circulares
- Implementar List Density selector
- Implementar sección de Notificaciones con toggles Material 3
- Implementar sección Session & Security con "View Devices" y "Sign Out Everywhere"
- Crossfade animado al cambiar de tema

### T11-07 — Calculadora simplificada (UI0005B1)
- Refactorizar `CalculatorScreen` con "Consumo Real" como flujo principal
- Ocultar modos secundarios tras dropdown/expansión
- Implementar vista previa en tiempo real con actualización reactiva
- Implementar bloque de liquidación con propuesta de transferencias mínimas
- Validación inline con mensajes de error, no modales

### T11-08 — Sistema de animaciones
- Implementar SharedAxis para transiciones entre pantallas del bottom nav
- Implementar animateContentSize para expansión de cards y secciones
- Implementar animateItemPlacement para reordenamiento de listas
- Implementar efecto neon glow en botones primarios (modo oscuro) con drawWithContent + Blur
- Implementar botón press con scale 0.98
- Respetar `isAccessibilityAnimationsEnabled` para reduced-motion

## Referencias de diseño
- Concepto completo: `app/documentation/Concepto Diseño Pantallas/stitch_splitflow_expenses/`
- Guía de estilo light: `app/documentation/Concepto Diseño Pantallas/stitch_splitflow_expenses/neo_fintech_precision_1/DESIGN.md`
- Guía de estilo dark: `app/documentation/Concepto Diseño Pantallas/stitch_splitflow_expenses/neo_fintech_precision_2/DESIGN.md`
- Documentos UI versión B: `documentation/ui/UI0001B1` a `UI0007B1`
- Sistema visual: `documentation/nfr/NFR0001B1-experiencia-visual-y-personalizacion.md`

## Riesgos o bloqueos
- El efecto de glow neón puede impactar rendimiento en dispositivos de gama baja; requiere fallback sin blur
- La migración de tipografía (TT Hoves Pro → Geist) puede requerir ajustes de métricas en pantallas existentes
- El count-up animation debe coordinarse con el ciclo de vida de Compose para evitar recomposiciones innecesarias
- La nueva pantalla Dashboard cambia el entry point de la app; puede afectar tests de navegación existentes

## Definition of Done
- [ ] Sistema de diseño (colores, tipografía, spacing, shapes) configurado y aplicado al Theme
- [ ] Panel de Control implementado como pantalla principal con indicadores, alertas y actividad
- [ ] Eventos, Detalle, Perfiles, Ajustes y Calculadora rediseñados según specs versión B
- [ ] Modo claro y modo oscuro funcionales con toggle y crossfade
- [ ] Animaciones implementadas: SharedAxis, count-up, staggered, neon glow, press scale
- [ ] Accesibilidad: reduced-motion respetado, contraste WCAG AA verificado
- [ ] Sin regresiones en funcionalidad existente (eventos, perfiles, cálculos, persistencia)
- [ ] Documentos de diseño referenciados correctamente en cada pantalla

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-05-14 | B | 1 | Alta | Creación del sprint 11: rediseño UI completo Neo-Fintech Precision con nueva pantalla Dashboard, sistema de diseño y animaciones. |
