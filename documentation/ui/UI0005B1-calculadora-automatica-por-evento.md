# UI0005B1: Calculadora automática por evento — Rediseño simplificado

> **Código:** UI0005B1
> **Versión:** B
> **Revisión:** 1
> **Fecha:** 2026-05-14

## Resumen
Rediseño de la calculadora automática con enfoque simplificado: "Consumo Real" como flujo principal y automático, modos secundarios ocultos tras expansión, y jerarquía visual clara entre selección, parámetros, vista previa y confirmación.

## Historia de usuario relacionada
Como usuario, quiero una calculadora que haga el trabajo pesado automáticamente, con la opción de ajustar manualmente si lo necesito, sin abrumarme con modos que no entiendo.

## Objetivo de la pantalla
Reducir la carga cognitiva al máximo, haciendo que el reparto automático sea el camino por defecto y los modos avanzados accesibles pero no predominantes.

## Componentes visibles
| Componente | Tipo | Descripción | Obligatorio |
|---|---|---|---|
| Selector de modo | dropdown/tabs | "Consumo Real" por defecto, modos secundarios expandibles | Sí |
| Lista de ítems | lista editable | Nombre e importe de cada gasto del evento | Sí |
| Parámetros dinámicos | formulario | Campos que cambian según el modo seleccionado | Sí |
| Vista previa por perfil | tabla/resumen | Cuánto pagaría cada perfil, actualización en tiempo real | Sí |
| Bloque de liquidación | card | Propuesta de transferencias mínimas entre participantes | Sí |
| Botón Aplicar cálculo | acción principal | Guarda el reparto, solo habilitado si validación OK | Sí |
| Botón Cancelar | acción secundaria | Cierra sin persistir cambios | Sí |

## Estados de la interfaz
- No disponible si el evento no tiene perfiles asociados
- Apertura desde `UI0002B1` con contexto del evento
- Modo "Consumo Real" activo: muestra solo campos esenciales
- Modos secundarios expandidos: campos adicionales aparecen con animación
- Vista previa: se actualiza en tiempo real al modificar parámetros
- Validación error: campos inválidos destacados en rojo, botón "Aplicar" deshabilitado
- Confirmación: resumen del reparto aplicado antes de cerrar

## Reglas de interacción
- La calculadora solo se abre si el evento tiene perfiles
- "Consumo Real" es el modo por defecto y recomendado
- Modos secundarios se expanden/colapsan con animación
- La vista previa se actualiza en tiempo real ante cualquier cambio
- "Aplicar cálculo" solo habilitado cuando toda la validación pasa
- Al confirmar, se cierra la calculadora y `UI0002B1` refleja los nuevos importes
- "Cancelar" descarta todos los cambios sin confirmar

## Navegación
- Origen: `UI0002B1` (detalle de evento)
- Destino de retorno: `UI0002B1` con cantidades actualizadas
- Se presenta como bottom sheet (móvil) o modal centrado (desktop)

## Consideraciones UX/UI
- **Jerarquía visual**: 4 pasos claramente diferenciados — modo → parámetros → preview → confirmación
- **Simplificación**: el usuario promedio solo necesita ver "Consumo Real" y los ítems; todo lo demás es opcional
- **Vista previa en tiempo real**: los montos se actualizan con cada cambio de parámetro, sin necesidad de botón "calcular"
- **Bloque de liquidación**: propone transferencias mínimas para saldar deudas (algoritmo de simplificación de deudas)
- **Animaciones**:
  - Apertura: slide-up desde bottom (móvil) o fade-in + scale (desktop)
  - Expansión de modos secundarios: animateContentSize
  - Actualización de vista previa: animateContent con crossfade en los montos
  - Botón "Aplicar": scale-98 al pulsar, glow neón en modo oscuro
- **Validación visual**: campos inválidos con borde rojo y mensaje inline, no modal de error

## Referencias de diseño
- Concepto de simplificación: `documentation/ui/UI0006A1-simplificacion-calculadora.md`
- Detalle de evento (origen): `app/documentation/Concepto Diseño Pantallas/stitch_splitflow_expenses/detalle_del_evento_neon_edit_1/` y `_2/`
- Guía de estilo light: `app/documentation/Concepto Diseño Pantallas/stitch_splitflow_expenses/neo_fintech_precision_1/DESIGN.md`
- Guía de estilo dark: `app/documentation/Concepto Diseño Pantallas/stitch_splitflow_expenses/neo_fintech_precision_2/DESIGN.md`
- Sistema visual completo: `documentation/nfr/NFR0001B1-experiencia-visual-y-personalizacion.md`

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-05-14 | B | 1 | Alta | Rediseño simplificado con "Consumo Real" como flujo principal, modos secundarios ocultos, vista previa en tiempo real. Alineado con concepto de simplificación UI0006A1. |
