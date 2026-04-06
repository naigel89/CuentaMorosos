package com.cuentamorosos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.cuentamorosos.data.CuentaMorososLocalStore
import com.cuentamorosos.data.NotificationScheduler
import com.cuentamorosos.ui.CuentaMorososApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationScheduler.ensureChannel(this)
        setContent {
            val store = remember(applicationContext) {
                CuentaMorososLocalStore(applicationContext)
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                CuentaMorososApp(store = store)
            }
        }
    }
}
