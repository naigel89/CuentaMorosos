pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // PREFER_SETTINGS is required for Kotlin Multiplatform: the KMP plugin adds an ivy repository
    // for Kotlin/Native prebuilts at the project level. PREFER_SETTINGS allows it to be added
    // but settings repositories take precedence. We declare the ivy repo here so it works.
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        // Kotlin/Native prebuilt toolchain (used by KMP iOS targets on macOS runners).
        // The KMP plugin resolves: :kotlin-native-prebuilt-macos-aarch64:1.9.24 (empty group)
        // URL: https://download.jetbrains.com/kotlin/native/builds/releases/1.9.24/macos-aarch64/kotlin-native-prebuilt-macos-aarch64-1.9.24.tar.gz
        ivy {
            url = uri("https://download.jetbrains.com/kotlin/native/builds")
            patternLayout {
                artifact("releases/[revision]/[classifier]/[artifact]-[revision].[ext]")
            }
            metadataSources { artifact() }
        }
    }
}

rootProject.name = "CuentaMorosos"
include(":app")
include(":shared")
