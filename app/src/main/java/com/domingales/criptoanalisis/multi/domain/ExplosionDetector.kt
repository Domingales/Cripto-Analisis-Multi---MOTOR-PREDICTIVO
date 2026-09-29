package com.domingales.criptoanalisis.multi.domain

import kotlin.math.abs
import kotlin.math.max

enum class ExplosionDirection { BULLISH, BEARISH }

/**
 * Resultado del detector paralelo de movimientos extremos. No interviene en
 * SignalEngine ni modifica score, confianza, probabilidad o recomendación.
 */
data class ExplosionAssessment(
    val symbol: String,
    val timeframe: String,
    val timestamp: Long,
    val candleCloseTime: Long,
    val price: Double,
    val direction: ExplosionDirection?,
    val confirmed: Boolean,
    val volumeConfirmed: Boolean,
    val breakoutConfirmed: Boolean,
    val atrConfirmed: Boolean,
    val adxConfirmed: Boolean,
    val mtfConfirmed: Boolean,
    val volumeRatio: Double,
    val atr: Double,
    val atrExpansionRatio: Double,
    val adx: Double,
    val previousAdx: Double,
    val alignedTimeframes: Int,
    val availableTimeframes: Int,
    val breakoutLevel: Double,
    val reasons: List<String>
) {
    val confirmations: Int
        get() = listOf(volumeConfirmed, breakoutConfirmed, atrConfirmed, adxConfirmed, mtfConfirmed).count { it }
}

object ExplosionDetector {
    const val MIN_VOLUME_RATIO = 2.0
    const val MIN_ADX = 25.0
    const val MIN_ATR_EXPANSION_RATIO = 1.10
    private const val ATR_PERIOD = 14
    private const val ATR_BASELINE_WINDOWS = 20

    fun evaluate(analysis: AnalysisResult, candles: List<Candle>): ExplosionAssessment {
        val usable = candles.filter { it.closeTime <= analysis.candleCloseTime }.sortedBy { it.openTime }
        val indicators = analysis.indicators
        val direction = when {
            indicators.breakoutUp && !indicators.breakoutDown -> ExplosionDirection.BULLISH
            indicators.breakoutDown && !indicators.breakoutUp -> ExplosionDirection.BEARISH
            else -> null
        }

        val volumeConfirmed = indicators.volumeRatio >= MIN_VOLUME_RATIO
        val breakoutConfirmed = direction != null && !indicators.falseBreakout && when (direction) {
            ExplosionDirection.BULLISH -> indicators.price > indicators.resistance
            ExplosionDirection.BEARISH -> indicators.price < indicators.support
        }

        val hasPreviousAdx = usable.size >= 31
        val previousAdx = if (hasPreviousAdx) adxAt(usable.dropLast(1)) else 0.0
        val previousAtr = atrAt(usable, usable.size - 1) ?: 0.0

        val previousAtrValues = if (usable.size >= ATR_PERIOD + ATR_BASELINE_WINDOWS + 1) {
            ((usable.size - ATR_BASELINE_WINDOWS) until usable.size).mapNotNull { endExclusive ->
                atrAt(usable, endExclusive)
            }
        } else emptyList()
        val baselineAtr = previousAtrValues.takeIf { it.isNotEmpty() }?.average() ?: 0.0
        val atrExpansionRatio = if (baselineAtr > 0.0) indicators.atr14 / baselineAtr else 0.0
        val atrConfirmed = baselineAtr > 0.0 && indicators.atr14 > 0.0 &&
            atrExpansionRatio >= MIN_ATR_EXPANSION_RATIO &&
            previousAtr > 0.0 && indicators.atr14 > previousAtr

        val directionDiConfirmed = when (direction) {
            ExplosionDirection.BULLISH -> indicators.plusDi > indicators.minusDi
            ExplosionDirection.BEARISH -> indicators.minusDi > indicators.plusDi
            null -> false
        }
        val adxConfirmed = hasPreviousAdx && indicators.adx14 >= MIN_ADX &&
            indicators.adx14 > previousAdx && directionDiConfirmed

        val usableMtf = analysis.mtf.directionByTimeframe.filterValues { it != Direction.WAIT }
        val alignedTimeframes = usableMtf.values.count { mtfDirection ->
            when (direction) {
                ExplosionDirection.BULLISH -> mtfDirection == Direction.BUY || mtfDirection == Direction.REVERSAL_BUY
                ExplosionDirection.BEARISH -> mtfDirection == Direction.SELL || mtfDirection == Direction.REVERSAL_SELL
                null -> false
            }
        }
        val oppositeTimeframes = usableMtf.size - alignedTimeframes
        val primaryAligned = analysis.mtf.directionByTimeframe[analysis.timeframe]?.let { mtfDirection ->
            when (direction) {
                ExplosionDirection.BULLISH -> mtfDirection == Direction.BUY || mtfDirection == Direction.REVERSAL_BUY
                ExplosionDirection.BEARISH -> mtfDirection == Direction.SELL || mtfDirection == Direction.REVERSAL_SELL
                null -> false
            }
        } ?: false
        val higherAligned = listOf("4h", "1d").any { tf ->
            analysis.mtf.directionByTimeframe[tf]?.let { mtfDirection ->
                when (direction) {
                    ExplosionDirection.BULLISH -> mtfDirection == Direction.BUY || mtfDirection == Direction.REVERSAL_BUY
                    ExplosionDirection.BEARISH -> mtfDirection == Direction.SELL || mtfDirection == Direction.REVERSAL_SELL
                    null -> false
                }
            } ?: false
        }
        val mtfConfirmed = alignedTimeframes >= 3 && oppositeTimeframes == 0 && primaryAligned && higherAligned

        val reasons = listOf(
            "Volumen ${"%.2f".format(indicators.volumeRatio)}x (mínimo ${"%.1f".format(MIN_VOLUME_RATIO)}x): ${yesNo(volumeConfirmed)}",
            "Ruptura cerrada de nivel relevante: ${yesNo(breakoutConfirmed)}",
            "ATR ${"%.2f".format(atrExpansionRatio)}x de su media reciente: ${yesNo(atrConfirmed)}",
            "ADX ${"%.2f".format(indicators.adx14)} frente a ${"%.2f".format(previousAdx)} y DMI coherente: ${yesNo(adxConfirmed)}",
            "MTF $alignedTimeframes/${usableMtf.size} sin contradicciones: ${yesNo(mtfConfirmed)}"
        )
        val allFive = volumeConfirmed && breakoutConfirmed && atrConfirmed && adxConfirmed && mtfConfirmed
        return ExplosionAssessment(
            symbol = analysis.symbol,
            timeframe = analysis.timeframe,
            timestamp = analysis.timestamp,
            candleCloseTime = analysis.candleCloseTime,
            price = indicators.price,
            direction = direction,
            confirmed = analysis.closedCandleEvaluation && analysis.dataComplete && allFive,
            volumeConfirmed = volumeConfirmed,
            breakoutConfirmed = breakoutConfirmed,
            atrConfirmed = atrConfirmed,
            adxConfirmed = adxConfirmed,
            mtfConfirmed = mtfConfirmed,
            volumeRatio = indicators.volumeRatio,
            atr = indicators.atr14,
            atrExpansionRatio = atrExpansionRatio,
            adx = indicators.adx14,
            previousAdx = previousAdx,
            alignedTimeframes = alignedTimeframes,
            availableTimeframes = usableMtf.size,
            breakoutLevel = when (direction) {
                ExplosionDirection.BULLISH -> indicators.resistance
                ExplosionDirection.BEARISH -> indicators.support
                null -> 0.0
            },
            reasons = reasons
        )
    }

    /** Primer toque a +1 ATR o -1 ATR; null significa que sigue pendiente. */
    fun resolve(entry: Double, atr: Double, direction: ExplosionDirection, candles: List<Candle>): OutcomeStatus? {
        if (entry <= 0.0 || atr <= 0.0) return null
        val target = if (direction == ExplosionDirection.BULLISH) entry + atr else entry - atr
        val stop = if (direction == ExplosionDirection.BULLISH) entry - atr else entry + atr
        candles.sortedBy { it.openTime }.forEach { candle ->
            val hit = if (direction == ExplosionDirection.BULLISH) candle.high >= target else candle.low <= target
            val fail = if (direction == ExplosionDirection.BULLISH) candle.low <= stop else candle.high >= stop
            if (hit && fail) return OutcomeStatus.NEUTRAL
            if (hit) return OutcomeStatus.HIT
            if (fail) return OutcomeStatus.FAIL
        }
        return null
    }

    private fun atrAt(candles: List<Candle>, endExclusive: Int): Double? {
        if (endExclusive > candles.size || endExclusive <= ATR_PERIOD) return null
        val start = endExclusive - ATR_PERIOD
        var total = 0.0
        for (index in start until endExclusive) {
            if (index <= 0) return null
            val candle = candles[index]
            val previousClose = candles[index - 1].close
            total += max(candle.high - candle.low, max(abs(candle.high - previousClose), abs(candle.low - previousClose)))
        }
        return total / ATR_PERIOD
    }

    /** Mismo DMI/ADX matemático, limitado al valor anterior que necesita este módulo. */
    private fun adxAt(candles: List<Candle>, period: Int = 14): Double {
        if (candles.size < period * 2 + 2) return 0.0
        val dxs = mutableListOf<Double>()
        for (end in candles.size - period until candles.size) {
            if (end < period) continue
            var trSum = 0.0; var plusSum = 0.0; var minusSum = 0.0
            for (index in end - period + 1..end) {
                val up = candles[index].high - candles[index - 1].high
                val down = candles[index - 1].low - candles[index].low
                plusSum += if (up > down && up > 0.0) up else 0.0
                minusSum += if (down > up && down > 0.0) down else 0.0
                trSum += max(candles[index].high - candles[index].low, max(abs(candles[index].high - candles[index - 1].close), abs(candles[index].low - candles[index - 1].close)))
            }
            if (trSum > 0.0) {
                val plus = 100.0 * plusSum / trSum; val minus = 100.0 * minusSum / trSum; val denominator = plus + minus
                dxs += if (denominator == 0.0) 0.0 else 100.0 * abs(plus - minus) / denominator
            }
        }
        return if (dxs.isEmpty()) 0.0 else dxs.average()
    }

    private fun yesNo(value: Boolean) = if (value) "SÍ" else "NO"
}
