# Guía: Configuración de GitHub Actions para iOS y Android

## Resumen

Este proyecto usa dos workflows de CI/CD:

| Workflow | Runner | Propósito |
|---|---|---|
| `android-build.yml` | Ubuntu | Compila el APK y ejecuta tests en cada push |
| `ios-build.yml` | macOS 14 | Compila el framework `shared` para iOS en cada push |

---

## Paso 1: Obtener los archivos de configuración de Firebase

### Android — `google-services.json`
Ya deberías tener este archivo en `app/google-services.json`.

### iOS — `GoogleService-Info.plist`
1. Ve a [console.firebase.google.com](https://console.firebase.google.com)
2. Abre el proyecto **CuentaMorosos**
3. Haz clic en ⚙️ → **Configuración del proyecto**
4. En **Tus apps**, haz clic en **Añadir app** → icono 🍎 (Apple)
5. **Bundle ID:** `com.cuentamorosos`
6. **Nombre de la app:** `CuentaMorosos`
7. Haz clic en **Registrar app**
8. Descarga `GoogleService-Info.plist`
9. **No lo subas al repositorio.** Se añadirá como Secret de GitHub (ver Paso 2).

---

## Paso 2: Añadir los secretos en GitHub

Los archivos de Firebase contienen claves privadas y **nunca deben estar en el repositorio**. Se almacenan como "Secrets" cifrados en GitHub.

### Cómo codificar los archivos a base64

Abre una terminal en Ubuntu y ejecuta:

```bash
# Para google-services.json (Android)
base64 -w 0 app/google-services.json

# Para GoogleService-Info.plist (iOS) — cuando lo tengas
base64 -w 0 GoogleService-Info.plist
```

Copia el resultado (una cadena larga de texto sin saltos de línea).

### Cómo añadir el Secret en GitHub

1. Ve a tu repositorio en GitHub: `https://github.com/naigel89/CuentaMorosos`
2. Haz clic en **Settings** (pestaña superior)
3. En el menú lateral: **Secrets and variables** → **Actions**
4. Haz clic en **New repository secret**
5. Añade los siguientes secrets:

| Nombre del Secret | Contenido |
|---|---|
| `GOOGLE_SERVICES_JSON` | Salida del comando `base64 -w 0 app/google-services.json` |
| `GOOGLE_SERVICE_INFO_PLIST` | Salida del comando `base64 -w 0 GoogleService-Info.plist` (cuando lo tengas) |

---

## Paso 3: Activar los workflows

Una vez que hagas `git push`, los workflows se activarán automáticamente. También puedes ejecutarlos manualmente:

1. Ve a la pestaña **Actions** de tu repositorio en GitHub
2. Selecciona el workflow que quieras ejecutar
3. Haz clic en **Run workflow** → **Run workflow**

---

## Paso 4: Ver los resultados

Tras cada ejecución podrás:

- ✅ **Ver los logs** de compilación para detectar errores de iOS desde Ubuntu
- 📦 **Descargar los artefactos:**
  - `app-debug.apk` — instalable en Android directamente
  - `shared-framework-ios-simulator` — el framework compilado para iOS (no es el IPA final, es la fase 1)

---

## Fase futura: Generación del IPA para tester iOS

Cuando el framework compile sin errores, el siguiente paso será:

1. Crear un proyecto Xcode básico en `iosApp/` que consuma el framework `shared`
2. Usar **TestFlight** (cuenta Apple Developer, 99 €/año) o **Diawi** (distribución ad-hoc gratuita pero requiere UDID del dispositivo)
3. Añadir al workflow la firma con certificados (almacenados como Secrets en GitHub)

Este paso se documentará en una fase posterior del Sprint 09.
