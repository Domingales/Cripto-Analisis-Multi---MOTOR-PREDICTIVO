package com.domingales.criptoanalisis.multi.service

import com.domingales.criptoanalisis.multi.data.AppDatabase
import com.domingales.criptoanalisis.multi.util.Prefs
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class RemoteMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        if (Prefs.watching(this)) HybridRemoteClient.registerToken(this, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        if (!Prefs.watching(this)) return
        val receivedAt = System.currentTimeMillis()
        val requestedAt = message.data["requestedAt"]?.toLongOrNull() ?: receivedAt
        val delaySeconds = ((receivedAt - requestedAt).coerceAtLeast(0L) / 1000L)
        AppDatabase.get(this).log(
            "SYSTEM", "REMOTE",
            "FCM recibido • retraso ${delaySeconds}s • prioridad ${message.priority}/${message.originalPriority} • análisis solicitado"
        )
        MonitoringScheduler.remoteTrigger(this, requestedAt, receivedAt, message.priority, message.originalPriority)
    }
}
