# SPR0010A1: Sprint 10 - Optimización, offline y publicación

> **Código:** SPR0010A1
> **Versión:** A
> **Revisión:** 1
> **Fecha:** 2026-04-30

## Objetivo del sprint
Hacer la app robusta para uso sin conexión mediante caché local, optimizar el rendimiento de las consultas a Firestore, pulir la experiencia en iOS y publicar las versiones finales en Google Play Store y Apple App Store.

## Estado
Pendiente

## Requisitos e historias incluidas
| ID | Tipo | Nombre | Prioridad | Estado | Dependencias |
|---|---|---|---|---|---|
| US-11 | US | Usar la app sin conexión a internet | Alta | Pendiente | SPR0007 |

## Tareas técnicas

### T5-01 — Configuración de Room para caché local
- Añadir dependencia de `androidx.room` (o equivalente KMP) al módulo `shared`.
- Crear entidades Room para `EventItem`, `EventDebtItem` y `EventExpenseItem`.
- Crear DAOs con las operaciones necesarias.
- Configurar la base de datos Room.

### T5-02 — OfflineFirstRepository
- Implementar `OfflineFirstEventRepository` que combina Room (lectura inmediata) con Firestore (fuente de verdad).
- Al abrir la app, servir los datos locales y actualizar en segundo plano desde Firestore.
- Los cambios realizados offline se encolan y se sincronizan al recuperar la conexión.

### T5-03 — Indicador de estado de conexión
- Añadir un `NetworkMonitor` que observe el estado de la conectividad del dispositivo.
- Mostrar un banner o icono en la cabecera cuando la app esté en modo offline.

### T5-04 — Pruebas de sincronización offline
- Verificar que los cambios hechos offline se sincronizan correctamente al volver a conectarse.
- Verificar que no se pierden datos ni se producen duplicados durante la sincronización.

### T5-05 — Optimización de consultas a Firestore
- Revisar todas las consultas y añadir los índices compuestos necesarios.
- Implementar paginación en la lista de eventos si el número de documentos es elevado.
- Reducir el número de lecturas usando snapshots incrementales en lugar de consultas completas.

### T5-06 — Revisión de UI/UX en iOS
- Revisar todas las pantallas en iOS según las Human Interface Guidelines de Apple.
- Ajustar márgenes, tipografía y componentes de navegación donde sean necesarios.
- Verificar el comportamiento en diferentes tamaños de pantalla de iPhone.

### T5-07 — Pruebas de regresión en Android
- Ejecutar el flujo completo de la app en Android: autenticación, eventos, gastos, deudas, colaboración, offline.
- Corregir los errores encontrados.

### T5-08 — Pruebas de regresión en iOS
- Ejecutar el flujo completo de la app en el simulador de iPhone y en un dispositivo físico si es posible.
- Corregir los errores encontrados.

### T5-09 — Preparación de metadatos para las tiendas
- Redactar la descripción de la app en español e inglés.
- Capturar screenshots en los tamaños requeridos por Play Store y App Store.
- Preparar el icono de la app en las resoluciones requeridas.

### T5-10 — Publicación en Google Play Store
- Generar el APK/AAB de release firmado.
- Subir a Play Console y completar el formulario de lanzamiento.

### T5-11 — Publicación en Apple App Store
- Realizar el Archive desde Xcode.
- Subir a App Store Connect mediante Xcode Organizer.
- Completar los metadatos y enviar a revisión de Apple (1-3 días hábiles).

## Riesgos o bloqueos
- La revisión de Apple puede tardar varios días y podría rechazar la app si no cumple sus guías.
- La implementación de Room en KMP puede requerir el uso de librerías alternativas como SQLDelight para soporte multiplataforma completo.

## Definition of Done
- [ ] La app funciona correctamente sin conexión a internet
- [ ] Los cambios offline se sincronizan al recuperar la conexión sin pérdida de datos
- [ ] Se muestra un indicador visual cuando la app está en modo offline
- [ ] No hay regresiones en Android ni en iOS
- [ ] La app está publicada en Google Play Store
- [ ] La app ha sido enviada a revisión en Apple App Store

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-30 | A | A.1 | Alta | Creación del sprint 10 con optimización, modo offline y publicación. |
