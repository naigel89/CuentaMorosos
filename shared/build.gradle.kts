plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    // iOS targets are only configured when running on macOS (requires Xcode / Kotlin/Native)
    val isMac = System.getProperty("os.name").lowercase().contains("mac")
    if (isMac) {
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
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.components.resources)

            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
            implementation("dev.gitlive:firebase-auth:1.13.0")
            implementation("dev.gitlive:firebase-firestore:1.13.0")

            // Lifecycle / ViewModel KMP (no AndroidX dependency)
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel:2.8.0")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.8.0")
        }
        androidMain.dependencies {
            implementation(compose.preview)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
            implementation("androidx.activity:activity-compose:1.9.1")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        if (isMac) {
            val iosX64Main = findByName("iosX64Main")
            val iosArm64Main = findByName("iosArm64Main")
            val iosSimulatorArm64Main = findByName("iosSimulatorArm64Main")
            if (iosX64Main != null && iosArm64Main != null && iosSimulatorArm64Main != null) {
                val iosMain by creating {
                    dependsOn(commonMain.get())
                    iosX64Main.dependsOn(this)
                    iosArm64Main.dependsOn(this)
                    iosSimulatorArm64Main.dependsOn(this)
                }
            }
        }
    }
}

android {
    namespace = "com.cuentamorosos.shared"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}
