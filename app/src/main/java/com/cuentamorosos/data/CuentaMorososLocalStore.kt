package com.cuentamorosos.data

import android.content.Context
import android.content.SharedPreferences
import com.cuentamorosos.model.EventDebtItem
import com.cuentamorosos.model.EventExpenseItem
import com.cuentamorosos.model.EventItem
import com.cuentamorosos.notifications.NotificationContentFactory
import com.cuentamorosos.notifications.NotificationDedupEntries
import com.cuentamorosos.model.EventParticipant
import com.cuentamorosos.model.EventRole
import com.cuentamorosos.model.EventState
import com.cuentamorosos.model.ProfileItem
import com.cuentamorosos.model.UserPreferences
import org.json.JSONArray
import org.json.JSONObject

class CuentaMorososLocalStore(
    private val prefs: SharedPreferences
) {
    /**
     * Production constructor: creates [EncryptedSharedPreferences] with AES-256-GCM
     * and performs a one-time migration from the old plain-text store.
     */
    constructor(context: Context) : this(
        EncryptedPrefsFactory.createWithMigration(context)
    )

    fun loadEvents(): List<EventItem> = readArray(KEY_EVENTS) { item ->
        val id = item.optString("id")
        val name = item.optString("name").trim()
        if (id.isBlank() || name.isBlank()) {
            null
        } else {
            val dateMillis = item.optLong("dateMillis", System.currentTimeMillis())
            val ownerId = item.optString("ownerId", "")
            val memberIds = buildList {
                val ids = item.optJSONArray("memberIds") ?: JSONArray()
                for (index in 0 until ids.length()) {
                    ids.optString(index)?.takeIf { it.isNotBlank() }?.let(::add)
                }
            }
            val participants = loadParticipants(item, ownerId, dateMillis)
            EventItem(
                id = id,
                name = name,
                dateMillis = dateMillis,
                ownerId = ownerId,
                memberIds = memberIds,
                participants = participants,
                startDateMillis = item.optLong("startDateMillis", dateMillis),
                endDateMillis = item.optLong("endDateMillis", dateMillis),
                baseCurrency = item.optString("baseCurrency", ""),
                creatorId = item.optString("creatorId", ""),
                state = runCatching {
                    EventState.valueOf(item.optString("state", "OPEN"))
                }.getOrDefault(EventState.OPEN),
                lastCalculationMode = item.optString("lastCalculationMode").takeIf { it.isNotBlank() },
                lastCalculationTotal = item.optDouble("lastCalculationTotal").takeIf { item.has("lastCalculationTotal") },
                lastCalculationTimestamp = item.optLong("lastCalculationTimestamp").takeIf { item.has("lastCalculationTimestamp") },
                lastCalculationSummary = item.optString("lastCalculationSummary").takeIf { it.isNotBlank() }
            )
        }
    }.sortedByDescending { it.dateMillis }

    private fun loadParticipants(
        item: JSONObject,
        ownerId: String,
        dateMillis: Long,
    ): List<EventParticipant> {
        // Try to load participants array first
        val participantsArray = item.optJSONArray("participants")
        if (participantsArray != null && participantsArray.length() > 0) {
            return buildList {
                for (i in 0 until participantsArray.length()) {
                    val p = participantsArray.optJSONObject(i) ?: continue
                    val profileId = p.optString("profileId").takeIf { it.isNotBlank() } ?: continue
                    val role = runCatching {
                        EventRole.valueOf(p.optString("role", "CONTRIBUTOR"))
                    }.getOrDefault(EventRole.CONTRIBUTOR)
                    val joinedAt = p.optLong("joinedAtMillis", dateMillis)
                    add(EventParticipant(profileId = profileId, role = role, joinedAtMillis = joinedAt))
                }
            }
        }
        // Migration: derive from memberIds (ownerId → OWNER, rest → CONTRIBUTOR)
        return buildList {
            if (ownerId.isNotBlank()) {
                add(EventParticipant(profileId = ownerId, role = EventRole.OWNER, joinedAtMillis = dateMillis))
            }
            val ids = item.optJSONArray("memberIds") ?: JSONArray()
            for (index in 0 until ids.length()) {
                val mid = ids.optString(index)?.takeIf { it.isNotBlank() } ?: continue
                if (mid != ownerId) {
                    add(EventParticipant(profileId = mid, role = EventRole.CONTRIBUTOR, joinedAtMillis = dateMillis))
                }
            }
        }
    }

    fun saveEvents(events: List<EventItem>) {
        val payload = JSONArray().apply {
            events.forEach { event ->
                put(
                    JSONObject()
                        .put("id", event.id)
                        .put("name", event.name)
                        .put("dateMillis", event.dateMillis)
                        .put("ownerId", event.ownerId)
                        .put("memberIds", JSONArray().apply { event.memberIds.forEach(::put) })
                        .put("participants", JSONArray().apply {
                            event.participants.forEach { p ->
                                put(JSONObject()
                                    .put("profileId", p.profileId)
                                    .put("role", p.role.name)
                                    .put("joinedAtMillis", p.joinedAtMillis)
                                )
                            }
                        })
                        .put("startDateMillis", event.startDateMillis)
                        .put("endDateMillis", event.endDateMillis)
                        .put("baseCurrency", event.baseCurrency)
                        .put("creatorId", event.creatorId)
                        .put("state", event.state.name)
                        .apply {
                            event.lastCalculationMode?.let { put("lastCalculationMode", it) }
                            event.lastCalculationTotal?.let { put("lastCalculationTotal", it) }
                            event.lastCalculationTimestamp?.let { put("lastCalculationTimestamp", it) }
                            event.lastCalculationSummary?.let { put("lastCalculationSummary", it) }
                        }
                )
            }
        }
        prefs.edit().putString(KEY_EVENTS, payload.toString()).apply()
    }

    fun loadProfiles(): List<ProfileItem> = readArray(KEY_PROFILES) { item ->
        val id = item.optString("id")
        val name = item.optString("name").trim()
        if (id.isBlank() || name.isBlank()) {
            null
        } else {
            ProfileItem(
                id = id,
                name = name,
                totalPendingEuros = item.optDouble("totalPendingEuros", 0.0),
                isGhost = item.optBoolean("isGhost", false),
                linkedEmail = item.optString("linkedEmail", ""),
                ownerId = item.optString("ownerId", ""),
                photoUrl = item.optString("photoUrl").takeIf { it.isNotBlank() },
                username = item.optString("username").takeIf { it.isNotBlank() },
                customNames = loadCustomNames(item),
            )
        }
    }.sortedBy { it.name.lowercase() }

    fun saveProfiles(profiles: List<ProfileItem>) {
        val payload = JSONArray().apply {
            profiles.forEach { profile ->
                put(
                    JSONObject()
                        .put("id", profile.id)
                        .put("name", profile.name)
                        .put("totalPendingEuros", profile.totalPendingEuros)
                        .put("isGhost", profile.isGhost)
                        .put("linkedEmail", profile.linkedEmail)
                        .put("ownerId", profile.ownerId)
                        .put("photoUrl", profile.photoUrl)
                        .put("username", profile.username)
                        .put("customNames", JSONObject().apply {
                            profile.customNames.forEach { (k, v) -> put(k, v) }
                        })
                )
            }
        }
        prefs.edit().putString(KEY_PROFILES, payload.toString()).apply()
    }

    fun loadDebts(): List<EventDebtItem> = readArray(KEY_DEBTS) { item ->
        val id = item.optString("id")
        val eventId = item.optString("eventId")
        val profileId = item.optString("profileId")
        if (id.isBlank() || eventId.isBlank() || profileId.isBlank()) {
            null
        } else {
            EventDebtItem(
                id = id,
                eventId = eventId,
                profileId = profileId,
                creditorId = item.optString("creditorId").takeIf { it.isNotBlank() },
                amountEuros = item.optDouble("amountEuros", 0.0),
                notes = item.optString("notes"),
                paid = item.optBoolean("paid", false),
                calculationMode = item.optString("calculationMode").takeIf { it.isNotBlank() }
            )
        }
    }

    fun saveDebts(debts: List<EventDebtItem>) {
        val payload = JSONArray().apply {
            debts.forEach { debt ->
                put(
                    JSONObject()
                        .put("id", debt.id)
                        .put("eventId", debt.eventId)
                        .put("profileId", debt.profileId)
                        .put("amountEuros", debt.amountEuros)
                        .put("notes", debt.notes)
                        .put("paid", debt.paid)
                        .apply {
                            debt.calculationMode?.let { put("calculationMode", it) }
                            debt.creditorId?.let { put("creditorId", it) }
                        }
                )
            }
        }
        prefs.edit().putString(KEY_DEBTS, payload.toString()).apply()
    }

    fun loadExpenses(): List<EventExpenseItem> = readArray(KEY_EXPENSES) { item ->
        val id = item.optString("id")
        val eventId = item.optString("eventId")
        val name = item.optString("name").trim()
        if (id.isBlank() || eventId.isBlank() || name.isBlank()) {
            null
        } else {
            EventExpenseItem(
                id = id,
                eventId = eventId,
                name = name,
                amountEuros = item.optDouble("amountEuros", 0.0),
                category = item.optString("category").ifBlank { "shared" },
                assignedProfileIds = buildList {
                    val ids = item.optJSONArray("assignedProfileIds") ?: JSONArray()
                    for (index in 0 until ids.length()) {
                        ids.optString(index)?.takeIf { it.isNotBlank() }?.let(::add)
                    }
                },
                paidByProfileId = item.optString("paidByProfileId", ""),
                profileWeights = buildMap {
                    val weights = item.optJSONObject("profileWeights") ?: JSONObject()
                    weights.keys().forEach { key ->
                        put(key, weights.optDouble(key, 0.0))
                    }
                },
                splitMode = item.optString("splitMode").ifBlank { "SIMPLE_AVG" },
                payerContributions = buildMap {
                    val contributions = item.optJSONObject("payerContributions") ?: JSONObject()
                    if (contributions.length() > 0) {
                        contributions.keys().forEach { key ->
                            put(key, contributions.optDouble(key, 0.0))
                        }
                    }
                }.takeIf { it.isNotEmpty() }?.let { it } ?: run {
                    val paidBy = item.optString("paidByProfileId", "")
                    val amount = item.optDouble("amountEuros", 0.0)
                    if (paidBy.isNotBlank() && amount > 0.0) mapOf(paidBy to amount) else emptyMap()
                },
                debtorIds = buildList {
                    val ids = item.optJSONArray("debtorIds") ?: JSONArray()
                    for (index in 0 until ids.length()) {
                        ids.optString(index)?.takeIf { it.isNotBlank() }?.let(::add)
                    }
                },
                exchangeRate = item.optDouble("exchangeRate").takeIf { item.has("exchangeRate") },
                itemCurrency = item.optString("itemCurrency").takeIf { it.isNotBlank() },
                createdAtMillis = item.optLong("createdAtMillis", 0),
                createdByProfileId = item.optString("createdByProfileId", ""),
            )
        }
    }

    fun saveExpenses(expenses: List<EventExpenseItem>) {
        val payload = JSONArray().apply {
            expenses.forEach { expense ->
                put(
                    JSONObject()
                        .put("id", expense.id)
                        .put("eventId", expense.eventId)
                        .put("name", expense.name)
                        .put("amountEuros", expense.amountEuros)
                        .put("category", expense.category)
                        .put(
                            "assignedProfileIds",
                            JSONArray().apply {
                                expense.assignedProfileIds.forEach(::put)
                            }
                        )
                        .put(
                            "profileWeights",
                            JSONObject().apply {
                                expense.profileWeights.forEach { (k, v) -> put(k, v) }
                            }
                        )
                        .put("splitMode", expense.splitMode)
                        .put(
                            "payerContributions",
                            JSONObject().apply {
                                expense.payerContributions.forEach { (k, v) -> put(k, v) }
                            }
                        )
                        .put(
                            "debtorIds",
                            JSONArray().apply {
                                expense.debtorIds.forEach(::put)
                            }
                        )
                        .put("createdAtMillis", expense.createdAtMillis)
                        .put("createdByProfileId", expense.createdByProfileId)
                        .apply {
                            if (expense.paidByProfileId.isNotBlank()) {
                                put("paidByProfileId", expense.paidByProfileId)
                            }
                            expense.exchangeRate?.let { put("exchangeRate", it) }
                            expense.itemCurrency?.let { put("itemCurrency", it) }
                        }
                )
            }
        }
        prefs.edit().putString(KEY_EXPENSES, payload.toString()).apply()
    }

    fun loadPreferences(): UserPreferences {
        val rawValue = prefs.getString(KEY_PREFERENCES, null) ?: return UserPreferences()
        val item = runCatching { JSONObject(rawValue) }.getOrElse { return UserPreferences() }

        return UserPreferences(
            themeMode = item.optString("themeMode").ifBlank { "system" },
            accentColorId = item.optString("accentColorId").ifBlank { "rose" }, // silently ignored, kept for backward compat
            reminderDays = item.optInt("reminderDays", 7).coerceAtLeast(1),
            remindersEnabled = item.optBoolean("remindersEnabled", true)
        )
    }

    fun savePreferences(preferences: UserPreferences) {
        val payload = JSONObject()
            .put("themeMode", preferences.themeMode)
            .put("reminderDays", preferences.reminderDays)
            .put("remindersEnabled", preferences.remindersEnabled)

        prefs.edit().putString(KEY_PREFERENCES, payload.toString()).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    // ── Orphan Cleanup Flag (GPS-REQ-006) ──────────────────────────────────

    fun isOrphanCleanupDone(): Boolean =
        prefs.getBoolean(KEY_ORPHAN_CLEANUP_DONE, false)

    fun markOrphanCleanupDone() {
        prefs.edit().putBoolean(KEY_ORPHAN_CLEANUP_DONE, true).apply()
    }

    private fun <T> readArray(key: String, mapper: (JSONObject) -> T?): List<T> {
        val rawValue = prefs.getString(key, null) ?: return emptyList()
        val jsonArray = runCatching { JSONArray(rawValue) }.getOrElse { return emptyList() }

        return buildList {
            for (index in 0 until jsonArray.length()) {
                val value = jsonArray.optJSONObject(index)?.let(mapper)
                if (value != null) {
                    add(value)
                }
            }
        }
    }

    private fun loadCustomNames(item: JSONObject): Map<String, String> {
        val obj = item.optJSONObject("customNames") ?: return emptyMap()
        return buildMap {
            obj.keys().forEach { key ->
                put(key, obj.optString(key, ""))
            }
        }
    }

    // ── Dedup Registry ─────────────────────────────────────────────────────

    /**
     * Checks whether a notification with the given [fingerprint] has already been sent.
     * Returns `false` for null or blank fingerprints without throwing.
     * Thread-safe via [synchronized] on the underlying [SharedPreferences].
     */
    fun hasNotificationBeenSent(fingerprint: String): Boolean = synchronized(prefs) {
        NotificationDedupEntries.contains(readFingerprints(), fingerprint)
    }

    /**
     * Records that a notification with the given [fingerprint] has been sent.
     * No-ops safely for blank fingerprints.
     * Encoding lives in [NotificationDedupEntries], shared with iOS.
     * Thread-safe via [synchronized] on the underlying [SharedPreferences].
     */
    fun recordNotificationSent(fingerprint: String) {
        synchronized(prefs) {
            val current = readFingerprints()
            val updated = NotificationDedupEntries.record(
                current,
                fingerprint,
                System.currentTimeMillis(),
            )
            if (updated != current) writeFingerprints(updated)
        }
    }

    /**
     * Prunes fingerprint entries older than [maxAgeDays] (default 30 days).
     * Malformed entries are removed too. Pruning rules live in
     * [NotificationDedupEntries], shared with iOS.
     * Thread-safe via [synchronized] on the underlying [SharedPreferences].
     */
    fun cleanupOldEntries(maxAgeDays: Int = NotificationDedupEntries.DEFAULT_MAX_AGE_DAYS) {
        synchronized(prefs) {
            val current = readFingerprints()
            if (current.isEmpty()) return
            val pruned = NotificationDedupEntries.prune(
                current,
                System.currentTimeMillis(),
                maxAgeDays,
            )
            if (pruned.size != current.size) writeFingerprints(pruned)
        }
    }

    /**
     * Seeds the dedup registry on first launch after deployment.
     * Only runs when [KEY_SENT_FINGERPRINTS] does not yet exist.
     * For each [EventItem] with [EventState.CALCULATED] state, registers
     * a `CALCULATION_COMPLETED:{eventId}` fingerprint. Non-calculated events
     * (OPEN, CLOSED) are skipped.
     * Thread-safe via [synchronized] on the underlying [SharedPreferences].
     */
    fun seedDedupMigration(events: List<EventItem>) {
        if (events.isEmpty()) return
        synchronized(prefs) {
            if (prefs.contains(KEY_SENT_FINGERPRINTS)) return

            val now = System.currentTimeMillis()
            val entries = events
                .filter { it.state == EventState.CALCULATED }
                .fold(emptySet<String>()) { acc, event ->
                    NotificationDedupEntries.record(
                        acc,
                        "${NotificationContentFactory.TAG_CALCULATION_COMPLETED}:${event.id}",
                        now,
                    )
                }

            if (entries.isNotEmpty()) writeFingerprints(entries)
        }
    }

    private fun readFingerprints(): Set<String> =
        prefs.getStringSet(KEY_SENT_FINGERPRINTS, emptySet()) ?: emptySet()

    private fun writeFingerprints(entries: Set<String>) {
        prefs.edit().putStringSet(KEY_SENT_FINGERPRINTS, entries).apply()
    }

    companion object {
        const val PREFS_NAME_OLD = "cuenta_morosos_store"
        const val PREFS_NAME_ENCRYPTED = "cuenta_morosos_store_encrypted"
        @Deprecated("Use PREFS_NAME_ENCRYPTED", ReplaceWith("PREFS_NAME_ENCRYPTED"))
        const val PREFS_NAME = PREFS_NAME_ENCRYPTED
        const val KEY_EVENTS = "events"
        const val KEY_PROFILES = "profiles"
        const val KEY_DEBTS = "debts"
        const val KEY_EXPENSES = "expenses"
        const val KEY_PREFERENCES = "preferences"
        const val KEY_SENT_FINGERPRINTS = "sent_fingerprints"
        const val KEY_ORPHAN_CLEANUP_DONE = "orphan_cleanup_done"

        private val MIGRATION_KEYS_STRING = listOf(
            KEY_EVENTS, KEY_PROFILES, KEY_DEBTS, KEY_EXPENSES, KEY_PREFERENCES
        )

        @androidx.annotation.VisibleForTesting
        internal fun migrateFromOldStore(context: Context, targetPrefs: SharedPreferences) {
            val oldStore = context.getSharedPreferences(PREFS_NAME_OLD, Context.MODE_PRIVATE)
            val hasOldData = oldStore.all.isNotEmpty()
            if (!hasOldData) return

            // Migrate String keys: events, profiles, debts, expenses, preferences
            for (key in MIGRATION_KEYS_STRING) {
                val rawValue = oldStore.getString(key, null)
                if (rawValue != null) {
                    targetPrefs.edit().putString(key, rawValue).apply()
                }
            }

            // Migrate StringSet key: sent_fingerprints
            val sentFingerprints = oldStore.getStringSet(KEY_SENT_FINGERPRINTS, emptySet())
            if (sentFingerprints != null && sentFingerprints.isNotEmpty()) {
                targetPrefs.edit().putStringSet(KEY_SENT_FINGERPRINTS, sentFingerprints).apply()
            }

            // Migrate Boolean key: orphan_cleanup_done
            if (oldStore.contains(KEY_ORPHAN_CLEANUP_DONE)) {
                val orphanCleanup = oldStore.getBoolean(KEY_ORPHAN_CLEANUP_DONE, false)
                targetPrefs.edit().putBoolean(KEY_ORPHAN_CLEANUP_DONE, orphanCleanup).apply()
            }

            // Discard old plain-text store
            oldStore.edit().clear().apply()
        }
    }
}
