package com.domingales.criptoanalisis.multi.domain

import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class BacktestEngineTest {
    @Test fun evaluates_windows_when_historical_mtf_is_available() {
        val primary = candles(320, 60 * 60_000L, 1_000_000_000L)
        val secondary = candles(320, 4 * 60 * 60_000L, 1_000_000_000L - 320L * 4 * 60 * 60_000L)
        val result = BacktestEngine.run("ADA", "1h", mapOf("1h" to primary, "4h" to secondary), 55)
        assertTrue(result.evaluatedWindows > 0)
        assertEquals(result.signals, result.buy.signals + result.sell.signals)
        assertTrue(result.contextNote.contains("1440m"))
    }

    @Test fun applies_only_historical_btc_context() {
        val start = 2_000_000_000L
        val primary = candles(340, 60 * 60_000L, start)
        val secondary = candles(340, 4 * 60 * 60_000L, start - 340L * 4 * 60 * 60_000L)
        val btc = candles(600, 4 * 60 * 60_000L, start - 500L * 4 * 60 * 60_000L)
        val result = BacktestEngine.run("ADA", "1h", mapOf("1h" to primary, "4h" to secondary), 55, btcCandles4h = btc)
        assertTrue(result.evaluatedWindows > 0)
        assertTrue(result.btcContextWindows > 0)
        assertTrue(result.contextNote.contains("sin futuro"))
    }

    private fun candles(count: Int, step: Long, start: Long): List<Candle> = (0 until count).map { index ->
        val openTime = start + index * step
        val price = 10.0 + index * 0.05
        Candle(openTime, price, price + 0.08, price - 0.04, price + 0.05, 1_000.0 + index, openTime + step - 1)
    }
}
