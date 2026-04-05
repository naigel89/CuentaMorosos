# UI0004A1: Ajustes y apariencia

> **Código:** UI0004A1
> **Versión:** A
> **Revisión:** 1
> **Fecha:** 2026-04-05

## Resumen
Pantalla o sección de preferencias desde la que el usuario puede personalizar el aspecto visual y ciertos comportamientos de la aplicación.

## Historia de usuario relacionada
Como usuario, quiero elegir tema y color secundario para adaptar la app a mis preferencias visuales.

## Objetivo de la pantalla
Permitir la configuración de apariencia y futuros parámetros como los días de recordatorio.

## Componentes visibles
| Componente | Tipo | Descripción | Obligatorio |
|---|---|---|---|
| selector de tema | control | cambia entre modo claro y oscuro | Sí |
| selector de color secundario | paleta/opciones | permite elegir rosa, rojo, verde, azul, amarillo, etc. | Sí |
| vista previa del estilo | bloque visual | muestra cómo se aplica el tema | No |
| configuración de recordatorios | campo/selector | ajusta los días para recordar pagos pendientes | No |

## Estados de la interfaz
- preferencias por defecto
- tema claro activo
- tema oscuro activo
- color secundario personalizado

## Reglas de interacción
- el cambio de tema debe reflejarse inmediatamente o tras guardar
- el color secundario se aplica a bordes y elementos destacados
- las preferencias deben persistir entre sesiones

## Navegación
- origen: menú o ajustes generales de la app
- destino: vuelve a la pantalla anterior manteniendo la preferencia elegida

## Consideraciones UX/UI
El estilo debe ser minimalista, con gradients suaves y consistentes con la identidad de la app.

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-05 | A | 1 | Alta | Creación inicial del documento UI de ajustes y apariencia. |
