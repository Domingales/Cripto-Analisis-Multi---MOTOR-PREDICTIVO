package com.domingales.criptoanalisis.multi.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SignalEngineTest {
    @Test fun intravela_can_only_be_presignal() {
        val result = SignalEngine.evaluate(input(closed = false, calibration = HistoricalCalibration(0, 0, 0, 0, null, StatisticalConfidence.INSUFFICIENT)))
        assertFalse(result.isSignal)
        assertTrue(result.isPreSignal)
        assertTrue(result.rejectionReason.contains("cierre"))
        assertFalse(result.closedCandleEvaluation)
    }

    @Test fun closed_candle_can_be_final_signal() {
        val result = SignalEngine.evaluate(input(closed = true, calibration = HistoricalCalibration(0, 0, 0, 0, null, StatisticalConfidence.INSUFFICIENT)))
        assertTrue(result.isSignal)
        assertTrue(result.closedCandleEvaluation)
        assertTrue(result.allowBuy)
        assertTrue(result.allowSell)
        assertTrue(result.decisionThreshold == 55)
    }

    @Test fun probability_is_hidden_below_ten_cases_even_if_legacy_value_exists() {
        val result = SignalEngine.evaluate(input(closed = true, calibration = HistoricalCalibration(5, 4, 1, 0, 80, StatisticalConfidence.INSUFFICIENT)))
        assertNull(result.probability)
    }

    @Test fun historical_component_only_weights_the_calibrated_side() {
        val calibration = HistoricalCalibration(20, 16, 4, 0, 80, StatisticalConfidence.HIGH)
        val buy = SignalEngine.evaluate(input(true, calibration).copy(calibrationDirection = Direction.BUY))
        val opposite = SignalEngine.evaluate(input(true, calibration).copy(calibrationDirection = Direction.SELL))
        assertEquals(8, buy.breakdown.historical)
        assertEquals(0, opposite.breakdown.historical)
    }

    @Test fun rejects_late_buy_without_confirmed_breakout() {
        val base = input(true, HistoricalCalibration(0, 0, 0, 0, null, StatisticalConfidence.INSUFFICIENT))
        val result = SignalEngine.evaluate(base.copy(indicators = base.indicators.copy(
            rsi14 = 76.0,
            breakoutUp = false,
            resistance = 125.0,
            resistanceZone = SupportResistanceZone(124.0, 125.0, 3, 60)
        )))
        assertFalse(result.isSignal)
        assertTrue(result.rejectionReason.contains("tardía"))
    }

    @Test fun btc_context_only_penalizes_contradiction_instead_of_creating_bonus() {
        val base = input(true, HistoricalCalibration(0, 0, 0, 0, null, StatisticalConfidence.INSUFFICIENT))
        val aligned = SignalEngine.evaluate(base.copy(btcRegime = Regime.TREND_UP))
        val contradicted = SignalEngine.evaluate(base.copy(btcRegime = Regime.TREND_DOWN))
        assertEquals(0, aligned.breakdown.btcContext)
        assertEquals(-5, contradicted.breakdown.btcContext)
    }

    private fun input(closed: Boolean, calibration: HistoricalCalibration): SignalEngine.Input {
        val zone = SupportResistanceZone(99.0, 99.5, 4, 80)
        val indicators = IndicatorSet(
            price = 110.0, ema20 = 105.0, ema50 = 100.0, ema200 = 90.0,
            ema20SlopePct = 1.2, ema50SlopePct = 0.8, rsi14 = 62.0, rsiSlope = 2.0,
            rsiBullishDivergence = false, rsiBearishDivergence = false,
            atr14 = 2.0, atrPct = 1.8, adx14 = 30.0, plusDi = 35.0, minusDi = 12.0,
            volumeRatio = 1.4, volumeExpansion = true, support = 99.0, resistance = 109.0,
            supportZone = zone, resistanceZone = SupportResistanceZone(108.0, 109.0, 3, 60),
            structure = MarketStructure.BULLISH, regime = Regime.TREND_UP,
            breakoutUp = true, breakoutDown = false, falseBreakout = false, expectedMoveAtr = 1.8
        )
        val mtf = MultiTimeframeContext(
            mapOf("1h" to Regime.TREND_UP, "4h" to Regime.TREND_UP),
            mapOf("1h" to Direction.BUY, "4h" to Direction.BUY), 12, "1h:BUY • 4h:BUY"
        )
        return SignalEngine.Input("ADA", "1h", 1_000L, indicators, mtf, calibration,
            FundamentalContext(0, 0, 0, 0, "sin noticias", 0), Regime.TREND_UP, 55, closed)
    }
}
