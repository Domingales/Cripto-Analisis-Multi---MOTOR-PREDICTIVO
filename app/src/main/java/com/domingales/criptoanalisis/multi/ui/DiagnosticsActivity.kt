package com.domingales.criptoanalisis.multi.ui

import android.app.Activity
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.PowerManager
import android.widget.ScrollView
import com.domingales.criptoanalisis.multi.data.AppDatabase
import com.domingales.criptoanalisis.multi.data.BinanceMarketDataSource
import com.domingales.criptoanalisis.multi.util.Prefs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class DiagnosticsActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this); val root = Ui.root(this); scroll.addView(root)
        root.addView(Ui.title(this, "Diagnóstico y auditoría"))
        val db = AppDatabase.get(this); val stats = db.diagnosticStats(); val health = db.engineHealth()
        val date = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        fun instant(value: Long?) = value?.let { date.format(Date(it)) } ?: "N/D"

        val state = Ui.card(this)
        state.addView(Ui.text(this, "Vigilancia programada: ${if (Prefs.watching(this)) "ACTIVA" else "DETENIDA"}", 18f, true))
        state.addView(Ui.text(this, "ForegroundService: ${if (Prefs.serviceResponsive(this)) "RESPONDIENDO" else if (Prefs.watching(this)) "SIN LATIDO RECIENTE" else "DETENIDO"} • último latido ${instant(Prefs.serviceHeartbeat(this).takeIf { it > 0L })}"))
        val batteryUnrestricted = getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)
        state.addView(Ui.text(this, "Optimización de batería: ${if (batteryUnrestricted) "SIN RESTRICCIONES" else "ACTIVA; puede limitar al fabricante"}"))
        state.addView(Ui.text(this, "Internet: ${if (hasInternet()) "CONECTADO" else "SIN CONEXIÓN"} • API Binance: comprobando…", 15f, true).also { apiLabel ->
            executor.execute {
                val result = try { BinanceMarketDataSource().currentPrice("BTC"); "OPERATIVA" } catch (t: Throwable) { "ERROR: ${t.message ?: "sin respuesta"}" }
                runOnUiThread { apiLabel.text = "Internet: ${if (hasInternet()) "CONECTADO" else "SIN CONEXIÓN"} • API Binance: $result" }
            }
        })
        state.addView(Ui.text(this, "Criptos activas ${Prefs.watchlist(this).size} • alarmas de precio activas ${health.activePriceAlarms}"))
        state.addView(Ui.text(this, "Última revisión completada: ${instant(health.lastCompletedCycle)}"))
        state.addView(Ui.text(this, "Último análisis: ${instant(health.lastAnalysis)} • análisis de hoy ${health.analysesToday}"))
        state.addView(Ui.text(this, "Modo ${if (Prefs.closedCandleMode(this)) "VELA CERRADA" else "INTRAVELA"} • umbral ${Prefs.threshold(this)}% • local ${Prefs.intervalMinutes(this)} min • respaldo ${com.domingales.criptoanalisis.multi.service.MonitoringScheduler.backupMinutes(this)} min"))
        state.addView(Ui.text(this, "Último origen ${Prefs.lastCycleOrigin(this)} • inicio ${instant(Prefs.lastCycleStarted(this).takeIf { it > 0L })} • fin ${instant(Prefs.lastCycleFinished(this).takeIf { it > 0L })}"))
        root.addView(state)

        root.addView(Ui.section(this, "Contadores trazables"))
        root.addView(Ui.twoColumn(this, Ui.metricCard(this, db.analysisCount().toString(), "Evaluaciones"), Ui.metricCard(this, db.preSignalCount().toString(), "Preseñales")))
        root.addView(Ui.twoColumn(this, Ui.metricCard(this, db.signalCount().toString(), "Señales"), Ui.metricCard(this, db.alertCount().toString(), "Alertas enviadas")))
        val audit = Ui.card(this)
        audit.addView(Ui.text(this, "Ciclos ${stats.cycles} • rechazos ${stats.rejections} • errores ${stats.errors}", 15f, true))
        audit.addView(Ui.text(this, "Duplicados/cooldown ${stats.duplicateSignals} • eventos de seguimiento ${stats.followups}"))
        audit.addView(Ui.text(this, "Integridad: una alerta de señal solo se envía después de persistir señal, snapshot, seguimiento y outbox."))
        root.addView(audit)

        root.addView(Ui.section(this, "Estado individual por criptomoneda"))
        db.assetHealth(Prefs.watchlist(this)).forEach { item ->
            val card = Ui.card(this)
            card.addView(Ui.text(this, item.symbol, 17f, true))
            card.addView(Ui.text(this, item.lastAnalysis?.let { "${item.timeframe} • ${item.state} • ${instant(it)}" } ?: "Sin análisis registrado"))
            if (item.lastError != null) card.addView(Ui.text(this, "Último error ${instant(item.lastErrorAt)}: ${item.lastError}", 12f))
            root.addView(card)
        }

        root.addView(Ui.section(this, "Últimos eventos / motivos exactos"))
        db.diagnostics().forEach { root.addView(Ui.text(this, it, 12f)) }
        setContentView(scroll)
    }

    private fun hasInternet(): Boolean {
        val manager = getSystemService(ConnectivityManager::class.java)
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    override fun onDestroy() { executor.shutdownNow(); super.onDestroy() }
}
