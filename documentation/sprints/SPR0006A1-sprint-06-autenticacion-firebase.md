# SPR0006A1: Sprint 06 - Autenticación Firebase

> **Código:** SPR0006A1
> **Versión:** A
> **Revisión:** 2
> **Fecha:** 2026-05-14

## Objetivo del sprint
Implementar el sistema de cuentas de usuario mediante Firebase Auth: registro, inicio de sesión, recuperación de contraseña y cierre de sesión.

## Estado
Parcial — código implementado en `shared/src/commonMain/` pero **NO integrado** en `MainActivity`. La app actual sigue usando la versión sin auth.

## Requisitos e historias incluidas
| ID | Tipo | Nombre | Prioridad | Estado | Dependencias |
|---|---|---|---|---|---|
| US-01 | US | Registro de nuevo usuario | Alta | Parcial | — |
| US-02 | US | Inicio de sesión | Alta | Parcial | US-01 |
| US-03 | US | Recuperación de contraseña | Media | Parcial | US-01 |
| US-04 | US | Cerrar sesión | Media | Parcial | US-02 |

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

### T1-10 — Pruebas manuales ⏳
- Pendiente de integración con MainActivity para validar flujo completo.

## Riesgos o bloqueos
- **BLOQUEO PRINCIPAL**: `MainActivity` no usa `CuentaMorososApp` de `shared/`. Todo el auth code existe pero está desconectado.
- Requiere conexión a internet para todas las operaciones de autenticación.

## Definition of Done
- [x] Código de LoginScreen, RegisterScreen, ForgotPasswordScreen, UserProfileScreen implementado en shared/
- [x] NavHost auth-aware implementado en shared/
- [x] Migración de datos locales a Firestore implementada
- [ ] MainActivity wireada para usar CuentaMorososApp de shared/
- [ ] Flujo completo de registro → login → cierre de sesión validado en app real

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-30 | A | A.1 | Alta | Creación del sprint 06 con autenticación Firebase. |
| 2026-04-30 | A | A.2 | Alta | Sprint 06 implementado en shared/: LoginScreen, RegisterScreen, ForgotPasswordScreen, UserProfileScreen, NavHost auth-aware. |
| 2026-05-14 | A | A.3 | Corrección | Estado cambiado a Parcial: código existe en shared/ pero MainActivity no está integrada. |
