# UI0007B1: Panel de Control (Dashboard) — Nueva pantalla principal

> **Código:** UI0007B1
> **Versión:** B
> **Revisión:** 1
> **Fecha:** 2026-05-14

## Resumen
Nueva pantalla principal de la aplicación que reemplaza al listado de eventos como punto de entrada. Ofrece una vista de alto nivel con indicadores financieros, alertas inteligentes y actividad reciente.

## Historia de usuario relacionada
Como usuario, quiero al abrir la app ver inmediatamente cuánto me deben, cuánto debo yo, qué acciones requieren mi atención y qué ha pasado recientemente en mis eventos.

## Objetivo de la pantalla
Ser el centro de comando de la aplicación: un dashboard que resume el estado financiero del usuario, destaca acciones pendientes y permite navegación rápida a cualquier sección.

## Componentes visibles
| Componente | Tipo | Descripción | Obligatorio |
|---|---|---|---|
| Top App Bar | header | Avatar del usuario, nombre de la app, toggle de tema, notificaciones | Sí |
| Main Indicators | grid 2 columnas | "Total Owed to You" + "Total You Owe" | Sí |
| Card Te deben | card | Monto grande, icono trending_up, borde superior verde, badge de tendencia (+4.2%) | Sí |
| Card Debes | card | Monto grande, icono trending_down, borde superior rojo, botón "Settle Balances" | Sí |
| Smart Alerts | sección | Lista de alertas proactivas con iconos y acciones | Sí |
| Alerta sin participantes | item | Icono group_off en rojo, "3 events without participants" | Sí |
| Alerta sin items | item | Icono receipt_long en gris, "1 event without items" | Sí |
| Alerta cálculos pendientes | item | Icono calculate en tertiary, "Calculations pending" | Sí |
| Recent Activity | sección | Feed de actividad reciente con "View All" | Sí |
| Activity item | fila | Icono contextual, nombre del evento, timestamp, monto, badge de estado | Sí |
| Bottom Navigation | barra | Events, Profiles, Settings | Sí (móvil) |

## Estados de la interfaz
- Dashboard completo: todos los indicadores con datos, alertas y actividad
- Sin alertas: sección "Smart Alerts" muestra mensaje "All clear" con icono check
- Sin actividad: sección "Recent Activity" muestra placeholder "No recent activity"
- Hover en alertas: borde cambia a verde neón con transition-colors
- Actividad resuelta: items al 75% de opacidad
- Toggle de tema: cambia entre light/dark con crossfade

## Reglas de interacción
- Pulsar una alerta: navega directamente al evento o acción requerida
- Pulsar un item de actividad: abre el detalle del evento (`UI0002B1`)
- "View All": lleva al listado completo de actividad
- "Settle Balances": abre flujo de liquidación de deudas
- Toggle de tema: cambia inmediatamente con AnimatedContent crossfade
- Notificaciones: abre panel de notificaciones
- Avatar: abre perfil del usuario o settings

## Navegación
- Origen: pantalla inicial de la app (reemplaza a `UI0001A1` como entry point)
- Destinos: `UI0001B1` (eventos), `UI0002B1` (detalle), `UI0003B1` (perfiles), `UI0004B1` (settings)
- Bottom nav en móvil: Events, Profiles, Settings (el dashboard es la pantalla por defecto al abrir Events)

## Consideraciones UX/UI
- **Pantalla principal**: esta es la primera pantalla que ve el usuario al abrir la app
- **Indicadores grandes**: los montos usan display-lg (48px desktop, 32px mobile) en Geist 700 con letter-spacing -0.04em
- **Bordes superiores**: las cards de indicadores tienen un borde superior de 4px en el color semántico (verde/rojo)
- **Smart Alerts**: cada alerta es tappable, con icono circular de fondo tintado, texto principal en negrita y subtítulo descriptivo
- **Actividad reciente**: separada por reglas horizontales de 4% opacidad, iconos en círculos con fondo surface-container
- **Badges de estado**: "Active" (verde), "Settling" (rojo), "Closed" (gris) en pills con texto uppercase
- **Animaciones**:
  - Entrada del dashboard: fade-in general, luego count-up en los montos grandes (duration-800)
  - Smart Alerts: slide-in desde arriba con stagger de 50ms entre cada una
  - Iconos de alerta: pulse sutil (scale 1.05 → 1.0) cuando hay acción requerida
  - Actividad reciente: staggered list animation con 50ms de delay entre items
  - Hover en alertas: border-color transition a primary-container (neon green)
  - Toggle de tema: crossfade con AnimatedContent entre light y dark
- **Colores**: verde neón (#39FF14) para montos positivos y acciones, rojo error para deudas, tertiary para información neutral

## Referencias de diseño
- Modo claro: `app/documentation/Concepto Diseño Pantallas/stitch_splitflow_expenses/panel_de_control_1/`
- Modo oscuro: `app/documentation/Concepto Diseño Pantallas/stitch_splitflow_expenses/panel_de_control_2/`
- Guía de estilo light: `app/documentation/Concepto Diseño Pantallas/stitch_splitflow_expenses/neo_fintech_precision_1/DESIGN.md`
- Guía de estilo dark: `app/documentation/Concepto Diseño Pantallas/stitch_splitflow_expenses/neo_fintech_precision_2/DESIGN.md`
- Sistema visual completo: `documentation/nfr/NFR0001B1-experiencia-visual-y-personalizacion.md`

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-05-14 | B | 1 | Alta | Nueva pantalla principal (Dashboard/Panel de Control) con indicadores financieros, Smart Alerts y actividad reciente. Basada en concepto Neo-Fintech Precision. |
