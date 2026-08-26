package com.cuentamorosos.data

import com.cuentamorosos.model.UserPreferences
import platform.Foundation.NSUserDefaults

/**
 * Persistencia de [UserPreferences] en iOS, sobre `NSUserDefaults`.
 *
 * Son cuatro escalares y deben estar disponibles antes de que la UI pinte el
 * primer frame, así que no compensa meterlos en SQLDelight.
 */
class IosUserPreferencesStore(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) {

    fun load(): UserPreferences {
        val defaultPreferences = UserPreferences()
        return UserPreferences(
            themeMode = defaults.stringForKey(KEY_THEME_MODE) ?: defaultPreferences.themeMode,
            reminderDays = defaults.objectForKey(KEY_REMINDER_DAYS)
                ?.let { defaults.integerForKey(KEY_REMINDER_DAYS).toInt() }
                ?: defaultPreferences.reminderDays,
            remindersEnabled = defaults.objectForKey(KEY_REMINDERS_ENABLED)
                ?.let { defaults.boolForKey(KEY_REMINDERS_ENABLED) }
                ?: defaultPreferences.remindersEnabled,
        )
    }

    fun save(preferences: UserPreferences) {
        defaults.setObject(preferences.themeMode, forKey = KEY_THEME_MODE)
        defaults.setInteger(preferences.reminderDays.toLong(), forKey = KEY_REMINDER_DAYS)
        defaults.setBool(preferences.remindersEnabled, forKey = KEY_REMINDERS_ENABLED)
    }

    private companion object {
        // objectForKey distingue "nunca guardado" de "guardado como 0/false";
        // integerForKey y boolForKey por sí solos no.
        const val KEY_THEME_MODE = "cm_theme_mode"
        const val KEY_REMINDER_DAYS = "cm_reminder_days"
        const val KEY_REMINDERS_ENABLED = "cm_reminders_enabled"
    }
}
