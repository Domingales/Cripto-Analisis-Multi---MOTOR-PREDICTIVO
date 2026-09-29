package com.domingales.criptoanalisis.multi.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TechnicalEngineIncrementalTest {
    @Test fun incremental_ema_and_rsi_match_full_calculation() {
        val candles = candles(240)
        val previous = TechnicalEngine.calculateWithState(candles.dropLast(1))
        val incremental = TechnicalEngine.calculateIncremental(candles, previous.state)
        val complete = TechnicalEngine.calculate(candles)

        assertNotNull(incremental)
        val actual = incremental!!.indicators
        assertEquals(complete.ema20, actual.ema20, 1e-10)
        assertEquals(complete.ema50, actual.ema50, 1e-10)
        assertEquals(complete.ema200, actual.ema200, 1e-10)
        assertEquals(complete.ema20SlopePct, actual.ema20SlopePct, 1e-10)
        assertEquals(complete.ema50SlopePct, actual.ema50SlopePct, 1e-10)
        assertEquals(complete.rsi14, actual.rsi14, 1e-10)
        assertEquals(complete.rsiSlope, actual.rsiSlope, 1e-10)
    }

    private fun candles(count: Int): List<Candle> = (0 until count).map { index ->
        val openTime = 1_000_000L + index * 60_000L
        val close = 100.0 + index * 0.04 + kotlin.math.sin(index / 4.0)
        Candle(openTime, close - 0.2, close + 0.6, close - 0.5, close, 1_000.0 + index * 3, openTime + 59_999L)
    }
}
