# UI0001B1: Pantalla de eventos — Rediseño Neo-Fintech

> **Código:** UI0001B1
> **Versión:** B
> **Revisión:** 1
> **Fecha:** 2026-05-14

## Resumen
Rediseño de la pantalla de eventos con layout tipo "Bento Grid", resumen de balance en la parte superior y tarjetas de evento con información enriquecida (icono, estado, participantes, saldo).

## Historia de usuario relacionada
Como usuario, quiero ver de un vistazo mi balance general y mis eventos activos con toda la información relevante para tomar decisiones rápidas sobre mis cuentas pendientes.

## Objetivo de la pantalla
Evolucionar de un listado simple a un dashboard visual que combine métricas de balance con la gestión de eventos, manteniendo el acceso rápido a creación y filtrado.

## Componentes visibles
| Componente | Tipo | Descripción | Obligatorio |
|---|---|---|---|
| Balance Summary (Bento Grid) | sección | Tarjeta principal de balance total + 2 quick stats | Sí |
| Tarjeta Total Balance | card | Monto total, indicador "Te deben", badge de tendencia | Sí |
| Quick Stats | cards laterales | Eventos activos (contador) + Total gastado | Sí |
| Sección Recent Events | lista/grid | Título + subtítulo + botón crear evento (desktop) | Sí |
| Tarjeta de evento | card | Icono, badge estado, título, fechas, gasto total, tu parte, avatares, saldo | Sí |
| Tarjeta crear evento | card vacía | Borde discontinuo, icono +, texto descriptivo | Sí |
| FAB (móvil) | botón flotante | Botón circular para crear evento rápido | Sí |
| Bottom Navigation | barra | Events, Profiles, Settings | Sí |

## Estados de la interfaz
- Sin eventos: se muestra solo la tarjeta "Crear Nuevo Evento" con borde discontinuo
- Con eventos activos: grid de tarjetas con badge "Active" en verde neón
- Con eventos cerrados: tarjetas con opacidad reducida y badge "Settled"
- Hover en tarjeta: elevación de sombra (0_8px_30px) y título cambia a color primario
- Avatares apilados: cuando hay más de 3 participantes, se muestra "+N"

## Reglas de interacción
- Pulsar una tarjeta de evento abre su detalle (`UI0002B1`)
- FAB móvil: animación de escala 0.95 al pulsar
- Botón "Create Event" desktop: mismo comportamiento que FAB
- Tarjeta "Crear Nuevo Evento": hover con scale-110 en el icono +
- Bottom nav: el ítem activo usa icon relleno (FILL=1) y texto en negrita
- Transición entre pantallas: SharedAxis (Material 3)

## Navegación
- Origen: puede ser pantalla inicial o desde bottom nav
- Destino: `UI0002B1` al abrir un evento
- Bottom nav: Events (activo), Profiles, Settings

## Consideraciones UX/UI
- **Layout Bento Grid**: la tarjeta de balance ocupa 2 columnas en desktop, los quick stats se apilan en columna lateral
- **Tipografía**: Geist para headlines y body (modo oscuro), Hanken Grotesk + Inter (modo claro). Montos en JetBrains Mono para alineación tabular
- **Color**: el verde neón (#39FF14) se usa en badges "Active", montos positivos y borde superior de la tarjeta de balance
- **Animaciones**:
  - Entrada de tarjetas: staggered fade-in con delay de 50ms entre cada una
  - Hover en cards: transición de sombra duration-300
  - Icono "+" en crear evento: scale-110 en hover con duration-300
  - Bottom nav: scale-98 en el ítem activo con transition-all duration-200
- **Espaciado**: gap de 24px entre secciones, 16px de margen lateral en móvil

## Referencias de diseño
- Modo claro: `app/documentation/Concepto Diseño Pantallas/stitch_splitflow_expenses/eventos_neon_edit_1/`
- Modo oscuro: `app/documentation/Concepto Diseño Pantallas/stitch_splitflow_expenses/eventos_neon_edit_2/`
- Guía de estilo light: `app/documentation/Concepto Diseño Pantallas/stitch_splitflow_expenses/neo_fintech_precision_1/DESIGN.md`
- Guía de estilo dark: `app/documentation/Concepto Diseño Pantallas/stitch_splitflow_expenses/neo_fintech_precision_2/DESIGN.md`
- Sistema visual completo: `documentation/nfr/NFR0001B1-experiencia-visual-y-personalizacion.md`

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-05-14 | B | 1 | Alta | Rediseño completo con layout Bento Grid, balance summary y tarjetas enriquecidas. Basado en concepto Neo-Fintech Precision. |
