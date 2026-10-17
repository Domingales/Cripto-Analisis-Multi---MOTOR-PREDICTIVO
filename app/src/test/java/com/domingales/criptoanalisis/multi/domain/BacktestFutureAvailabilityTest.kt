package com.domingales.criptoanalisis.multi.domain

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sin

class BacktestFutureAvailabilityTest {
    private fun sample(step: Long, size: Int) = (0 until size).map { i ->
        val price = 100 + sin(i * .17) * 4 + i * .01
        Candle(i * step, price, price + 2, price - 2, price + sin(i * .31),
            1000.0 + i % 11 * 90, i * step + step - 1)
    }

    private val primary = sample(900000L, 580)
    private val contexts = linkedMapOf("15m" to primary, "1h" to primary.map {
        it.copy(close = it.close * 1.001, high = it.high * 1.001)
    })
    private val fine = sample(300000L, 1740)
    private val boundary = primary[400].closeTime

    private data class Replay(val analyses: List<AnalysisResult>, val accepted: List<Long>, val result: BacktestResult)

    private fun replay(all: Map<String, List<Candle>>, outcomes: List<Candle>): Replay {
        val analyses = mutableListOf<AnalysisResult>()
        val accepted = mutableListOf<Long>()
        val result = BacktestEngine.run("ADA", "15m", all, 0,
            btcCandles4h = contexts.getValue("1h"), outcomeCandles5m = outcomes,
            auditThroughLatest = true, onEvaluation = { analyses.add(it) },
            onAccepted = { accepted.add(it.candleCloseTime) })
        return Replay(analyses, accepted, result)
    }

    private fun past(replay: Replay) = replay.analyses.filter { it.candleCloseTime <= boundary }
        .map { it.copy(timestamp = 0L) }

    @Test fun a_missing_future_candle_does_not_change_past_acceptance_or_analysis() {
        val complete = replay(contexts, fine)
        val missingAt = boundary + 1 + 300000L
        val missing = fine.filter { it.openTime != missingAt }
        assertEquals(fine.size - 1, missing.size)
        val gap = replay(contexts, missing)
        assertTrue(complete.accepted.any { it <= boundary })
        assertTrue(gap.result.missingFiveMinuteData > 0)
        assertTrue(gap.result.cases.any { it.outcome == OutcomeStatus.PENDING && it.outcomeReason == "MISSING_5M_DATA" })
        assertEquals(past(complete), past(gap))
        assertEquals(complete.accepted.filter { it <= boundary }, gap.accepted.filter { it <= boundary })
    }

    @Test fun changed_future_prices_do_not_change_past_acceptance_or_analysis() {
        val complete = replay(contexts, fine)
        val changed = contexts.mapValues { (_, candles) -> candles.map {
            if (it.closeTime > boundary) it.copy(open = it.open * 2, high = it.high * 2,
                low = it.low * 2, close = it.close * 2) else it
        } }
        val future = replay(changed, fine)
        assertTrue(past(complete).isNotEmpty())
        assertEquals(past(complete), past(future))
        assertEquals(complete.accepted.filter { it <= boundary }, future.accepted.filter { it <= boundary })
    }
}
