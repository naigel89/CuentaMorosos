# UI0003B1: Pantalla de perfiles — Rediseño Neo-Fintech

> **Código:** UI0003B1
> **Versión:** B
> **Revisión:** 1
> **Fecha:** 2026-05-14

## Resumen
Rediseño de la pantalla de perfiles con dashboard de resumen (Bento Grid), botones de acción y lista de perfiles con avatares, saldos y badges de estado.

## Historia de usuario relacionada
Como usuario, quiero ver de un vistazo cuánto me debe cada persona y cuánto debo yo, con acceso rápido al detalle de sus eventos pendientes.

## Objetivo de la pantalla
Ofrecer una vista agregada por persona con métricas de balance global y estados visuales claros (te debe / debes / saldado).

## Componentes visibles
| Componente | Tipo | Descripción | Obligatorio |
|---|---|---|---|
| Summary Dashboard | Bento Grid | "Total Owed to You" (+$1,240.50) vs "Total You Owe" (-$350.00) | Sí |
| Tarjeta Te deben | card | Monto positivo en verde, icono arrow_downward, contador de perfiles | Sí |
| Tarjeta Debes | card | Monto negativo en rojo, icono arrow_upward, contador de perfiles | Sí |
| Botones de acción | fila | "Add Profile" (primario neón) + "Filter" (secundario) | Sí |
| Lista de perfiles | lista | Avatar, nombre, eventos compartidos, monto, badge de estado | Sí |
| Badge de estado | chip | "Owes you" (verde), "You owe" (rojo), "Settled up" (gris) | Sí |
| Bottom Navigation | barra | Events, Profiles (activo), Settings | Sí |
| Desktop nav flotante | pill bar | Barra pill flotante centrada en la parte inferior | Sí (desktop) |

## Estados de la interfaz
- Sin perfiles: lista vacía con mensaje y botón "Add Profile" destacado
- Perfiles con deuda: monto en verde neón o rojo según dirección
- Perfiles saldados: opacidad 75%, badge "Settled up" en gris
- Avatares: foto real o iniciales sobre fondo de color primario
- Hover en perfil: fondo surface-container-low con transition-colors

## Reglas de interacción
- Pulsar un perfil abre el desglose de eventos pendientes de esa persona
- "Add Profile" abre formulario de creación de perfil
- "Filter" abre opciones de filtrado (por deuda, alfabético, eventos)
- El monto visible excluye eventos ya pagados
- Bottom nav: ítem activo con icon relleno y fondo surface-container

## Navegación
- Origen: bottom nav o navegación principal
- Destino secundario: detalle del perfil con desglose por evento
- Desktop: barra de navegación flotante tipo pill en la parte inferior central

## Consideraciones UX/UI
- **Bento Grid**: las dos tarjetas de resumen ocupan 2 columnas en desktop, apiladas en móvil
- **Tipografía**: Geist para nombres y títulos, JetBrains Mono para montos (figuras tabulares)
- **Badges de estado**: pills con fondo tintado al 10-20% del color y texto en color completo
- **Avatares**: si no hay foto, se muestran iniciales sobre fondo primary-container con texto en on-primary-container
- **Animaciones**:
  - Entrada del dashboard: fade-in con count-up en los montos grandes
  - Hover en perfiles: transition-colors duration-200
  - Bottom nav: scale-98 en ítem activo con transition-all duration-200
  - Desktop nav pill: aparece con slide-up + fade al cargar
- **Colores**: verde neón (#39FF14) para "Owes you", rojo error para "You owe", gris tertiary para "Settled up"

## Referencias de diseño
- Modo claro: `app/documentation/Concepto Diseño Pantallas/stitch_splitflow_expenses/perfiles_neon_edit_1/`
- Modo oscuro: `app/documentation/Concepto Diseño Pantallas/stitch_splitflow_expenses/perfiles_neon_edit_2/`
- Guía de estilo light: `app/documentation/Concepto Diseño Pantallas/stitch_splitflow_expenses/neo_fintech_precision_1/DESIGN.md`
- Guía de estilo dark: `app/documentation/Concepto Diseño Pantallas/stitch_splitflow_expenses/neo_fintech_precision_2/DESIGN.md`
- Sistema visual completo: `documentation/nfr/NFR0001B1-experiencia-visual-y-personalizacion.md`

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-05-14 | B | 1 | Alta | Rediseño con dashboard Bento Grid de balance, badges de estado con tintes de color, avatares con iniciales fallback. Basado en concepto Neo-Fintech Precision. |
