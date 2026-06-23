package com.cuentamorosos.security

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Verifies AndroidManifest.xml security requirements:
 * - data-at-rest R003: allowBackup="false"
 * - network-security R001: networkSecurityConfig declared
 */
class ManifestSecurityTest {

    private val manifestFile = File("src/main/AndroidManifest.xml")

    @Test
    fun `manifest disables Android backup`() {
        assertTrue("AndroidManifest.xml must exist", manifestFile.exists())

        val doc = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(manifestFile)

        val appElements = doc.getElementsByTagName("application")
        assertTrue("application element must exist", appElements.length > 0)

        val appElement = appElements.item(0)
        val allowBackup = appElement.attributes.getNamedItem("android:allowBackup")

        assertNotNull("allowBackup attribute must exist on application element", allowBackup)
        assertEquals(
            "allowBackup must be false to prevent unencrypted data extraction via ADB backup",
            "false",
            allowBackup.nodeValue
        )
    }

    @Test
    fun `manifest declares network security config`() {
        val doc = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(manifestFile)

        val appElements = doc.getElementsByTagName("application")
        val appElement = appElements.item(0)
        val networkConfig = appElement.attributes.getNamedItem("android:networkSecurityConfig")

        assertNotNull(
            "networkSecurityConfig attribute must exist for certificate pinning",
            networkConfig
        )
        assertEquals(
            "networkSecurityConfig must reference the XML network security config",
            "@xml/network_security_config",
            networkConfig.nodeValue
        )
    }
}
