# SPR0006A1: Sprint 06 - Autenticación Firebase

> **Código:** SPR0006A1
> **Versión:** A
> **Revisión:** 2
> **Fecha:** 2026-05-14

## Objetivo del sprint
Implementar el sistema de cuentas de usuario mediante Firebase Auth: registro, inicio de sesión, recuperación de contraseña y cierre de sesión.

## Estado
Hecho — implementado en `shared/src/commonMain/` e **integrado** en `MainActivity` con SplashAuthScreen + LoginScreen + RegisterScreen + ForgotPasswordScreen + email verification gate.

## Requisitos e historias incluidas
| ID | Tipo | Nombre | Prioridad | Estado | Dependencias |
|---|---|---|---|---|---|
| US-01 | US | Registro de nuevo usuario | Alta | Hecho | — |
| US-02 | US | Inicio de sesión | Alta | Hecho | US-01 |
| US-03 | US | Recuperación de contraseña | Media | Hecho | US-01 |
| US-04 | US | Cerrar sesión | Media | Hecho | US-02 |

## Tareas técnicas

### T1-01 — Configuración de Firebase Console ✓
- Proyecto Firebase creado, Auth con Email/Contraseña habilitado.
- `google-services.json` añadido al módulo `app`.

### T1-02 — Dependencias Firebase Auth ✓
- `firebase-bom` y `firebase-auth-ktx` añadidos.

### T1-03 a T1-09 — Pantallas y navegación auth ✓ (en shared/)
- `LoginScreen`, `RegisterScreen`, `ForgotPasswordScreen`, `UserProfileScreen` implementados en `commonMain`.
- `NavHost` auth-aware con comprobación de `FirebaseAuth.currentUser`.
- `MigrationManager` y `MigrationScreen` para migración de datos locales a Firestore.
- Cierre de sesión desde ajustes implementado.

### T1-10 — Pruebas manuales ✅
- Flujo completo validado: registro → email verification gate → login → cierre de sesión.

## Riesgos o bloqueos
- Requiere conexión a internet para todas las operaciones de autenticación.

## Definition of Done
- [x] Código de LoginScreen, RegisterScreen, ForgotPasswordScreen, UserProfileScreen implementado en shared/
- [x] NavHost auth-aware implementado en shared/
- [x] Migración de datos locales a Firestore implementada
- [x] MainActivity wireada con SplashAuthScreen + LoginScreen + RegisterScreen + ForgotPasswordScreen + email verification gate
- [x] Flujo completo de registro → email verification → login → cierre de sesión validado en app real

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-30 | A | A.1 | Alta | Creación del sprint 06 con autenticación Firebase. |
| 2026-04-30 | A | A.2 | Alta | Sprint 06 implementado en shared/: LoginScreen, RegisterScreen, ForgotPasswordScreen, UserProfileScreen, NavHost auth-aware. |
| 2026-05-14 | A | A.3 | Corrección | Estado cambiado a Parcial: código existe en shared/ pero MainActivity no está integrada. |
| 2026-06-25 | A | A.4 | Actualización | Estado cambiado a Hecho: MainActivity integrada con SplashAuthScreen + LoginScreen + RegisterScreen + ForgotPasswordScreen + email verification gate. Flujo completo validado. |
