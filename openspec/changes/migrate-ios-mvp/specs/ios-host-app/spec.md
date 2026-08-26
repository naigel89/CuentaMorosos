# iOS Host App Specification

## Purpose

Define el host iOS mínimo que consume el framework de `shared`. Hoy el framework se compila en
cada push y nadie lo enlaza; esta capability cierra ese hueco.

Todos los criterios están redactados para ser comprobables **desde CI sobre `macos-15`**, porque
no hay Mac disponible. Un requisito que solo pueda validarse abriendo Xcode a mano no pertenece
aquí.

## Requirements

### R001: El proyecto Xcode se genera, no se versiona

El repositorio SHALL versionar un `iosApp/project.yml` de XcodeGen y NO SHALL versionar ningún
`.xcodeproj` ni `.pbxproj`. El `.gitignore` SHALL excluir `iosApp/*.xcodeproj` y
`iosApp/*.xcworkspace`.

#### Scenario: el proyecto se materializa en CI

- GIVEN un checkout limpio sin `.xcodeproj`
- WHEN CI ejecuta `xcodegen generate` dentro de `iosApp/`
- THEN se produce `iosApp.xcodeproj` con el target `iosApp`
- AND el paso siguiente de `pod install` lo integra en `iosApp.xcworkspace`

#### Scenario: un cambio de configuración es revisable

- GIVEN alguien añade una capability o cambia el bundle ID
- WHEN abre un PR
- THEN el diff SHALL mostrar líneas legibles de `project.yml`, no UUIDs regenerados

### R002: Entry point y superficie del framework

`iosMain` SHALL exponer exactamente una función de entrada, `MainViewController()`, que devuelve
un `UIViewController` con la app Compose montada. El código Swift NO SHALL referenciar ningún
otro símbolo del módulo compartido.

#### Scenario: Swift arranca la app

- GIVEN `FirebaseApp.configure()` ya se ha ejecutado
- WHEN `iOSApp.swift` instancia `MainViewController()`
- THEN se renderiza `CuentaMorososApp` dentro de `CuentaMorososTheme`
- AND la pantalla visible es la de login, porque no hay sesión

#### Scenario: la superficie no se ensancha por descuido

- GIVEN un PR que añade una llamada de Swift a otro símbolo de `shared`
- WHEN se revisa
- THEN SHALL rechazarse o justificarse explícitamente, por ser superficie que solo CI valida

### R003: Firebase nativo linkado

`iosApp/Podfile` SHALL declarar los pods `FirebaseCore`, `FirebaseAuth` y `FirebaseFirestore` con
versiones exactas, compatibles con `dev.gitlive:firebase-*:1.13.0`. El `Gemfile.lock` SHALL
generarse y versionarse desde el toolchain de CI.

#### Scenario: gitlive encuentra su SDK nativo

- GIVEN el binario enlazado con los pods de Firebase
- WHEN se ejecutan los tests Native de `shared`
- THEN SHALL ejecutarse de verdad, no solo compilarse
- AND `ios-build.yml` SHALL sustituir `compileTestKotlinIosSimulatorArm64` por la tarea de test

#### Scenario: falta el archivo de configuración

- GIVEN un checkout sin `GoogleService-Info.plist` (no se versiona: lleva claves del proyecto)
- WHEN CI construye
- THEN SHALL usar un plist de marcador inyectado por el propio workflow
- AND el fallo, si lo hay, SHALL nombrar el archivo ausente en vez de morir en el link

### R004: Carga de imágenes en iOS

`iosMain` SHALL declarar un fetcher de red para Coil (`coil-network-ktor3` más un motor Darwin).

#### Scenario: un avatar remoto se pinta

- GIVEN un perfil con `photoUrl` apuntando a Firebase Storage
- WHEN la pantalla de perfiles se renderiza en el simulador
- THEN la imagen SHALL descargarse y mostrarse

#### Scenario: el hueco no vuelve a pasar desapercibido

- GIVEN que la ausencia de fetcher NO produce error de compilación
- WHEN se retire el fetcher de `iosMain`
- THEN la captura de simulador de CI SHALL mostrar el avatar vacío, delatándolo

### R005: CI construye el host completo

`ios-build.yml` SHALL encadenar: link del framework → `xcodegen generate` → `pod install` →
`xcodebuild` del esquema `iosApp` para simulador. Cada paso SHALL fallar ruidosamente.

#### Scenario: cascada verde

- WHEN se dispara el workflow sobre `main`
- THEN los cuatro pasos SHALL terminar en éxito
- AND SHALL publicarse como artefacto el `.app` construido

### R006: Prueba visual desde CI

CI SHALL arrancar un simulador, instalar el `.app`, capturar la pantalla y publicarla como
artefacto. Es la única evidencia visual disponible sin un Mac.

#### Scenario: la captura demuestra que Compose renderiza

- WHEN el workflow termina
- THEN el artefacto SHALL contener un PNG de la pantalla de login
- AND SHALL distinguirse de una pantalla en blanco, que es el fallo típico de integración

## Acceptance Criteria

1. No hay ningún `.pbxproj` versionado; `project.yml` y `Podfile` son legibles en un diff
2. `xcodebuild` del esquema `iosApp` termina en verde en `macos-15`
3. Los tests Native de `shared` se **ejecutan**, no solo compilan
4. El artefacto de CI incluye una captura del simulador con la pantalla de login renderizada
5. Los 897 tests existentes siguen en verde; ninguna aserción se modificó para lograrlo
6. Swift referencia exactamente un símbolo de `shared`: `MainViewController()`
