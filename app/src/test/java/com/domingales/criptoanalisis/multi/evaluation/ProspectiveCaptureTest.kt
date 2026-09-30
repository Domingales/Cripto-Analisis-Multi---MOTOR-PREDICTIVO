package com.domingales.criptoanalisis.multi.evaluation

import com.domingales.criptoanalisis.multi.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.Base64

/** Replays known history for calibration, then records only the latest decision. */
class ProspectiveCaptureTest {
    @Test fun captureLatestClosedDecision() {
        val input = System.getenv("PROSPECTIVE_DATA_DIR")?.let(::File) ?: return
        val output = File(System.getenv("PROSPECTIVE_REPORT_DIR") ?: error("Missing output"))
        fun read(name: String) = File(input, "$name.csv").readLines().drop(1).map { line ->
            val v = line.split(',')
            Candle(v[0].toLong(), v[1].toDouble(), v[2].toDouble(), v[3].toDouble(),
                v[4].toDouble(), v[5].toDouble(), v[6].toLong())
        }
        val series = mapOf("1h" to read("ADA_1h"), "4h" to read("ADA_4h"), "1d" to read("ADA_1d"))
        var latest: AnalysisResult? = null
        var acceptedAt = -1L
        BacktestEngine.run("ADA", "1h", series, 81, btcCandles4h = read("BTC_4h"),
            outcomeCandles5m = read("ADA_5m"), auditThroughLatest = true,
            onEvaluation = { latest = it }, onAccepted = { acceptedAt = it.candleCloseTime })
        val a = requireNotNull(latest)
        assertEquals(series.getValue("1h").last().closeTime, a.candleCloseTime)
        assertTrue(a.closedCandleEvaluation)
        val recordedAt = System.currentTimeMillis()
        // Explicitly delayed cohort: never count market movement before recording.
        val firstOpen = (recordedAt / 300_000L + 1L) * 300_000L
        val snapshot = Base64.getEncoder().encodeToString(a.toString().toByteArray(Charsets.UTF_8))
        output.mkdirs()
        File(output, "decision.tsv").writeText(listOf(a.candleCloseTime, recordedAt,
            acceptedAt == a.candleCloseTime, a.direction, a.indicators.price,
            OutcomeTracker.movementThresholdPct(a.indicators.price, a.indicators.atr14),
            firstOpen, firstOpen + 86_400_000L - 1L, snapshot).joinToString("\t") + "\n")
    }
}
