package com.cuentamorosos.security

import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * Verifies proguard-rules.pro requirements (code-obfuscation R002):
 * - Keep rules for Firebase SDKs (Auth, Firestore, Storage, Messaging)
 * - Keep rules for Kotlin coroutines and serialization
 * - Keep rules for Jetpack Compose
 * - Keep rules for SQLDelight generated code
 * - Keep rules for gitlive-firebase KMP SDK
 */
class ProguardRulesTest {

    private val proguardFile = File("proguard-rules.pro")

    @Test
    fun `proguard file exists and has substantive keep rules`() {
        assertTrue("proguard-rules.pro must exist", proguardFile.exists())
        val content = proguardFile.readText().trim()
        assertTrue("proguard-rules.pro must not be empty", content.isNotEmpty())

        // Must have real keep rules, not just placeholder comment
        val substantiveLines = content.lines().filter { line ->
            val trimmed = line.trim()
            trimmed.isNotEmpty() && !trimmed.startsWith("#")
        }
        assertTrue(
            "proguard-rules.pro must contain actual keep rules, not just comments. Found ${substantiveLines.size} substantive lines",
            substantiveLines.isNotEmpty()
        )
    }

    @Test
    fun `keeps Firebase Auth classes`() {
        val content = proguardFile.readText()
        assertTrue(
            "Must keep Firebase Auth classes to prevent ClassNotFoundException in release",
            content.contains("firebase.auth") || content.contains("firebase-auth")
        )
    }

    @Test
    fun `keeps Firebase Firestore classes`() {
        val content = proguardFile.readText()
        assertTrue(
            "Must keep Firebase Firestore classes for serialization in release",
            content.contains("firebase.firestore") || content.contains("firestore")
        )
    }

    @Test
    fun `keeps Firebase Storage classes`() {
        val content = proguardFile.readText()
        assertTrue(
            "Must keep Firebase Storage classes",
            content.contains("firebase.storage")
        )
    }

    @Test
    fun `keeps Firebase Messaging classes`() {
        val content = proguardFile.readText()
        assertTrue(
            "Must keep Firebase Messaging classes for push notifications in release",
            content.contains("firebase.messaging")
        )
    }

    @Test
    fun `keeps Kotlin coroutines internal classes`() {
        val content = proguardFile.readText()
        assertTrue(
            "Must keep kotlinx.coroutines internal classes (MainDispatcherFactory, etc.)",
            content.contains("kotlinx.coroutines")
        )
    }

    @Test
    fun `keeps Kotlin serialization classes`() {
        val content = proguardFile.readText()
        assertTrue(
            "Must keep kotlinx.serialization classes for Firestore data mapping",
            content.contains("kotlinx.serialization")
        )
    }

    @Test
    fun `keeps Jetpack Compose classes`() {
        val content = proguardFile.readText()
        assertTrue(
            "Must keep androidx.compose classes to prevent recomposition crashes in release",
            content.contains("androidx.compose")
        )
    }

    @Test
    fun `keeps SQLDelight generated code`() {
        val content = proguardFile.readText()
        assertTrue(
            "Must keep SQLDelight generated query classes for local database in release",
            content.contains("sqldelight", ignoreCase = true) ||
                content.contains("com.cuentamorosos.db")
        )
    }

    @Test
    fun `keeps gitlive-firebase KMP SDK classes`() {
        val content = proguardFile.readText()
        assertTrue(
            "Must keep dev.gitlive.firebase classes for KMP Firebase integration",
            content.contains("gitlive.firebase") || content.contains("dev.gitlive")
        )
    }
}
