package com.domingales.criptoanalisis.multi.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BacktestFiveMinuteAuditTest {
    private val step = 300_000L
    private val start = 899_999L
    private val end = start + 4 * step

    @Test fun only_complete_closed_series_can_resolve_a_signal() {
        val candles = (0 until 4).map { i ->
            val open = start + 1 + i * step
            Candle(open, 100.0, 101.0, 99.0, 100.0, 1.0, open + step - 1)
        }
        assertTrue(BacktestEngine.completeFiveMinuteWindow(start, end, candles))
        assertFalse(BacktestEngine.completeFiveMinuteWindow(start, end, candles.drop(1)))
        assertFalse(BacktestEngine.completeFiveMinuteWindow(start, end, candles.filterIndexed { i, _ -> i != 2 }))
        assertFalse(BacktestEngine.completeFiveMinuteWindow(start, end, candles.dropLast(1)))
    }
}
