plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
    id("app.cash.sqldelight")
}

kotlin {
    jvmToolchain(17)
    jvm()

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

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

            // Coil — KMP image loading
            implementation("io.coil-kt.coil3:coil:3.0.4")
            implementation("io.coil-kt.coil3:coil-compose:3.0.4")

            // SQLDelight runtime (KMP)
            implementation("app.cash.sqldelight:coroutines-extensions:2.0.2")
        }
        androidMain.dependencies {
            implementation(compose.preview)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
            implementation("androidx.activity:activity-compose:1.9.1")
            // SQLDelight Android driver
            implementation("app.cash.sqldelight:android-driver:2.0.2")
            // Coil network fetcher for loading images from URLs
            implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.4")
        }
        jvmMain.dependencies {
            // SQLDelight JVM (SQLite) driver
            implementation("app.cash.sqldelight:sqlite-driver:2.0.2")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("app.cash.sqldelight:sqlite-driver:2.0.2")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
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
                    dependencies {
                        // SQLDelight iOS driver
                        implementation("app.cash.sqldelight:native-driver:2.0.2")
                    }
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

sqldelight {
    databases {
        create("CuentaMorososDatabase") {
            packageName.set("com.cuentamorosos.db")
        }
    }
}
