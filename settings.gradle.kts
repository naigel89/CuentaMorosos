pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
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
    }
}

rootProject.name = "CuentaMorosos"
include(":app")
include(":shared")
