# FR0005A1: Calculadora automática de cuentas por evento

> **Código:** FR0005A1
> **Versión:** A
> **Revisión:** 1
> **Fecha:** 2026-04-05

## Resumen
La aplicación debe ofrecer una calculadora automática dentro del detalle de un evento para repartir gastos entre los perfiles asociados y escribir el resultado final en la deuda de cada uno.

## Historia de usuario
Como usuario de `CuentaMorosos`, quiero repartir automáticamente los gastos de un evento entre sus participantes para evitar cálculos manuales y registrar de forma rápida cuánto debe cada perfil.

## Descripción funcional
### Disponibilidad
- La calculadora solo estará disponible si el evento tiene al menos **un perfil asociado**.
- Si no hay perfiles adjuntos, el botón u opción de calculadora **no debe mostrarse**.
- Si existen perfiles pero todavía no tienen cantidades asignadas, la pantalla mostrará un **banner o card de sugerencia no intrusivo** invitando a usar la calculadora.
- El banner desaparecerá en cuanto al menos un perfil tenga una cantidad asignada o cuando se aplique un cálculo.

### Flujo principal
Al activar la calculadora, se abrirá un modal o pantalla dedicada con:
1. lista de ítems del evento (`nombre + importe`)
2. selector de modo de reparto
3. vista previa del resultado antes de confirmar
4. botón `Aplicar cálculo` para escribir el importe en cada perfil

### Modos de reparto soportados
| ID | Nombre | Lógica |
|---|---|---|
| `simple_avg` | Media simple | `total / número de perfiles` |
| `real_consumption` | Consumo real | cada ítem se asigna a uno o varios perfiles |
| `custom_percentage` | Porcentaje personalizado | el usuario reparte manualmente el `100%` |
| `by_category` | Por categoría | cada categoría usa una regla distinta |
| `by_weight` | Por peso / factor | reparto proporcional a un factor numérico |
| `by_income` | Por capacidad de pago | reparto proporcional al valor económico introducido |
| `base_plus_surplus` | Mínimo + excedente | cuota base común y resto repartido con otro modo |
| `by_attendance` | Por asistencia | reparto proporcional a días o sesiones asistidas |
| `mixed` | Cálculo mixto | combinación de varios modos por ítem o grupo |

### Funcionalidades complementarias
- **Liquidación de deudas**: mostrar el número mínimo de transferencias necesarias para saldar el evento.
- **Simulación `¿y si…?`**: cambiar el modo y ver el resultado en tiempo real sin aplicarlo todavía.
- **Descuentos y exenciones**: excluir perfiles de ítems concretos y redistribuir el importe.
- **Comparativa de métodos**: tabla comparativa por perfil y modo de reparto.

### Comportamiento al confirmar
- `Aplicar cálculo` escribe el resultado en el campo de deuda del perfil dentro del evento.
- El evento guarda también el **modo utilizado** y los **parámetros del cálculo** para reproducibilidad futura.
- Tras aplicar, debe mostrarse un resumen final con **modo usado**, **total del evento** y **desglose por perfil**.

## Reglas relacionadas
- `RN0002A1`
- `RN0003A1`

## Criterios de aceptación
- [ ] el botón de calculadora solo aparece si el evento tiene perfiles asociados
- [ ] si no hay cantidades asignadas todavía, se muestra una sugerencia no intrusiva para usar la calculadora
- [ ] el usuario puede previsualizar el reparto antes de confirmarlo
- [ ] el sistema soporta los nueve modos de reparto definidos
- [ ] `custom_percentage` bloquea la confirmación mientras la suma no sea exactamente `100%`
- [ ] `by_category` no permite aplicar si hay ítems sin categorizar
- [ ] los importes se redondean a `2` decimales y el céntimo restante se asigna al primer perfil de la lista
- [ ] al aplicar el cálculo, las deudas del evento se actualizan automáticamente
- [ ] se guardan los parámetros usados para reproducir el cálculo más adelante
- [ ] el sistema muestra resumen final y comparativa cuando corresponda

## Casos límite
- evento sin perfiles asociados
- evento con perfiles pero sin ítems añadidos
- porcentajes que no suman `100%`
- ítems exentos o asignados solo a parte de los participantes
- diferencias de redondeo por céntimos
- cambio repetido de modo durante la simulación sin confirmar

## Dependencias
- `FR0003A1`
- `UI0002A1`
- `UI0005A1`
- `DD0002A1`
- `RN0003A1`

## Suposiciones
- Esta funcionalidad actúa sobre el detalle de un evento ya creado y con perfiles adjuntos.

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-05 | A | 1 | Alta | Creación inicial del requisito de calculadora automática de cuentas por evento. |
