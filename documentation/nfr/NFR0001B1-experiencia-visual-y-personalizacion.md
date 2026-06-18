# NFR0001B1: Experiencia visual y personalización — Sistema Neo-Fintech Precision

> **Código:** NFR0001B1
> **Versión:** B
> **Revisión:** 1
> **Fecha:** 2026-05-14

## Objetivo
Definir el sistema de diseño completo para la versión B de CuentaMorosos, incluyendo paleta de colores dual (claro/oscuro), tipografía, espaciado, elevación, formas, componentes y guías de animación bajo el concepto "Neo-Fintech Precision".

## Categoría
Usabilidad / diseño visual / personalización / sistema de diseño.

## Métrica o criterio medible

### Paleta de colores
La app debe ofrecer dos temas completos con los siguientes tokens:

| Token | Light Mode | Dark Mode | Uso |
|-------|-----------|-----------|-----|
| Background | #F8F9FA | #131313 | Fondo principal |
| Surface | #FFFFFF | #201F1F | Cards y contenedores |
| Surface Container Low | #F3F4F5 | #1C1B1B | Superficies elevadas claras |
| Surface Container | #EDEEEF | #201F1F | Superficies intermedias |
| Surface Container High | #E7E8E9 | #2A2A2A | Superficies elevadas oscuras |
| Surface Container Lowest | #FFFFFF | #0E0E0E | Superficie más elevada |
| On Surface | #191C1D | #E5E2E1 | Texto principal |
| On Surface Variant | #3C4B35 | #BACC B0 | Texto secundario |
| Primary Container | #00A651 | #00C853 | Verde WCAG AA (acento principal) |
| Primary Fixed Dim | #008F47 | #00B84A | Verde alternativo WCAG AA |
| Error | #BA1A1A | #FFB4AB | Deudas y alertas |
| Error Container | #FFDAD6 | #93000A | Fondo de errores |
| Outline Variant | #BACC B0 | #3C4B35 | Bordes y separadores |
| Tertiary Container | #D6DDED | #DFDCE1 | Información neutral |
| Secondary | #5E5E5E | #C8C5CB | Texto e iconos secundarios |

**Nota de cumplimiento WCAG AA**: El verde neón original del diseño conceptual (#39FF14) fue ajustado a #00A651 (claro) / #00C853 (oscuro) para garantizar contraste WCAG AA mínimo en todos los pares texto/fondo. Los valores originales (#39FF14) provenían de un diseño aspiracional; los valores actuales son los definidos en `NeoFintechColors.kt`. El comportamiento visual se mantiene: en modo claro se usa como acento sutil (bordes, badges), en modo oscuro como fondo sólido de botones primarios.

### Tipografía
- **Fuente principal**: Geist (reemplaza a TT Hoves Pro y Hanken Grotesk/Inter de la versión A)
- **Fuente de datos**: JetBrains Mono (para montos, etiquetas, IDs de transacción)

| Estilo | Familia | Tamaño | Peso | Letter Spacing | Uso |
|--------|---------|--------|------|----------------|-----|
| Display Large | Geist | 48px (32px mobile) | 700 | -0.04em (-0.02em mobile) | Montos grandes en dashboard |
| Headline Medium | Geist | 24px | 600 | -0.02em | Títulos de sección |
| Body Large | Geist | 16px | 400 | 0em | Texto de cuerpo |
| Body Small | Geist | 14px | 400 | 0em | Texto secundario |
| Label Caps | JetBrains Mono | 12px | 500 | 0.1em (uppercase) | Etiquetas, badges, chips |
| Numeric Data | JetBrains Mono | 18px | 600 | -0.01em | Montos en listas y tablas |

### Espaciado
Escala basada en 4px con ritmo de 8px:

| Token | Valor | Uso |
|-------|-------|-----|
| base | 4px | Espaciado interno mínimo |
| xs | 8px | Gap entre elementos pequeños |
| sm | 16px | Margen lateral móvil, padding de cards |
| md | 24px | Gap entre secciones, padding de contenido |
| lg | 40px | Separación de secciones principales |
| xl | 64px | Separación de bloques grandes |
| margin-mobile | 16px | Margen lateral en móvil |
| margin-desktop | 48px | Margen lateral en desktop |

### Border Radius
| Token | Valor | Uso |
|-------|-------|-----|
| DEFAULT | 0.25rem (4px) | Base, inputs |
| lg | 0.5rem (8px) | Botones secundarios |
| xl | 0.75rem (12px) | Cards, contenedores |
| full | 9999px | Chips, badges, pills, avatares |

### Elevación y profundidad
- **Modo claro**: sombras sutiles (0_4px_20px rgba(0,0,0,0.04)) + bordes hairline
- **Modo oscuro**: layering tonal + bordes al 8% de opacidad blanca, sin sombras pesadas
- **Nivel 0** (fondo): #050505
- **Nivel 1** (cards): #1A1A1E al 80% + borde blanco al 8%
- **Nivel 2** (overlays): backdrop blur 20-30px
- **Hover**: sombra difusa (0_8px_30px rgba(0,0,0,0.08)) en modo claro, borde neón en modo oscuro

### Lenguaje de formas
"Calculated Softness": elementos con redondeo moderado (8-12px) que mantienen seriedad fintech sin ser rígidos. Se evitan formas excesivamente redondeadas o pill en elementos estructurales.

## Justificación
La versión A usaba un enfoque visual genérico con gradients suaves. La versión B adopta una identidad "Neo-Fintech Precision" que comunica confianza institucional, velocidad y claridad financiera, similar a plataformas como Revolut pero con un acento neón distintivo.

## Validación
- Revisión visual de todas las pantallas en modo claro y oscuro
- Comprobación de contraste WCAG AA mínimo en todos los pares de color texto/fondo
- Verificación de que los verdes WCAG AA (#00A651 / #00C853) son consistentes con NeoFintechColors.kt
- Comprobación de que Geist y JetBrains Mono cargan correctamente
- Verificación de animaciones: transiciones suaves, sin jank, respetando preferencias de reduced-motion

## Impacto técnico
- Sistema de theming en Compose con Material 3 dynamic color support
- Definición de ColorScheme completo para light y dark
- Integración de fuentes Geist y JetBrains Mono como recursos
- Sistema de animaciones con Compose Animation API (AnimatedContent, animateContentSize, animateItemPlacement)
- Efecto de glow neón en modo oscuro mediante Modifier.drawWithContent + Blur

## Animaciones

### Navegación entre pantallas
- **SharedAxis** (Material 3) para transiciones entre secciones del bottom nav
- **FadeThrough** para entrada/salida de modales y bottom sheets
- Duración estándar: 300ms

### Interacciones táctiles
- **Botones**: scale 0.98 al pulsar con animatedContent
- **Cards**: animateContentSize al expandir detalles
- **Listas**: animateItemPlacement para reordenamiento de items
- **Ripple**: efecto ripple estándar de Material 3 en todos los elementos clicables

### Micro-interacciones
- **Neon glow**: efecto de brillo sutil en botones primarios en modo oscuro usando drawWithContent + Blur
- **Toggle de tema**: crossfade entre modos con AnimatedContent
- **Smart Alerts**: entrada con slide-in + fade desde arriba, staggered 50ms por item
- **Montos grandes**: animación count-up al entrar en pantalla (duration 800ms)
- **Actividad reciente**: staggered list animation con 50ms de delay entre items
- **Iconos de alerta**: pulse sutil (scale 1.05 → 1.0) cuando hay acción requerida

### Preferencias de accesibilidad
- Respetar `isAccessibilityAnimationsEnabled` para reducir o eliminar animaciones
- Todas las animaciones deben tener duración configurable

## Suposiciones
- La fuente Geist es open source (GitHub) y no requiere licencia
- JetBrains Mono es open source (SIL Open Font License)
- El efecto de glow neón puede requerir composición adicional en Compose para rendimiento óptimo en dispositivos de gama baja
- Se recomienda probar el contraste del verde neón sobre fondos oscuros en dispositivos OLED reales

## Referencias de diseño
- Concepto completo: `app/documentation/Concepto Diseño Pantallas/stitch_splitflow_expenses/`
- Guía de estilo light: `app/documentation/Concepto Diseño Pantallas/stitch_splitflow_expenses/neo_fintech_precision_1/DESIGN.md`
- Guía de estilo dark: `app/documentation/Concepto Diseño Pantallas/stitch_splitflow_expenses/neo_fintech_precision_2/DESIGN.md`
- Pantallas de referencia: ver secciones "Referencias de diseño" en cada documento UI versión B

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-05-14 | B | 1 | Alta | Sistema de diseño completo Neo-Fintech Precision: paleta dual, tipografía Geist + JetBrains Mono, espaciado, elevación, animaciones y componentes. Reemplaza NFR0001A1 como referencia visual principal. |
| 2026-06-18 | B | 2 | Actualización | Corregidos tokens Primary Container y Primary Fixed Dim para coincidir con NeoFintechColors.kt (#00A651/#00C853 y #008F47/#00B84A). Añadida nota de cumplimiento WCAG AA. |
