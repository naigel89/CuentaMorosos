# UI0008B1: Pantallas de autenticación Firebase

> **Código:** UI0008B1
> **Versión:** B
> **Revisión:** 2
> **Fecha:** 2026-06-20

## Resumen
Conjunto de pantallas de autenticación que permiten al usuario registrarse, iniciar sesión y recuperar su contraseña mediante Firebase Auth. Incluye pantalla de entrada animada (splash+login unificado). Implementadas en `shared/src/commonMain/kotlin/com/cuentamorosos/ui/auth/`.

## Historia de usuario relacionada
Como usuario, quiero crear una cuenta, iniciar sesión y recuperar mi contraseña para acceder a mis eventos y datos desde cualquier dispositivo, con una experiencia de entrada pulida y profesional.

## Objetivo de las pantallas
Proveer un flujo completo de autenticación con una entrada animada de marca, validación de campos, manejo de errores descriptivos y navegación entre pantallas de auth.

## Componentes visibles

| Pantalla | Componentes | Descripción |
|---|---|---|
| **SplashAuthScreen** | Logo animado (fade-in + slide-up + scale-down), título "CuentaMorosos", subtítulo "Inicia sesión para continuar", Email field, Password field, Visibility toggle, "Iniciar sesión" button, "¿Olvidaste tu contraseña?" link, "Regístrate" link | **Pantalla principal unificada de autenticación**. Reemplaza la antigua `LoginScreen`. Ejecuta una coreografía de 5 fases al entrar: (1) logo fade-in 400ms, (2) logo slide-up + scale-down 700ms, (3) título slide-in a los 1400ms, (4) subtítulo a los 1800ms, (5) campos del formulario en stagger a los 1900-2100ms. ~3s de animación total. El formulario es completamente interactivo: valida email y contraseña en tiempo real y ejecuta Firebase Auth al pulsar "Iniciar sesión". |
| **RegisterScreen** | Email field, Password field, Confirm password field, "Registrarse" button, "Ya tengo cuenta" link | Pantalla de registro. Valida coincidencia de contraseñas, longitud mínima (8 caracteres) y formato de email. |
| **ForgotPasswordScreen** | Email field, "Enviar enlace" button, Success confirmation message, "Volver al login" link | Pantalla de recuperación. Envía email de restablecimiento mediante `FirebaseAuth.sendPasswordResetEmail()`. Muestra confirmación tras envío exitoso. |

## Animación del Splash

| Fase | Timing | Descripción |
|---|---|---|
| 1. Logo fade-in | 0–400ms | El logo aparece con fade-in desde alpha 0 a 1 |
| 2. Logo slide-up + scale-down | 400–1100ms | El logo se desliza hacia arriba (~220dp) y se escala de 280dp a 164dp |
| 3. Título | 1400ms | "CuentaMorosos" entra con slide-up |
| 4. Subtítulo | 1800ms | "Inicia sesión para continuar" entra con slide-up |
| 5. Formulario | 1900–2100ms | Email, password, botón y links entran en stagger con slide-up (50ms de separación) |

**Parámetros ajustables** en el código: `logoSizeDp` (280), `logoEndSizeDp` (164), `logoStartFromTopDp` (300), `logoEndFromTopDp` (80).

**Respeto de accesibilidad**: si `LocalAnimationsEnabled` es `false`, la animación se omite y el formulario aparece directamente.

## Validaciones

| Campo | Regla |
|---|---|
| Email | Formato válido (contiene `@` y `.`), no vacío |
| Password | Mínimo 8 caracteres, no vacío |
| Confirm password | Debe coincidir con Password |

## Navegación

| Origen | Destino |
|---|---|
| App (no autenticado) | `SplashAuthScreen` (reemplaza a `LoginScreen`) |
| `SplashAuthScreen` → "Regístrate" | `RegisterScreen` |
| `SplashAuthScreen` → "¿Olvidaste tu contraseña?" | `ForgotPasswordScreen` |
| `RegisterScreen` → "Ya tengo cuenta" | `SplashAuthScreen` |
| `ForgotPasswordScreen` → "Volver al login" | `SplashAuthScreen` |
| Cualquier pantalla (auth exitoso) | `DashboardScreen` (`UI0007B1`) |

## Nota técnica: `expect/actual` para Firebase

Las operaciones de Firebase Auth utilizan el patrón `expect`/`actual` de Kotlin Multiplatform. La interfaz común se declara en `commonMain` y las implementaciones específicas de plataforma residen en `androidMain` y `iosMain`. Esto permite que las pantallas de auth sean compartidas entre Android e iOS mientras Firebase Auth se integra de forma nativa en cada plataforma.

## Referencias
- Código fuente: `shared/src/commonMain/kotlin/com/cuentamorosos/ui/auth/`
- SplashAuthScreen: `SplashAuthScreen.kt` (297 líneas)
- Integración Android: `app/src/main/java/com/cuentamorosos/MainActivity.kt` → `AuthFlow()`
- Dependencia Firebase KMP: `dev.gitlive:firebase-auth`
- Documentación funcional relacionada: `FR0009A1-user-stories-online-ios.md` (Épica 1: Autenticación)
- Sistema de diseño: `NFR0001B1-experiencia-visual-y-personalizacion.md`

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-06-18 | B | 1 | Alta | Documentación inicial de pantallas de autenticación Firebase: LoginScreen, RegisterScreen, ForgotPasswordScreen. |
| 2026-06-20 | B | 2 | Actualización | Documentada SplashAuthScreen: pantalla unificada splash+login con animación de 5 fases. Actualizada navegación y referencias. |
