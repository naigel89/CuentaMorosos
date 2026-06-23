package com.cuentamorosos.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.EventState
import com.cuentamorosos.model.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for encrypted storage and dual-read migration in [CuentaMorososLocalStore].
 *
 * The production code uses [EncryptedSharedPreferences] via [EncryptedPrefsFactory].
 * In Robolectric tests, the factory falls back to plain [SharedPreferences] when
 * the Android Keystore is unavailable — the behavior (save/load/migrate) is identical.
 *
 * Migration logic ([CuentaMorososLocalStore.migrateFromOldStore]) is tested directly
 * to verify the copy-and-clear behavior independently of the encryption layer.
 */
@RunWith(AndroidJUnit4::class)
class CuentaMorososLocalStoreEncryptionTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Clean both old plain store and new store before each test
        context.getSharedPreferences(OLD_PREFS, Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences(NEW_PREFS, Context.MODE_PRIVATE).edit().clear().commit()
    }

    // ── Migration via factory (production path) ───────────────────────────

    @Test
    fun `factory migrates string keys from old plain store`() {
        // GIVEN: seed data in old plain SharedPreferences
        val oldPrefs = context.getSharedPreferences(OLD_PREFS, Context.MODE_PRIVATE)
        oldPrefs.edit()
            .putString("events", EVENTS_JSON)
            .putString("profiles", PROFILES_JSON)
            .putString("debts", DEBTS_JSON)
            .putString("expenses", EXPENSES_JSON)
            .putString("preferences", PREFS_JSON)
            .commit()

        // WHEN: factory creates prefs with migration
        val prefs = EncryptedPrefsFactory.createWithMigration(context)
        val store = CuentaMorososLocalStore(prefs)

        // THEN: all data is accessible
        val events = store.loadEvents()
        assertEquals("Should migrate events", 1, events.size)
        assertEquals("evt-1", events[0].id)
        assertEquals("Test Event", events[0].name)

        val profiles = store.loadProfiles()
        assertEquals("Should migrate profiles", 1, profiles.size)
        assertEquals("prof-1", profiles[0].id)

        val debts = store.loadDebts()
        assertEquals("Should migrate debts", 1, debts.size)
        assertEquals("debt-1", debts[0].id)

        val expenses = store.loadExpenses()
        assertEquals("Should migrate expenses", 1, expenses.size)
        assertEquals("exp-1", expenses[0].id)

        val preferences = store.loadPreferences()
        assertEquals("Should migrate theme", "dark", preferences.themeMode)
        assertEquals("Should migrate reminderDays", 14, preferences.reminderDays)
    }

    @Test
    fun `factory migrates StringSet fingerprints from old plain store`() {
        val oldPrefs = context.getSharedPreferences(OLD_PREFS, Context.MODE_PRIVATE)
        oldPrefs.edit().putStringSet("sent_fingerprints", setOf("$TIMESTAMP|CALCULATION_COMPLETED:evt-1")).commit()

        val prefs = EncryptedPrefsFactory.createWithMigration(context)
        val store = CuentaMorososLocalStore(prefs)

        assertTrue("Fingerprint must be migrated", store.hasNotificationBeenSent("CALCULATION_COMPLETED:evt-1"))
    }

    @Test
    fun `factory migrates Boolean orphan cleanup flag`() {
        val oldPrefs = context.getSharedPreferences(OLD_PREFS, Context.MODE_PRIVATE)
        oldPrefs.edit().putBoolean("orphan_cleanup_done", true).commit()

        val prefs = EncryptedPrefsFactory.createWithMigration(context)
        val store = CuentaMorososLocalStore(prefs)

        assertTrue("Orphan cleanup flag must be migrated", store.isOrphanCleanupDone())
    }

    @Test
    fun `factory clears old plain store after migration`() {
        val oldPrefs = context.getSharedPreferences(OLD_PREFS, Context.MODE_PRIVATE)
        oldPrefs.edit()
            .putString("events", EVENTS_JSON)
            .putString("profiles", PROFILES_JSON)
            .commit()

        EncryptedPrefsFactory.createWithMigration(context)

        val afterPrefs = context.getSharedPreferences(OLD_PREFS, Context.MODE_PRIVATE)
        assertTrue("Old plain store must be empty after migration", afterPrefs.all.isEmpty())
    }

    @Test
    fun `factory is no-op when old store has no data`() {
        // Old store is empty (cleaned in setUp), factory should not throw
        val prefs = EncryptedPrefsFactory.createWithMigration(context)
        val store = CuentaMorososLocalStore(prefs)

        assertTrue("Empty old store should not affect target", store.loadEvents().isEmpty())
    }

    // ── Store roundtrip (Context constructor, fallback path in tests) ─────

    @Test
    fun `clean install saves and loads data correctly`() {
        val store = CuentaMorososLocalStore(context)
        store.clearAll()

        store.saveEvents(
            listOf(
                EventItem(
                    id = "evt-clean", name = "Clean Install", dateMillis = TIMESTAMP,
                    ownerId = "owner-1", state = EventState.OPEN,
                )
            )
        )

        val newStore = CuentaMorososLocalStore(context)
        val loaded = newStore.loadEvents()
        assertEquals("Data must survive recreation", 1, loaded.size)
        assertEquals("evt-clean", loaded[0].id)
    }

    @Test
    fun `store survives roundtrip across recreation`() {
        val store = CuentaMorososLocalStore(context)
        store.clearAll()

        store.saveEvents(
            listOf(
                EventItem(id = "evt-rt", name = "Roundtrip", dateMillis = TIMESTAMP,
                    ownerId = "owner-1", state = EventState.OPEN)
            )
        )
        store.savePreferences(UserPreferences(themeMode = "light", reminderDays = 30, remindersEnabled = false))
        store.markOrphanCleanupDone()
        store.recordNotificationSent("INVITATION_RECEIVED:evt-rt:inv-1")

        val newStore = CuentaMorososLocalStore(context)
        assertEquals("Events must survive restart", 1, newStore.loadEvents().size)
        assertEquals("evt-rt", newStore.loadEvents()[0].id)
        assertEquals("Prefs must survive restart", "light", newStore.loadPreferences().themeMode)
        assertEquals("Reminder days must survive restart", 30, newStore.loadPreferences().reminderDays)
        assertFalse("Reminders must survive restart", newStore.loadPreferences().remindersEnabled)
        assertTrue("Orphan flag must survive restart", newStore.isOrphanCleanupDone())
        assertTrue("Fingerprint must survive restart", newStore.hasNotificationBeenSent("INVITATION_RECEIVED:evt-rt:inv-1"))
    }

    // ── Migration idempotency ─────────────────────────────────────────────

    @Test
    fun `migration is idempotent via factory`() {
        val oldPrefs = context.getSharedPreferences(OLD_PREFS, Context.MODE_PRIVATE)
        oldPrefs.edit()
            .putString("events", EVENTS_JSON)
            .putStringSet("sent_fingerprints", setOf("$TIMESTAMP|CALCULATION_COMPLETED:evt-1"))
            .commit()

        // First migration
        val prefs1 = EncryptedPrefsFactory.createWithMigration(context)
        val store1 = CuentaMorososLocalStore(prefs1)
        assertEquals(1, store1.loadEvents().size)

        // Second migration (old store already cleared)
        val prefs2 = EncryptedPrefsFactory.createWithMigration(context)
        val store2 = CuentaMorososLocalStore(prefs2)
        assertEquals("Second migration must not duplicate", 1, store2.loadEvents().size)
        assertEquals("evt-1", store2.loadEvents()[0].id)
        assertTrue(store2.hasNotificationBeenSent("CALCULATION_COMPLETED:evt-1"))
    }

    // ── clearAll ──────────────────────────────────────────────────────────

    @Test
    fun `clearAll empties the store`() {
        val store = CuentaMorososLocalStore(context)
        store.clearAll()
        store.saveEvents(
            listOf(EventItem(id = "evt-del", name = "To Delete", dateMillis = TIMESTAMP,
                ownerId = "owner-1", state = EventState.OPEN))
        )
        assertTrue("Events must exist before clearAll", store.loadEvents().isNotEmpty())

        store.clearAll()
        assertTrue("Events must be empty after clearAll", store.loadEvents().isEmpty())
        assertTrue("Profiles must be empty after clearAll", store.loadProfiles().isEmpty())
    }

    // ── Migration: empty Strings and edge cases ──────────────────────────

    @Test
    fun `migration handles empty StringSet without error`() {
        val oldPrefs = context.getSharedPreferences(OLD_PREFS, Context.MODE_PRIVATE)
        oldPrefs.edit().putStringSet("sent_fingerprints", emptySet<String>()).commit()
        oldPrefs.edit().putString("events", EVENTS_JSON).commit()

        val prefs = EncryptedPrefsFactory.createWithMigration(context)
        val store = CuentaMorososLocalStore(prefs)
        assertEquals("Events should still migrate", 1, store.loadEvents().size)
    }

    @Test
    fun `migration handles false orphan cleanup flag`() {
        val oldPrefs = context.getSharedPreferences(OLD_PREFS, Context.MODE_PRIVATE)
        oldPrefs.edit().putBoolean("orphan_cleanup_done", false).commit()
        oldPrefs.edit().putString("events", EVENTS_JSON).commit()

        val prefs = EncryptedPrefsFactory.createWithMigration(context)
        val store = CuentaMorososLocalStore(prefs)
        assertFalse("False flag must be preserved", store.isOrphanCleanupDone())
        assertEquals("Events should still migrate", 1, store.loadEvents().size)
    }

    // ── Negative: empty store behavior ────────────────────────────────────

    @Test
    fun `loadEvents returns empty when no data exists`() {
        val store = CuentaMorososLocalStore(context)
        store.clearAll()
        assertTrue("Empty store must return empty list", store.loadEvents().isEmpty())
    }

    @Test
    fun `loadPreferences returns defaults when no data exists`() {
        val store = CuentaMorososLocalStore(context)
        store.clearAll()
        val prefs = store.loadPreferences()
        assertEquals("system", prefs.themeMode)
        assertEquals(7, prefs.reminderDays)
    }

    // ── Backward compatibility: clean upgrade from old store ──────────────

    @Test
    fun `full upgrade path preserves all entity types`() {
        // GIVEN: an old version stored all data types in plain prefs
        val oldPrefs = context.getSharedPreferences(OLD_PREFS, Context.MODE_PRIVATE)
        oldPrefs.edit()
            .putString("events", EVENTS_JSON)
            .putString("profiles", PROFILES_JSON)
            .putString("debts", DEBTS_JSON)
            .putString("expenses", EXPENSES_JSON)
            .putString("preferences", PREFS_JSON)
            .putStringSet("sent_fingerprints", setOf("$TIMESTAMP|CALCULATION_COMPLETED:evt-1"))
            .putBoolean("orphan_cleanup_done", false)
            .commit()

        // WHEN: the hardened version creates the store (migration runs)
        val store = CuentaMorososLocalStore(context)

        // THEN: all data is accessible and old store is cleared
        assertEquals(1, store.loadEvents().size)
        assertEquals(1, store.loadProfiles().size)
        assertEquals(1, store.loadDebts().size)
        assertEquals(1, store.loadExpenses().size)
        assertEquals("dark", store.loadPreferences().themeMode)
        assertTrue(store.hasNotificationBeenSent("CALCULATION_COMPLETED:evt-1"))
        assertFalse(store.isOrphanCleanupDone())

        // Old plain store must be cleared
        assertTrue(context.getSharedPreferences(OLD_PREFS, Context.MODE_PRIVATE).all.isEmpty())
    }

    private companion object {
        const val OLD_PREFS = "cuenta_morosos_store"
        const val NEW_PREFS = "cuenta_morosos_store_encrypted"
        const val TIMESTAMP = 1719000000000L

        const val EVENTS_JSON = """[{"id":"evt-1","name":"Test Event","dateMillis":$TIMESTAMP,"ownerId":"owner-1"}]"""
        const val PROFILES_JSON = """[{"id":"prof-1","name":"Test Profile","totalPendingEuros":0.0,"isGhost":false,"linkedEmail":"","ownerId":"owner-1"}]"""
        const val DEBTS_JSON = """[{"id":"debt-1","eventId":"evt-1","profileId":"prof-1","amountEuros":50.0}]"""
        const val EXPENSES_JSON = """[{"id":"exp-1","eventId":"evt-1","name":"Test Expense","amountEuros":100.0}]"""
        const val PREFS_JSON = """{"themeMode":"dark","reminderDays":14,"remindersEnabled":true}"""
    }
}
