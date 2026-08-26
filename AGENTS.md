# Agent Guidelines — CuentaMorosos

## Qué es

App Kotlin Multiplatform para repartir gastos y deudas de eventos entre varias
personas. Android está en producción; iOS está en curso.

## Dónde vive cada cosa

Dos módulos. `app/` es un shell Android delgado (entry point, SDK nativo de
Firebase, WorkManager, permisos, deep links). **Todo lo demás está en `shared/`,
incluida la UI Compose**: si vas a tocar una pantalla, el archivo está en
`shared/src/commonMain`, no en `app/`.

- `shared/src/commonMain/kotlin/com/cuentamorosos/ui` — pantallas y componentes
- `shared/src/commonMain/kotlin/com/cuentamorosos/model` — motores de cálculo puros
- `shared/src/commonMain/kotlin/com/cuentamorosos/data/repository` — repositorios
- `shared/src/commonMain/kotlin/com/cuentamorosos/notifications` — reglas de notificación
- `shared/src/iosMain` — implementaciones iOS de los puertos
- `iosApp/` — host iOS (proyecto Xcode generado, no versionado)

## Cosas que sorprenden

- **La persistencia principal es SQLDelight**, no SharedPreferences. La UI
  consume siempre los repositorios `OfflineFirst*`, que escriben primero en local
  y luego intentan el remoto. `CuentaMorososLocalStore` solo queda para migrar
  datos legacy y deduplicar notificaciones.
- **`derivedStateOf`**: con el delegado `by` **no** accedas a `.value`.
- **Smart casts**: las propiedades con getter propio (las delegadas por
  `remember`) no admiten smart cast. Asígnalas a una local antes de comprobar
  `null`.
- **Nombres de test**: `commonTest` también se compila para Kotlin/Native, que
  rechaza caracteres como la coma dentro de nombres entre backticks. Un nombre
  legal en JVM puede romper el build de iOS.
- **En Linux y Windows los targets iOS no existen**: `shared/build.gradle.kts`
  los declara solo si el host es macOS. No fallan, simplemente no están.

## Comandos

```bash
./gradlew :app:assembleDebug                 # APK debug (lo que corre CI)
./gradlew :shared:jvmTest                    # tests del módulo shared — lo más rápido
./gradlew :app:testDebugUnitTest --continue  # tests del módulo app
./gradlew :shared:compileKotlinJvm           # proxy barato: detecta fugas de Android en commonMain
```

No hay linter ni formatter: la única puerta de calidad es el compilador de Kotlin
y los tests.

Para el detalle de arquitectura, permisos, sistema de diseño y flujo OpenSpec,
lee [`CLAUDE.md`](CLAUDE.md).
