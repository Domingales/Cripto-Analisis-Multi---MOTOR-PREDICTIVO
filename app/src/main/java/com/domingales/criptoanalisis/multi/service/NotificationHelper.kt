package com.domingales.criptoanalisis.multi.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import com.domingales.criptoanalisis.multi.domain.AnalysisResult
import com.domingales.criptoanalisis.multi.domain.Direction
import com.domingales.criptoanalisis.multi.data.AppDatabase
import com.domingales.criptoanalisis.multi.ui.CryptoDetailActivity
import com.domingales.criptoanalisis.multi.ui.MainActivity
import com.domingales.criptoanalisis.multi.ui.PriceAlarmsActivity
import com.domingales.criptoanalisis.multi.ui.ExplosionActivity
import com.domingales.criptoanalisis.multi.util.Prefs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NotificationHelper {
    const val SERVICE_CHANNEL = "watch_service"
    const val SIGNAL_CHANNEL = "market_signals_sound_vibration"
    private const val SIGNAL_SOUND = "market_signals_sound"
    private const val SIGNAL_VIBRATION = "market_signals_vibration"
    private const val SIGNAL_SILENT = "market_signals_silent"
    private const val EXPLOSION_BOTH = "explosion_sound_vibration"
    private const val EXPLOSION_SOUND = "explosion_sound"
    private const val EXPLOSION_VIBRATION = "explosion_vibration"
    private const val EXPLOSION_SILENT = "explosion_silent"

    private fun eventDateTime(eventAt: Long): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(eventAt))

    fun createChannels(c: Context) {
        val n = c.getSystemService(NotificationManager::class.java)
        n.createNotificationChannel(NotificationChannel(SERVICE_CHANNEL, "Vigilancia", NotificationManager.IMPORTANCE_LOW).apply { description = "Servicio permanente de vigilancia multcripto" })
        createSignalChannel(n, SIGNAL_CHANNEL, "Señales: sonido y vibración", true, true)
        createSignalChannel(n, SIGNAL_SOUND, "Señales: solo sonido", true, false)
        createSignalChannel(n, SIGNAL_VIBRATION, "Señales: solo vibración", false, true)
        createSignalChannel(n, SIGNAL_SILENT, "Señales silenciosas", false, false)
        createSignalChannel(n, EXPLOSION_BOTH, "Posibles explosiones: sonido y vibración", true, true)
        createSignalChannel(n, EXPLOSION_SOUND, "Posibles explosiones: solo sonido", true, false)
        createSignalChannel(n, EXPLOSION_VIBRATION, "Posibles explosiones: solo vibración", false, true)
        createSignalChannel(n, EXPLOSION_SILENT, "Posibles explosiones silenciosas", false, false)
    }

    private fun createSignalChannel(manager: NotificationManager, id: String, name: String, sound: Boolean, vibration: Boolean) {
        manager.createNotificationChannel(NotificationChannel(id, name, if (sound || vibration) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Alertas configurables de CriptoAnálisis Multi"
            enableVibration(vibration); vibrationPattern = if (vibration) longArrayOf(0, 350, 180, 500) else null
            if (sound) setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build()) else setSound(null, null)
        })
    }

    private fun channel(c: Context, symbol: String): String {
        val sound = Prefs.symbolSoundEnabled(c, symbol)
        val vibration = Prefs.symbolVibrationEnabled(c, symbol)
        return when { sound && vibration -> SIGNAL_CHANNEL; sound -> SIGNAL_SOUND; vibration -> SIGNAL_VIBRATION; else -> SIGNAL_SILENT }
    }

    private fun explosionChannel(c: Context): String = when {
        Prefs.explosionSoundEnabled(c) && Prefs.explosionVibrationEnabled(c) -> EXPLOSION_BOTH
        Prefs.explosionSoundEnabled(c) -> EXPLOSION_SOUND
        Prefs.explosionVibrationEnabled(c) -> EXPLOSION_VIBRATION
        else -> EXPLOSION_SILENT
    }

    fun testSignal(c: Context) {
        val pi = PendingIntent.getActivity(c, 77, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        c.getSystemService(NotificationManager::class.java).notify(4077,
            android.app.Notification.Builder(c, channel(c, "SYSTEM"))
                .setSmallIcon(android.R.drawable.stat_notify_more)
                .setContentTitle("🟢 PRUEBA — canal de Señales")
                .setContentText("Si oyes sonido y notas vibración, las alertas están configuradas correctamente.")
                .setAutoCancel(true).setContentIntent(pi).build()
        )
    }

    fun testExplosion(c: Context) {
        val pi = PendingIntent.getActivity(c, 78, Intent(c, ExplosionActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        c.getSystemService(NotificationManager::class.java).notify(4078,
            android.app.Notification.Builder(c, explosionChannel(c))
                .setSmallIcon(android.R.drawable.stat_notify_more)
                .setContentTitle("🚀 PRUEBA — posible explosión")
                .setContentText("Prueba del canal independiente de sonido y vibración.")
                .setAutoCancel(true).setContentIntent(pi).build()
        )
    }

    fun serviceNotification(c: Context, text: String): android.app.Notification {
        val pi = PendingIntent.getActivity(c, 1, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return android.app.Notification.Builder(c, SERVICE_CHANNEL).setSmallIcon(android.R.drawable.ic_popup_sync).setContentTitle("CriptoAnálisis Multi activo").setContentText(text).setContentIntent(pi).setOngoing(true).build()
    }

    fun signal(c: Context, a: AnalysisResult) {
        val detail = Intent(c, CryptoDetailActivity::class.java).putExtra("symbol", a.symbol)
        val pi = PendingIntent.getActivity(c, (a.symbol + a.timeframe).hashCode(), detail, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val buy = a.direction == Direction.BUY || a.direction == Direction.REVERSAL_BUY
        val label = when (a.direction) {
            Direction.REVERSAL_BUY -> "POSIBLE GIRO ALCISTA"
            Direction.REVERSAL_SELL -> "POSIBLE GIRO BAJISTA"
            Direction.BUY -> "COMPRA"
            Direction.SELL -> "VENTA"
            else -> "ESPERAR"
        }
        val title = (if (buy) "🟢" else "🔴") + " ${a.symbol} — $label ${a.confidence}%"
        val probability = a.probability?.let { " • prob. $it%" } ?: " • prob. sin calibrar"
        val cases = if (a.calibration.comparableCases > 0) " • ${a.calibration.comparableCases} casos" else ""
        val body = "${a.timeframe} • Señal ${eventDateTime(a.candleCloseTime)} • Precio ${"%.6f".format(a.indicators.price)}$probability$cases • Riesgo ${a.risk}"
        c.getSystemService(NotificationManager::class.java).notify((a.symbol + a.timeframe + a.candleCloseTime).hashCode(), android.app.Notification.Builder(c, channel(c, a.symbol))
            .setSmallIcon(android.R.drawable.stat_notify_more).setContentTitle(title).setContentText(body)
            .setStyle(android.app.Notification.BigTextStyle().bigText(body + "\n" + a.explanation.take(5).joinToString(" • ")))
            .setWhen(a.candleCloseTime).setShowWhen(true)
            .setAutoCancel(true).setContentIntent(pi).build())
    }

    fun signal(c: Context, a: AppDatabase.PendingAlert) {
        val detail = Intent(c, CryptoDetailActivity::class.java).putExtra("symbol", a.symbol)
        val pi = PendingIntent.getActivity(c, (a.symbol + a.timeframe).hashCode(), detail, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val buy = a.direction.contains("BUY")
        val probability = a.probability?.let { " • prob. $it%" } ?: " • prob. sin calibrar"
        val cases = if (a.comparableCases > 0) " • ${a.comparableCases} casos" else ""
        val body = "${a.timeframe} • Señal ${eventDateTime(a.eventAt)} • Precio ${"%.6f".format(a.price)}$probability$cases • Riesgo ${a.risk}"
        c.getSystemService(NotificationManager::class.java).notify(a.uid.hashCode(), android.app.Notification.Builder(c, channel(c, a.symbol))
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle((if (buy) "🟢" else "🔴") + " ${a.symbol} — ${a.direction.replace('_', ' ')} ${a.confidence}%")
            .setContentText(body).setStyle(android.app.Notification.BigTextStyle().bigText(body))
            .setWhen(a.eventAt).setShowWhen(true)
            .setAutoCancel(true).setContentIntent(pi).build())
    }

    fun priceAlarm(c: Context, alarm: AppDatabase.PriceAlarm) {
        val detail = Intent(c, PriceAlarmsActivity::class.java)
        val pi = PendingIntent.getActivity(c, alarm.uid.hashCode(), detail, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val relation = if (alarm.condition == "ABOVE") "ha superado" else "ha bajado de"
        fun price(value: Double) = String.format(Locale.ROOT, "%.8f", value).trimEnd('0').trimEnd('.')
        val eventAt = alarm.triggeredAt ?: System.currentTimeMillis()
        val body = "${alarm.symbol} $relation ${price(alarm.target)} • Alarma ${eventDateTime(eventAt)} • precio ${alarm.lastPrice?.let(::price) ?: "N/D"}${alarm.observation.takeIf { it.isNotBlank() }?.let { " • $it" } ?: ""}"
        c.getSystemService(NotificationManager::class.java).notify(alarm.uid.hashCode(), android.app.Notification.Builder(c, channel(c, alarm.symbol))
            .setSmallIcon(android.R.drawable.stat_notify_more).setContentTitle("🔔 Alarma de precio ${alarm.symbol}")
            .setContentText(body).setStyle(android.app.Notification.BigTextStyle().bigText(body))
            .setWhen(eventAt).setShowWhen(true).setAutoCancel(true).setContentIntent(pi).build())
    }

    fun explosion(c: Context, alert: AppDatabase.PendingExplosionAlert) {
        val pi = PendingIntent.getActivity(c, alert.uid.hashCode(), Intent(c, ExplosionActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val bullish = alert.direction == "BULLISH"
        val title = if (bullish) "🚀 POSIBLE EXPLOSIÓN ALCISTA" else "📉 POSIBLE EXPLOSIÓN BAJISTA"
        val body = "${alert.symbol} ${alert.timeframe} • Señal ${eventDateTime(alert.eventAt)} • 5/5 • Volumen ${"%.2f".format(alert.volumeRatio)}x • ATR ${"%.2f".format(alert.atrRatio)}x • ADX ${"%.1f".format(alert.adx)} ↑ • MTF ${alert.alignedTimeframes}/${alert.availableTimeframes} • sin calibrar"
        c.getSystemService(NotificationManager::class.java).notify(alert.uid.hashCode(), android.app.Notification.Builder(c, explosionChannel(c))
            .setSmallIcon(android.R.drawable.stat_notify_more).setContentTitle(title).setContentText(body)
            .setStyle(android.app.Notification.BigTextStyle().bigText(body + "\nRuptura confirmada: ${"%.8f".format(alert.breakoutLevel).trimEnd('0').trimEnd('.')}"))
            .setWhen(alert.eventAt).setShowWhen(true)
            .setAutoCancel(true).setContentIntent(pi).build())
    }
}
