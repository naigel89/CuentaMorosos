# SPR0009A1: Sprint 09 - Migración a Compose Multiplatform (iOS)

> **Código:** SPR0009A1
> **Versión:** A
> **Revisión:** 1
> **Fecha:** 2026-04-30

## Objetivo del sprint
Reestructurar el proyecto a arquitectura Kotlin Multiplatform (KMP) con Compose Multiplatform para que la app sea compilable y funcional en iOS, compartiendo la lógica de negocio y la mayor parte de la UI con Android.

## Estado
Pendiente

## Requisitos e historias incluidas
| ID | Tipo | Nombre | Prioridad | Estado | Dependencias |
|---|---|---|---|---|---|
| US-12 | US | Usar la app en un iPhone | Alta | Pendiente | SPR0008 |
| US-13 | US | Sincronización entre Android e iOS | Alta | Pendiente | US-12 |

## Tareas técnicas

### T4-01 — Reestructuración del proyecto a KMP
- Crear módulo `shared` con estructura `commonMain`, `androidMain`, `iosMain`.
- Crear módulo `androidApp` que consume `shared`.
- Crear proyecto Xcode en `iosApp/` con la configuración inicial.
- Actualizar `settings.gradle.kts` para incluir los nuevos módulos.

### T4-02 — Mover modelos a commonMain
- Trasladar `Models.kt` y `CalculatorEngine.kt` a `shared/src/commonMain/`.
- Resolver dependencias de `java.util.UUID` y `java.text.SimpleDateFormat` usando `expect/actual` o equivalentes de KMP.

### T4-03 — Mover repositorios y ViewModels a commonMain
- Trasladar los repositorios de Firestore a `commonMain` usando `dev.gitlive:firebase-firestore`.
- Trasladar los ViewModels a `commonMain` usando `lifecycle-viewmodel` de KMP o equivalente.

### T4-04 — Reemplazar Firebase Android SDK por wrapper KMP
- Sustituir `com.google.firebase:firebase-auth-ktx` por `dev.gitlive:firebase-auth`.
- Sustituir `com.google.firebase:firebase-firestore-ktx` por `dev.gitlive:firebase-firestore`.
- Verificar que el comportamiento es idéntico en Android.

### T4-05 — Configuración del proyecto Xcode
- Integrar el framework `shared` en el proyecto Xcode mediante CocoaPods o Swift Package Manager.
- Añadir `GoogleService-Info.plist` al proyecto Xcode (target iOS en Firebase Console).

### T4-06 — Target iOS en Firebase Console
- Registrar la app iOS con su Bundle ID en Firebase Console.
- Descargar `GoogleService-Info.plist` y añadirlo al proyecto Xcode.

### T4-07 — Pantallas Compose a commonMain
- Trasladar las pantallas principales (`EventsScreen`, `EventDetailScreen`, etc.) a `commonMain`.
- Usar `expect/actual` para los elementos de navegación específicos de plataforma.

### T4-08 — Código expect/actual para funcionalidades nativas
- Implementar `expect/actual` para: generación de UUID, formateo de fechas, notificaciones locales y back navigation.

### T4-09 — Adaptación de navegación para iOS
- Ajustar el componente de navegación para que el gesto de swipe-back funcione nativamente en iOS.
- Eliminar referencias al botón físico de retroceso de Android donde sea necesario.

### T4-10 — Primera compilación en simulador de iPhone
- Compilar el target iOS sin errores.
- Verificar que las pantallas principales se renderizan correctamente en el simulador.
- Verificar que el login y la gestión de eventos funcionan en iOS.

## Riesgos o bloqueos
- Este sprint **requiere un Mac** con Xcode instalado. Sin Mac no es posible compilar para iOS.
- La migración a KMP puede introducir regresiones en Android; ejecutar pruebas de regresión completas tras cada cambio.
- Algunas APIs de Android (como `SharedPreferences` o `Context`) no están disponibles en `commonMain` y deben abstraerse.

## Definition of Done
- [ ] El proyecto compila sin errores para Android e iOS
- [ ] Las pantallas principales son funcionales en el simulador de iPhone
- [ ] Login, gestión de eventos y gastos funcionan en iOS
- [ ] La sincronización con Firestore funciona en iOS
- [ ] No hay regresiones en la versión Android

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-30 | A | 1 | Alta | Creación del sprint 09 con migración a Compose Multiplatform para iOS. |
