package com.domingales.criptoanalisis.multi.domain

import org.junit.Assert.*
import org.junit.Test

class ForecastCriterionTest {
    private val start = ForecastCriterion.STEP - 1
    private val day = ForecastCriterion.DAY
    private val contract = ForecastCriterion.Contract(100.0, Direction.BUY, start, 4.0)
    private fun bars(days: Int = 5, upAt: Int = -1, high: Double = 100.0, low: Double = 100.0): List<Candle> =
        (1..days * 288).map { n ->
            Candle(n * ForecastCriterion.STEP, 100.0, if (n == upAt) high else 100.0,
                if (n == upAt) low else 100.0, 100.0, 1.0, (n + 1) * ForecastCriterion.STEP - 1)
        }
    private fun status(bars: List<Candle>, days: Int = 5) = ForecastCriterion.evaluate(contract, bars, start + days * day).status

    @Test fun fullTargetWithinDayIsHit() {
        assertEquals(ForecastCriterion.Status.HIT, status(bars(upAt = 100, high = 104.0), 1))
    }
    @Test fun threePercentInTwoDaysIsApproximate() {
        val result = ForecastCriterion.evaluate(contract, bars(upAt = 576, high = 103.0), start + 2 * day)
        assertEquals(ForecastCriterion.Status.APPROXIMATE, result.status)
        assertEquals(start + 2 * day, result.firstApproximateAt)
        assertNull(result.firstTargetAt)
    }
    @Test fun onePercentInFiveDaysFails() {
        assertEquals(ForecastCriterion.Status.FAIL, status(bars(upAt = 800, high = 101.0)))
    }
    @Test fun onlyFallingFails() {
        assertEquals(ForecastCriterion.Status.FAIL, status(bars(upAt = 200, low = 90.0)))
    }
    @Test fun sufficientMoveOnThirdDayIsLateNotHit() {
        assertEquals(ForecastCriterion.Status.LATE, status(bars(upAt = 600, high = 104.0)))
    }
    @Test fun exactAtDeadlineCounts() {
        assertEquals(ForecastCriterion.Status.HIT, status(bars(upAt = 288, high = 104.0), 1))
    }
    @Test fun exactAfterDeadlineIsOnlyApproximate() {
        assertEquals(ForecastCriterion.Status.APPROXIMATE, status(bars(upAt = 289, high = 104.0), 2))
    }
    @Test fun earlyPartialWaitsForExactDeadline() {
        val result = ForecastCriterion.evaluate(contract, bars(upAt = 100, high = 103.0), start + day / 2)
        assertEquals(ForecastCriterion.Status.PENDING, result.status)
    }
    @Test fun missingCandleBeforeTargetPreventsAward() {
        assertEquals(ForecastCriterion.Status.PENDING, status(bars(upAt = 100, high = 104.0).filterIndexed { i, _ -> i != 50 }))
    }
    @Test fun missingFinalCandleCannotBecomeFailure() {
        assertEquals(ForecastCriterion.Status.PENDING, status(bars().dropLast(1)))
    }
    @Test fun futureCandlesCannotAwardEarlyHit() {
        assertEquals(ForecastCriterion.Status.PENDING, status(bars(upAt = 400, high = 104.0), 1))
    }
    @Test fun partialCandleAtEmissionIsExcluded() {
        val c = Candle(0, 100.0, 110.0, 100.0, 100.0, 1.0, start)
        assertEquals(ForecastCriterion.Status.FAIL, status(listOf(c) + bars()))
    }
    @Test fun sellUsesLowAndReportsAdverseExcursion() {
        val c = contract.copy(direction = Direction.SELL)
        val result = ForecastCriterion.evaluate(c, bars(upAt = 100, high = 106.0, low = 96.0), start + day)
        assertEquals(ForecastCriterion.Status.HIT, result.status)
        assertEquals(-6.0, result.mae, 1e-8)
    }
    @Test fun completedHitDoesNotNeedLaterMissingData() {
        assertEquals(ForecastCriterion.Status.HIT, status(bars(upAt = 20, high = 104.0).take(20)))
    }
    @Test fun unorderedInputIsSorted() {
        assertEquals(ForecastCriterion.Status.HIT, status(bars(upAt = 100, high = 104.0).reversed()))
    }
    @Test(expected = IllegalArgumentException::class) fun invalidTargetRejected() {
        contract.copy(targetPct = Double.NaN)
    }
}
