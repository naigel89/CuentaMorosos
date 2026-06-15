package com.cuentamorosos

import android.app.Application
import com.cuentamorosos.data.CuentaMorososLocalStore

class CuentaMorososApp : Application() {

    /**
     * RepositoryProvider instance, set after user login in MainActivity.
     * Accessed by ReminderWorker for background notification generation.
     * Null when no user is logged in.
     */
    var repositoryProvider: RepositoryProvider? = null
        internal set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Run periodic cleanup of old notification fingerprints on every app start
        CuentaMorososLocalStore(this).cleanupOldEntries()
    }

    companion object {
        @Volatile
        private var instance: CuentaMorososApp? = null

        fun getInstance(): CuentaMorososApp =
            instance ?: throw IllegalStateException(
                "CuentaMorososApp not initialized. " +
                "Ensure android:name=\".CuentaMorososApp\" in AndroidManifest.xml"
            )
    }
}
