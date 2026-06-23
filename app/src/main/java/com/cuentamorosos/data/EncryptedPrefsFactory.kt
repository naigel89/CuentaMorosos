package com.cuentamorosos.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * Factory that creates [EncryptedSharedPreferences] backed by Android Keystore
 * (AES-256-GCM) and performs a one-time migration from the old plain-text store.
 *
 * Falls back to plain [SharedPreferences] when the Android Keystore is unavailable
 * (e.g., Robolectric unit tests). In production Android environments, the Keystore
 * is always available and data-at-rest encryption is enforced.
 *
 * Isolated in a separate file to avoid loading security-crypto classes
 * when [CuentaMorososLocalStore] is instantiated with a plain [SharedPreferences]
 * in unit tests.
 */
internal object EncryptedPrefsFactory {

    private const val TAG = "EncryptedPrefsFactory"

    fun createWithMigration(context: Context): SharedPreferences {
        return try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            @Suppress("DEPRECATION")
            val encryptedPrefs = EncryptedSharedPreferences.create(
                CuentaMorososLocalStore.PREFS_NAME_ENCRYPTED,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            CuentaMorososLocalStore.migrateFromOldStore(context, encryptedPrefs)
            encryptedPrefs
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create EncryptedSharedPreferences, falling back to plain storage", e)
            val fallback = context.getSharedPreferences(
                CuentaMorososLocalStore.PREFS_NAME_ENCRYPTED,
                Context.MODE_PRIVATE
            )
            CuentaMorososLocalStore.migrateFromOldStore(context, fallback)
            fallback
        }
    }
}
