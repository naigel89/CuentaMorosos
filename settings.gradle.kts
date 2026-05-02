pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        // Compose Multiplatform plugin
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

dependencyResolutionManagement {
    // PREFER_SETTINGS is required for Kotlin Multiplatform: the KMP plugin adds an ivy repository
    // for Kotlin/Native prebuilts at the project level. PREFER_SETTINGS allows it without error.
    // kotlin.native.distribution.downloadFromMaven=true (gradle.properties) redirects
    // Kotlin/Native downloads to Maven Central so no ivy repo is needed here.
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        // Compose Multiplatform runtime artifacts
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

rootProject.name = "CuentaMorosos"
include(":app")
include(":shared")
