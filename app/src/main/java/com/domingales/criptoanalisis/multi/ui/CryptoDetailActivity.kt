package com.domingales.criptoanalisis.multi.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.ScrollView
import android.widget.Toast
import com.domingales.criptoanalisis.multi.data.AppDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONArray

class CryptoDetailActivity : Activity() {
    private var pendingExportContent: String? = null

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val symbol = intent.getStringExtra("symbol") ?: "?"
        val s = ScrollView(this); val r = Ui.root(this); s.addView(r)
        val db = AppDatabase.get(this); val m = db.latestAnalysis(symbol)
        r.addView(Ui.title(this, "$symbol • ficha completa"))
        r.addView(Ui.subtitle(this, "Qué ve el motor, por qué decide y qué ocurrió con señales anteriores"))

        if (m == null) {
            r.addView(Ui.subtitle(this, "Todavía no hay análisis para $symbol."))
        } else {
            val analyses = db.recentAnalyses(symbol, 10)
            val signals = db.recentSignals(symbol, 5)
            val explosions = db.explosionEvents(20, symbol)
            val appVersion = try {
                packageManager.getPackageInfo(packageName, 0).versionName ?: "N/D"
            } catch (_: Exception) {
                "N/D"
            }
            val export = CryptoDetailExportFormatter.build(symbol, m, analyses, signals, appVersion)
            r.addView(Ui.button(this, "EXPORTAR FICHA COMPLETA") { showExportOptions(export) })

            fun card(title: String, vararg lines: String) {
                r.addView(Ui.section(this, title)); val c = Ui.card(this); lines.forEach { c.addView(Ui.text(this, it)) }; r.addView(c)
            }
            val direction = m["direction"] ?: "WAIT"; val conf = m["confidence"] ?: "0"; val score = m["score"] ?: "0"
            val state = m["state"] ?: "EVALUATION"
            val color = when { direction.contains("BUY") -> Color.rgb(0,120,70); direction.contains("SELL") -> Color.rgb(180,35,35); else -> Color.rgb(120,90,0) }
            val head = Ui.card(this)
            val decision = when (state) { "SIGNAL" -> "SEÑAL $direction"; "PRESIGNAL" -> "PRESEÑAL $direction"; else -> "NO OPERAR • $direction candidata" }
            head.addView(Ui.badge(this, "$decision • confianza $conf%", color))
            val calibrated = m["probability"]?.takeIf { it != "null" && it != "—" && it.isNotBlank() }?.let { "$it%" } ?: "sin calibrar"
            head.addView(Ui.text(this, "Score técnico $score/100 • Probabilidad $calibrated", 16f, true))
            head.addView(Ui.text(this, "Precio ${m["price"]} • Variación ${m["change_pct"]?.toDoubleOrNull()?.let { "%+.2f%%".format(it) } ?: "N/D"} • ${m["timeframe"]} • Riesgo ${m["risk"]}"))
            head.addView(Ui.text(this, "Régimen ${m["regime"]} • Estructura ${m["structure"]}"))
            r.addView(head)

            explosions.firstOrNull { it.status == "PENDING" }?.let { explosion ->
                val c = Ui.card(this); val bullish = explosion.direction == "BULLISH"
                c.addView(Ui.badge(this, if (bullish) "🚀 POSIBLE EXPLOSIÓN ALCISTA • 5/5" else "📉 POSIBLE EXPLOSIÓN BAJISTA • 5/5", if (bullish) Color.rgb(70,205,130) else Color.rgb(245,95,85)))
                c.addView(Ui.text(this, "Módulo independiente • señal extrema sin calibrar", 13f, true))
                c.addView(Ui.text(this, "${explosion.timeframe} • volumen ${"%.2f".format(explosion.volumeRatio)}x • ATR ${"%.2f".format(explosion.atrRatio)}x • ADX ${"%.1f".format(explosion.adx)} ↑ • MTF ${explosion.alignedTimeframes}/${explosion.availableTimeframes}"))
                r.addView(c)
            }

            r.addView(Ui.section(this, "Gráfico técnico"))
            val chartCard = Ui.card(this)
            val chart = MarketChartView(this)
            chart.setData(db.cachedCandles(symbol, m["timeframe"] ?: "1h", 100), m["support"]?.toDoubleOrNull(), m["resistance"]?.toDoubleOrNull())
            chartCard.addView(chart, android.widget.LinearLayout.LayoutParams(-1, (260 * resources.displayMetrics.density).toInt()))
            chartCard.addView(Ui.text(this, "Línea dorada: cierres • verde: soporte • rojo: resistencia", 12f))
            r.addView(chartCard)

            val forecastLow = m["forecast_low"]?.toDoubleOrNull()?.takeIf { it > 0.0 }
            val forecastHigh = m["forecast_high"]?.toDoubleOrNull()?.takeIf { it > 0.0 }
            val invalidation = m["invalidation"]?.toDoubleOrNull()?.takeIf { it > 0.0 }
            card("Predicción y plan", "Rango esperado: ${if (forecastLow != null && forecastHigh != null) "${"%.2f".format(forecastLow)}% a ${"%.2f".format(forecastHigh)}%" else "N/D"}", "Invalidación: ${invalidation?.let { if (it >= 1000) "%.2f".format(it) else "%.6f".format(it) } ?: "N/D"}", "La invalidación combina estructura y volatilidad, limitada entre 1,15 y 3 ATR.", "La predicción expresa rango/probabilidad, no un precio exacto garantizado.")
            card("Tendencia / momentum / volatilidad",
                "EMA20 ${m["ema20"]} • EMA50 ${m["ema50"]} • EMA200 ${m["ema200"]}",
                "Pendientes EMA20 ${m["ema20_slope"]}% • EMA50 ${m["ema50_slope"]}%",
                "RSI ${m["rsi"]} • ADX ${m["adx"]} • +DI ${m["plus_di"]} • -DI ${m["minus_di"]}",
                "ATR ${m["atr"]} (${m["atr_pct"]}%) • Volumen actual/medio x${m["volume_ratio"]} • expansión ${if (m["volume_expansion"] == "1") "SÍ" else "NO"}",
                "Estructura ${m["structure"]} • Régimen ${m["regime"]}")
            card("Soportes y resistencias",
                "Soporte ${m["support"]} • zona ${m["support_low"]} – ${m["support_high"]}",
                "Resistencia ${m["resistance"]} • zona ${m["resistance_low"]} – ${m["resistance_high"]}")
            card("Análisis multitemporal", "${m["mtf_summary"]}", "Puntuación de alineación MTF: ${m["mtf_score"]}")
            card("Indicador Maestro desglosado", "Tendencia ${m["trend_score"]} • Momentum ${m["momentum_score"]}", "Volumen ${m["volume_score"]} • Estructura ${m["structure_score"]} • S/R ${m["sr_score"]}", "MTF ${m["mtf_component"]} • Volatilidad ${m["volatility_score"]}", "Histórico ${m["historical_score"]} • Contexto BTC ${m["btc_score"]} • Noticias ${m["fundamental_score"]}")
            val historicalProbability = m["hist_probability"]?.takeIf { it != "null" && it != "—" && it.isNotBlank() }?.let { "$it%" } ?: "N/D"
            card("Aprendizaje histórico", "Casos comparables ${m["comparable_cases"]}", "Probabilidad histórica $historicalProbability", "Confianza estadística ${m["stat_confidence"]}")
            card("Auditoría de esta decisión", if (m["rejection"].isNullOrBlank()) "Filtros superados / posible señal" else "NO señal: ${m["rejection"]}", "Explicación del motor:\n${formatExplanation(m["explanation"])}")

            r.addView(Ui.section(this, "Últimas evaluaciones"))
            val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
            analyses.forEach { a ->
                val c = Ui.card(this)
                c.addView(Ui.text(this, "${sdf.format(Date(a.ts))} • ${a.timeframe} • ${a.state}", 13f, true))
                c.addView(Ui.text(this, "${a.direction} • score ${a.score} • confianza ${a.confidence}% • prob. ${a.probability?.let { "$it%" } ?: "N/D"}"))
                c.addView(Ui.text(this, "Régimen ${a.regime}${if (a.rejection.isNotBlank()) " • rechazo: ${a.rejection}" else " • filtros OK"}", 12f))
                r.addView(c)
            }

            r.addView(Ui.section(this, "Seguimiento de señales"))
            if (signals.isEmpty()) r.addView(Ui.subtitle(this, "Todavía no hay señales registradas para $symbol."))
            signals.forEach { x ->
                val c = Ui.card(this)
                val quality = if (x.outcomeQuality == "EXACT_FIRST_TOUCH_5M_V2") "primer toque 5m" else "reclasificación pendiente/heredada"
                c.addView(Ui.text(this, "${x.direction} ${x.confidence}% • ${x.timeframe} • operación ${x.status} • $quality", 15f, true))
                c.addView(Ui.text(this, "Persistencia ${x.persistenceStatus}${x.legacyStatus?.let { " • clasificación anterior $it" } ?: ""} • ${x.outcomeReason.ifBlank { "pendiente" }}", 12f))
                c.addView(Ui.text(this, "Objetivo ${x.targetPrice?.let { "%.6f".format(it) } ?: "N/D"} • stop ${x.stopPrice?.let { "%.6f".format(it) } ?: "N/D"}", 12f))
                c.addView(Ui.text(this, "MFE ${"%.2f".format(x.mfe)}% • MAE ${"%.2f".format(x.mae)}%"))
                x.followups.forEach { f ->
                    val marketTime = f.marketTs?.let { " • vela ${sdf.format(Date(it))}" }.orEmpty()
                    c.addView(Ui.text(this, "${labelHorizon(f.horizonMin)}: ${f.status}${f.movePct?.let { " • ${"%.2f".format(it)}%" } ?: " • pendiente"}$marketTime", 12f))
                }
                r.addView(c)
            }

            r.addView(Ui.section(this, "Posibles explosiones — historial separado"))
            if (explosions.isEmpty()) r.addView(Ui.subtitle(this, "Ninguna evaluación de $symbol ha cumplido todavía las cinco condiciones extremas."))
            explosions.forEach { explosion ->
                val c = Ui.card(this); val bullish = explosion.direction == "BULLISH"
                c.addView(Ui.badge(this, if (bullish) "🚀 POSIBLE EXPLOSIÓN ALCISTA • 5/5" else "📉 POSIBLE EXPLOSIÓN BAJISTA • 5/5", if (bullish) Color.rgb(70,205,130) else Color.rgb(245,95,85)))
                c.addView(Ui.text(this, "${explosion.timeframe} • ${explosion.status} • ${sdf.format(Date(explosion.detectedAt))} • sin calibrar", 14f, true))
                c.addView(Ui.text(this, "Entrada ${explosion.price} • objetivo ${explosion.targetPrice} • invalidación ${explosion.stopPrice}"))
                c.addView(Ui.text(this, "Volumen ${"%.2f".format(explosion.volumeRatio)}x • ATR ${"%.2f".format(explosion.atrRatio)}x • ADX ${"%.1f".format(explosion.adx)} ↑ • MTF ${explosion.alignedTimeframes}/${explosion.availableTimeframes}", 12f))
                runCatching { JSONArray(explosion.conditions) }.getOrNull()?.let { conditions ->
                    for (index in 0 until conditions.length()) c.addView(Ui.text(this, "• ${conditions.optString(index)}", 12f))
                }
                r.addView(c)
            }
            r.addView(Ui.button(this, "VER HISTORIAL COMPLETO DE POSIBLES EXPLOSIONES") { startActivity(Intent(this, ExplosionActivity::class.java)) })
        }
        r.addView(Ui.button(this, "CONFIGURAR $symbol") { startActivity(Intent(this, CryptoConfigActivity::class.java).putExtra("symbol", symbol)) })
        setContentView(s)
    }

    private fun showExportOptions(export: CryptoDetailExportFormatter.ExportPackage) {
        val options = arrayOf(
            "Copiar al portapapeles",
            "Compartir informe",
            "Guardar como TXT",
            "Guardar CSV para Excel",
            "Guardar Word / RTF"
        )
        AlertDialog.Builder(this)
            .setTitle("Exportar ficha completa")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> copyToClipboard(export.text)
                    1 -> shareReport(export.text, export.shareSubject)
                    2 -> createDocument(export.text, "text/plain", "${export.fileBase}.txt", REQUEST_SAVE_TXT)
                    3 -> createDocument(export.csv, "text/csv", "${export.fileBase}.csv", REQUEST_SAVE_CSV)
                    4 -> createDocument(export.rtf, "application/rtf", "${export.fileBase}.rtf", REQUEST_SAVE_RTF)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun copyToClipboard(content: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Ficha completa de CriptoAnálisis Multi", content))
        Toast.makeText(this, "Ficha completa copiada al portapapeles", Toast.LENGTH_SHORT).show()
    }

    private fun shareReport(content: String, subject: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, content)
        }
        startActivity(Intent.createChooser(send, "Compartir ficha completa"))
    }

    private fun createDocument(content: String, mimeType: String, fileName: String, requestCode: Int) {
        pendingExportContent = content
        val create = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeType
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
        startActivityForResult(create, requestCode)
    }

    @Deprecated("Deprecated in Android; retained for compatibility with the project's Activity base class")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode !in setOf(REQUEST_SAVE_TXT, REQUEST_SAVE_CSV, REQUEST_SAVE_RTF)) return
        if (resultCode != RESULT_OK) {
            pendingExportContent = null
            return
        }
        val uri = data?.data
        val content = pendingExportContent
        pendingExportContent = null
        if (uri == null || content == null) {
            Toast.makeText(this, "No se pudo preparar el archivo", Toast.LENGTH_LONG).show()
            return
        }
        try {
            contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8).use { writer ->
                requireNotNull(writer) { "No se pudo abrir el archivo seleccionado" }
                writer.write(content)
            }
            Toast.makeText(this, "Ficha exportada correctamente", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo guardar: ${e.message ?: "error desconocido"}", Toast.LENGTH_LONG).show()
        }
    }
    private fun formatExplanation(raw: String?): String {
        if (raw.isNullOrBlank()) return "Sin explicación disponible"
        return try {
            val values = JSONArray(raw)
            (0 until values.length()).map { values.optString(it) }.filter { it.isNotBlank() }.joinToString("\n") { "• $it" }
        } catch (_: Throwable) {
            raw
        }
    }
    private fun labelHorizon(m: Int) = when (m) { 15 -> "15m"; 30 -> "30m"; 60 -> "1h"; 120 -> "2h"; 240 -> "4h"; 480 -> "8h"; 720 -> "12h"; 1440 -> "24h"; 4320 -> "3d"; 10080 -> "7d"; 20160 -> "14d"; else -> "$m min" }

    companion object {
        private const val REQUEST_SAVE_TXT = 4101
        private const val REQUEST_SAVE_CSV = 4102
        private const val REQUEST_SAVE_RTF = 4103
    }
}
