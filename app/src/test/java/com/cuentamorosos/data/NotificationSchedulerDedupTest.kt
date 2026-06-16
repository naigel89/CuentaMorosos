package com.cuentamorosos.data

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowNotificationManager

@RunWith(AndroidJUnit4::class)
class NotificationSchedulerDedupTest {

    private lateinit var context: Context
    private lateinit var store: CuentaMorososLocalStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        store = CuentaMorososLocalStore(context)
        store.clearAll()
    }

    private fun shadowManager(): ShadowNotificationManager {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return org.robolectric.Shadows.shadowOf(manager)
    }

    // ── Helper: create ReminderMessage objects with eventId for testing ──

    private fun reminderMsg(
        eventId: String,
        dateFormatted: String = "01/01/2026",
        type: ReminderType = ReminderType.PENDING_DEBT,
    ) = ReminderMessage(
        title = "Test $eventId",
        body = "Body for $eventId",
        type = type,
        eventId = eventId,
        daysUntil = 3,
        dateFormatted = dateFormatted,
    )

    // ── Fingerprint format: "reminder:{eventId}:{dateFormatted}:{type}" ──

    @Test
    fun `fingerprint format is stable and deterministic`() {
        val fingerprint1 = NotificationScheduler.fingerprintFor(reminderMsg("evt-1", "01/01/2026", ReminderType.PENDING_DEBT))
        val fingerprint2 = NotificationScheduler.fingerprintFor(reminderMsg("evt-1", "01/01/2026", ReminderType.PENDING_DEBT))

        assertEquals(
            "Same eventId + date + type must produce same fingerprint",
            fingerprint1, fingerprint2
        )
        assertEquals(
            "reminder:evt-1:01/01/2026:PENDING_DEBT",
            fingerprint1
        )
    }

    @Test
    fun `different eventIds produce different fingerprints`() {
        val fp1 = NotificationScheduler.fingerprintFor(reminderMsg("evt-1"))
        val fp2 = NotificationScheduler.fingerprintFor(reminderMsg("evt-2"))

        assertTrue(
            "Different eventIds must produce different fingerprints",
            fp1 != fp2
        )
    }

    @Test
    fun `different types for same event produce different fingerprints`() {
        val fp1 = NotificationScheduler.fingerprintFor(reminderMsg("evt-1", type = ReminderType.PENDING_DEBT))
        val fp2 = NotificationScheduler.fingerprintFor(reminderMsg("evt-1", type = ReminderType.INCOMPLETE_EVENT))

        assertTrue(
            "Different types for same event must produce different fingerprints",
            fp1 != fp2
        )
    }

    @Test
    fun `fingerprint with null eventId and null dateFormatted uses empty strings`() {
        val msg = ReminderMessage(
            title = "No ID", body = "No ID body",
            type = ReminderType.PENDING_DEBT,
            eventId = null, daysUntil = null, dateFormatted = null,
        )
        val fingerprint = NotificationScheduler.fingerprintFor(msg)

        assertEquals(
            "reminder:::PENDING_DEBT",
            fingerprint
        )
    }

    // ── Filtering: mixed list ──────────────────────────────────────────

    @Test
    fun `mixed list filters sent and posts only unsent`() {
        val messages = listOf(
            reminderMsg("evt-1", type = ReminderType.UPCOMING_EVENT),
            reminderMsg("evt-2", type = ReminderType.UPCOMING_EVENT),
            reminderMsg("evt-3", type = ReminderType.UPCOMING_EVENT),
        )

        // Pre-register evt-2 as already sent
        store.recordNotificationSent(
            NotificationScheduler.fingerprintFor(reminderMsg("evt-2", type = ReminderType.UPCOMING_EVENT))
        )

        val unsent = messages.filter { msg ->
            !store.hasNotificationBeenSent(NotificationScheduler.fingerprintFor(msg))
        }

        assertEquals(
            "Only unsent reminders should pass through the filter",
            2, unsent.size
        )
        assertEquals("evt-1", unsent[0].eventId)
        assertEquals("evt-3", unsent[1].eventId)
    }

    @Test
    fun `all already sent produces zero to post`() {
        val messages = listOf(
            reminderMsg("evt-1"),
            reminderMsg("evt-2"),
        )

        // Pre-register all as already sent
        messages.forEach { msg ->
            store.recordNotificationSent(NotificationScheduler.fingerprintFor(msg))
        }

        val unsent = messages.filter { msg ->
            !store.hasNotificationBeenSent(NotificationScheduler.fingerprintFor(msg))
        }

        assertTrue(
            "All already sent must produce empty unsent list",
            unsent.isEmpty()
        )
    }

    @Test
    fun `none sent produces all to post`() {
        val messages = listOf(
            reminderMsg("evt-1"),
            reminderMsg("evt-2"),
            reminderMsg("evt-3"),
        )

        val unsent = messages.filter { msg ->
            !store.hasNotificationBeenSent(NotificationScheduler.fingerprintFor(msg))
        }

        assertEquals(
            "All unsent reminders must pass through",
            3, unsent.size
        )
    }

    // ── Record after post ─────────────────────────────────────────────

    @Test
    fun `record after post marks fingerprint as sent`() {
        val msg = reminderMsg("evt-record")

        // Initial: not sent
        val fp = NotificationScheduler.fingerprintFor(msg)
        assertFalse("Fingerprint must NOT be registered before post", store.hasNotificationBeenSent(fp))

        // Simulate post + record
        store.recordNotificationSent(fp)

        // After: fingerprint is registered
        assertTrue("Fingerprint must be registered after record", store.hasNotificationBeenSent(fp))
    }

    @Test
    fun `record is idempotent - recording same fingerprint twice does not throw`() {
        val fp = NotificationScheduler.fingerprintFor(reminderMsg("evt-idempotent"))
        store.recordNotificationSent(fp)
        store.recordNotificationSent(fp) // second time: should no-op safely
        assertTrue(store.hasNotificationBeenSent(fp))
    }

    // ── Dispatch integration: postReminders must filter via store ─────

    @Test
    fun `postReminders with store dispatches only unsent reminders`() {
        val messages = listOf(
            reminderMsg("evt-a", type = ReminderType.UPCOMING_EVENT),
            reminderMsg("evt-b", type = ReminderType.UPCOMING_EVENT),
            reminderMsg("evt-c", type = ReminderType.UPCOMING_EVENT),
        )

        // Pre-register evt-b as sent
        store.recordNotificationSent(
            NotificationScheduler.fingerprintFor(reminderMsg("evt-b", type = ReminderType.UPCOMING_EVENT))
        )

        // Call postReminders with store
        NotificationScheduler.postReminders(context, messages, store)

        // Only 2 notifications should be posted (evt-a and evt-c, NOT evt-b)
        assertEquals(
            "Only unsent reminders must be posted",
            2, shadowManager().size()
        )
    }

    @Test
    fun `postReminders with store and all sent posts zero notifications`() {
        val messages = listOf(
            reminderMsg("evt-1"),
            reminderMsg("evt-2"),
        )

        messages.forEach { msg ->
            store.recordNotificationSent(NotificationScheduler.fingerprintFor(msg))
        }

        val beforeCount = shadowManager().size()

        NotificationScheduler.postReminders(context, messages, store)

        assertEquals(
            "No notifications must be posted when all are already sent",
            beforeCount, shadowManager().size()
        )
    }

    @Test
    fun `postReminders records fingerprint after each successful post`() {
        val messages = listOf(
            reminderMsg("evt-x", type = ReminderType.UPCOMING_EVENT),
            reminderMsg("evt-y", type = ReminderType.UPCOMING_EVENT),
        )

        // Both should be unsent initially
        messages.forEach { msg ->
            assertFalse(store.hasNotificationBeenSent(NotificationScheduler.fingerprintFor(msg)))
        }

        NotificationScheduler.postReminders(context, messages, store)

        // Both must be recorded after posting
        messages.forEach { msg ->
            assertTrue(
                "Fingerprint for ${msg.eventId} must be recorded after post",
                store.hasNotificationBeenSent(NotificationScheduler.fingerprintFor(msg))
            )
        }
    }

    @Test
    fun `postReminders without store works for backward compat`() {
        val messages = listOf(
            reminderMsg("evt-bwd-1", type = ReminderType.UPCOMING_EVENT),
            reminderMsg("evt-bwd-2", type = ReminderType.UPCOMING_EVENT),
        )

        // No store → all should post (no dedup)
        NotificationScheduler.postReminders(context, messages)

        assertEquals(
            "Without store, all reminders must be posted (backward compat)",
            2, shadowManager().size()
        )
    }
}
