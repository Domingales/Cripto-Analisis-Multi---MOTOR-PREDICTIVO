package com.domingales.criptoanalisis.multi.data

import com.domingales.criptoanalisis.multi.domain.Candle
import com.domingales.criptoanalisis.multi.domain.CryptoAsset
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs

class BinanceMarketDataSource {
    private val allowedIntervals = setOf("15m", "30m", "1h", "4h", "1d")
    private val internalIntervals = allowedIntervals + "5m"

    fun candles(symbol: String, interval: String, limit: Int = 260, requireIndicatorHistory: Boolean = true): List<Candle> {
        val safeInterval = if (interval in allowedIntervals) interval else "1h"
        val pair = symbol.uppercase() + "USDT"
        val url = "https://api.binance.com/api/v3/klines?symbol=$pair&interval=$safeInterval&limit=${limit.coerceIn(2, 1000)}"
        val body = requestWithRetry(url)
        val out = parseCandles(body)
        validateCandles(out, safeInterval, if (requireIndicatorHistory) 210 else 2)
        return out
    }

    /** Velas de 5 minutos para reconstruir seguimientos sin usar el precio tardío del ciclo. */
    fun candlesBetween(symbol: String, interval: String = "5m", startTime: Long, endTime: Long): List<Candle> {
        require(endTime > startTime) { "Rango temporal inválido" }
        val safeInterval = interval.takeIf { it in internalIntervals } ?: "5m"
        val step = intervalMillis(safeInterval)
        val safeStart = (startTime - step).coerceAtLeast(0L)
        val pair = symbol.uppercase() + "USDT"
        val merged = linkedMapOf<Long, Candle>()
        var cursor = safeStart
        var pages = 0
        while (cursor <= endTime && pages < 10) {
            val url = "https://api.binance.com/api/v3/klines?symbol=$pair&interval=$safeInterval&startTime=$cursor&endTime=$endTime&limit=1000"
            val page = parseCandles(requestWithRetry(url)).filter { it.closeTime <= endTime }
            if (page.isEmpty()) break
            page.forEach { merged[it.openTime] = it }
            val next = page.last().openTime + step
            if (next <= cursor || page.size < 1000) break
            cursor = next
            pages++
        }
        val out = merged.values.sortedBy { it.openTime }
        validateCandles(out, safeInterval, 1, requireFresh = false)
        return out
    }

    private fun parseCandles(body: String): List<Candle> {
        val arr = JSONArray(body)
        return ArrayList<Candle>(arr.length()).apply {
            for (idx in 0 until arr.length()) {
                val k = arr.getJSONArray(idx)
                add(Candle(k.getLong(0), k.getString(1).toDouble(), k.getString(2).toDouble(), k.getString(3).toDouble(),
                    k.getString(4).toDouble(), k.getString(5).toDouble(), k.getLong(6)))
            }
        }
    }

    fun currentPrice(symbol: String): Double {
        val body = requestWithRetry("https://api.binance.com/api/v3/ticker/price?symbol=${symbol.uppercase()}USDT")
        val p = JSONObject(body).getString("price").toDouble()
        require(p > 0.0 && p.isFinite()) { "Precio inválido" }
        return p
    }

    /** Catálogo Spot USDT real, sin imponer un límite artificial de monedas. */
    fun availableAssets(): List<CryptoAsset> {
        val root = JSONObject(requestWithRetry("https://api.binance.com/api/v3/exchangeInfo"))
        val symbols = root.getJSONArray("symbols")
        val out = linkedMapOf<String, CryptoAsset>()
        for (index in 0 until symbols.length()) {
            val item = symbols.getJSONObject(index)
            val base = item.optString("baseAsset").uppercase()
            val spot = !item.has("isSpotTradingAllowed") || item.optBoolean("isSpotTradingAllowed", true)
            if (item.optString("status") != "TRADING" || item.optString("quoteAsset") != "USDT" || !spot) continue
            if (base.isBlank()) continue
            out[base] = CryptoAsset(base, base)
        }
        require(out.isNotEmpty()) { "Catálogo Binance vacío" }
        return out.values.sortedBy { it.symbol }
    }

    private fun requestWithRetry(url: String, attempts: Int = 3): String {
        var last: Throwable? = null
        repeat(attempts) { attempt ->
            try {
                return request(url)
            } catch (t: Throwable) {
                last = t
                if (attempt < attempts - 1) Thread.sleep((700L shl attempt).coerceAtMost(2800L))
            }
        }
        throw IllegalStateException("Error de mercado tras $attempts intentos: ${last?.message}", last)
    }

    private fun request(urlText: String): String {
        val conn = (URL(urlText).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 12_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "CriptoAnalisisMulti/2.0")
        }
        try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code == 429 || code == 418) throw IllegalStateException("Binance rate-limit HTTP $code")
            if (code !in 200..299) throw IllegalStateException("Binance HTTP $code: ${body.take(180)}")
            if (body.isBlank()) throw IllegalStateException("Respuesta vacía de Binance")
            return body
        } finally {
            conn.disconnect()
        }
    }

    private fun validateCandles(c: List<Candle>, interval: String, minimum: Int, requireFresh: Boolean = true) {
        require(c.size >= minimum) { "Histórico insuficiente: ${c.size} velas" }
        var prev = -1L
        for (x in c) {
            require(x.openTime > prev) { "Velas desordenadas o duplicadas" }
            require(x.open > 0 && x.close > 0 && x.high > 0 && x.low > 0) { "OHLC inválido" }
            require(x.high >= maxOf(x.open, x.close, x.low)) { "Máximo incoherente" }
            require(x.low <= minOf(x.open, x.close, x.high)) { "Mínimo incoherente" }
            require(x.volume >= 0 && x.volume.isFinite()) { "Volumen inválido" }
            prev = x.openTime
        }
        val expected = intervalMillis(interval)
        val checked = if (requireFresh) c.takeLast(20) else c
        for (i in 1 until checked.size) {
            val gap = checked[i].openTime - checked[i - 1].openTime
            require(abs(gap - expected) <= expected / 5) { "Hueco temporal anormal (${gap / 1000}s)" }
        }
        val last = c.last()
        require(last.closeTime > last.openTime) { "Timestamp de vela incoherente" }
        if (requireFresh) {
            val maxAge = expected * 2 + 10 * 60_000L
            require(System.currentTimeMillis() - last.openTime < maxAge) { "Datos demasiado atrasados" }
        }
    }

    companion object {
        fun intervalMillis(interval: String): Long = when (interval) {
            "5m" -> 5 * 60_000L
            "15m" -> 15 * 60_000L
            "30m" -> 30 * 60_000L
            "1h" -> 60 * 60_000L
            "4h" -> 4 * 60 * 60_000L
            "1d" -> 24 * 60 * 60_000L
            else -> 60 * 60_000L
        }
    }
}
