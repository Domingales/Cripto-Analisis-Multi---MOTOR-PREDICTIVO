package com.domingales.criptoanalisis.multi.data

import android.content.Context
import com.domingales.criptoanalisis.multi.domain.*
import com.domingales.criptoanalisis.multi.util.Prefs
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

class MarketRepository(
    private val context: Context,
    private val source: BinanceMarketDataSource = BinanceMarketDataSource(),
    private val news: NewsSentimentRepository = NewsSentimentRepository()
) {
    private data class CacheEntry(val at: Long, val candles: List<Candle>)
    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val newsCache = ConcurrentHashMap<String, FundamentalContext>()
    private val db = AppDatabase.get(context)

    fun analyze(symbol: String, timeframe: String, threshold: Int, btcRegime: Regime? = null): AnalysisResult =
        analyzeCycle(symbol, timeframe, threshold, btcRegime).last()

    /**
     * En modo intravela se producen dos resultados independientes: la última
     * vela cerrada puede confirmar SIGNAL y la vela abierta sólo puede generar
     * PRESIGNAL. En modo cerrado se devuelve únicamente la primera.
     */
    fun analyzeCycle(symbol: String, timeframe: String, threshold: Int, btcRegime: Regime? = null): List<AnalysisResult> {
        val primary = candles(symbol, timeframe, 300)
        val fundamental = if (Prefs.newsEnabled(context)) {
            val cachedNews = newsCache[symbol]
            if (cachedNews != null && System.currentTimeMillis() - cachedNews.updatedAt < 15 * 60_000L) cachedNews
            else news.context(symbol).also { newsCache[symbol] = it }
        } else FundamentalContext(0, 0, 0, 0, "módulo de noticias desactivado", System.currentTimeMillis())
        val closedCandles = lastClosedCandles(primary)
        val closedResult = analyzeSnapshot(symbol, timeframe, threshold, btcRegime, fundamental, closedCandles, true)
        if (Prefs.closedCandleMode(context)) return listOf(closedResult)
        // Si excepcionalmente la fuente no incluye una vela abierta, no se
        // fabrica una preseñal duplicando el último cierre.
        if (primary.last().closeTime <= System.currentTimeMillis()) return listOf(closedResult)
        val intravelaResult = analyzeSnapshot(symbol, timeframe, threshold, btcRegime, fundamental, primary, false)
        return listOf(closedResult, intravelaResult)
    }

    private fun analyzeSnapshot(
        symbol: String,
        timeframe: String,
        threshold: Int,
        btcRegime: Regime?,
        fundamental: FundamentalContext,
        sourceCandles: List<Candle>,
        closedEvaluation: Boolean
    ): AnalysisResult {
        val indicatorSet = indicators(symbol, timeframe, sourceCandles, closedEvaluation)
        val mtf = buildMtf(symbol, timeframe, closedEvaluation)
        val allowBuy = Prefs.symbolAllowBuy(context, symbol)
        val allowSell = Prefs.symbolAllowSell(context, symbol)
        val emptyCalibration = HistoricalCalibration(0, 0, 0, 0, null, StatisticalConfidence.INSUFFICIENT)
        fun evaluate(calibration: HistoricalCalibration, calibrationDirection: Direction? = null) = SignalEngine.evaluate(
            SignalEngine.Input(symbol, timeframe, sourceCandles.last().closeTime, indicatorSet, mtf, calibration, fundamental, btcRegime,
                threshold, closedEvaluation, allowBuy, allowSell, calibrationDirection)
        )

        val preliminary = evaluate(emptyCalibration)
        if (preliminary.direction == Direction.WAIT) return preliminary
        var calibrationDirection = preliminary.direction
        repeat(3) {
            val calibration = db.calibration(symbol, timeframe, indicatorSet, calibrationDirection, mtf.alignmentScore, btcRegime)
            val result = evaluate(calibration, calibrationDirection)
            if (result.direction == Direction.WAIT || sameSide(calibrationDirection, result.direction)) return result
            calibrationDirection = result.direction
        }
        // Si la componente histórica provocase una oscilación BUY/SELL, se usa
        // la evaluación técnica neutral para no mezclar casos del lado opuesto.
        return preliminary
    }

    private fun sameSide(first: Direction, second: Direction): Boolean =
        (first == Direction.BUY || first == Direction.REVERSAL_BUY) == (second == Direction.BUY || second == Direction.REVERSAL_BUY) &&
            (first == Direction.SELL || first == Direction.REVERSAL_SELL) == (second == Direction.SELL || second == Direction.REVERSAL_SELL)

    fun quickRegime(symbol: String, timeframe: String = "4h"): Regime {
        val all = candles(symbol, timeframe, 260)
        val usable = lastClosedCandles(all)
        return indicators(symbol, timeframe, usable, true).regime
    }

    fun currentPrice(symbol: String): Double = source.currentPrice(symbol)

    /** Detector paralelo: reutiliza el histórico en caché y no modifica SignalEngine. */
    fun explosionAssessment(analysis: AnalysisResult): ExplosionAssessment {
        val history = lastClosedCandles(candles(analysis.symbol, analysis.timeframe, 300))
        return ExplosionDetector.evaluate(analysis, history)
    }

    fun rawCandles(symbol: String, timeframe: String, limit: Int = 500): List<Candle> = source.candles(symbol, timeframe, limit)
    fun followupCandles(symbol: String, startTime: Long, endTime: Long = System.currentTimeMillis()): List<Candle> {
        val step = BinanceMarketDataSource.intervalMillis("5m")
        val cached = db.cachedCandlesBetween(symbol, "5m", (startTime - step).coerceAtLeast(0L), endTime)
        val cacheCoversStart = cached.firstOrNull()?.openTime?.let { it <= startTime } == true
        val fetchStart = if (cacheCoversStart && cached.isNotEmpty()) cached.last().openTime + step else startTime
        val downloaded = if (fetchStart + step <= endTime) source.candlesBetween(symbol, "5m", fetchStart, endTime) else emptyList()
        var merged = (cached + downloaded).associateBy { it.openTime }.values.sortedBy { it.openTime }
            .filter { it.closeTime <= endTime }
        if (!cacheCoversStart || (merged.size >= 2 && !continuous(merged, "5m"))) {
            merged = source.candlesBetween(symbol, "5m", startTime, endTime)
        }
        db.upsertCandles(symbol, "5m", merged)
        return merged
    }

    private fun candles(symbol: String, timeframe: String, limit: Int): List<Candle> {
        val key = "$symbol-$timeframe-$limit"
        val now = System.currentTimeMillis()
        val ttl = minOf(120_000L, BinanceMarketDataSource.intervalMillis(timeframe) / 4)
        cache[key]?.takeIf { now - it.at <= ttl }?.let { return it.candles }

        val persisted = db.cachedCandles(symbol, timeframe, limit)
        val result = if (persisted.size >= 210) {
            try {
                val latest = source.candles(symbol, timeframe, 5, requireIndicatorHistory = false)
                val merged = (persisted + latest).associateBy { it.openTime }.values.sortedBy { it.openTime }.takeLast(limit)
                if (merged.size >= 210 && continuous(merged.takeLast(minOf(30, merged.size)), timeframe)) merged
                else source.candles(symbol, timeframe, limit)
            } catch (_: Throwable) { source.candles(symbol, timeframe, limit) }
        } else source.candles(symbol, timeframe, limit)

        db.upsertCandles(symbol, timeframe, result)
        cache[key] = CacheEntry(now, result)
        return result
    }

    private fun indicators(symbol: String, timeframe: String, candles: List<Candle>, cacheAllowed: Boolean): IndicatorSet {
        val closeTime = candles.last().closeTime
        if (cacheAllowed) db.indicatorPayload(symbol, timeframe, closeTime)?.let { IndicatorSetCodec.decode(it) }?.let { return it }
        val previous = if (cacheAllowed && candles.size >= 2) {
            db.indicatorPayload(symbol, timeframe, candles[candles.lastIndex - 1].closeTime)?.let(IndicatorSetCodec::decodeComputation)
        } else null
        val computation = previous?.let { TechnicalEngine.calculateIncremental(candles, it.state) }
            ?: TechnicalEngine.calculateWithState(candles)
        if (cacheAllowed) db.saveIndicatorPayload(symbol, timeframe, closeTime, IndicatorSetCodec.encode(computation))
        return computation.indicators
    }

    private fun buildMtf(symbol: String, primary: String, closed: Boolean): MultiTimeframeContext {
        val wanted = linkedSetOf("15m", primary, "4h", "1d")
        val regimes = linkedMapOf<String, Regime>()
        val dirs = linkedMapOf<String, Direction>()
        var score = 0
        for (tf in wanted) {
            try {
                val c = candles(symbol, tf, 260)
                val usable = if (closed) lastClosedCandles(c) else c
                val i = indicators(symbol, tf, usable, closed)
                regimes[tf] = i.regime
                val d = when {
                    i.price > i.ema20 && i.plusDi >= i.minusDi -> Direction.BUY
                    i.price < i.ema20 && i.minusDi > i.plusDi -> Direction.SELL
                    else -> Direction.WAIT
                }
                dirs[tf] = d
                val weight = when (tf) { "1d", "4h" -> 5; primary -> 4; else -> 2 }
                score += when (d) { Direction.BUY, Direction.REVERSAL_BUY -> weight; Direction.SELL, Direction.REVERSAL_SELL -> -weight; else -> 0 }
            } catch (_: Throwable) { }
        }
        return MultiTimeframeContext(regimes, dirs, score.coerceIn(-16, 16), wanted.joinToString(" • ") { tf -> "$tf:${dirs[tf]?.name ?: "N/D"}" })
    }

    private fun continuous(candles: List<Candle>, timeframe: String): Boolean {
        if (candles.size < 2) return false
        val expected = BinanceMarketDataSource.intervalMillis(timeframe)
        return candles.zipWithNext().all { (a, b) -> abs((b.openTime - a.openTime) - expected) <= expected / 5 }
    }

    private fun lastClosedCandles(candles: List<Candle>): List<Candle> {
        val now = System.currentTimeMillis()
        val closed = candles.dropLastWhile { it.closeTime > now }
        require(closed.size >= 210) { "Histórico cerrado insuficiente: ${closed.size} velas" }
        return closed
    }
}
