# UI0005A1: Calculadora automática por evento

> **Código:** UI0005A1
> **Versión:** A
> **Revisión:** 1
> **Fecha:** 2026-04-05

## Resumen
Modal o pantalla dedicada para introducir ítems del evento, seleccionar el modo de reparto y previsualizar cómo se distribuye el gasto entre los perfiles asociados.

## Historia de usuario relacionada
Como usuario, quiero una interfaz clara para simular y aplicar el reparto automático de gastos dentro de un evento.

## Objetivo de la pantalla
Reducir el trabajo manual al rellenar las deudas de cada perfil y permitir comparar distintos métodos antes de confirmar.

## Componentes visibles
| Componente | Tipo | Descripción | Obligatorio |
|---|---|---|---|
| listado de ítems | lista editable | nombre e importe de cada gasto del evento | Sí |
| selector de modo | tabs/dropdown | elige el método de reparto | Sí |
| panel de parámetros dinámicos | formulario | porcentajes, factores, categorías, asistencias, etc. | Sí |
| vista previa por perfil | tabla/resumen | muestra cuánto pagaría cada perfil | Sí |
| comparativa de métodos | tabla | compara varios modos a la vez | No |
| bloque de liquidación | card/resumen | propone transferencias mínimas | No |
| botón `Aplicar cálculo` | acción principal | guarda el reparto en el evento | Sí |
| botón `Cancelar` | acción secundaria | cierra sin persistir cambios | Sí |

## Estados de la interfaz
- acceso no disponible si el evento no tiene perfiles
- apertura con sugerencia desde el detalle del evento
- error de validación en porcentajes o categorías
- simulación con actualización en tiempo real
- confirmación final con resumen del reparto aplicado

## Reglas de interacción
- la calculadora se abre solo si el evento tiene perfiles asociados
- el modo seleccionado cambia dinámicamente los campos requeridos
- la vista previa se actualiza antes de aplicar
- `Aplicar cálculo` solo está habilitado cuando la validación es correcta
- al confirmar, se cierra la calculadora y la vista del evento refleja los nuevos importes

## Navegación
- origen: `UI0002A1`
- destino de retorno: `UI0002A1` con las cantidades ya escritas

## Consideraciones UX/UI
El flujo debe ser claro aunque existan múltiples modos de reparto. Se recomienda usar una jerarquía visual muy marcada entre selección del modo, parámetros, previsualización y confirmación.

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-05 | A | 1 | Alta | Creación inicial del documento UI de la calculadora automática por evento. |
