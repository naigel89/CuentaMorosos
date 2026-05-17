# UI0004B1: Ajustes y apariencia — Rediseño Neo-Fintech

> **Código:** UI0004B1
> **Versión:** B
> **Revisión:** 1
> **Fecha:** 2026-05-14

## Resumen
Rediseño de la pantalla de ajustes con layout de dos columnas (desktop), secciones de Apariencia, Notificaciones y Sesión/Seguridad con toggles y controles segmentados.

## Historia de usuario relacionada
Como usuario, quiero personalizar el tema, color de acento y preferencias de notificación desde una interfaz clara y organizada.

## Objetivo de la pantalla
Centralizar todas las preferencias del usuario en secciones bien diferenciadas con controles visuales intuitivos y vista previa inmediata.

## Componentes visibles
| Componente | Tipo | Descripción | Obligatorio |
|---|---|---|---|
| Sidebar de navegación | nav (desktop) | Preferences, Security, Account con iconos | Sí (desktop) |
| Sección Appearance | sección | Theme, Accent Color, List Density | Sí |
| Theme Toggle | segmented control | Light / Dark con iconos de sol/luna | Sí |
| Accent Color picker | swatches | 3+ colores circulares seleccionables | Sí |
| List Density | dropdown/select | Compact, Standard, Comfortable | Sí |
| Sección Notifications | sección | Push, Email Summaries, Promotional | Sí |
| Toggle switches | interruptores | On/Off con color neón cuando activo | Sí |
| Sección Session & Security | sección | Active Sessions + Sign Out Everywhere | Sí |
| Botón View Devices | acción | Lista dispositivos activos | Sí |
| Botón Sign Out Everywhere | acción destructiva | Cierra todas las sesiones | Sí |
| Bottom Navigation | barra | Events, Profiles, Settings (activo) | Sí (móvil) |

## Estados de la interfaz
- Preferencias por defecto: tema light, color neón, densidad standard
- Toggle activo: fondo primary-container (#39FF14) con thumb blanco
- Toggle inactivo: fondo surface-variant
- Color de acento seleccionado: ring-2 con ring-offset
- Hover en opciones de sidebar: fondo surface-container-low
- Sección activa en sidebar: fondo surface-container-high con texto primario

## Reglas de interacción
- Cambio de tema: reflejo inmediato con AnimatedContent crossfade
- Selección de color: aplica inmediatamente a elementos destacados
- Toggles: persisten automáticamente al cambiar
- "View Devices": abre lista de dispositivos con opción de revocar individualmente
- "Sign Out Everywhere": requiere confirmación antes de ejecutar
- Sidebar desktop: al hacer clic en una categoría, scroll suave a la sección

## Navegación
- Origen: bottom nav (móvil) o sidebar (desktop)
- Destino: vuelve a la pantalla anterior manteniendo preferencias
- Desktop: navegación interna por secciones con sidebar

## Consideraciones UX/UI
- **Layout desktop**: grid de 12 columnas, sidebar ocupa 3, contenido 9
- **Layout móvil**: columna única con secciones apiladas
- **Segmented control**: el tema se elige con un control de 2 botones dentro de un contenedor redondeado, no con toggle simple
- **Swatches de color**: círculos de 32px con hover scale-110, el seleccionado tiene ring
- **Toggles**: estilo Material 3 con thumb que se desliza, fondo neón cuando activo
- **Animaciones**:
  - Cambio de tema: crossfade con AnimatedContent
  - Toggle: slide del thumb con transition-all
  - Hover en sidebar items: background transition duration-200
  - Botón "Sign Out Everywhere": hover con cambio de color a rojo más intenso
- **Secciones**: cada sección tiene un título en label-md uppercase con tracking-wider y color primary

## Referencias de diseño
- Modo claro: `app/documentation/Concepto Diseño Pantallas/stitch_splitflow_expenses/ajustes_neon_edit_1/`
- Modo oscuro: `app/documentation/Concepto Diseño Pantallas/stitch_splitflow_expenses/ajustes_neon_edit_2/`
- Guía de estilo light: `app/documentation/Concepto Diseño Pantallas/stitch_splitflow_expenses/neo_fintech_precision_1/DESIGN.md`
- Guía de estilo dark: `app/documentation/Concepto Diseño Pantallas/stitch_splitflow_expenses/neo_fintech_precision_2/DESIGN.md`
- Sistema visual completo: `documentation/nfr/NFR0001B1-experiencia-visual-y-personalizacion.md`

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-05-14 | B | 1 | Alta | Rediseño con layout dos columnas, segmented control para tema, swatches de color, toggles neón, sección de seguridad. Basado en concepto Neo-Fintech Precision. |
