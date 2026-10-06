package com.domingales.criptoanalisis.multi.evaluation

import com.domingales.criptoanalisis.multi.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.Base64
import kotlin.math.max

/** Separate research cohort: only the latest newly closed candle is recorded. */
class SelectedProspectiveTest {
    @Test fun captureSelectedClosedDecisions() {
        val input=System.getenv("SELECTED_DATA_DIR")?.let(::File) ?: return
        val output=File(System.getenv("SELECTED_OUTPUT_DIR") ?: error("Missing output"))
        val pairs=(System.getenv("SELECTED_PAIRS") ?: error("Missing pairs")).split(',').map { it.split(':') }
        val cache=mutableMapOf<String,List<Candle>>()
        fun read(name: String): List<Candle> = cache.getOrPut(name) {
            File(input,"$name.csv").useLines { lines -> lines.drop(1).map { line ->
                val v=line.split(','); Candle(v[0].toLong(),v[1].toDouble(),v[2].toDouble(),v[3].toDouble(),v[4].toDouble(),v[5].toDouble(),v[6].toLong())
            }.toList() }
        }
        output.mkdirs()
        File(output,"decisions.tsv").printWriter().use { writer ->
            for ((symbol,tf) in pairs) {
                val all=linkedSetOf("15m",tf,"4h","1d").associateWith { read(symbol+"USDT_"+it) }
                var latest: AnalysisResult?=null; var acceptedAt=-1L
                ExpandedResearchEngine.run(symbol,tf,all,81,btcCandles4h=read("BTCUSDT_4h"),
                    outcomeCandles5m=read(symbol+"USDT_5m"),auditThroughLatest=true,
                    onEvaluation={latest=it},onAccepted={acceptedAt=it.candleCloseTime})
                val a=requireNotNull(latest); assertEquals(all.getValue(tf).last().closeTime,a.candleCloseTime)
                assertTrue(a.closedCandleEvaluation)
                val recorded=System.currentTimeMillis()
                val price=read(symbol+"USDT_5m").last()
                require(price.closeTime<=recorded && recorded-price.closeTime<60*60_000L) { "Stale price" }
                val first=(recorded/300_000L+1)*300_000L
                val end=first+OutcomeTracker.outcomeWindowMinutes(tf)*60_000L-1
                val snapshot=Base64.getEncoder().encodeToString(a.toString().toByteArray(Charsets.UTF_8))
                writer.println(listOf(symbol,tf,a.candleCloseTime,recorded,acceptedAt==a.candleCloseTime,
                    a.direction,price.close,max(.45,a.indicators.atrPct*.55),first,end,price.closeTime,snapshot).joinToString("\t"))
            }
        }
    }
}
