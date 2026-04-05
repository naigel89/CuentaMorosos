# RN0003A1: Reglas de reparto y redondeo automático

> **Código:** RN0003A1
> **Versión:** A
> **Revisión:** 1
> **Fecha:** 2026-04-05

## Descripción
La calculadora automática de un evento debe repartir el total según el modo seleccionado, respetar las validaciones previas y almacenar un resultado reproducible para cada perfil del evento.

## Motivación
Evitar errores manuales al repartir gastos y mantener trazabilidad sobre cómo se obtuvo cada deuda.

## Condiciones
- la calculadora solo está disponible si el evento tiene al menos un perfil asociado
- si no hay perfiles, el botón o acceso no debe renderizarse
- si todos los perfiles del evento tienen la deuda vacía, se muestra una sugerencia no intrusiva para usar la calculadora
- todos los importes calculados se redondean a `2` decimales
- si queda una diferencia de redondeo, el céntimo restante se asigna al primer perfil de la lista
- el modo `custom_percentage` exige que la suma sea exactamente `100%` antes de aplicar
- el modo `by_category` exige que todos los ítems estén categorizados
- las exenciones o descuentos redistribuyen el importe solo entre quienes sigan participando en el ítem
- el resultado no modifica la deuda real hasta pulsar `Aplicar cálculo`
- el evento debe guardar el modo usado y los parámetros del cálculo para reproducibilidad futura

## Impacto funcional
- acelera la carga de cantidades por perfil dentro del evento
- permite comparar varios métodos antes de decidir cuál aplicar
- mantiene coherencia entre el cálculo mostrado y la deuda realmente almacenada

## Ejemplos
- **Válido:** total `30,00 €`, tres perfiles y modo `simple_avg` → `10,00 €` por perfil
- **Válido:** total `10,00 €`, tres perfiles → `3,33 €`, `3,33 €` y `3,34 €` asignando el céntimo restante al primer perfil
- **No válido:** permitir aplicar `custom_percentage` con una suma del `95%`
- **No válido:** aplicar `by_category` con ítems sin categoría asignada

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-05 | A | 1 | Alta | Creación inicial de las reglas de reparto y redondeo automático. |
