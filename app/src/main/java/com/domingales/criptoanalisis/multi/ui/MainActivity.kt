package com.domingales.criptoanalisis.multi.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.ScrollView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import com.domingales.criptoanalisis.multi.data.AppDatabase
import com.domingales.criptoanalisis.multi.service.MonitoringScheduler
import com.domingales.criptoanalisis.multi.service.MarketWatchService
import com.domingales.criptoanalisis.multi.service.NotificationHelper
import com.domingales.criptoanalisis.multi.util.Prefs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.applyV371MenuOnRight(this)
        NotificationHelper.createChannels(this)
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 99)
        }
        if (Prefs.watching(this)) MonitoringScheduler.reconfigure(this)
    }
    override fun onResume() { super.onResume(); render() }

    private fun render() {
        val scroll = ScrollView(this); val root = Ui.root(this); scroll.addView(root)
        val db = AppDatabase.get(this); val active = Prefs.watching(this); val items = db.dashboard(); val stats = db.diagnosticStats()

        root.addView(Ui.title(this, "CriptoAnálisis Multi 3.8.0"))
        root.addView(Ui.subtitle(this, "Central de vigilancia, análisis, predicción y auditoría multcripto"))

        val service = Ui.card(this)
        service.addView(Ui.text(this, if (active) "● MOTOR DE VIGILANCIA ACTIVO" else "○ MOTOR DE VIGILANCIA DETENIDO", 18f, true).apply {
            setTextColor(if (active) Color.rgb(0, 125, 70) else Color.rgb(150, 45, 45))
        })
        val remote = Prefs.remoteEndpoint(this).isNotBlank()
        service.addView(Ui.text(this, "${Prefs.watchlist(this).size} criptos • ${Prefs.timeframe(this)} base • umbral ${Prefs.threshold(this)}% • local ${MonitoringScheduler.effectiveLocalMinutes(this)} min"))
        service.addView(Ui.text(this, "Servicio permanente: ${if (Prefs.serviceResponsive(this)) "RESPONDIENDO" else if (active) "INICIANDO/RECUPERANDO" else "DETENIDO"}${if (remote) " • respaldo remoto configurado" else " • remoto pendiente"}", 13f))
        service.addView(Ui.text(this, "Modo ${if (Prefs.closedCandleMode(this)) "VELA CERRADA" else "INTRAVELA"} • cooldown ${Prefs.cooldownMinutes(this)} min • noticias ${if (Prefs.newsEnabled(this)) "ON" else "OFF"}", 13f))
        service.addView(Ui.button(this, if (active) "DETENER VIGILANCIA" else "ACTIVAR VIGILANCIA") {
            if (active) { MonitoringScheduler.disable(this); Toast.makeText(this, "Vigilancia detenida", Toast.LENGTH_SHORT).show() }
            else { MonitoringScheduler.enable(this); Toast.makeText(this, "Vigilancia permanente activada", Toast.LENGTH_SHORT).show() }
            render()
        })
        val batteryUnrestricted = getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)
        service.addView(Ui.text(this, "Batería: ${if (batteryUnrestricted) "SIN RESTRICCIONES" else "REVISA EL AHORRO DE ENERGÍA"}", 13f, true))
        service.addView(Ui.button(this, "AJUSTES DE BATERÍA") {
            try {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (_: Throwable) {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
            }
        })
        service.addView(Ui.button(this, "ANALIZAR AHORA") {
            startForegroundService(Intent(this, MarketWatchService::class.java).setAction(MarketWatchService.ACTION_MANUAL))
            Toast.makeText(this, "Análisis manual iniciado", Toast.LENGTH_SHORT).show()
        })
        root.addView(service)

        root.addView(Ui.section(this, "Actividad del motor"))
        root.addView(Ui.twoColumn(this,
            Ui.metricCard(this, db.analysisCount().toString(), "Evaluaciones"),
            Ui.metricCard(this, db.preSignalCount().toString(), "Preseñales")
        ))
        root.addView(Ui.twoColumn(this,
            Ui.metricCard(this, db.signalCount().toString(), "Señales"),
            Ui.metricCard(this, db.alertCount().toString(), "Alertas")
        ))
        root.addView(Ui.twoColumn(this,
            Ui.metricCard(this, db.activePriceAlarmCount().toString(), "Alarmas de precio activas"),
            Ui.metricCard(this, items.count { it.activeFollowups > 0 }.toString(), "Criptos en seguimiento")
        ))
        val explosionSummary = db.explosionSummary()
        root.addView(Ui.twoColumn(this,
            Ui.metricCard(this, explosionSummary.total.toString(), "Explosiones 5/5"),
            Ui.metricCard(this, explosionSummary.pending.toString(), "Explosiones pendientes")
        ))
        val audit = Ui.card(this)
        audit.addView(Ui.text(this, "Auditoría rápida", 16f, true))
        audit.addView(Ui.text(this, "Ciclos completados: ${stats.cycles} • Rechazos explicados: ${stats.rejections}"))
        audit.addView(Ui.text(this, "Errores: ${stats.errors} • Duplicados/cooldown nuevos: ${stats.duplicateSignals} • Eventos seguimiento: ${stats.followups}"))
        root.addView(audit)

        root.addView(Ui.section(this, "Radar multitemporal / oportunidades"))
        val sortLabels = listOf("Confianza", "Variación", "Nombre", "Señal", "Última alerta")
        val sortKeys = listOf("CONFIDENCE", "CHANGE", "NAME", "SIGNAL", "ALERT")
        val sortRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val sortSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, sortLabels)
            setSelection(sortKeys.indexOf(Prefs.dashboardSort(this@MainActivity)).coerceAtLeast(0))
        }
        sortRow.addView(sortSpinner, LinearLayout.LayoutParams(0, -2, 1f))
        sortRow.addView(Ui.button(this, "ORDENAR") { Prefs.setDashboardSort(this, sortKeys[sortSpinner.selectedItemPosition]); render() })
        root.addView(sortRow)
        if (items.isEmpty()) root.addView(Ui.subtitle(this, "Todavía no hay evaluaciones. Activa la vigilancia para poblar el radar."))
        val sdf = SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault())
        val ordered = when (Prefs.dashboardSort(this)) {
            "CHANGE" -> items.sortedByDescending { it.changePct }
            "NAME" -> items.sortedBy { it.symbol }
            "SIGNAL" -> items.sortedWith(compareByDescending<com.domingales.criptoanalisis.multi.domain.DashboardItem> { it.state == "SIGNAL" }.thenByDescending { it.confidence })
            "ALERT" -> items.sortedByDescending { it.lastAlertAt ?: 0L }
            else -> items.sortedByDescending { it.confidence }
        }
        ordered.take(12).forEach { item ->
            val buy = item.direction.contains("BUY"); val sell = item.direction.contains("SELL")
            val signalColor = when { item.activeFollowups > 0 -> Ui.statusColor("FOLLOWING"); item.state == "SIGNAL" && buy -> Color.rgb(70, 205, 130); item.state == "SIGNAL" && sell -> Color.rgb(245, 95, 85); item.state == "PRESIGNAL" -> Ui.statusColor("PRESIGNAL"); else -> Color.LTGRAY }
            val card = Ui.card(this)
            val decision = when (item.state) { "SIGNAL" -> item.direction.replace('_',' '); "PRESIGNAL" -> "PRESEÑAL ${item.direction.replace('_',' ')}"; else -> "NO OPERAR • ${item.direction.replace('_',' ')} candidata" }
            card.addView(Ui.badge(this, "${traffic(item.direction, item.state, item.activeFollowups)} ${item.symbol} • $decision • ${item.confidence}%", signalColor))
            card.addView(Ui.text(this, "Score ${item.score}/100 • Probabilidad ${item.probability?.let { "$it%" } ?: "sin calibrar"} • Riesgo ${item.risk}", 14f, true))
            card.addView(Ui.text(this, "${item.timeframe} • Régimen ${item.regime} • Precio ${formatPrice(item.price)} • Variación ${"%+.2f".format(item.changePct)}%", 13f))
            db.latestPendingExplosion(item.symbol, item.timeframe)?.let { explosion ->
                val bullishExplosion = explosion.direction == "BULLISH"
                card.addView(Ui.badge(this, if (bullishExplosion) "🚀 POSIBLE EXPLOSIÓN ALCISTA • 5/5 • SIN CALIBRAR" else "📉 POSIBLE EXPLOSIÓN BAJISTA • 5/5 • SIN CALIBRAR", if (bullishExplosion) Color.rgb(70,205,130) else Color.rgb(245,95,85)))
                card.addView(Ui.text(this, "Volumen ${"%.2f".format(explosion.volumeRatio)}x • ATR ${"%.2f".format(explosion.atrRatio)}x • ADX ${"%.1f".format(explosion.adx)} ↑ • MTF ${explosion.alignedTimeframes}/${explosion.availableTimeframes}", 12f))
            }
            if (item.activeFollowups > 0) card.addView(Ui.text(this, "🟣 Señal en seguimiento • ${item.activeFollowups} revisiones pendientes", 13f, true))
            card.addView(Ui.text(this, "Última evaluación ${sdf.format(Date(item.updatedAt))}", 12f))
            item.lastAlertAt?.let { card.addView(Ui.text(this, "Última alerta ${sdf.format(Date(it))}", 12f)) }
            card.setOnClickListener { startActivity(Intent(this, CryptoDetailActivity::class.java).putExtra("symbol", item.symbol)) }
            root.addView(card)
        }
        root.addView(Ui.button(this, "VER TODAS LAS OPORTUNIDADES") { startActivity(Intent(this, OpportunitiesActivity::class.java)) })

        setContentView(scroll)
    }

    private fun traffic(d: String, state: String, activeFollowups: Int) = when { activeFollowups > 0 -> "🟣"; state == "SIGNAL" && d.contains("BUY") -> "🟢"; state == "SIGNAL" && d.contains("SELL") -> "🔴"; state == "PRESIGNAL" -> "🟡"; else -> "⚪" }
    private fun formatPrice(v: Double) = if (v >= 1000) "%.2f".format(v) else "%.6f".format(v)
}
