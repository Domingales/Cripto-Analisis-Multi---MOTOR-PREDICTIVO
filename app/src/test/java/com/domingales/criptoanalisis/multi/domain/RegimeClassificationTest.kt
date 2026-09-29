package com.domingales.criptoanalisis.multi.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class RegimeClassificationTest {
    @Test fun neutral_market_is_mixed_not_reversal() {
        val regime = TechnicalEngine.classifyRegime(
            price = 100.0, ema20 = 101.0, ema50 = 99.0, ema200 = 100.5,
            adx = 18.0, atrPct = 1.5, breakoutUp = false, breakoutDown = false,
            bullishDivergence = false, bearishDivergence = false, falseBreakout = false
        )
        assertEquals(Regime.MIXED, regime)
    }

    @Test fun reversal_requires_evidence() {
        val regime = TechnicalEngine.classifyRegime(
            price = 100.0, ema20 = 101.0, ema50 = 99.0, ema200 = 100.5,
            adx = 18.0, atrPct = 1.5, breakoutUp = false, breakoutDown = false,
            bullishDivergence = true, bearishDivergence = false, falseBreakout = false
        )
        assertEquals(Regime.POSSIBLE_REVERSAL, regime)
    }
}
