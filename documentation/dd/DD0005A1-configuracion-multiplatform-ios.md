# Guía de Configuración Multiplatform (iOS)

## Objetivo

Esta guía detalla el proceso para migrar el proyecto Android actual a **Kotlin Multiplatform (KMP)** con **Compose Multiplatform**, de forma que se pueda compilar y ejecutar una versión nativa para iOS compartiendo la mayor parte del código con Android.

---

## 1. Prerrequisitos

### Hardware y Software Obligatorio

| Requisito              | Detalle                                          |
|------------------------|--------------------------------------------------|
| Mac con Apple Silicon o Intel | macOS Ventura 13.x o superior           |
| Xcode                  | Versión 15 o superior (desde la App Store)       |
| Android Studio         | Hedgehog o superior con plugin KMP instalado     |
| Kotlin                 | 1.9.x o superior                                 |
| JDK                    | 17 o superior                                    |
| CocoaPods              | `sudo gem install cocoapods`                     |
| Cuenta Apple Developer | Necesaria para firmar y publicar en la App Store |

> **Nota importante**: La compilación para iOS **solo es posible en un Mac**. No es posible compilar el target de iOS desde Windows o Linux.

---

## 2. Estructura del Proyecto KMP

Tras la migración, la estructura de carpetas del proyecto quedará así:

```
CuentaMorosos/
├── shared/                          ← Módulo compartido (commonMain)
│   └── src/
│       ├── commonMain/kotlin/       ← Lógica 100% compartida
│       │   ├── model/               ← Models.kt, CalculatorEngine.kt
│       │   ├── repository/          ← Repositorios (Firestore, etc.)
│       │   └── viewmodel/           ← ViewModels compartidos
│       ├── androidMain/kotlin/      ← Código específico Android
│       └── iosMain/kotlin/          ← Código específico iOS
│
├── app/                             ← Módulo Android (UI Compose)
│   └── src/main/java/com/cuentamorosos/
│       └── ui/                      ← Pantallas Compose (Android)
│
└── iosApp/                          ← Módulo iOS (Xcode project)
    └── iosApp.xcodeproj
```

---

## 3. Pasos de Configuración

### Paso 1: Instalar el plugin KMP en Android Studio

1. Abrir Android Studio → `Settings` → `Plugins`.
2. Buscar **Kotlin Multiplatform** e instalar.
3. Reiniciar Android Studio.

### Paso 2: Modificar `build.gradle.kts` (raíz)

```kotlin
plugins {
    kotlin("multiplatform") version "1.9.x" apply false
    id("com.android.application") version "8.x.x" apply false
    id("org.jetbrains.compose") version "1.5.x" apply false
}
```

### Paso 3: Configurar el módulo `shared`

```kotlin
// shared/build.gradle.kts
kotlin {
    androidTarget()
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation("dev.gitlive:firebase-auth:1.x.x")
            implementation("dev.gitlive:firebase-firestore:1.x.x")
        }
        androidMain.dependencies {
            implementation("androidx.activity:activity-compose:1.x.x")
        }
    }
}
```

> Se usa `dev.gitlive:firebase-*` que es el wrapper KMP oficial de Firebase para que funcione tanto en Android como en iOS.

### Paso 4: Integrar el framework en el proyecto Xcode

1. Abrir el proyecto Xcode en `iosApp/`.
2. En `Build Phases` → `Link Binary with Libraries`, añadir `shared.framework`.
3. En `Build Settings`, configurar el `Framework Search Path` apuntando a la carpeta de salida del build de Kotlin.
4. Alternativamente, usar **CocoaPods** para la integración automática:

```ruby
# iosApp/Podfile
target 'iosApp' do
  use_frameworks!
  pod 'shared', :path => '../shared'
end
```

Ejecutar `pod install` desde la carpeta `iosApp/`.

### Paso 5: Añadir el target de iOS en Firebase Console

1. Ir a Firebase Console → tu proyecto → `Añadir app` → iOS.
2. Introducir el Bundle ID de la app iOS (ej: `com.cuentamorosos.ios`).
3. Descargar el archivo `GoogleService-Info.plist`.
4. Arrastrarlo al proyecto Xcode dentro de la carpeta `iosApp/`.

---

## 4. Código Específico por Plataforma

Algunas funcionalidades requieren implementación nativa. Se usa `expect/actual`:

```kotlin
// commonMain - Declaración esperada
expect fun getPlatformName(): String

// androidMain - Implementación Android
actual fun getPlatformName(): String = "Android"

// iosMain - Implementación iOS
actual fun getPlatformName(): String = "iOS"
```

Ejemplos de código que necesitará `expect/actual`:
- Gestión de notificaciones locales.
- Acceso a la cámara o galería de fotos.
- Estilos de navegación (back button en Android vs. swipe en iOS).

---

## 5. Compilar y Ejecutar en iOS

### Desde Android Studio (Simulador)

1. Seleccionar el target `iosApp` en el selector de run configurations.
2. Elegir un simulador de iPhone.
3. Pulsar Run.

### Desde Xcode (Dispositivo físico)

1. Abrir `iosApp/iosApp.xcworkspace` en Xcode.
2. Conectar el iPhone por USB.
3. Seleccionar el dispositivo en el selector de destino.
4. Pulsar el botón de Run (▶).

> Para instalar en un dispositivo físico, la cuenta de Apple Developer debe estar configurada en Xcode (`Settings` → `Accounts`).

---

## 6. Publicación en la App Store

1. Crear un **App ID** en Apple Developer Portal.
2. Crear los **Provisioning Profiles** de distribución.
3. En Xcode: `Product` → `Archive`.
4. Subir el archivo a App Store Connect mediante Xcode Organizer.
5. Completar los metadatos de la app (descripción, capturas de pantalla, categoría).
6. Enviar a revisión de Apple (el proceso tarda entre 1 y 3 días hábiles).

---

## 7. Diferencias de Diseño Android vs. iOS

| Elemento               | Android                      | iOS                          |
|------------------------|------------------------------|------------------------------|
| Barra de navegación    | Bottom Navigation Bar        | Tab Bar (parte inferior)     |
| Botón atrás            | Hardware/sistema             | Swipe desde el borde izquierdo |
| Tipografía por defecto | Roboto                       | San Francisco                |
| Notificaciones         | Notification Channels        | UNUserNotificationCenter     |
| Permisos               | `AndroidManifest.xml`        | `Info.plist`                 |

Se recomienda revisar las [Human Interface Guidelines de Apple](https://developer.apple.com/design/human-interface-guidelines/) para asegurar que la experiencia en iOS sea natural para los usuarios de ese ecosistema.

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-05 | A | 1 | Alta | Creación inicial de la guía de configuración KMP para iOS. |
| 2026-06-18 | A | 2 | Actualización | Actualizado `androidApp/` → `app/` en árbol de directorios para reflejar estructura real del proyecto. |
