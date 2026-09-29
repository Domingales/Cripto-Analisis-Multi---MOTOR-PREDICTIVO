package com.domingales.criptoanalisis.multi.ui

import com.domingales.criptoanalisis.multi.data.AppDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONArray

object CryptoDetailExportFormatter {
    data class ExportPackage(
        val text: String,
        val csv: String,
        val rtf: String,
        val fileBase: String,
        val shareSubject: String
    )

    fun build(
        symbol: String,
        analysis: Map<String, String>,
        recentAnalyses: List<AppDatabase.AnalysisAuditRow>,
        recentSignals: List<AppDatabase.SignalDetailRow>,
        appVersion: String
    ): ExportPackage {
        val generatedAt = System.currentTimeMillis()
        val fileStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.ROOT).format(Date(generatedAt))
        val fileSymbol = symbol.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val timeframe = value(analysis, "timeframe")
        val fileTimeframe = timeframe.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val fileBase = "CriptoAnalisisMulti_${fileSymbol}_${fileTimeframe}_$fileStamp"
        val reportRows = rows(symbol, analysis, recentAnalyses, recentSignals, generatedAt, appVersion)

        val text = buildString {
            appendLine("CRIPTOANÁLISIS MULTI — FICHA COMPLETA")
            appendLine("========================================")
            var currentSection = ""
            reportRows.forEach { row ->
                if (row.section != currentSection) {
                    if (currentSection.isNotEmpty()) appendLine()
                    currentSection = row.section
                    appendLine(currentSection.uppercase())
                    appendLine("-".repeat(currentSection.length.coerceAtLeast(3)))
                }
                appendLine("${row.field}: ${row.value}")
            }
            appendLine()
            appendLine("Aviso: la predicción expresa escenarios y probabilidades; no garantiza un precio futuro ni constituye asesoramiento financiero.")
        }

        val csv = buildString {
            append('\uFEFF')
            appendLine("seccion;campo;valor")
            reportRows.forEach { row ->
                append(csvCell(row.section)).append(';')
                    .append(csvCell(row.field)).append(';')
                    .append(csvCell(row.value)).append('\n')
            }
            append(csvCell("Aviso")).append(';')
                .append(csvCell("Limitación")).append(';')
                .append(csvCell("La predicción expresa escenarios y probabilidades; no garantiza un precio futuro ni constituye asesoramiento financiero."))
                .append('\n')
        }

        return ExportPackage(
            text = text,
            csv = csv,
            rtf = plainToRtf(text),
            fileBase = fileBase,
            shareSubject = "Ficha completa $symbol $timeframe — CriptoAnálisis Multi"
        )
    }

    private fun rows(
        symbol: String,
        analysis: Map<String, String>,
        recentAnalyses: List<AppDatabase.AnalysisAuditRow>,
        recentSignals: List<AppDatabase.SignalDetailRow>,
        generatedAt: Long,
        appVersion: String
    ): List<ReportRow> = buildList {
        val fullDate = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val shortDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val analysisTs = analysis["ts"]?.toLongOrNull()
        val state = value(analysis, "state")
        val direction = value(analysis, "direction")
        val decision = when (state) {
            "SIGNAL" -> "SEÑAL $direction"
            "PRESIGNAL" -> "PRESEÑAL $direction"
            else -> "NO OPERAR • $direction candidata"
        }

        add("Identificación", "Criptomoneda", symbol)
        add("Identificación", "Intervalo", value(analysis, "timeframe"))
        add("Identificación", "Fecha del análisis", analysisTs?.let { fullDate.format(Date(it)) } ?: "N/D")
        add("Identificación", "Fecha de exportación", fullDate.format(Date(generatedAt)))
        add("Identificación", "Versión de la aplicación", appVersion)

        add("Decisión actual", "Decisión", decision)
        add("Decisión actual", "Estado interno", state)
        add("Decisión actual", "Precio", value(analysis, "price"))
        add("Decisión actual", "Variación", withSuffix(analysis["change_pct"], "%", "N/D"))
        add("Decisión actual", "Score técnico", "${value(analysis, "score")}/100")
        add("Decisión actual", "Confianza", "${value(analysis, "confidence")}%")
        add("Decisión actual", "Probabilidad", percentageOr(analysis["probability"], "Sin calibrar"))
        add("Decisión actual", "Riesgo", value(analysis, "risk"))
        add("Decisión actual", "Régimen", value(analysis, "regime"))
        add("Decisión actual", "Estructura", value(analysis, "structure"))

        add("Predicción y plan", "Rango esperado mínimo", positivePercentageOr(analysis["forecast_low"], "N/D"))
        add("Predicción y plan", "Rango esperado máximo", positivePercentageOr(analysis["forecast_high"], "N/D"))
        add("Predicción y plan", "Invalidación", valueOr(analysis["invalidation"], "N/D"))
        add("Predicción y plan", "Criterio", "La invalidación combina estructura y volatilidad, limitada entre 1,15 y 3 ATR")

        add("Tendencia, momentum y volatilidad", "RSI", value(analysis, "rsi"))
        add("Tendencia, momentum y volatilidad", "ADX", value(analysis, "adx"))
        add("Tendencia, momentum y volatilidad", "+DI / -DI", "${value(analysis, "plus_di")} / ${value(analysis, "minus_di")}")
        add("Tendencia, momentum y volatilidad", "EMA20 / EMA50 / EMA200", "${value(analysis, "ema20")} / ${value(analysis, "ema50")} / ${value(analysis, "ema200")}")
        add("Tendencia, momentum y volatilidad", "Pendientes EMA20 / EMA50", "${withSuffix(analysis["ema20_slope"], "%", "N/D")} / ${withSuffix(analysis["ema50_slope"], "%", "N/D")}")
        add("Tendencia, momentum y volatilidad", "ATR", value(analysis, "atr"))
        add("Tendencia, momentum y volatilidad", "ATR porcentual", withSuffix(analysis["atr_pct"], "%", "N/D"))
        add("Tendencia, momentum y volatilidad", "Volumen actual/medio", withPrefix(analysis["volume_ratio"], "x", "N/D"))
        add("Tendencia, momentum y volatilidad", "Expansión de volumen", if (analysis["volume_expansion"] == "1") "SÍ" else "NO")
        add("Tendencia, momentum y volatilidad", "Estructura", value(analysis, "structure"))
        add("Tendencia, momentum y volatilidad", "Régimen", value(analysis, "regime"))
        add("Soportes y resistencias", "Soporte", value(analysis, "support"))
        add("Soportes y resistencias", "Zona de soporte", "${value(analysis, "support_low")} – ${value(analysis, "support_high")}")
        add("Soportes y resistencias", "Resistencia", value(analysis, "resistance"))
        add("Soportes y resistencias", "Zona de resistencia", "${value(analysis, "resistance_low")} – ${value(analysis, "resistance_high")}")

        add("Análisis multitemporal", "Resumen MTF", value(analysis, "mtf_summary"))
        add("Análisis multitemporal", "Puntuación de alineación", value(analysis, "mtf_score"))

        add("Indicador Maestro", "Tendencia", value(analysis, "trend_score"))
        add("Indicador Maestro", "Momentum", value(analysis, "momentum_score"))
        add("Indicador Maestro", "Volumen", value(analysis, "volume_score"))
        add("Indicador Maestro", "Estructura", value(analysis, "structure_score"))
        add("Indicador Maestro", "Soporte/Resistencia", value(analysis, "sr_score"))
        add("Indicador Maestro", "MTF", value(analysis, "mtf_component"))
        add("Indicador Maestro", "Volatilidad", value(analysis, "volatility_score"))
        add("Indicador Maestro", "Histórico", value(analysis, "historical_score"))
        add("Indicador Maestro", "Contexto BTC", value(analysis, "btc_score"))
        add("Indicador Maestro", "Noticias/Fundamental", value(analysis, "fundamental_score"))

        add("Aprendizaje histórico", "Casos comparables", value(analysis, "comparable_cases"))
        add("Aprendizaje histórico", "Probabilidad histórica", percentageOr(analysis["hist_probability"], "N/D"))
        add("Aprendizaje histórico", "Confianza estadística", value(analysis, "stat_confidence"))

        add("Auditoría de la decisión", "Filtros", analysis["rejection"].takeUnless { it.isNullOrBlank() }?.let { "NO señal: $it" } ?: "Filtros superados / posible señal")
        val explanation = explanationLines(analysis["explanation"])
        if (explanation.isEmpty()) add("Auditoría de la decisión", "Explicación", "Sin explicación disponible")
        explanation.forEachIndexed { index, line -> add("Auditoría de la decisión", "Evidencia ${index + 1}", line) }

        if (recentAnalyses.isEmpty()) {
            add("Últimas evaluaciones", "Estado", "No hay evaluaciones registradas")
        } else {
            recentAnalyses.forEachIndexed { index, item ->
                val prefix = "Evaluación ${index + 1}"
                add("Últimas evaluaciones", "$prefix — fecha", shortDate.format(Date(item.ts)))
                add("Últimas evaluaciones", "$prefix — intervalo/estado", "${item.timeframe} • ${item.state}")
                add("Últimas evaluaciones", "$prefix — decisión", "${item.direction} • score ${item.score} • confianza ${item.confidence}% • probabilidad ${item.probability?.let { "$it%" } ?: "N/D"}")
                add("Últimas evaluaciones", "$prefix — régimen/filtros", "${item.regime}${if (item.rejection.isBlank()) " • filtros OK" else " • rechazo: ${item.rejection}"}")
            }
        }

        if (recentSignals.isEmpty()) {
            add("Seguimiento de señales", "Estado", "No hay señales registradas para $symbol")
        } else {
            recentSignals.forEachIndexed { index, signal ->
                val prefix = "Señal ${index + 1}"
                add("Seguimiento de señales", "$prefix — UID", signal.uid)
                add("Seguimiento de señales", "$prefix — fecha", shortDate.format(Date(signal.ts)))
                add("Seguimiento de señales", "$prefix — resultado operativo", "${signal.direction} ${signal.confidence}% • ${signal.timeframe} • ${signal.status} • ${signal.outcomeReason.ifBlank { "pendiente" }}")
                add("Seguimiento de señales", "$prefix — persistencia", "${signal.persistenceStatus}${signal.legacyStatus?.let { " • clasificación anterior $it" } ?: ""}")
                add("Seguimiento de señales", "$prefix — objetivo y stop", "${signal.targetPrice ?: "N/D"} / ${signal.stopPrice ?: "N/D"}")
                add("Seguimiento de señales", "$prefix — calidad del resultado", if (signal.outcomeQuality == "EXACT_FIRST_TOUCH_5M_V2") "Primer toque cronológico con velas de 5 minutos posteriores al cierre" else "Reclasificación pendiente o histórico heredado; excluido del aprendizaje V2")
                add("Seguimiento de señales", "$prefix — MFE/MAE", "${formatPct(signal.mfe)} / ${formatPct(signal.mae)}")
                if (signal.snapshot.isEmpty()) {
                    add("Seguimiento de señales", "$prefix — snapshot", "Registro anterior sin snapshot técnico completo")
                } else {
                    add("Seguimiento de señales", "$prefix — RSI/ADX/DMI", "RSI ${snapshot(signal, "rsi")} (pendiente ${snapshot(signal, "rsi_slope")}) • divergencia alcista ${yesNo(signal, "rsi_bull_div")} • divergencia bajista ${yesNo(signal, "rsi_bear_div")} • ADX ${snapshot(signal, "adx")} • +DI ${snapshot(signal, "plus_di")} • -DI ${snapshot(signal, "minus_di")}")
                    add("Seguimiento de señales", "$prefix — ATR/volumen", "ATR ${snapshot(signal, "atr")} (${snapshot(signal, "atr_pct")}%) • volumen x${snapshot(signal, "volume_ratio")}")
                    add("Seguimiento de señales", "$prefix — EMA", "EMA20 ${snapshot(signal, "ema20")} • EMA50 ${snapshot(signal, "ema50")} • EMA200 ${snapshot(signal, "ema200")}")
                    add("Seguimiento de señales", "$prefix — estructura", "${snapshot(signal, "structure")} • régimen ${snapshot(signal, "regime")} • S ${snapshot(signal, "support")} (${snapshot(signal, "support_touches")} contactos, fuerza ${snapshot(signal, "support_strength")}) • R ${snapshot(signal, "resistance")} (${snapshot(signal, "resistance_touches")} contactos, fuerza ${snapshot(signal, "resistance_strength")})")
                    add("Seguimiento de señales", "$prefix — rupturas", "Alcista ${yesNo(signal, "breakout_up")} • bajista ${yesNo(signal, "breakout_down")} • falsa ruptura ${yesNo(signal, "false_breakout")}")
                    add("Seguimiento de señales", "$prefix — MTF/contexto", "MTF ${snapshot(signal, "mtf_score")} (${snapshot(signal, "mtf_summary")}) • BTC ${snapshot(signal, "btc_regime")}")
                    add("Seguimiento de señales", "$prefix — Indicador Maestro", "T ${snapshot(signal, "trend_score")} • M ${snapshot(signal, "momentum_score")} • V ${snapshot(signal, "volume_score")} • E ${snapshot(signal, "structure_score")} • S/R ${snapshot(signal, "sr_score")} • MTF ${snapshot(signal, "mtf_component")} • Vol ${snapshot(signal, "volatility_score")} • Hist ${snapshot(signal, "historical_score")} • BTC ${snapshot(signal, "btc_score")} • Noticias ${snapshot(signal, "fundamental_score")}")
                    add("Seguimiento de señales", "$prefix — aprendizaje original", "${snapshot(signal, "comparable_cases")} casos • ${snapshot(signal, "calibration_wins")} aciertos • ${snapshot(signal, "calibration_losses")} fallos • ${snapshot(signal, "calibration_neutral")} neutros • prob. ${snapshot(signal, "hist_probability")} • confianza ${snapshot(signal, "stat_confidence")}")
                    add("Seguimiento de señales", "$prefix — noticias originales", "${snapshot(signal, "fundamental_headlines")} titulares • ${snapshot(signal, "fundamental_positive")} positivos • ${snapshot(signal, "fundamental_negative")} negativos • ${snapshot(signal, "fundamental_summary")}")
                    add("Seguimiento de señales", "$prefix — configuración original", "Umbral ${snapshot(signal, "decision_threshold")}% • vela cerrada ${yesNo(signal, "closed_candle")} • BUY ${yesNo(signal, "allow_buy")} • SELL ${yesNo(signal, "allow_sell")}")
                    add("Seguimiento de señales", "$prefix — plan original", "Rango ${snapshot(signal, "forecast_low")}%–${snapshot(signal, "forecast_high")}% • invalidación ${snapshot(signal, "invalidation")}")
                }
                if (signal.followups.isEmpty()) add("Seguimiento de señales", "$prefix — seguimientos", "Sin seguimientos registrados")
                signal.followups.forEach { followup ->
                    val move = followup.movePct?.let { " • ${formatPct(it)}" } ?: " • pendiente"
                    val marketTime = followup.marketTs?.let { " • vela ${shortDate.format(Date(it))}" }.orEmpty()
                    add("Seguimiento de señales", "$prefix — ${horizonLabel(followup.horizonMin)}", "${followup.status}$move$marketTime")
                }
            }
        }
    }

    private data class ReportRow(val section: String, val field: String, val value: String)

    private fun MutableList<ReportRow>.add(section: String, field: String, value: String) {
        add(ReportRow(section, field, value))
    }

    private fun value(values: Map<String, String>, key: String) = valueOr(values[key], "N/D")

    private fun valueOr(raw: String?, fallback: String): String =
        raw?.takeIf { it.isNotBlank() && it != "null" && it != "—" } ?: fallback

    private fun percentageOr(raw: String?, fallback: String): String =
        raw?.toDoubleOrNull()?.takeIf { it >= 0.0 }?.let { "${formatNumber(it)}%" } ?: fallback

    private fun positivePercentageOr(raw: String?, fallback: String): String =
        raw?.toDoubleOrNull()?.takeIf { it > 0.0 }?.let { "${formatNumber(it)}%" } ?: fallback

    private fun withSuffix(raw: String?, suffix: String, fallback: String): String =
        valueOr(raw, "").takeIf { it.isNotEmpty() }?.let { "$it$suffix" } ?: fallback

    private fun withPrefix(raw: String?, prefix: String, fallback: String): String =
        valueOr(raw, "").takeIf { it.isNotEmpty() }?.let { "$prefix$it" } ?: fallback

    private fun formatNumber(number: Double): String =
        if (number % 1.0 == 0.0) number.toInt().toString() else String.format(Locale.getDefault(), "%.2f", number)

    private fun formatPct(number: Double) = String.format(Locale.getDefault(), "%.2f%%", number)

    private fun explanationLines(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val values = JSONArray(raw)
            (0 until values.length()).map { values.optString(it) }.filter { it.isNotBlank() }
        } catch (_: Throwable) {
            raw.lines().filter { it.isNotBlank() }
        }
    }

    private fun horizonLabel(minutes: Int) = when (minutes) {
        15 -> "15m"
        30 -> "30m"
        60 -> "1h"
        120 -> "2h"
        240 -> "4h"
        480 -> "8h"
        720 -> "12h"
        1440 -> "24h"
        4320 -> "3d"
        10080 -> "7d"
        20160 -> "14d"
        else -> "$minutes min"
    }

    private fun snapshot(signal: AppDatabase.SignalDetailRow, key: String) = valueOr(signal.snapshot[key], "N/D")

    private fun yesNo(signal: AppDatabase.SignalDetailRow, key: String) = when (signal.snapshot[key]) {
        "1" -> "SÍ"
        "0" -> "NO"
        else -> "N/D"
    }

    private fun plainToRtf(plain: String): String = buildString {
        append("{\\rtf1\\ansi\\deff0{\\fonttbl{\\f0 Arial;}}\\fs20 ")
        plain.forEach { ch ->
            when (ch) {
                '\\' -> append("\\\\")
                '{' -> append("\\{")
                '}' -> append("\\}")
                '\n' -> append("\\par\n")
                '\r' -> Unit
                else -> if (ch.code in 32..126) append(ch) else {
                    val signedCode = if (ch.code > 32767) ch.code - 65536 else ch.code
                    append("\\u$signedCode?")
                }
            }
        }
        append('}')
    }

    private fun csvCell(value: String): String = "\"${value.replace("\"", "\"\"").replace("\r\n", "\n").replace('\r', '\n')}\""
}
