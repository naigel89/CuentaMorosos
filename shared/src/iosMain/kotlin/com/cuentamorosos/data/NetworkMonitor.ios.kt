package com.cuentamorosos.data

import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_get_status
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_get_main_queue
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class IosNetworkMonitor : NetworkMonitor {
    override val isOnline: Flow<Boolean> = callbackFlow {
        val monitor = nw_path_monitor_create()
        
        nw_path_monitor_set_update_handler(monitor) { path ->
            val status = nw_path_get_status(path)
            trySend(status == nw_path_status_satisfied)
        }
        
        nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
        nw_path_monitor_start(monitor)
        
        awaitClose {
            // In a real app, we'd stop the monitor here.
            // nw_path_monitor_cancel(monitor)
        }
    }.distinctUntilChanged()
}

actual class NetworkMonitorFactory {
    actual fun create(): NetworkMonitor = IosNetworkMonitor()
}
