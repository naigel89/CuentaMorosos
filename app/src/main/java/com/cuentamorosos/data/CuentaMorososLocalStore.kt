package com.cuentamorosos.data

import android.content.Context
import com.cuentamorosos.model.EventDebtItem
import com.cuentamorosos.model.EventExpenseItem
import com.cuentamorosos.model.EventItem
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
            EventItem(
                id = id,
                name = name,
                dateMillis = item.optLong("dateMillis", System.currentTimeMillis()),
                ownerId = item.optString("ownerId").ifBlank { "" },
                memberIds = buildList {
                    val ids = item.optJSONArray("memberIds") ?: JSONArray()
                    for (i in 0 until ids.length()) {
                        ids.optString(i)?.takeIf { it.isNotBlank() }?.let(::add)
                    }
                },
                lastCalculationMode = item.optString("lastCalculationMode").takeIf { it.isNotBlank() },
                lastCalculationTotal = item.optDouble("lastCalculationTotal").takeIf { item.has("lastCalculationTotal") },
                lastCalculationTimestamp = item.optLong("lastCalculationTimestamp").takeIf { item.has("lastCalculationTimestamp") },
                lastCalculationSummary = item.optString("lastCalculationSummary").takeIf { it.isNotBlank() }
            )
        }
    }.sortedByDescending { it.dateMillis }

    fun saveEvents(events: List<EventItem>) {
        val payload = JSONArray().apply {
            events.forEach { event ->
                put(
                    JSONObject()
                        .put("id", event.id)
                        .put("name", event.name)
                        .put("dateMillis", event.dateMillis)
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
                icon = item.optString("icon").ifBlank { "🙂" },
                totalPendingEuros = item.optDouble("totalPendingEuros", 0.0),
                isGhost = item.optBoolean("isGhost", false),
                linkedEmail = item.optString("linkedEmail").takeIf { it.isNotBlank() }
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
                        .put("icon", profile.icon)
                        .put("totalPendingEuros", profile.totalPendingEuros)
                        .put("isGhost", profile.isGhost)
                        .put("linkedEmail", profile.linkedEmail ?: JSONObject.NULL)
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
                    profileWeights = buildMap {
                        val weightsObj = item.optJSONObject("profileWeights") ?: JSONObject()
                        weightsObj.keys().forEach { key ->
                            val value = weightsObj.optDouble(key, Double.NaN)
                            if (!value.isNaN()) put(key, value)
                        }
                    }
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
                                expense.profileWeights.forEach { (id, weight) ->
                                    put(id, weight)
                                }
                            }
                        )
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
            accentColorId = item.optString("accentColorId").ifBlank { "rose" },
            reminderDays = item.optInt("reminderDays", 7).coerceAtLeast(1),
            remindersEnabled = item.optBoolean("remindersEnabled", true)
        )
    }

    fun savePreferences(preferences: UserPreferences) {
        val payload = JSONObject()
            .put("themeMode", preferences.themeMode)
            .put("accentColorId", preferences.accentColorId)
            .put("reminderDays", preferences.reminderDays)
            .put("remindersEnabled", preferences.remindersEnabled)

        prefs.edit().putString(KEY_PREFERENCES, payload.toString()).apply()
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

    private companion object {
        const val PREFS_NAME = "cuenta_morosos_store"
        const val KEY_EVENTS = "events"
        const val KEY_PROFILES = "profiles"
        const val KEY_DEBTS = "debts"
        const val KEY_EXPENSES = "expenses"
        const val KEY_PREFERENCES = "preferences"
    }
}
