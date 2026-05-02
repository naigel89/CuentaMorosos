# SPR0006A1: Sprint 06 - Autenticación Firebase

> **Código:** SPR0006A1
> **Versión:** A
> **Revisión:** 1
> **Fecha:** 2026-04-30

## Objetivo del sprint
Implementar el sistema de cuentas de usuario mediante Firebase Auth: registro, inicio de sesión, recuperación de contraseña y cierre de sesión, sentando las bases para la funcionalidad online.

## Estado
Hecho

## Requisitos e historias incluidas
| ID | Tipo | Nombre | Prioridad | Estado | Dependencias |
|---|---|---|---|---|---|
| US-01 | US | Registro de nuevo usuario | Alta | Pendiente | — |
| US-02 | US | Inicio de sesión | Alta | Pendiente | US-01 |
| US-03 | US | Recuperación de contraseña | Media | Pendiente | US-01 |
| US-04 | US | Cerrar sesión | Media | Pendiente | US-02 |

## Tareas técnicas

### T1-01 — Configuración de Firebase Console
- Crear proyecto en Firebase Console.
- Habilitar Firebase Auth con proveedor Email/Contraseña.
- Descargar y añadir `google-services.json` al módulo `app`.
- Añadir el plugin `com.google.gms.google-services` en `build.gradle.kts`.

### T1-02 — Dependencias Firebase Auth
- Añadir `platform("com.google.firebase:firebase-bom")` al catálogo de versiones.
- Añadir `firebase-auth-ktx` en las dependencias del módulo.

### T1-03 — Pantalla de Login
- Nuevo composable `LoginScreen` con campos email y contraseña.
- Validación de formato de email y longitud mínima de contraseña (8 caracteres).
- Llamada a `FirebaseAuth.signInWithEmailAndPassword`.
- Mensajes de error descriptivos según el tipo de excepción de Firebase.

### T1-04 — Pantalla de Registro
- Nuevo composable `RegisterScreen` con campos email, contraseña y confirmación.
- Llamada a `FirebaseAuth.createUserWithEmailAndPassword`.
- Al registrarse correctamente, crear documento `users/{uid}` en Firestore.

### T1-05 — Pantalla de Recuperación de contraseña
- Nuevo composable `ForgotPasswordScreen` con campo email.
- Llamada a `FirebaseAuth.sendPasswordResetEmail`.
- Confirmación visual de envío del email.

### T1-06 — Pantalla de Perfil de usuario
- Nuevo composable `UserProfileScreen` mostrando email y nombre del usuario.
- Campo editable para el nombre de visualización.

### T1-07 — Cierre de sesión
- Opción "Cerrar sesión" en la pantalla de ajustes.
- Al cerrar sesión, llamar a `FirebaseAuth.signOut()` y limpiar caché local.

### T1-08 — Documento de usuario en Firestore
- Al registrarse, crear `users/{uid}` con campos `uid`, `email`, `displayName`, `createdAt`.

### T1-09 — Navegación auth vs. app principal
- El `NavHost` principal comprueba el estado de `FirebaseAuth.currentUser`.
- Si no hay sesión activa, redirigir al grafo de autenticación.
- Si hay sesión activa, redirigir a la pantalla principal.

### T1-10 — Pruebas manuales
- Flujo completo de registro → login → cierre de sesión.
- Verificar que la sesión persiste tras cerrar y reabrir la app.
- Verificar el email de recuperación de contraseña.

## Riesgos o bloqueos
- Requiere conexión a internet para todas las operaciones de autenticación.
- La cuenta de Firebase debe estar correctamente configurada antes de empezar.

## Definition of Done
- [x] Un usuario puede registrarse con email y contraseña
- [x] Un usuario puede iniciar sesión con sus credenciales
- [x] La sesión persiste entre cierres de la app
- [x] El flujo de recuperación de contraseña envía un email correctamente
- [x] Un usuario puede cerrar sesión desde ajustes
- [x] Se crea el documento `users/{uid}` en Firestore al registrarse

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-30 | A | A.1 | Alta | Creación del sprint 06 con autenticación Firebase. |
| 2026-04-30 | A | A.2 | Alta | Sprint 06 implementado: LoginScreen, RegisterScreen, ForgotPasswordScreen, UserProfileScreen, NavHost auth-aware, botón de cerrar sesión en Ajustes. |
| 2026-05-02 | A | A.3 | Actualización | Ajuste de robustez alineado con SPR0008A1: consolidada la sincronización de `users/{uid}` (uid/email/displayName) en un manager común para registro, login y edición de perfil. |
