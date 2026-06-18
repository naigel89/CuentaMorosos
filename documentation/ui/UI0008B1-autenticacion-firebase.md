# UI0008B1: Pantallas de autenticación Firebase

> **Código:** UI0008B1
> **Versión:** B
> **Revisión:** 1
> **Fecha:** 2026-06-18

## Resumen
Conjunto de pantallas de autenticación que permiten al usuario registrarse, iniciar sesión y recuperar su contraseña mediante Firebase Auth. Implementadas en `shared/src/commonMain/kotlin/com/cuentamorosos/ui/auth/`.

## Historia de usuario relacionada
Como usuario, quiero crear una cuenta, iniciar sesión y recuperar mi contraseña para acceder a mis eventos y datos desde cualquier dispositivo.

## Objetivo de la pantalla
Proveer un flujo completo de autenticación con validación de campos, manejo de errores descriptivos y navegación entre pantallas de auth.

## Componentes visibles

| Pantalla | Componentes | Descripción |
|---|---|---|
| **LoginScreen** | Email field, Password field, Visibility toggle, "Iniciar sesión" button, "¿Olvidaste tu contraseña?" link, "Crear cuenta" link | Pantalla principal de autenticación. Valida formato de email y contraseña no vacía antes de llamar a Firebase Auth. |
| **RegisterScreen** | Email field, Password field, Confirm password field, "Registrarse" button, "Ya tengo cuenta" link | Pantalla de registro. Valida coincidencia de contraseñas, longitud mínima (8 caracteres) y formato de email. |
| **ForgotPasswordScreen** | Email field, "Enviar enlace" button, Success confirmation message, "Volver al login" link | Pantalla de recuperación. Envía email de restablecimiento mediante `FirebaseAuth.sendPasswordResetEmail()`. Muestra confirmación tras envío exitoso. |

## Validaciones

| Campo | Regla |
|---|---|
| Email | Formato válido (contiene `@` y `.`), no vacío |
| Password | Mínimo 8 caracteres, no vacío |
| Confirm password | Debe coincidir con Password |

## Navegación

| Origen | Destino |
|---|---|
| App (no autenticado) | `LoginScreen` |
| `LoginScreen` → "Crear cuenta" | `RegisterScreen` |
| `LoginScreen` → "¿Olvidaste tu contraseña?" | `ForgotPasswordScreen` |
| `RegisterScreen` → "Ya tengo cuenta" | `LoginScreen` |
| `ForgotPasswordScreen` → "Volver al login" | `LoginScreen` |
| Cualquier pantalla (auth exitoso) | `DashboardScreen` (`UI0007B1`) |

## Nota técnica: `expect/actual` para Firebase

Las operaciones de Firebase Auth utilizan el patrón `expect`/`actual` de Kotlin Multiplatform. La interfaz común se declara en `commonMain` y las implementaciones específicas de plataforma residen en `androidMain` y `iosMain`. Esto permite que las pantallas de auth sean compartidas entre Android e iOS mientras Firebase Auth se integra de forma nativa en cada plataforma.

## Referencias
- Código fuente: `shared/src/commonMain/kotlin/com/cuentamorosos/ui/auth/`
- Dependencia Firebase KMP: `dev.gitlive:firebase-auth`
- Documentación funcional relacionada: `FR0009A1-user-stories-online-ios.md` (Épica 1: Autenticación)

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-06-18 | B | 1 | Alta | Documentación inicial de pantallas de autenticación Firebase: LoginScreen, RegisterScreen, ForgotPasswordScreen. |
