package com.domingales.criptoanalisis.multi.service

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.domingales.criptoanalisis.multi.data.AppDatabase
import com.domingales.criptoanalisis.multi.util.Prefs

/** Coordinador permanente mientras el usuario mantenga activa la vigilancia. */
class MarketWatchService : Service() {
    companion object {
        const val ACTION_START = "com.domingales.criptoanalisis.multi.START_WATCH"
        const val ACTION_RESTORE = "com.domingales.criptoanalisis.multi.RESTORE_WATCH"
        const val ACTION_RECONFIGURE = "com.domingales.criptoanalisis.multi.RECONFIGURE_WATCH"
        const val ACTION_REMOTE = "com.domingales.criptoanalisis.multi.REMOTE_SCAN"
        const val ACTION_MANUAL = "com.domingales.criptoanalisis.multi.MANUAL_SCAN"
        const val EXTRA_RUN_IMMEDIATELY = "run_immediately"
        const val EXTRA_REQUESTED_AT = "requested_at"
        const val EXTRA_RECEIVED_AT = "received_at"
        const val EXTRA_PRIORITY = "priority"
        const val EXTRA_ORIGINAL_PRIORITY = "original_priority"
        private const val NOTIFICATION_ID = 4101
    }

    private val handler = Handler(Looper.getMainLooper())
    private val localTick = object : Runnable {
        override fun run() {
            if (!Prefs.watching(this@MarketWatchService)) return
            requestCycle("LOCAL_FGS", false, 0L)
            armLocalLoop()
        }
    }
    private val heartbeat = object : Runnable {
        override fun run() {
            if (!Prefs.watching(this@MarketWatchService)) return
            Prefs.setServiceHeartbeat(this@MarketWatchService)
            handler.postDelayed(this, 60_000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        startForeground(NOTIFICATION_ID, NotificationHelper.serviceNotification(this, "Preparando vigilancia permanente…"))
        Prefs.setServiceHeartbeat(this)
        AppDatabase.get(this).log("SYSTEM", "SERVICE", "ForegroundService coordinador iniciado")
        handler.post(heartbeat)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Prefs.watching(this) && intent?.action != ACTION_MANUAL) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf(startId)
            return START_NOT_STICKY
        }
        Prefs.setServiceHeartbeat(this)
        val action = intent?.action ?: ACTION_RESTORE
        when (action) {
            ACTION_MANUAL -> requestCycle("MANUAL", true, 0L)
            ACTION_REMOTE -> {
                val requestedAt = intent?.getLongExtra(EXTRA_REQUESTED_AT, 0L) ?: 0L
                val receivedAt = intent?.getLongExtra(EXTRA_RECEIVED_AT, System.currentTimeMillis()) ?: System.currentTimeMillis()
                val priority = intent?.getIntExtra(EXTRA_PRIORITY, 0) ?: 0
                val original = intent?.getIntExtra(EXTRA_ORIGINAL_PRIORITY, 0) ?: 0
                val deliveryMillis = if (requestedAt > 0L) (receivedAt - requestedAt).coerceAtLeast(0L) else 0L
                AppDatabase.get(this).log("SYSTEM", "REMOTE", "Coordinador despierto • entrega ${deliveryMillis}ms • prioridad $priority/$original")
                requestCycle("REMOTO_FCM", false, requestedAt)
            }
            ACTION_RECONFIGURE -> AppDatabase.get(this).log("SYSTEM", "SERVICE", "Intervalo local actualizado a ${Prefs.intervalMinutes(this)} min")
            ACTION_RESTORE -> AppDatabase.get(this).log("SYSTEM", "SERVICE", "Vigilancia restaurada por Android/arranque")
        }
        if (Prefs.watching(this)) armLocalLoop()
        if (intent?.getBooleanExtra(EXTRA_RUN_IMMEDIATELY, false) == true) {
            requestCycle(if (action == ACTION_RESTORE) "RECUPERACION" else "ACTIVACION", false, 0L)
        }
        updateNotification(if (Prefs.watching(this)) "Vigilancia activa • ciclo local cada ${Prefs.intervalMinutes(this)} min" else "Análisis manual en curso…")
        return if (Prefs.watching(this)) START_STICKY else START_NOT_STICKY
    }

    private fun requestCycle(origin: String, force: Boolean, requestedAt: Long) {
        val started = MarketScanRunner.start(this, force, origin, requestedAt) { retry ->
            handler.post {
                Prefs.setServiceHeartbeat(this)
                if (!Prefs.watching(this)) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    updateNotification(if (retry) "Último ciclo con incidencia • se reintentará" else "Vigilancia activa • último ciclo completado")
                }
            }
        }
        if (!started && Prefs.watching(this)) updateNotification("Vigilancia activa • activación duplicada omitida")
    }

    private fun armLocalLoop() {
        handler.removeCallbacks(localTick)
        handler.postDelayed(localTick, Prefs.intervalMinutes(this).coerceAtLeast(1) * 60_000L)
    }

    private fun updateNotification(text: String) {
        startForeground(NOTIFICATION_ID, NotificationHelper.serviceNotification(this, text))
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        AppDatabase.get(this).log("SYSTEM", "SERVICE", "Interfaz retirada; vigilancia permanece activa")
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        AppDatabase.get(this).log("SYSTEM", "SERVICE", "Coordinador destruido; Android podrá restaurarlo si la vigilancia sigue activa")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
