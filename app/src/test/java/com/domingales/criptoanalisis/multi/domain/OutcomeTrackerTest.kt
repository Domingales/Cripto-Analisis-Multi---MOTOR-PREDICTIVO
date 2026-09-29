package com.domingales.criptoanalisis.multi.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class OutcomeTrackerTest {
    @Test fun excursions_use_intrabar_high_and_low_for_buy() {
        val result = OutcomeTracker.excursions(100.0, "BUY", listOf(candle(0, 100.0, 106.0, 97.0, 103.0)))
        assertEquals(6.0, result.mfe, 0.0001)
        assertEquals(-3.0, result.mae, 0.0001)
    }

    @Test fun excursions_are_direction_aware_for_sell() {
        val result = OutcomeTracker.excursions(100.0, "SELL", listOf(candle(0, 100.0, 104.0, 92.0, 96.0)))
        assertEquals(8.0, result.mfe, 0.0001)
        assertEquals(-4.0, result.mae, 0.0001)
    }

    @Test fun horizon_uses_latest_closed_market_candle_not_a_later_price() {
        val due = 1_000_000L
        val candles = listOf(
            candle(800_000L, 100.0, 102.0, 99.0, 101.0, 900_000L),
            candle(950_000L, 101.0, 110.0, 100.0, 109.0, 1_050_000L)
        )
        val observation = OutcomeTracker.observationAt(100.0, "BUY", due, candles, 0.5)
        assertNotNull(observation)
        assertEquals(900_000L, observation!!.marketTimestamp)
        assertEquals(101.0, observation.price, 0.0001)
    }

    @Test fun backtest_uses_first_touch_instead_of_final_close() {
        val candles = listOf(
            candle(0, 100.0, 102.0, 99.8, 101.0),
            candle(1, 101.0, 101.2, 96.0, 97.0)
        )
        assertEquals(OutcomeStatus.HIT, OutcomeTracker.firstTouch(100.0, Direction.BUY, candles, 1.0))
    }

    @Test fun stale_candle_does_not_resolve_a_horizon() {
        val due = 2_000_000L
        val stale = listOf(candle(0, 100.0, 101.0, 99.0, 100.5, 900_000L))
        assertNull(OutcomeTracker.observationAt(100.0, "BUY", due, stale, 0.5))
    }

    @Test fun ambiguous_same_candle_touch_is_neutral() {
        val candles = listOf(candle(0, 100.0, 102.0, 98.0, 101.0))
        assertEquals(OutcomeStatus.NEUTRAL, OutcomeTracker.firstTouch(100.0, Direction.BUY, candles, 1.0))
        assertEquals("BOTH_TOUCHED_SAME_5M_CANDLE", OutcomeTracker.firstTouchResolution(100.0, Direction.BUY, candles, 1.0)?.reason)
    }

    @Test fun unresolved_first_touch_remains_pending_until_caller_closes_window() {
        val candles = listOf(candle(0, 100.0, 100.3, 99.8, 100.1))
        assertNull(OutcomeTracker.firstTouchResolution(100.0, Direction.BUY, candles, 1.0))
    }

    @Test fun horizons_are_adapted_to_signal_timeframe() {
        assertEquals(listOf(60, 240, 720, 1440), OutcomeTracker.persistenceHorizonsMinutes("1h"))
        assertEquals(listOf(240, 720, 1440, 4320), OutcomeTracker.persistenceHorizonsMinutes("4h"))
        assertEquals(listOf(1440, 4320, 10080, 20160), OutcomeTracker.persistenceHorizonsMinutes("1d"))
        assertEquals(20160, OutcomeTracker.outcomeWindowMinutes("1d"))
    }

    @Test fun target_and_stop_are_direction_aware() {
        assertEquals(101.0, OutcomeTracker.targetPrice(100.0, "BUY", 1.0), 0.0001)
        assertEquals(99.0, OutcomeTracker.stopPrice(100.0, "BUY", 1.0), 0.0001)
        assertEquals(99.0, OutcomeTracker.targetPrice(100.0, "SELL", 1.0), 0.0001)
        assertEquals(101.0, OutcomeTracker.stopPrice(100.0, "SELL", 1.0), 0.0001)
    }

    @Test fun candle_started_before_signal_is_never_used_for_excursions() {
        val signalClose = 299_999L
        val contaminated = candle(0L, 100.0, 150.0, 50.0, 100.0, signalClose)
        val firstClean = candle(300_000L, 100.0, 102.0, 99.0, 101.0, 599_999L)
        val selected = OutcomeTracker.candlesStrictlyAfter(signalClose, 599_999L, listOf(contaminated, firstClean))
        assertEquals(listOf(firstClean), selected)
        val excursions = OutcomeTracker.excursions(100.0, "BUY", selected)
        assertEquals(2.0, excursions.mfe, 0.0001)
        assertEquals(-1.0, excursions.mae, 0.0001)
    }

    private fun candle(openTime: Long, open: Double, high: Double, low: Double, close: Double, closeTime: Long = openTime + 299_999L) =
        Candle(openTime, open, high, low, close, 1_000.0, closeTime)
}
