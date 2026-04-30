package com.cuentamorosos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import com.cuentamorosos.data.CuentaMorososLocalStore
import com.cuentamorosos.data.NotificationScheduler
import com.cuentamorosos.data.ReminderWorker
import com.cuentamorosos.ui.CuentaMorososApp

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // El usuario aceptó o rechazó — no bloqueamos el flujo en ningún caso.
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationScheduler.ensureChannel(this)
        requestNotificationsPermissionIfNeeded()
        scheduleReminderWorkerIfEnabled()
        setContent {
            val store = remember(applicationContext) {
                CuentaMorososLocalStore(applicationContext)
            }
            CuentaMorososApp(store = store)
        }
    }

    private fun requestNotificationsPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun scheduleReminderWorkerIfEnabled() {
        val prefs = CuentaMorososLocalStore(applicationContext).loadPreferences()
        if (prefs.remindersEnabled) {
            ReminderWorker.schedule(applicationContext)
        } else {
            ReminderWorker.cancel(applicationContext)
        }
    }
}
