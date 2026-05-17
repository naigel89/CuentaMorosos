# SPR0009A1: Sprint 09 - Migración a Compose Multiplatform (iOS)

> **Código:** SPR0009A1
> **Versión:** A
> **Revisión:** 2
> **Fecha:** 2026-05-14

## Objetivo del sprint
Reestructurar el proyecto a arquitectura Kotlin Multiplatform (KMP) con Compose Multiplatform para que la app sea compilable y funcional en iOS, compartiendo la lógica de negocio y la mayor parte de la UI con Android.

## Estado
Parcial — módulo `shared/` creado y compilando para Android. iOS pendiente (requiere Mac). **Código NO integrado** en `MainActivity`.

## Requisitos e historias incluidas
| ID | Tipo | Nombre | Prioridad | Estado | Dependencias |
|---|---|---|---|---|---|
| US-12 | US | Usar la app en un iPhone | Alta | Pendiente | SPR0008 |
| US-13 | US | Sincronización entre Android e iOS | Alta | Pendiente | US-12 |

## Tareas técnicas

### T4-01 — Reestructuración del proyecto a KMP ✅
- Módulo `shared` con `commonMain`, `androidMain`, `iosMain`.
- `settings.gradle.kts` actualizado con `:shared`.

### T4-02 a T4-04 — Modelos, repositorios y Firebase en commonMain ✅
- `Models.kt`, `CalculatorEngine.kt` trasladados a `commonMain`.
- Interfaces de repositorio en `commonMain`.
- Implementaciones Firestore reescritas con `dev.gitlive:firebase-*`.

### T4-05 — Configuración del proyecto Xcode ⏳
- **Pendiente** — Requiere Mac con Xcode.

### T4-06 — Target iOS en Firebase Console ⏳
- **Pendiente** — Requiere Mac.

### T4-07 — Pantallas Compose a commonMain ✅
- `CuentaMorososApp`, `LoginScreen`, `RegisterScreen`, `ForgotPasswordScreen`, `UserProfileScreen` migrados a `commonMain`.
- Firebase extraído a lambdas de callback provistas por `:app`.

### T4-08 — Código expect/actual ✅
- `Platform.kt` con `expect` para UUID, fechas, formateo.
- `Platform.android.kt` y `Platform.ios.kt` implementados.

### T4-09 — Adaptación de navegación para iOS ⏳
- **Pendiente** — Requiere Mac con simulador iOS.

### T4-10 — Compilación Android sin regresiones ✅
- `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL.
- `./gradlew :app:testDebugUnitTest` → BUILD SUCCESSFUL.

## Riesgos o bloqueos
- **BLOQUEO PRINCIPAL**: `MainActivity` no usa `CuentaMorososApp` de `shared/`. Todo el código KMP existe pero está desconectado.
- iOS requiere Mac con Xcode instalado.

## Definition of Done
- [x] Módulo `shared` compila sin errores para Android
- [x] Modelos, repositorios e implementaciones Firestore en commonMain
- [x] Todas las pantallas Compose y ViewModels migrados a commonMain
- [x] Código expect/actual para funcionalidades nativas
- [x] Android compila y tests pasan sin regresiones
- [ ] MainActivity wireada para usar CuentaMorososApp de shared/
- [ ] Proyecto compila para iOS (requiere Mac)
- [ ] Pantallas funcionales en simulador de iPhone

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-30 | A | A.1 | Alta | Creación del sprint 09 con migración a Compose Multiplatform. |
| 2026-05-02 | A | A.3 | Alta | T4-07 completado: pantallas y ViewModels migrados a commonMain. |
| 2026-05-14 | A | A.4 | Corrección | Estado cambiado a Parcial: código en shared/ pero MainActivity no integrada. iOS pendiente. |
