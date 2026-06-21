package com.cuentamorosos.data

import android.content.Context
import com.cuentamorosos.model.EventDebtItem
import com.cuentamorosos.model.EventExpenseItem
import com.cuentamorosos.model.EventItem
import com.cuentamorosos.model.EventParticipant
import com.cuentamorosos.model.EventRole
import com.cuentamorosos.model.EventState
import com.cuentamorosos.model.ProfileItem
import com.cuentamorosos.model.UserPreferences
import org.json.JSONArray
import org.json.JSONObject

class CuentaMorososLocalStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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
                displayName = item.optString("displayName").takeIf { it.isNotBlank() },
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
                        .put("displayName", profile.displayName)
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
                    contributions.keys().forEach { key ->
                        put(key, contributions.optDouble(key, 0.0))
                    }
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
    fun hasNotificationBeenSent(fingerprint: String): Boolean {
        if (fingerprint.isBlank()) return false
        return synchronized(prefs) {
            val set = prefs.getStringSet(KEY_SENT_FINGERPRINTS, emptySet()) ?: emptySet()
            set.any { entry ->
                val separator = entry.indexOf('|')
                separator >= 0 && entry.substring(separator + 1) == fingerprint
            }
        }
    }

    /**
     * Records that a notification with the given [fingerprint] has been sent.
     * No-ops safely for null or blank fingerprints.
     * Stores as `"{epochMs}|{fingerprint}"` in a [StringSet].
     * Thread-safe via [synchronized] on the underlying [SharedPreferences].
     */
    fun recordNotificationSent(fingerprint: String) {
        if (fingerprint.isBlank()) return
        synchronized(prefs) {
            val currentSet = prefs.getStringSet(KEY_SENT_FINGERPRINTS, emptySet())
                ?.toMutableSet() ?: mutableSetOf()
            val entry = "${System.currentTimeMillis()}|$fingerprint"
            currentSet.add(entry)
            prefs.edit().putStringSet(KEY_SENT_FINGERPRINTS, currentSet).apply()
        }
    }

    /**
     * Prunes fingerprint entries older than [maxAgeDays] (default 30 days).
     * Each entry begins with an epoch-millis timestamp; entries whose timestamp
     * is before the cutoff are removed. Malformed entries are also removed.
     * Empty registry or no-op cleanup does not throw.
     * Thread-safe via [synchronized] on the underlying [SharedPreferences].
     */
    fun cleanupOldEntries(maxAgeDays: Int = 30) {
        synchronized(prefs) {
            val currentSet = prefs.getStringSet(KEY_SENT_FINGERPRINTS, emptySet())
                ?.toMutableSet() ?: mutableSetOf()
            if (currentSet.isEmpty()) return

            val cutoff = System.currentTimeMillis() - (maxAgeDays.toLong() * 24 * 60 * 60 * 1000)
            val pruned = currentSet.filter { entry ->
                val separator = entry.indexOf('|')
                if (separator < 0) return@filter false // malformed, remove
                val epochMs = entry.substring(0, separator).toLongOrNull() ?: return@filter false
                epochMs >= cutoff
            }.toMutableSet()

            if (pruned.size != currentSet.size) {
                prefs.edit().putStringSet(KEY_SENT_FINGERPRINTS, pruned).apply()
            }
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

            val entries = events
                .filter { it.state == EventState.CALCULATED }
                .map { "${System.currentTimeMillis()}|CALCULATION_COMPLETED:${it.id}" }
                .toMutableSet()

            if (entries.isNotEmpty()) {
                prefs.edit().putStringSet(KEY_SENT_FINGERPRINTS, entries).apply()
            }
        }
    }

    private companion object {
        const val PREFS_NAME = "cuenta_morosos_store"
        const val KEY_EVENTS = "events"
        const val KEY_PROFILES = "profiles"
        const val KEY_DEBTS = "debts"
        const val KEY_EXPENSES = "expenses"
        const val KEY_PREFERENCES = "preferences"
        const val KEY_SENT_FINGERPRINTS = "sent_fingerprints"
    }
}
