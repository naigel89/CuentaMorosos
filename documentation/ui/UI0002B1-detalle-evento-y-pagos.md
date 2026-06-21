# UI0002B1: Detalle de evento y pagos — Rediseño Neo-Fintech

> **Código:** UI0002B1
> **Versión:** B
> **Revisión:** 2
> **Fecha:** 2026-06-20

## Resumen
Rediseño del detalle de evento con layout de dos columnas (desktop), lista de gastos con iconos contextuales y panel lateral de liquidación con participantes y estados de pago. Incluye panel de recibo (`ReceiptPanel`) con resumen del cálculo aplicado.

## Historia de usuario relacionada
Como usuario, quiero ver dentro de un evento cada gasto detallado, quién lo pagó, cómo se reparte y quién debe cuánto, con acceso rápido a calcular totales y marcar pagados.

## Objetivo de la pantalla
Centralizar el seguimiento operativo de un evento con una jerarquía visual clara: gastos a la izquierda, liquidación a la derecha (desktop), o apilados (móvil).

## Componentes visibles
| Componente | Tipo | Descripción | Obligatorio |
|---|---|---|---|
| Header del evento | sección | Link "Back to Events", título, fechas | Sí |
| Total Event Cost | card | Monto total del evento destacado | Sí |
| Lista de gastos | lista | Icono, título, pagado por, tipo de reparto, monto, fecha | Sí |
| Botón Add Expense | acción | Abre formulario para nuevo gasto | Sí |
| Panel Settlement | sidebar/card | Botón "Calculate Totals" + lista de participantes | Sí |
| Participante | fila | Avatar (iniciales), nombre, estado de deuda. Checkbox de pago solo visible si `eventState != OPEN` | Sí |
| ReceiptPanel | card/sección | Visible tras aplicar cálculo. Muestra modo de cálculo (etiqueta legible), total, fecha y resumen por perfil | Condicional |
| Bottom Navigation | barra | Events, Profiles, Settings | Sí |

## Estados de la interfaz
- Evento sin gastos: lista vacía con mensaje placeholder
- Evento con gastos: lista con separadores horizontales (4% opacidad blanca en dark)
- Evento en estado OPEN: participantes visibles pero sin checkboxes de pago (regla `RN0001A1`)
- Evento en estado CALCULATED/CLOSED: participantes con checkboxes de pago y panel ReceiptPanel visible
- Participante sin pagar: monto en verde neón (deuda pequeña) o rojo (deuda grande)
- Participante pagado: texto "Settled" en gris, checkbox marcado
- Hover en gasto: fondo ligeramente más oscuro (transition-colors)

## Reglas de interacción
- "Back to Events" vuelve a `UI0001B1`
- "Add Expense" abre modal/bottom sheet para registrar gasto
- "Calculate Totals" abre o ejecuta la calculadora (`UI0005B1`)
- SettlementPanel permite **selección múltiple de deudas** con checkboxes y aplicar varias transferencias de una sola vez mediante `applyCalculation()`
- Checkbox de participante: solo visible si `eventState != OPEN`. Al marcar, cambia estado a "Settled" con animación
- Al aplicar pagos, `paidTransferIndices` se pasan a `applyCalculation()` para aplicar las transferencias de forma **atómica y secuencial**, protegido por un `Mutex` que evita colisiones con el sync
- Pulsar un gasto: expande detalles o abre edición
- ReceiptPanel: muestra el modo de cálculo con etiqueta legible (ej. "Consumo Real" en vez de `real_consumption`) usando `SplitMode.fromId().label`
- Los montos usan JetBrains Mono con figuras tabulares para alineación

## Navegación
- Origen: `UI0001B1` (pantalla de eventos)
- Destino secundario: `UI0005B1` (calculadora), formulario de edición de gasto
- Bottom nav siempre visible en móvil

## Consideraciones UX/UI
- **Layout**: en desktop, grid de 3 columnas donde gastos ocupan 2 y settlement 1. En móvil, todo apilado verticalmente
- **Tipografía**: Geist para títulos y cuerpo, JetBrains Mono para montos y etiquetas de reparto
- **Iconos de gasto**: círculos con fondo de color según categoría (vuelo = tertiary, alojamiento = secondary, comida = error)
- **Badges de reparto**: "Split equally (4)" o "Unequal split" en pills con fondo surface-container
- **Animaciones**:
  - Checkbox: ripple + color transition del estado
  - Entrada de la página: fade-in del header, luego staggered de gastos
  - Hover en gastos: background transition duration-200
  - Botón "Calculate Totals": scale-98 al pulsar
- **Colores de deuda**: verde neón para deudas normales, rojo saturado para deudas significativas (umbral configurable)

## Referencias de diseño
- Modo claro: `app/documentation/Concepto Diseño Pantallas/stitch_splitflow_expenses/detalle_del_evento_neon_edit_1/`
- Modo oscuro: `app/documentation/Concepto Diseño Pantallas/stitch_splitflow_expenses/detalle_del_evento_neon_edit_2/`
- Guía de estilo light: `app/documentation/Concepto Diseño Pantallas/stitch_splitflow_expenses/neo_fintech_precision_1/DESIGN.md`
- Guía de estilo dark: `app/documentation/Concepto Diseño Pantallas/stitch_splitflow_expenses/neo_fintech_precision_2/DESIGN.md`
- Sistema visual completo: `documentation/nfr/NFR0001B1-experiencia-visual-y-personalizacion.md`

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-05-14 | B | 1 | Alta | Rediseño con layout dos columnas, panel de settlement con checkboxes, iconos contextuales por categoría de gasto. Basado en concepto Neo-Fintech Precision. |
| 2026-06-20 | B | 2 | Actualización | Sincronizado con código: checkboxes de pago solo visibles en estados CALCULATED/CLOSED (regla RN0001A1). Agregado ReceiptPanel con etiqueta legible de modo de cálculo (`SplitMode.fromId().label`). |
| 2026-06-21 | B | 3 | Actualización | SettlementPanel permite selección múltiple de deudas. Aplicación atómica y secuencial mediante `applyCalculation()` con `paidTransferIndices` y `Mutex` sync-safe. |
