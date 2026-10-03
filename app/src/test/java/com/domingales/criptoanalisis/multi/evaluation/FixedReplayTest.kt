package com.domingales.criptoanalisis.multi.evaluation

import com.domingales.criptoanalisis.multi.domain.BacktestEngine
import com.domingales.criptoanalisis.multi.domain.Candle
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.max

/** Research adapter: chronological decisions are flushed before outcome resolution. */
class FixedReplayTest {
    private fun series(dir: File, name: String): List<Candle> = File(dir, "$name.csv").useLines { lines ->
        lines.drop(1).map { line ->
            val v = line.split(',')
            Candle(v[0].toLong(), v[1].toDouble(), v[2].toDouble(), v[3].toDouble(),
                v[4].toDouble(), v[5].toDouble(), v[6].toLong())
        }.toList()
    }
    @Test fun emitFixedKotlinDecisions() {
        val dir = System.getenv("REPLAY_DATA_DIR")?.let(::File) ?: return
        val output = File(System.getenv("REPLAY_OUTPUT_DIR") ?: error("REPLAY_OUTPUT_DIR"))
        output.mkdirs()
        val all = mapOf("1h" to series(dir, "ADAUSDT_1h"), "4h" to series(dir, "ADAUSDT_4h"),
            "1d" to series(dir, "ADAUSDT_1d"))
        val btc = series(dir, "BTCUSDT_4h")
        val fine = series(dir, "ADAUSDT_5m")
        val steps = mapOf("1h" to 3600000L, "4h" to 14400000L, "1d" to 86400000L)
        // Precompute validity only from each series' closed 300-candle lookback.
        fun valid(series: List<Candle>, step: Long): Set<Long> = series.indices.filter { i ->
            i >= 209 && (max(0, i-299)..i).all { j ->
                series[j].closeTime == series[j].openTime+step-1 &&
                    (j == max(0, i-299) || series[j].openTime == series[j-1].openTime+step)
            }
        }.map { series[it].closeTime }.toSet()
        val validTimes = all.mapValues { (tf, candles) -> valid(candles, steps.getValue(tf)) }
        val btcValid = valid(btc, 14400000L)
        fun latest(candles: List<Candle>, t: Long): Long? {
            var low = 0; var high = candles.size
            while (low < high) { val m = (low+high)/2; if (candles[m].closeTime <= t) low=m+1 else high=m }
            return candles.getOrNull(low-1)?.closeTime
        }
        var excluded = 0
        File(output, "kotlin_decisions.csv").printWriter().use { decisions ->
            File(output, "kotlin_accepted.csv").printWriter().use { accepted ->
                decisions.println("signalCloseTime,direction,entry,targetPct,confidence,probability,isSignal,regime,atrPct,volumeRatio,adx,rsi,mtfScore,comparableCases,rejectionReason")
                accepted.println("signalCloseTime")
                val result = BacktestEngine.run("ADA", "1h", all, threshold=81, btcCandles4h=btc,
                    outcomeCandles5m=fine, auditThroughLatest=true,
                    evaluationEligible={ t ->
                        val eligible = all.all { (tf, candles) -> (latest(candles, t) == ((t+1)/steps.getValue(tf))*steps.getValue(tf)-1 && latest(candles, t) in validTimes.getValue(tf)) } &&
                            (latest(btc, t) == ((t+1)/14400000L)*14400000L-1 && latest(btc, t) in btcValid)
                        if (!eligible) excluded++
                        eligible
                    },
                    onEvaluation={ a ->
                        decisions.println(listOf(a.candleCloseTime, a.direction, a.indicators.price,
                            max(0.45, a.indicators.atrPct*0.55), a.confidence, a.probability ?: "",
                            a.isSignal, a.indicators.regime, a.indicators.atrPct, a.indicators.volumeRatio,
                            a.indicators.adx14, a.indicators.rsi14, a.mtf.alignmentScore,
                            a.calibration.comparableCases).joinToString(",") + ",\"" +
                            a.rejectionReason.replace("\"", "\"\"") + "\"")
                        decisions.flush()
                    }, onAccepted={ a -> accepted.println(a.candleCloseTime); accepted.flush() })
                assertTrue("No chronological windows", result.evaluatedWindows > 0)
                File(output, "kotlin_summary.txt").writeText("evaluated=${result.evaluatedWindows}\nexcluded_context=$excluded\nmissing_5m=${result.missingFiveMinuteData}\nfixed_code_known_resolved_patterns=true\n")
            }
        }
    }
}
