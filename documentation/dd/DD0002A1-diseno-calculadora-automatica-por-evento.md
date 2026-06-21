# DD0002A1: Diseño de la calculadora automática por evento

> **Código:** DD0002A1
> **Versión:** A
> **Revisión:** 1
> **Fecha:** 2026-04-05

## Resumen técnico
Se define el diseño funcional y técnico de una calculadora de reparto para eventos, capaz de calcular deudas por perfil, simular escenarios y persistir los parámetros utilizados.

## Historia de usuario relacionada
Como usuario, quiero introducir los gastos de un evento y obtener automáticamente cuánto debe pagar cada perfil según diferentes reglas de reparto.

## Componentes involucrados
- detalle de evento
- modal o pantalla de calculadora
- motor de cálculo de reparto
- módulo de previsualización y comparativa
- persistencia de metadatos del cálculo aplicado
- módulo de simplificación de deudas o liquidación

## Flujo de solución
1. el usuario entra en el detalle del evento
2. si existen perfiles y no hay importes cargados, se muestra una sugerencia contextual
3. el usuario abre la calculadora y añade o revisa los ítems del evento
4. selecciona un modo de reparto y sus parámetros
5. el sistema recalcula el resultado en tiempo real sin persistirlo todavía
6. el usuario revisa comparativa, liquidación y resumen final
7. al confirmar, se escriben los importes en cada perfil y se guardan los metadatos del cálculo

## Modelo de datos
### Entidades o estructuras sugeridas
- **EventoItem**: `id`, `eventoId`, `nombre`, `importeEuro`, `categoria/opcional`
- **ParametroCalculo**: `modo`, `porcentajes`, `factores`, `exenciones`, `asistencias`, `modoExcedente/opcional`
- **ResultadoCalculoPerfil**: `perfilId`, `importeCalculado`, `detalle`, `ordenRedondeo`
- **Evento.calculoMetadata**: `ultimoModo`, `fechaCalculo`, `parametrosSerializados`, `totalEvento`

## Lógica de cálculo
| Modo | Consideración técnica |
|---|---|
| `real_consumption` | cada ítem se reparte solo entre los perfiles asignados |
| `simple_avg` | división a partes iguales entre todos los participantes |
| `by_category` | ítems compartidos se dividen entre todos; el resto, entre asignados |
| `custom_percentage` | valida en tiempo real el `100%` |
| `exact` | cada perfil paga un importe fijo (debe coincidir con el total) |
| `parts` | cada perfil pone partes enteras (1-100); reparto proporcional |

## Dependencias y riesgos
- complejidad de UX al mostrar muchos modos en una sola pantalla
- riesgo de incoherencias si no se guardan bien los parámetros
- necesidad de redondeo consistente para evitar descuadres por céntimos
- la simplificación de deudas requiere validar bien la lógica del mínimo de transferencias

## Decisiones técnicas
- todos los cálculos deben redondearse a `2` decimales al final del proceso
- la simulación `¿y si...?` no persiste cambios hasta confirmación explícita
- el sistema debe guardar metadatos para reproducibilidad y trazabilidad
- la comparación de métodos se genera sobre el mismo conjunto de ítems y perfiles activos

## Suposiciones
- La primera implementación puede partir de un conjunto de ítems definidos dentro del propio evento sin necesidad de backend externo.

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-05 | A | 1 | Alta | Creación inicial del diseño de la calculadora automática por evento. |
