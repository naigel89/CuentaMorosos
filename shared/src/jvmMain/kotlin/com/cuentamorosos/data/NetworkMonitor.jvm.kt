package com.cuentamorosos.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Stub network monitor for JVM (always online). */
class JvmNetworkMonitor : NetworkMonitor {
    override val isOnline: Flow<Boolean> = flowOf(true)
}

actual class NetworkMonitorFactory {
    actual fun create(): NetworkMonitor = JvmNetworkMonitor()
}
