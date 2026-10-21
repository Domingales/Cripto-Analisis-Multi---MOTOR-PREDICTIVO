package com.domingales.criptoanalisis.multi.evaluation

import com.domingales.criptoanalisis.multi.domain.*
import org.junit.Test
import java.io.File

/** Evalúa en GitHub con la misma implementación Kotlin instalada en Android. */
class MagnitudeLiveTest {
    @Test fun evaluateRegisteredContracts() {
        val work = System.getenv("MAGNITUDE_WORK")?.let(::File) ?: return
        val data = File(System.getenv("MAGNITUDE_DATA") ?: error("Missing market data"))
        val cache = mutableMapOf<String, List<Candle>>()
        fun candles(symbol: String): List<Candle> = cache.getOrPut(symbol) {
            File(data, symbol + "USDT_5m.csv").useLines { lines ->
                lines.drop(1).map { line ->
                    val v = line.split(',')
                    Candle(v[0].toLong(),v[1].toDouble(),v[2].toDouble(),v[3].toDouble(),v[4].toDouble(),v[5].toDouble(),v[6].toLong())
                }.toList()
            }
        }
        File(work,"results.tsv").printWriter().use { writer ->
            File(work,"contracts.tsv").forEachLine { line ->
                if (line.isNotBlank()) {
                    val v = line.split('\t')
                    val contract = ForecastCriterion.Contract(v[3].toDouble(),Direction.valueOf(v[4]),v[5].toLong(),
                        v[6].toDouble(),v[7].toLong(),v[8].toLong(),v[9].toLong(),v[10].toDouble())
                    val r = ForecastCriterion.evaluate(contract,candles(v[2]),v[11].toLong())
                    writer.println(listOf(v[0],v[1],r.status.name,r.reason,r.firstTargetAt ?: "",
                        r.firstApproximateAt ?: "",r.mfe,r.mae).joinToString("\t"))
                }
            }
        }
    }
}
