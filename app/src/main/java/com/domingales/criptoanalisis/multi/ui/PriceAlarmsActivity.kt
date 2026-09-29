package com.domingales.criptoanalisis.multi.ui

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.Toast
import com.domingales.criptoanalisis.multi.data.AppDatabase
import com.domingales.criptoanalisis.multi.domain.Assets
import com.domingales.criptoanalisis.multi.service.MonitoringScheduler
import com.domingales.criptoanalisis.multi.util.Prefs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PriceAlarmsActivity : Activity() {
    private lateinit var db: AppDatabase
    private var renderedAlarmState: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = AppDatabase.get(this)
        renderScreen()
    }

    override fun onResume() {
        super.onResume()
        if (::db.isInitialized && alarmStateSignature() != renderedAlarmState) renderScreen()
    }

    private fun renderScreen() {
        val scroll = ScrollView(this); val root = Ui.root(this); scroll.addView(root)
        root.addView(Ui.title(this, "Alarmas de precio"))
        root.addView(Ui.subtitle(this, "La condición debe mantenerse durante D:HH:MM antes de dispararse."))

        val symbols = (Assets.supported.map { it.symbol } + Prefs.catalogSymbols(this) + Prefs.watchlist(this)).distinct().sorted()
        val symbol = Spinner(this).apply { adapter = ArrayAdapter(this@PriceAlarmsActivity, android.R.layout.simple_spinner_dropdown_item, symbols) }
        val condition = Spinner(this).apply { adapter = ArrayAdapter(this@PriceAlarmsActivity, android.R.layout.simple_spinner_dropdown_item, listOf("Mayor o igual", "Menor o igual")) }
        val target = EditText(this).apply { hint = "Valor objetivo, por ejemplo 0.30"; inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL }
        val duration = EditText(this).apply { hint = "D:HH:MM, por ejemplo 0:00:05"; setText("0:00:00") }
        val observation = EditText(this).apply { hint = "Observación opcional" }
        val form = Ui.card(this)
        form.addView(Ui.text(this, "CRIPTO", 13f, true)); form.addView(symbol)
        form.addView(Ui.text(this, "CONDICIÓN", 13f, true)); form.addView(condition)
        form.addView(Ui.text(this, "VALOR", 13f, true)); form.addView(target)
        form.addView(Ui.text(this, "DURACIÓN SOSTENIDA (D:HH:MM)", 13f, true)); form.addView(duration)
        form.addView(Ui.text(this, "OBSERVACIÓN", 13f, true)); form.addView(observation)
        form.addView(Ui.button(this, "CREAR ALARMA") {
            val value = target.text.toString().replace(',', '.').toDoubleOrNull()
            val seconds = parseDuration(duration.text.toString())
            if (value == null || value <= 0.0 || seconds == null) {
                Toast.makeText(this, "Revisa el valor y la duración D:HH:MM", Toast.LENGTH_LONG).show()
            } else {
                db.createPriceAlarm(symbol.selectedItem.toString(), if (condition.selectedItemPosition == 0) "ABOVE" else "BELOW", value, seconds, observation.text.toString())
                MonitoringScheduler.enable(this)
                Toast.makeText(this, "Alarma creada y vigilancia activada", Toast.LENGTH_SHORT).show()
                recreate()
            }
        })
        root.addView(form)

        root.addView(Ui.section(this, "Alarmas registradas"))
        val alarms = db.priceAlarms()
        renderedAlarmState = alarmStateSignature(alarms)
        if (alarms.isEmpty()) root.addView(Ui.subtitle(this, "Todavía no hay alarmas de precio."))
        val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        alarms.forEach { alarm ->
            val card = Ui.card(this)
            val relation = if (alarm.condition == "ABOVE") "≥" else "≤"
            card.addView(Ui.badge(this, "${statusLabel(alarm.status)} • ${alarm.symbol} $relation ${formatPrice(alarm.target)}", Ui.statusColor(alarm.status)))
            card.addView(Ui.text(this, "Duración ${formatDuration(alarm.durationSeconds)} • creada ${date.format(Date(alarm.createdAt))}", 13f))
            alarm.lastPrice?.let { card.addView(Ui.text(this, "Último precio ${formatPrice(it)}", 13f)) }
            if (alarm.status == "ACTIVE" && alarm.armedSince != null) card.addView(Ui.text(this, "Condición iniciada: ${date.format(Date(alarm.armedSince))}", 13f, true))
            if (alarm.observation.isNotBlank()) card.addView(Ui.text(this, alarm.observation, 13f))
            when (alarm.status) {
                "ACTIVE" -> {
                    card.addView(Ui.button(this, "CANCELAR") { db.setPriceAlarmStatus(alarm.id, "CANCELLED"); recreate() })
                    card.addView(Ui.button(this, "MARCAR EXPIRADA") { db.setPriceAlarmStatus(alarm.id, "EXPIRED"); recreate() })
                }
                else -> card.addView(Ui.button(this, "REACTIVAR") { db.setPriceAlarmStatus(alarm.id, "ACTIVE"); MonitoringScheduler.enable(this); recreate() })
            }
            root.addView(card)
        }
        setContentView(scroll)
    }

    private fun alarmStateSignature(alarms: List<AppDatabase.PriceAlarm> = db.priceAlarms()): String =
        alarms.joinToString("|") { alarm ->
            "${alarm.id}:${alarm.status}:${alarm.armedSince}:${alarm.triggeredAt}:${alarm.lastPrice}"
        }

    private fun parseDuration(raw: String): Long? {
        val match = Regex("^(\\d+):(\\d{1,2}):(\\d{1,2})$").matchEntire(raw.trim()) ?: return null
        val days = match.groupValues[1].toLongOrNull() ?: return null
        val hours = match.groupValues[2].toLongOrNull() ?: return null
        val minutes = match.groupValues[3].toLongOrNull() ?: return null
        if (hours !in 0..23 || minutes !in 0..59) return null
        return days * 86_400L + hours * 3_600L + minutes * 60L
    }

    private fun formatDuration(seconds: Long): String {
        val days = seconds / 86_400; val hours = seconds % 86_400 / 3_600; val minutes = seconds % 3_600 / 60
        return "%d:%02d:%02d".format(Locale.ROOT, days, hours, minutes)
    }
    private fun formatPrice(value: Double) = String.format(Locale.ROOT, "%.8f", value).trimEnd('0').trimEnd('.')
    private fun statusLabel(status: String) = when (status) { "ACTIVE" -> "ACTIVA"; "TRIGGERED" -> "DISPARADA"; "CANCELLED" -> "CANCELADA"; "EXPIRED" -> "EXPIRADA"; else -> status }
}
