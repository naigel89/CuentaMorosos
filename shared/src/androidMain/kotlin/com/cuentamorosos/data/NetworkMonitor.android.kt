package com.cuentamorosos.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart

class AndroidNetworkMonitor(private val context: Context) : NetworkMonitor {
    override val isOnline: Flow<Boolean> = callbackFlow {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                LogSanitizer.log("NetworkMonitor", "onAvailable → ONLINE")
                trySend(true)
            }
            override fun onLost(network: Network) {
                LogSanitizer.log("NetworkMonitor", "onLost → OFFLINE")
                trySend(false)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        LogSanitizer.log("NetworkMonitor", "Registering network callback")
        connectivityManager.registerNetworkCallback(request, callback)
        
        // Initial state
        val currentNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(currentNetwork)
        val hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        LogSanitizer.log("NetworkMonitor", "Initial state: hasInternet=$hasInternet, activeNetwork=${currentNetwork != null}")
        trySend(hasInternet)

        awaitClose {
            LogSanitizer.log("NetworkMonitor", "Unregistering network callback")
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()
}

actual class NetworkMonitorFactory(private val context: Context) {
    actual fun create(): NetworkMonitor = AndroidNetworkMonitor(context)
}
