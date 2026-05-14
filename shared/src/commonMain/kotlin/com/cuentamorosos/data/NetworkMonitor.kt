package com.cuentamorosos.data

import kotlinx.coroutines.flow.Flow

/** Platform-specific monitor for network connectivity. */
interface NetworkMonitor {
    /** A flow that emits true when the device is online, false otherwise. */
    val isOnline: Flow<Boolean>
}

/** Factory to provide the platform-specific NetworkMonitor implementation. */
expect class NetworkMonitorFactory {
    fun create(): NetworkMonitor
}
