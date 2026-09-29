package com.domingales.criptoanalisis.multi.data

import com.domingales.criptoanalisis.multi.domain.IndicatorSet
import com.domingales.criptoanalisis.multi.domain.MarketStructure
import com.domingales.criptoanalisis.multi.domain.Regime
import com.domingales.criptoanalisis.multi.domain.SupportResistanceZone
import com.domingales.criptoanalisis.multi.domain.TechnicalEngine
import org.json.JSONArray
import org.json.JSONObject

object IndicatorSetCodec {
    private const val SCHEMA_VERSION = 2

    fun encode(i: IndicatorSet): String = JSONObject().apply { put("schema", SCHEMA_VERSION); put("indicator", indicatorObject(i)) }.toString()

    fun encode(computation: TechnicalEngine.Computation): String = JSONObject().apply {
        put("schema", SCHEMA_VERSION)
        put("indicator", indicatorObject(computation.indicators))
        put("state", stateObject(computation.state))
    }.toString()

    private fun indicatorObject(i: IndicatorSet): JSONObject = JSONObject().apply {
        put("price", i.price); put("ema20", i.ema20); put("ema50", i.ema50); put("ema200", i.ema200)
        put("ema20SlopePct", i.ema20SlopePct); put("ema50SlopePct", i.ema50SlopePct); put("rsi14", i.rsi14); put("rsiSlope", i.rsiSlope)
        put("rsiBullishDivergence", i.rsiBullishDivergence); put("rsiBearishDivergence", i.rsiBearishDivergence)
        put("atr14", i.atr14); put("atrPct", i.atrPct); put("adx14", i.adx14); put("plusDi", i.plusDi); put("minusDi", i.minusDi)
        put("volumeRatio", i.volumeRatio); put("volumeExpansion", i.volumeExpansion); put("support", i.support); put("resistance", i.resistance)
        put("supportZone", zone(i.supportZone)); put("resistanceZone", zone(i.resistanceZone)); put("structure", i.structure.name); put("regime", i.regime.name)
        put("breakoutUp", i.breakoutUp); put("breakoutDown", i.breakoutDown); put("falseBreakout", i.falseBreakout); put("expectedMoveAtr", i.expectedMoveAtr)
    }

    fun decode(raw: String): IndicatorSet? = try {
        val o = JSONObject(raw)
        if (o.optInt("schema", 0) != SCHEMA_VERSION) null else decodeIndicator(o.getJSONObject("indicator"))
    } catch (_: Throwable) { null }

    fun decodeComputation(raw: String): TechnicalEngine.Computation? = try {
        val root = JSONObject(raw)
        require(root.optInt("schema", 0) == SCHEMA_VERSION) { "Caché de indicadores anterior" }
        val indicator = decodeIndicator(root.getJSONObject("indicator"))
        val state = decodeState(root.getJSONObject("state"))
        TechnicalEngine.Computation(indicator, state)
    } catch (_: Throwable) { null }

    private fun decodeIndicator(o: JSONObject): IndicatorSet =
        IndicatorSet(
            price=o.getDouble("price"), ema20=o.getDouble("ema20"), ema50=o.getDouble("ema50"), ema200=o.getDouble("ema200"),
            ema20SlopePct=o.getDouble("ema20SlopePct"), ema50SlopePct=o.getDouble("ema50SlopePct"), rsi14=o.getDouble("rsi14"), rsiSlope=o.getDouble("rsiSlope"),
            rsiBullishDivergence=o.getBoolean("rsiBullishDivergence"), rsiBearishDivergence=o.getBoolean("rsiBearishDivergence"),
            atr14=o.getDouble("atr14"), atrPct=o.getDouble("atrPct"), adx14=o.getDouble("adx14"), plusDi=o.getDouble("plusDi"), minusDi=o.getDouble("minusDi"),
            volumeRatio=o.getDouble("volumeRatio"), volumeExpansion=o.getBoolean("volumeExpansion"), support=o.getDouble("support"), resistance=o.getDouble("resistance"),
            supportZone=zone(o.getJSONObject("supportZone")), resistanceZone=zone(o.getJSONObject("resistanceZone")),
            structure=MarketStructure.valueOf(o.getString("structure")), regime=Regime.valueOf(o.getString("regime")),
            breakoutUp=o.getBoolean("breakoutUp"), breakoutDown=o.getBoolean("breakoutDown"), falseBreakout=o.getBoolean("falseBreakout"), expectedMoveAtr=o.getDouble("expectedMoveAtr")
        )

    private fun stateObject(s: TechnicalEngine.IncrementalState) = JSONObject().apply {
        put("candleClose", s.candleClose); put("ema20", s.ema20); put("ema50", s.ema50); put("ema200", s.ema200)
        put("ema20History", JSONArray(s.ema20History)); put("ema50History", JSONArray(s.ema50History))
        put("rsiAverageGain", s.rsiAverageGain); put("rsiAverageLoss", s.rsiAverageLoss); put("rsiHistory", JSONArray(s.rsiHistory))
    }

    private fun decodeState(o: JSONObject) = TechnicalEngine.IncrementalState(
        candleClose = o.getLong("candleClose"), ema20 = o.getDouble("ema20"), ema50 = o.getDouble("ema50"), ema200 = o.getDouble("ema200"),
        ema20History = doubles(o.getJSONArray("ema20History")), ema50History = doubles(o.getJSONArray("ema50History")),
        rsiAverageGain = o.getDouble("rsiAverageGain"), rsiAverageLoss = o.getDouble("rsiAverageLoss"), rsiHistory = doubles(o.getJSONArray("rsiHistory"))
    )

    private fun doubles(values: JSONArray) = (0 until values.length()).map { values.getDouble(it) }

    private fun zone(z: SupportResistanceZone) = JSONObject().apply { put("low", z.low); put("high", z.high); put("touches", z.touches); put("strength", z.strength) }
    private fun zone(o: JSONObject) = SupportResistanceZone(o.getDouble("low"), o.getDouble("high"), o.getInt("touches"), o.getInt("strength"))
}
