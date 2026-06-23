package com.cuentamorosos.security

import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * Verifies build script security requirements:
 * - R001: No hardcoded keystore passwords in build.gradle.kts
 * - R001: isMinifyEnabled = true for release
 */
class BuildSecurityTest {

    private val buildFile = File("build.gradle.kts")

    @Test
    fun `build file contains no hardcoded keystore passwords`() {
        assertTrue("build.gradle.kts must exist", buildFile.exists())
        val content = buildFile.readText()

        // Known hardcoded password from the current build file
        assertFalse(
            "Hardcoded password 'llevalatarara' must NOT appear in build.gradle.kts",
            content.contains("llevalatarara")
        )
    }

    @Test
    fun `build file reads keystore secrets from local properties`() {
        val content = buildFile.readText()
        // Must reference local.properties or System.getenv for credential sourcing
        val readsFromProperties = content.contains("local.properties") ||
            content.contains("localProperties")
        val readsFromEnv = content.contains("System.getenv") ||
            content.contains("getRequiredProperty")

        assertTrue(
            "build.gradle.kts must read credentials from local.properties or environment variables",
            readsFromProperties || readsFromEnv
        )
    }

    @Test
    fun `release build has minification enabled`() {
        val content = buildFile.readText()
        // Find the release block and verify isMinifyEnabled
        assertTrue(
            "isMinifyEnabled must be true for release build type",
            content.contains(Regex("isMinifyEnabled\\s*=\\s*true"))
        )
    }

    @Test
    fun `security crypto dependency is declared`() {
        val content = buildFile.readText()
        assertTrue(
            "androidx.security:security-crypto dependency must be declared",
            content.contains("security-crypto")
        )
    }
}
