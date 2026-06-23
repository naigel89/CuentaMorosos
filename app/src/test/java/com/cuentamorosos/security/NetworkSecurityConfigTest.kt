package com.cuentamorosos.security

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Verifies network_security_config.xml requirements:
 * - network R001: certificate pinning for Firebase domains
 * - network R002: at least 2 backup pins per domain
 * - network R003: cleartextTrafficPermitted="false"
 */
class NetworkSecurityConfigTest {

    private val configFile = File("src/main/res/xml/network_security_config.xml")

    @Test
    fun `network security config file exists`() {
        assertTrue(
            "network_security_config.xml must exist for certificate pinning",
            configFile.exists()
        )
    }

    @Test
    fun `base config disables cleartext traffic`() {
        val doc = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(configFile)

        val baseConfigs = doc.getElementsByTagName("base-config")
        assertTrue("At least one base-config element must exist", baseConfigs.length > 0)

        val cleartext = baseConfigs.item(0).attributes
            .getNamedItem("cleartextTrafficPermitted")
        assertNotNull(
            "cleartextTrafficPermitted must be explicitly declared on base-config",
            cleartext
        )
        assertEquals(
            "cleartextTrafficPermitted must be false — HTTP requests must be blocked",
            "false",
            cleartext.nodeValue
        )
    }

    @Test
    fun `domain config pins googleapis com with backup pins and expiration`() {
        val doc = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(configFile)

        val domainConfigs = doc.getElementsByTagName("domain-config")
        val googleapisConfig = findDomainConfig(domainConfigs, "googleapis.com")

        assertNotNull(
            "Must have a domain-config for *.googleapis.com (Firebase/Firestore API domain)",
            googleapisConfig
        )

        // Verify pin-set exists
        val pinSets = googleapisConfig!!.getElementsByTagName("pin-set")
        assertTrue("googleapis.com domain config must contain a pin-set", pinSets.length > 0)

        val pinSet = pinSets.item(0)
        val pins = pinSet.childNodes
        var pinCount = 0
        for (i in 0 until pins.length) {
            if (pins.item(i).nodeName == "pin") pinCount++
        }

        assertTrue(
            "googleapis.com must have at least 2 pins (primary + backup from different CA), found $pinCount",
            pinCount >= 2
        )

        // Verify expiration is set (90-day expiry per design)
        val expiration = pinSet.attributes.getNamedItem("expiration")
        assertNotNull("pin-set must have an expiration date (90-day policy)", expiration)
        assertTrue("expiration date must not be empty", expiration.nodeValue.isNotEmpty())
    }

    @Test
    fun `domain config pins firebaseio com with backup pins`() {
        val doc = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(configFile)

        val domainConfigs = doc.getElementsByTagName("domain-config")
        val firebaseioConfig = findDomainConfig(domainConfigs, "firebaseio.com")

        assertNotNull(
            "Must have a domain-config for *.firebaseio.com (Firebase Realtime Database domain)",
            firebaseioConfig
        )

        val pinSets = firebaseioConfig!!.getElementsByTagName("pin-set")
        assertTrue("firebaseio.com domain config must contain a pin-set", pinSets.length > 0)

        val pinSet = pinSets.item(0)
        val pins = pinSet.childNodes
        var pinCount = 0
        for (i in 0 until pins.length) {
            if (pins.item(i).nodeName == "pin") pinCount++
        }

        assertTrue(
            "firebaseio.com must have at least 2 pins (primary + backup), found $pinCount",
            pinCount >= 2
        )
    }

    private fun findDomainConfig(
        domainConfigs: org.w3c.dom.NodeList,
        domainName: String
    ): org.w3c.dom.Element? {
        for (i in 0 until domainConfigs.length) {
            val dc = domainConfigs.item(i) as? org.w3c.dom.Element ?: continue
            val domains = dc.getElementsByTagName("domain")
            for (j in 0 until domains.length) {
                val domainNode = domains.item(j)
                if (domainNode.textContent.contains(domainName)) {
                    return dc
                }
            }
        }
        return null
    }
}
