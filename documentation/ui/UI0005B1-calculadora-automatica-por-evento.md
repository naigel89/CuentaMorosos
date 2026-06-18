# UI0005B1: Calculadora automática por evento — Rediseño Neo-Fintech

> **Código:** UI0005B1
> **Versión:** B
> **Revisión:** 2
> **Fecha:** 2026-06-18

## Resumen
Calculadora automática con los 6 modos de `SplitMode` visibles como pestañas (`SplitMode.entries.forEach` en `ComparisonCard.kt` línea 105), vista previa por perfil en tiempo real y botón para aplicar el cálculo. Implementada en `ComparisonCard.kt`, `ModeSelectorChip.kt` y `SplitCalculator` dentro de `shared/src/commonMain/kotlin/com/cuentamorosos/ui/` y `.../model/`.

## Historia de usuario relacionada
Como usuario, quiero elegir entre distintos métodos de reparto y ver inmediatamente cómo se distribuye el gasto entre los participantes.

## Objetivo de la pantalla
Ofrecer todos los modos de reparto con igual visibilidad, permitiendo al usuario comparar y seleccionar el que mejor se adapte a su evento.

## Componentes visibles
| Componente | Tipo | Descripción | Obligatorio |
|---|---|---|---|
| Mode Tabs | fila de chips | 6 pestañas: EQUAL_SPLIT, PROPORTIONAL, CUSTOM_PERCENTAGE, CUSTOM_AMOUNTS, BY_CHECKIN, BY_CATEGORY | Sí |
| Lista de ítems | lista editable | Nombre e importe de cada gasto del evento | Sí |
| Parámetros dinámicos | formulario | Campos que cambian según el modo seleccionado | Sí |
| Vista previa por perfil | tabla/resumen | Cuánto pagaría cada perfil, actualización en tiempo real | Sí |
| Botón Aplicar cálculo | acción principal | Guarda el reparto, solo habilitado si validación OK | Sí |
| Botón Cancelar | acción secundaria | Cierra sin persistir cambios | Sí |

## Estados de la interfaz
- No disponible si el evento no tiene perfiles asociados
- Apertura desde `UI0002B1` con contexto del evento
- Modo seleccionado: chip con fondo `primaryContainer`, los otros 5 visibles pero no seleccionados
- Vista previa: se actualiza en tiempo real al modificar parámetros
- Validación error: campos inválidos destacados en rojo, botón "Aplicar" deshabilitado

## Reglas de interacción
- La calculadora solo se abre si el evento tiene perfiles
- Los 6 modos son siempre visibles como chips horizontales — no hay menú desplegable ni modos ocultos
- `SplitMode.entries.forEach` itera todos los modos y renderiza un `ModeSelectorChip` por cada uno
- La vista previa se actualiza en tiempo real ante cualquier cambio
- "Aplicar cálculo" solo habilitado cuando toda la validación pasa
- Al confirmar, se cierra la calculadora y `UI0002B1` refleja los nuevos importes

## Navegación
- Origen: `UI0002B1` (detalle de evento)
- Destino de retorno: `UI0002B1` con cantidades actualizadas

## Consideraciones UX/UI
- **6 modos visibles**: todos los `SplitMode` se muestran como chips en una fila horizontal con scroll
- **ModeSelectorChip**: chip con `primaryContainer` como color de selección
- **Vista previa en tiempo real**: los montos se actualizan con cada cambio de parámetro
- **Validación visual**: campos inválidos con borde rojo y mensaje inline

## Referencias de diseño
- Sistema visual completo: `documentation/nfr/NFR0001B1-experiencia-visual-y-personalizacion.md`
- Código fuente: `shared/src/commonMain/kotlin/com/cuentamorosos/ui/ComparisonCard.kt`, `ModeSelectorChip.kt`
- Motor de cálculo: `shared/src/commonMain/kotlin/com/cuentamorosos/model/CalculatorEngine.kt`

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-05-14 | B | 1 | Alta | Rediseño simplificado con "Consumo Real" como flujo principal, modos secundarios ocultos, vista previa en tiempo real. Alineado con concepto de simplificación UI0006A1. |
| 2026-06-18 | B | 2 | Actualización | Sincronizado con código real: los 6 modos de SplitMode son visibles como tabs (no ocultos). Documentado `SplitMode.entries.forEach` en ComparisonCard.kt. Eliminada referencia a simplificación/ocultamiento. |
