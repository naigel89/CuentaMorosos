package com.cuentamorosos.data

import android.util.Log
import com.cuentamorosos.notifications.NotificationDispatcher
import com.cuentamorosos.notifications.PushPayloadParser
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class CuentaMorososFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "CM_FCM"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token refreshed")
        serviceScope.launch {
            FirebaseUserSyncManager.saveTokenForCurrentUser(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val event = PushPayloadParser.parse(remoteMessage.data)
        if (event == null) {
            Log.w(TAG, "Payload FCM no reconocido, se descarta: type=${remoteMessage.data["type"]}")
            return
        }

        val localStore = CuentaMorososLocalStore(applicationContext)
        NotificationDispatcher(applicationContext, localStore = localStore).dispatch(event)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
