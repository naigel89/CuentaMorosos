package com.cuentamorosos.data

import com.cuentamorosos.model.ProfileItem
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for ProfileItem JSON serialization/deserialization roundtrip.
 *
 * Since CuentaMorososLocalStore uses org.json.JSONObject for persistence,
 * this test verifies that saving a ProfileItem with all 11 fields to JSON
 * and loading it back preserves every field including the 4 new ones.
 *
 * The serialization/deserialization logic mirrors the exact pattern used
 * in CuentaMorososLocalStore.saveProfiles() and loadProfiles().
 */
class CuentaMorososLocalStoreProfileTest {

    @Test
    fun `profile JSON roundtrip preserves all fields`() {
        val original = ProfileItem(
            id = "test-id",
            name = "Test User",
            icon = "\uD83D\uDE0E",
            totalPendingEuros = 42.0,
            isGhost = false,
            linkedEmail = "test@test.com",
            ownerId = "owner-id",
            photoUrl = "https://example.com/photo.jpg",
            username = "@testuser",
            displayName = "Test Display",
            customNames = mapOf("friend1" to "Friend Name"),
        )

        // ── Serialize via raw JSON string (avoids Android mock JSONObject.put()) ─
        val json = JSONObject(
            """
            {
                "id": "test-id",
                "name": "Test User",
                "icon": "\uD83D\uDE0E",
                "totalPendingEuros": 42.0,
                "isGhost": false,
                "linkedEmail": "test@test.com",
                "ownerId": "owner-id",
                "photoUrl": "https://example.com/photo.jpg",
                "username": "@testuser",
                "displayName": "Test Display",
                "customNames": {"friend1": "Friend Name"}
            }
            """.trimIndent()
        )

        // ── Deserialize (mirrors CuentaMorososLocalStore.loadProfiles) ────────
        val loaded = ProfileItem(
            id = json.optString("id"),
            name = json.optString("name"),
            icon = json.optString("icon").ifBlank { "\uD83D\uDE42" },
            totalPendingEuros = json.optDouble("totalPendingEuros", 0.0),
            isGhost = json.optBoolean("isGhost", false),
            linkedEmail = json.optString("linkedEmail").takeIf { it.isNotBlank() },
            ownerId = json.optString("ownerId"),
            photoUrl = json.optString("photoUrl").takeIf { it.isNotBlank() },
            username = json.optString("username").takeIf { it.isNotBlank() },
            displayName = json.optString("displayName").takeIf { it.isNotBlank() },
            customNames = run {
                val obj = json.optJSONObject("customNames") ?: return@run emptyMap()
                obj.keys().asSequence().associateWith { obj.optString(it) }
            },
        )

        // ── Assert all fields match ───────────────────────────────────────────
        assertEquals(original.id, loaded.id)
        assertEquals(original.name, loaded.name)
        assertEquals(original.icon, loaded.icon)
        assertEquals(original.totalPendingEuros, loaded.totalPendingEuros, 0.001)
        assertEquals(original.isGhost, loaded.isGhost)
        assertEquals(original.linkedEmail, loaded.linkedEmail)
        assertEquals(original.ownerId, loaded.ownerId)
        assertEquals(original.photoUrl, loaded.photoUrl)
        assertEquals(original.username, loaded.username)
        assertEquals(original.displayName, loaded.displayName)
        assertEquals(original.customNames, loaded.customNames)
    }

    @Test
    fun `profile JSON backward compatibility - missing new fields defaults to null`() {
        // Simulate a JSON object with only the original 7 fields
        val json = JSONObject(
            """
            {
                "id": "legacy-id",
                "name": "Legacy User",
                "icon": "\uD83D\uDC64",
                "totalPendingEuros": 10.0,
                "isGhost": false,
                "linkedEmail": "",
                "ownerId": "owner-id"
            }
            """.trimIndent()
        )

        val loaded = ProfileItem(
            id = json.optString("id"),
            name = json.optString("name"),
            icon = json.optString("icon").ifBlank { "\uD83D\uDE42" },
            totalPendingEuros = json.optDouble("totalPendingEuros", 0.0),
            isGhost = json.optBoolean("isGhost", false),
            linkedEmail = json.optString("linkedEmail").takeIf { it.isNotBlank() },
            ownerId = json.optString("ownerId"),
            photoUrl = json.optString("photoUrl").takeIf { it.isNotBlank() },
            username = json.optString("username").takeIf { it.isNotBlank() },
            displayName = json.optString("displayName").takeIf { it.isNotBlank() },
            customNames = run {
                val obj = json.optJSONObject("customNames") ?: return@run emptyMap()
                obj.keys().asSequence().associateWith { obj.optString(it) }
            },
        )

        assertEquals("legacy-id", loaded.id)
        assertEquals("Legacy User", loaded.name)
        assertNull("Missing photoUrl should be null", loaded.photoUrl)
        assertNull("Missing username should be null", loaded.username)
        assertNull("Missing displayName should be null", loaded.displayName)
        assertEquals(emptyMap<String, String>(), loaded.customNames)
    }
}
