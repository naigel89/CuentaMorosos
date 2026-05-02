# SPR0009A1: Sprint 09 - Migración a Compose Multiplatform (iOS)

> **Código:** SPR0009A1
> **Versión:** A
> **Revisión:** A.1
> **Fecha:** 2026-04-30

## Objetivo del sprint
Reestructurar el proyecto a arquitectura Kotlin Multiplatform (KMP) con Compose Multiplatform para que la app sea compilable y funcional en iOS, compartiendo la lógica de negocio y la mayor parte de la UI con Android.

## Estado
En progreso

## Requisitos e historias incluidas
| ID | Tipo | Nombre | Prioridad | Estado | Dependencias |
|---|---|---|---|---|---|
| US-12 | US | Usar la app en un iPhone | Alta | Pendiente | SPR0008 |
| US-13 | US | Sincronización entre Android e iOS | Alta | Pendiente | US-12 |

## Tareas técnicas

### T4-01 — Reestructuración del proyecto a KMP ✅
- Creado módulo `shared` con estructura `commonMain`, `androidMain`, `iosMain`.
- Actualizado `settings.gradle.kts` para incluir `:shared`.
- Añadido plugin `org.jetbrains.kotlin.multiplatform` al build raíz.
- Los targets iOS se activan solo en macOS (la compilación iOS requiere Xcode en Mac).

### T4-02 — Mover modelos a commonMain ✅
- Trasladados `Models.kt` y `CalculatorEngine.kt` a `shared/src/commonMain/`.
- `java.util.UUID` y `java.text.SimpleDateFormat` reemplazados con `expect/actual`.
- Los archivos originales en `:app` han sido eliminados.

### T4-03 — Mover repositorios y ViewModels a commonMain ✅
- Trasladadas las interfaces `EventRepository`, `DebtRepository`, `ExpenseRepository`, `ProfileRepository`, `InvitationRepository` a `commonMain`.
- Los archivos originales en `:app` han sido eliminados.
- `LocalProfileRepository` y `CompositeProfileRepository` permanecen en `:app` (Android-específicos).

### T4-04 — Reemplazar Firebase Android SDK por wrapper KMP ✅
- Las implementaciones Firestore (`FirestoreEventRepository`, `FirestoreDebtRepository`, `FirestoreExpenseRepository`, `FirestoreProfileRepository`, `FirestoreInvitationRepository`) reescritas en `commonMain` usando `dev.gitlive:firebase-auth:1.13.0` y `dev.gitlive:firebase-firestore:1.13.0`.
- Los archivos originales en `:app` han sido eliminados.

### T4-05 — Configuración del proyecto Xcode
- **Pendiente** — Requiere Mac con Xcode.

### T4-06 — Target iOS en Firebase Console
- **Pendiente** — Requiere Mac.

### T4-07 — Pantallas Compose a commonMain
- **Pendiente** — Las pantallas permanecen en `:app` hasta que se disponga de un Mac para validar el resultado en iOS.

### T4-08 — Código expect/actual para funcionalidades nativas ✅
- Implementado `Platform.kt` en `commonMain` con `expect` para: `generateUuid()`, `currentTimeMillis()`, `formatDateMillis()`, `parseDateString()`, `currentDateText()`.
- `Platform.android.kt` usa `java.util.UUID` y `java.text.SimpleDateFormat`.
- `Platform.ios.kt` usa `platform.Foundation.NSUUID` y `NSDateFormatter`.

### T4-09 — Adaptación de navegación para iOS
- **Pendiente** — Requiere Mac con simulador iOS.

### T4-10 — Primera compilación sin regresiones en Android ✅
- `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL.
- `./gradlew :app:testDebugUnitTest` → BUILD SUCCESSFUL (todos los tests pasan).

## Riesgos o bloqueos
- Este sprint **requiere un Mac** con Xcode instalado. Sin Mac no es posible compilar para iOS.
- La migración a KMP puede introducir regresiones en Android; ejecutar pruebas de regresión completas tras cada cambio.
- Algunas APIs de Android (como `SharedPreferences` o `Context`) no están disponibles en `commonMain` y deben abstraerse.

## Definition of Done
- [x] El módulo `shared` compila sin errores para Android
- [x] Los modelos, interfaces de repositorio e implementaciones Firestore están en `commonMain`
- [x] El proyecto Android compila y los tests pasan sin regresiones
- [ ] El proyecto compila sin errores para iOS (requiere Mac)
- [ ] Las pantallas principales son funcionales en el simulador de iPhone
- [ ] Login, gestión de eventos y gastos funcionan en iOS
- [ ] La sincronización con Firestore funciona en iOS
- [ ] No hay regresiones en la versión Android

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-30 | A | A.1 | Alta | Creación del sprint 09 con migración a Compose Multiplatform para iOS. |
| 2026-05-02 | A | A.2 | Alta | T4-01..04, T4-08, T4-10 completados: módulo shared KMP creado, modelos y repos migrados a commonMain, Firebase reemplazado por gitlive, expect/actual implementado, APK Android BUILD SUCCESSFUL. |
