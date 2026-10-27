package com.domingales.criptoanalisis.multi.evaluation

import com.domingales.criptoanalisis.multi.domain.*
import org.junit.Test
import java.io.File

/** Reconstrucción histórica nueva: no registra contratos prospectivos antiguos. */
class MagnitudeHistoricalTest {
    @Test fun labelHistoricalRangesWithAndroidCriterion() {
        val work=System.getenv("MAG_HISTORY_WORK")?.let(::File) ?: return
        val data=File(System.getenv("MAG_HISTORY_DATA") ?: error("MAG_HISTORY_DATA"))
        val fine=File(data,"XRPUSDT_5m.csv").useLines { lines -> lines.drop(1).map { line ->
            val v=line.split(',')
            Candle(v[0].toLong(),v[1].toDouble(),v[2].toDouble(),v[3].toDouble(),v[4].toDouble(),v[5].toDouble(),v[6].toLong())
        }.toList() }
        fun upper(t: Long): Int {
            var lo=0; var hi=fine.size
            while(lo<hi) { val m=(lo+hi)/2; if(fine[m].closeTime<=t) lo=m+1 else hi=m }
            return lo
        }
        val cutoff=1790953200000L
        File(work,"labels.tsv").printWriter().use { writer ->
            File(work,"contracts.tsv").forEachLine { line ->
                if(line.isNotBlank()) {
                    val v=line.split('\t'); val issued=v[0].toLong()
                    val c=ForecastCriterion.Contract(v[1].toDouble(),Direction.valueOf(v[2]),issued,v[3].toDouble())
                    val window=fine.subList(upper(issued),upper(minOf(cutoff,issued+5*ForecastCriterion.DAY)))
                    val r=ForecastCriterion.evaluate(c,window,cutoff)
                    writer.println(listOf(issued,v[2],r.status,r.reason,r.mfe,r.mae).joinToString("\t"))
                }
            }
        }
    }
}
