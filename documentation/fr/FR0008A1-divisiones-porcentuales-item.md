# FR0008A1: Divisiones porcentuales por ítem individual

> **Código:** FR0008A1
> **Versión:** A
> **Revisión:** 1
> **Fecha:** 2026-04-05

## Resumen
Permite asignar porcentajes específicos a cada participante en un ítem concreto (ej. 60% y 40%), en lugar de un reparto equitativo. Este modo está implementado en el motor de cálculo como `SplitMode.CUSTOM_PERCENTAGE`.

## Funcionamiento
El usuario introduce un porcentaje para cada perfil participante en el ítem. El sistema valida que la suma total de porcentajes sea exactamente 100% antes de permitir aplicar el cálculo. La validación se realiza en `SplitCalculator.calculatePercentage()`, ubicada en `shared/src/commonMain/kotlin/com/cuentamorosos/model/CalculatorEngine.kt`.

## Visualización
Los resultados del reparto porcentual se previsualizan en `ComparisonCard.kt`, donde cada perfil muestra su monto asignado según el porcentaje definido. El componente itera sobre `SplitMode.entries` — el modo `CUSTOM_PERCENTAGE` es uno de los 6 modos visibles como pestaña.

## Validación
- La suma de porcentajes debe ser exactamente 100%.
- Si la suma es distinta de 100%, el botón «Aplicar cálculo» permanece deshabilitado y se muestra un mensaje de error inline.
- Los porcentajes se almacenan con precisión de dos decimales.

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-05 | A | 1 | Alta | Creación inicial del requisito funcional de divisiones porcentuales por ítem. |
