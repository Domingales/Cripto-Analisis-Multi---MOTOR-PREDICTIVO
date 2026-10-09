"""Generate a research-only copy of BacktestEngine, never edit production.

Closed-window lookup and memoization preserve parity on complete inputs.
Acceptance cooldown is recorded before reading future outcome availability:
the untouched engine omits that update for MISSING_5M_DATA, a temporal leak.
CI proves parity on complete inputs and explicitly tests this audited exception.
"""
import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DOMAIN = ROOT / 'app/src/main/java/com/domingales/criptoanalisis/multi/domain'


def generate():
    source = (DOMAIN / 'BacktestEngine.kt').read_text()
    original_hash = hashlib.sha256(source.encode()).hexdigest()
    s = source.replace('object BacktestEngine {', 'object ExpandedResearchEngine {')
    def change(old, new):
        nonlocal s
        if s.count(old) != 1:
            raise ValueError('Source drift: expected exactly one adapter anchor: '+old[:60])
        s = s.replace(old, new)
    change('val mtf = buildHistoricalMtf(timeframe, indicators, asOf, candlesByTimeframe)',
           'val mtf = buildHistoricalMtf(timeframe, indicators, asOf, candlesByTimeframe, indicatorCache)')
    change('val btcRegime = historicalBtcRegime(symbol, asOf, btcCandles4h)',
           'val btcRegime = historicalBtcRegime(symbol, asOf, btcCandles4h, indicatorCache)')
    change('var signals = 0; var hits = 0; var fails = 0; var neutral = 0',
           'val indicatorCache = mutableMapOf<String, Pair<Long, IndicatorSet>>()\n        var signals = 0; var hits = 0; var fails = 0; var neutral = 0')
    change('onAccepted?.invoke(analysis)\n                val entry = indicators.price',
           'onAccepted?.invoke(analysis)\n                // Acceptance is known now; future data gaps cannot alter cooldown.\n                lastAccepted[side] = asOf to analysis.confidence\n                val entry = indicators.price')
    change('candlesByTimeframe: Map<String, List<Candle>>\n    ): MultiTimeframeContext',
           'candlesByTimeframe: Map<String, List<Candle>>,\n        cache: MutableMap<String, Pair<Long, IndicatorSet>>\n    ): MultiTimeframeContext')
    change('val historical = allCandles.asSequence().filter { it.closeTime <= asOf }.sortedBy { it.openTime }.toList()\n            if (historical.size >= 210) add(tf, TechnicalEngine.calculate(historical.takeLast(300)))',
           'val historical = closedSlice(allCandles, asOf)\n            if (historical.size >= 210) add(tf, cachedIndicators(tf, historical, cache))')
    change('private fun historicalBtcRegime(symbol: String, asOf: Long, candles: List<Candle>): Regime? {',
           'private fun historicalBtcRegime(symbol: String, asOf: Long, candles: List<Candle>, cache: MutableMap<String, Pair<Long, IndicatorSet>>): Regime? {')
    change('val historical = candles.asSequence().filter { it.closeTime <= asOf }.sortedBy { it.openTime }.toList()\n        return if (historical.size < 210) null else TechnicalEngine.calculate(historical.takeLast(300)).regime',
           'val historical = closedSlice(candles, asOf)\n        return if (historical.size < 210) null else cachedIndicators("BTC_CONTEXT", historical, cache).regime')
    change('val exact = OutcomeTracker.candlesStrictlyAfter(asOf, outcomeEnd, fiveMinute)',
           'val exact = fiveMinute.subList(upperClose(fiveMinute, asOf), upperClose(fiveMinute, outcomeEnd))')
    helpers = '''
    private fun upperClose(c: List<Candle>, t: Long): Int {
        var lo = 0; var hi = c.size
        while (lo < hi) { val m = (lo+hi)/2; if (c[m].closeTime <= t) lo=m+1 else hi=m }
        return lo
    }
    private fun closedSlice(c: List<Candle>, t: Long): List<Candle> {
        val end = upperClose(c,t)
        return c.subList(max(0,end-300),end)
    }
    private fun cachedIndicators(key: String, c: List<Candle>, cache: MutableMap<String, Pair<Long, IndicatorSet>>): IndicatorSet {
        val time = c.last().closeTime
        val old = cache[key]
        if (old != null && old.first == time) return old.second
        val value = TechnicalEngine.calculate(c)
        cache[key] = time to value
        return value
    }
'''
    s = s.rsplit('}', 1)[0]+helpers+'}\n'
    target = ROOT / 'app/src/test/java/com/domingales/criptoanalisis/multi/domain/ExpandedResearchEngine.kt'
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text('// Generated research adapter. Production SHA256: '+original_hash+'\n'+s)
    return original_hash


if __name__ == '__main__':
    print(generate())
