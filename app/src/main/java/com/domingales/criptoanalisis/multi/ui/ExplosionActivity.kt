package com.domingales.criptoanalisis.multi.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.ScrollView
import android.widget.Toast
import com.domingales.criptoanalisis.multi.data.AppDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExplosionActivity : Activity() {
    private var pendingContent: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    private fun render() {
        val scroll = ScrollView(this); val root = Ui.root(this); scroll.addView(root)
        val db = AppDatabase.get(this); val summary = db.explosionSummary(); val events = db.explosionEvents()
        root.addView(Ui.title(this, "Detector de posibles explosiones"))
        root.addView(Ui.subtitle(this, "Módulo independiente. No modifica señales, porcentajes, aprendizaje, alarmas ni el umbral general."))

        root.addView(Ui.twoColumn(this, Ui.metricCard(this, summary.total.toString(), "Detectadas 5/5"), Ui.metricCard(this, summary.pending.toString(), "Pendientes")))
        root.addView(Ui.twoColumn(this, Ui.metricCard(this, summary.hits.toString(), "Aciertos"), Ui.metricCard(this, summary.fails.toString(), "Fallos")))
        root.addView(Ui.twoColumn(this, Ui.metricCard(this, summary.neutral.toString(), "Neutras"), Ui.metricCard(this, if (summary.hits + summary.fails == 0) "—" else "%.1f%%".format(summary.accuracy), "Precisión resuelta")))

        val info = Ui.card(this)
        info.addView(Ui.text(this, "Criterio estricto 5/5", 16f, true))
        info.addView(Ui.text(this, "Volumen ≥2x • cierre fuera del nivel • ATR ≥1,10x y creciendo • ADX ≥25 y creciendo con DMI coherente • al menos 3 intervalos alineados sin contradicción."))
        info.addView(Ui.text(this, "Resultado propio: +1 ATR antes de −1 ATR dentro del horizonte. Si ambos niveles se tocan en la misma vela, se clasifica NEUTRA. Hasta disponer de suficientes casos aparece como sin calibrar.", 13f))
        root.addView(info)

        root.addView(Ui.section(this, "Exportación independiente"))
        root.addView(Ui.button(this, "COPIAR / PEGAR JSON") { copyJson(events) })
        root.addView(Ui.button(this, "GUARDAR HISTORIAL TXT") { save(buildText(events), "text/plain", "CriptoAnalisisMulti_explosiones.txt", REQUEST_TXT) })
        root.addView(Ui.button(this, "GUARDAR HISTORIAL CSV / EXCEL") { save(buildCsv(events), "text/csv", "CriptoAnalisisMulti_explosiones.csv", REQUEST_CSV) })

        root.addView(Ui.section(this, "Historial independiente"))
        if (events.isEmpty()) root.addView(Ui.subtitle(this, "Todavía no existe ninguna detección que haya cumplido las cinco condiciones."))
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        events.forEach { event ->
            val bullish = event.direction == "BULLISH"
            val card = Ui.card(this)
            card.addView(Ui.badge(this, if (bullish) "🚀 POSIBLE EXPLOSIÓN ALCISTA • 5/5" else "📉 POSIBLE EXPLOSIÓN BAJISTA • 5/5", if (bullish) Color.rgb(70, 205, 130) else Color.rgb(245, 95, 85)))
            card.addView(Ui.text(this, "${event.symbol} • ${event.timeframe} • ${sdf.format(Date(event.detectedAt))} • ${statusLabel(event.status)}", 15f, true))
            card.addView(Ui.text(this, "Entrada ${price(event.price)} • objetivo ${price(event.targetPrice)} • invalidación ${price(event.stopPrice)}"))
            card.addView(Ui.text(this, "Volumen ${"%.2f".format(event.volumeRatio)}x • ATR ${"%.2f".format(event.atrRatio)}x • ADX ${"%.1f".format(event.adx)} ↑ desde ${"%.1f".format(event.previousAdx)} • MTF ${event.alignedTimeframes}/${event.availableTimeframes}"))
            card.addView(Ui.text(this, "Ruptura ${price(event.breakoutLevel)} • MFE ${"%.2f".format(event.mfe)}% • MAE ${"%.2f".format(event.mae)}% • señal extrema sin calibrar", 13f))
            runCatching { JSONArray(event.conditions) }.getOrNull()?.let { conditions ->
                for (index in 0 until conditions.length()) card.addView(Ui.text(this, "• ${conditions.optString(index)}", 12f))
            }
            card.setOnClickListener { startActivity(Intent(this, CryptoDetailActivity::class.java).putExtra("symbol", event.symbol)) }
            root.addView(card)
        }
        setContentView(scroll)
    }

    private fun copyJson(events: List<AppDatabase.ExplosionEvent>) {
        val array = JSONArray()
        events.forEach { event ->
            array.put(JSONObject().apply {
                put("uid", event.uid); put("cripto", event.symbol); put("intervalo", event.timeframe); put("detectada", event.detectedAt)
                put("direccion", event.direction); put("estado", event.status); put("precio", event.price); put("objetivo", event.targetPrice); put("invalidacion", event.stopPrice)
                put("volumen_ratio", event.volumeRatio); put("atr_ratio", event.atrRatio); put("adx", event.adx); put("adx_anterior", event.previousAdx)
                put("mtf_alineados", event.alignedTimeframes); put("mtf_disponibles", event.availableTimeframes); put("nivel_ruptura", event.breakoutLevel)
                put("mfe", event.mfe); put("mae", event.mae); put("condiciones", runCatching { JSONArray(event.conditions) }.getOrElse { JSONArray() })
            })
        }
        val json = JSONObject().put("modulo", "POSIBLES_EXPLOSIONES_INDEPENDIENTE").put("calibracion", "SIN_CALIBRAR").put("eventos", array).toString(2)
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Posibles explosiones", json))
        Toast.makeText(this, "JSON copiado al portapapeles", Toast.LENGTH_SHORT).show()
    }

    private fun buildText(events: List<AppDatabase.ExplosionEvent>) = buildString {
        appendLine("CRIPTOANÁLISIS MULTI — POSIBLES EXPLOSIONES")
        appendLine("Módulo independiente • señal extrema sin calibrar")
        events.forEach { e -> appendLine("${e.symbol} ${e.timeframe} | ${e.direction} | ${e.status} | entrada=${e.price} | objetivo=${e.targetPrice} | stop=${e.stopPrice} | volumen=${e.volumeRatio}x | ATR=${e.atrRatio}x | ADX=${e.adx} | MTF=${e.alignedTimeframes}/${e.availableTimeframes} | MFE=${e.mfe}% | MAE=${e.mae}%") }
    }

    private fun buildCsv(events: List<AppDatabase.ExplosionEvent>) = buildString {
        appendLine("uid;cripto;intervalo;fecha_ms;direccion;estado;precio;objetivo;invalidacion;volumen_ratio;atr_ratio;adx;adx_anterior;mtf_alineados;mtf_disponibles;nivel_ruptura;mfe;mae")
        events.forEach { e -> appendLine(listOf(e.uid,e.symbol,e.timeframe,e.detectedAt,e.direction,e.status,e.price,e.targetPrice,e.stopPrice,e.volumeRatio,e.atrRatio,e.adx,e.previousAdx,e.alignedTimeframes,e.availableTimeframes,e.breakoutLevel,e.mfe,e.mae).joinToString(";")) }
    }

    private fun save(content: String, mime: String, name: String, request: Int) {
        pendingContent = content
        startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = mime; putExtra(Intent.EXTRA_TITLE, name) }, request)
    }

    @Deprecated("Compatibilidad con Activity")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode !in setOf(REQUEST_TXT, REQUEST_CSV) || resultCode != RESULT_OK) return
        val content = pendingContent ?: return; val uri = data?.data ?: return
        runCatching {
            val output = requireNotNull(contentResolver.openOutputStream(uri, "wt")) { "No se pudo abrir el archivo de destino" }
            output.bufferedWriter(Charsets.UTF_8).use { it.write(content) }
        }
            .onSuccess { Toast.makeText(this, "Historial guardado", Toast.LENGTH_SHORT).show() }
            .onFailure { Toast.makeText(this, "No se pudo guardar: ${it.message}", Toast.LENGTH_LONG).show() }
        pendingContent = null
    }

    private fun statusLabel(status: String) = when (status) { "PENDING" -> "PENDIENTE"; "HIT" -> "ACIERTO"; "FAIL" -> "FALLO"; else -> "NEUTRA" }
    private fun price(value: Double) = String.format(Locale.ROOT, "%.8f", value).trimEnd('0').trimEnd('.')

    companion object { private const val REQUEST_TXT = 7401; private const val REQUEST_CSV = 7402 }
}
