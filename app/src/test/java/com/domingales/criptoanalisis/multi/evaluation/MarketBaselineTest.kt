package com.domingales.criptoanalisis.multi.evaluation

import com.domingales.criptoanalisis.multi.domain.BacktestEngine
import com.domingales.criptoanalisis.multi.domain.Candle
import com.domingales.criptoanalisis.multi.domain.OutcomeStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Locale

/** Usa el motor de producción y las velas guardadas; no genera pronósticos inventados. */
class MarketBaselineTest {
    @Test fun auditAdaOneHour() {
        val input = System.getenv("MARKET_DATA_DIR")?.let(::File) ?: return
        val output = File(System.getenv("MARKET_REPORT_DIR") ?: error("MARKET_REPORT_DIR no definido"))
        val series = mapOf("1h" to read(input, "ADA_1h"), "4h" to read(input, "ADA_4h"),
            "1d" to read(input, "ADA_1d"))
        val fine = read(input, "ADA_5m")
        val btc = read(input, "BTC_4h")
        val result = BacktestEngine.run("ADA", "1h", series, threshold = 81,
            btcCandles4h = btc, outcomeCandles5m = fine)
        assertTrue("No hay ventanas históricas válidas", result.evaluatedWindows > 0)
        assertEquals("Faltan velas de 5 minutos para alguna señal", 0, result.missingFiveMinuteData)
        assertEquals(result.signals, result.cases.count { it.outcome != OutcomeStatus.PENDING })
        output.mkdirs()
        val ordered = result.cases.sortedBy { it.signalCloseTime }
        File(output, "ada_1h_casos.csv").printWriter().use { writer ->
            writer.println("symbol,timeframe,signalCloseTime,direction,entry,confidence,probability,targetPct,outcome,outcomeTime,reason")
            ordered.forEach { c ->
                writer.println(listOf(c.symbol, c.timeframe, c.signalCloseTime, c.direction, c.entry,
                    c.confidence, c.historicalProbability ?: "", c.targetPct, c.outcome,
                    c.outcomeTime ?: "", c.outcomeReason).joinToString(","))
            }
        }
        val cutoff = if (ordered.isEmpty()) 0L else ordered.first().signalCloseTime +
            (ordered.last().signalCloseTime - ordered.first().signalCloseTime) * 7 / 10
        val holdout = ordered.filter { it.signalCloseTime >= cutoff }
        File(output, "ada_1h_resumen.txt").writeText(buildString {
            appendLine("Versión base: 3.8.0-resultados-predictivos-v2; ADA 1h; umbral 81; Binance Spot USDT")
            appendLine("Velas: cerradas; resolución: primer toque con velas 5m; noticias históricas: ausentes")
            appendLine("Ventanas evaluadas: ${result.evaluatedWindows}; omitidas sin MTF: ${result.skippedWithoutMtf}")
            appendLine("Señales: ${result.signals}; HIT: ${result.hits}; FAIL: ${result.fails}; NEUTRAL: ${result.neutral}")
            appendLine("BUY: ${result.buy.signals}, HIT ${result.buy.hits}, FAIL ${result.buy.fails}, NEUTRAL ${result.buy.neutral}")
            appendLine("SELL: ${result.sell.signals}, HIT ${result.sell.hits}, FAIL ${result.sell.fails}, NEUTRAL ${result.sell.neutral}")
            appendLine("Precisión resueltas: ${accuracy(result.hits, result.fails)}; cobertura: ${ratio(result.signals, result.evaluatedWindows)}")
            appendLine("Último 30% cronológico (corte $cutoff): ${holdout.size} señales, HIT ${holdout.count { it.outcome == OutcomeStatus.HIT }}, FAIL ${holdout.count { it.outcome == OutcomeStatus.FAIL }}, NEUTRAL ${holdout.count { it.outcome == OutcomeStatus.NEUTRAL }}")
            appendLine("Precisión último tramo: ${accuracy(holdout.count { it.outcome == OutcomeStatus.HIT }, holdout.count { it.outcome == OutcomeStatus.FAIL })}")
            appendLine("Este último tramo es una descripción histórica, no validación prospectiva: el código y umbrales ya existían antes de esta auditoría.")
        })
        println(File(output, "ada_1h_resumen.txt").readText())
    }

    private fun read(dir: File, basename: String): List<Candle> {
        val file = File(dir, "$basename.csv")
        require(file.isFile) { "Falta $file" }
        return file.useLines { lines -> lines.drop(1).map { line ->
            val v = line.split(',')
            require(v.size == 7) { "CSV incorrecto: $basename" }
            Candle(v[0].toLong(), v[1].toDouble(), v[2].toDouble(), v[3].toDouble(),
                v[4].toDouble(), v[5].toDouble(), v[6].toLong())
        }.toList() }
    }
    private fun accuracy(hits: Int, fails: Int): String = if (hits + fails == 0) "sin muestra resuelta"
        else String.format(Locale.US, "%.1f%%", 100.0 * hits / (hits + fails))
    private fun ratio(n: Int, d: Int): String = if (d == 0) "sin ventanas"
        else String.format(Locale.US, "%.2f%%", 100.0 * n / d)
}
