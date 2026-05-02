pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // PREFER_SETTINGS is required for Kotlin Multiplatform: the KMP plugin adds an ivy repository
    // for Kotlin/Native prebuilts (download.jetbrains.com) at the project level, which would be
    // rejected by FAIL_ON_PROJECT_REPOS. Settings repositories still take precedence.
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        // Kotlin/Native prebuilt toolchain (used by KMP iOS targets on macOS runners)
        // URL pattern: https://download.jetbrains.com/kotlin/native/builds/releases/1.9.24/macos-aarch64/kotlin-native-prebuilt-macos-aarch64-1.9.24.tar.gz
        ivy {
            url = uri("https://download.jetbrains.com/kotlin/native/builds")
            patternLayout {
                artifact("releases/[revision]/[classifier]/[artifact]-[revision].[ext]")
            }
            metadataSources { artifact() }
            content { includeGroup("kotlin.native.prebuilt") }
        }
    }
}

rootProject.name = "CuentaMorosos"
include(":app")
include(":shared")
