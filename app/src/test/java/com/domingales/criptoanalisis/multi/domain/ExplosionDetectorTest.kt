package com.domingales.criptoanalisis.multi.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExplosionDetectorTest {
    @Test fun confirms_only_when_all_five_closed_candle_conditions_match() {
        val candles = explosionCandles()
        val analysis = analysis(candles, closed = true)
        val result = ExplosionDetector.evaluate(analysis, candles)
        assertTrue(result.confirmed)
        assertEquals(5, result.confirmations)
        assertEquals(ExplosionDirection.BULLISH, result.direction)
    }

    @Test fun intravela_never_becomes_a_confirmed_explosion() {
        val candles = explosionCandles()
        val result = ExplosionDetector.evaluate(analysis(candles, closed = false), candles)
        assertFalse(result.confirmed)
        assertEquals(5, result.confirmations)
    }

    @Test fun ordinary_volume_blocks_the_detector_without_changing_other_conditions() {
        val candles = explosionCandles()
        val base = analysis(candles, closed = true)
        val result = ExplosionDetector.evaluate(base.copy(indicators = base.indicators.copy(volumeRatio = 1.4)), candles)
        assertFalse(result.confirmed)
        assertFalse(result.volumeConfirmed)
    }

    @Test fun outcome_is_separate_and_uses_first_one_atr_touch() {
        val hit = Candle(
            openTime = 1L,
            open = 100.0,
            high = 102.2,
            low = 99.4,
            close = 101.0,
            volume = 2.0,
            closeTime = 2L
        )
        assertEquals(OutcomeStatus.HIT, ExplosionDetector.resolve(100.0, 2.0, ExplosionDirection.BULLISH, listOf(hit)))
        val unresolved = Candle(
            openTime = 1L,
            open = 100.0,
            high = 101.0,
            low = 99.5,
            close = 100.2,
            volume = 2.0,
            closeTime = 2L
        )
        assertNull(ExplosionDetector.resolve(100.0, 2.0, ExplosionDirection.BULLISH, listOf(unresolved)))
    }

    private fun analysis(candles: List<Candle>, closed: Boolean): AnalysisResult {
        val calculated = TechnicalEngine.calculate(candles)
        val indicators = calculated.copy(
            price = 140.0,
            resistance = 130.0,
            breakoutUp = true,
            breakoutDown = false,
            falseBreakout = false,
            volumeRatio = 3.2,
            adx14 = 36.0,
            plusDi = 42.0,
            minusDi = 9.0,
            atr14 = calculated.atr14 * 1.8
        )
        val mtf = MultiTimeframeContext(
            mapOf("15m" to Regime.TREND_UP, "1h" to Regime.BREAKOUT, "4h" to Regime.TREND_UP, "1d" to Regime.TREND_UP),
            mapOf("15m" to Direction.BUY, "1h" to Direction.BUY, "4h" to Direction.BUY, "1d" to Direction.BUY),
            16,
            "15m:BUY • 1h:BUY • 4h:BUY • 1d:BUY"
        )
        return AnalysisResult(
            "ADA", "1h", candles.last().closeTime, candles.last().closeTime, indicators, mtf, Regime.TREND_UP,
            HistoricalCalibration(0, 0, 0, 0, null, StatisticalConfidence.INSUFFICIENT),
            FundamentalContext(0, 0, 0, 0, "sin noticias", 0),
            ScoreBreakdown(0, 0, 0, 0, 0, 0, 0, 0, 0, 0), Direction.BUY, 0, 0, null,
            "MEDIO", 0.0, 0.0, 130.0, if (closed) EvaluationState.SIGNAL else EvaluationState.PRESIGNAL,
            !closed, closed, "", emptyList(), true, emptyList(), 81, closed, true, true
        )
    }

    private fun explosionCandles(): List<Candle> = (0 until 240).map { index ->
        val openTime = 1_000_000L + index * 60_000L
        val base = 100.0 + kotlin.math.sin(index / 2.0) * 0.4
        val range = if (index == 239) 14.0 else 1.2 + (index % 3) * 0.1
        val close = if (index == 239) 140.0 else base
        Candle(openTime, base, maxOf(base, close) + range * 0.55, minOf(base, close) - range * 0.45, close,
            if (index == 239) 10_000.0 else 1_000.0, openTime + 59_999L)
    }
}
