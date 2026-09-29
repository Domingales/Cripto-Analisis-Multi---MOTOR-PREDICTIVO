package com.domingales.criptoanalisis.multi.data

import com.domingales.criptoanalisis.multi.domain.FundamentalContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class NewsSentimentRepository {
    private val positive = setOf("surge", "gain", "bull", "rally", "approve", "approval", "adoption", "record", "upgrade", "growth", "positive")
    private val negative = setOf("hack", "drop", "bear", "ban", "lawsuit", "outage", "exploit", "fraud", "decline", "risk", "negative")

    fun context(symbol: String): FundamentalContext {
        val now = System.currentTimeMillis()
        return try {
            val conn = (URL("https://www.coindesk.com/arc/outboundfeeds/rss/").openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 6500
                setRequestProperty("User-Agent", "CriptoAnalisisMulti/2.0")
            }
            val xml = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val titles = Regex("<title><!\\[CDATA\\[(.*?)]]></title>|<title>(.*?)</title>", RegexOption.IGNORE_CASE)
                .findAll(xml).map { (it.groups[1]?.value ?: it.groups[2]?.value ?: "").replace(Regex("<.*?>"), "") }.filter { it.isNotBlank() }.take(80).toList()
            val aliases = aliases(symbol)
            val relevant = titles.filter { title -> aliases.any { a -> title.lowercase(Locale.ROOT).contains(a) } }.take(12)
            var pos = 0
            var neg = 0
            relevant.forEach { t ->
                val words = t.lowercase(Locale.ROOT).split(Regex("[^a-z0-9]+"))
                pos += words.count { it in positive }
                neg += words.count { it in negative }
            }
            val score = ((pos - neg) * 2).coerceIn(-10, 10)
            val summary = when {
                relevant.isEmpty() -> "sin titulares específicos recientes; ajuste neutral"
                score >= 4 -> "sesgo informativo positivo ($pos+ / $neg-)"
                score <= -4 -> "sesgo informativo negativo ($pos+ / $neg-)"
                else -> "noticias mixtas/neutrales ($pos+ / $neg-)"
            }
            FundamentalContext(score, relevant.size, pos, neg, summary, now)
        } catch (_: Throwable) {
            FundamentalContext(0, 0, 0, 0, "fuente de noticias no disponible; no modifica la señal", now)
        }
    }

    private fun aliases(symbol: String): Set<String> = when (symbol.uppercase()) {
        "BTC" -> setOf("bitcoin", "btc")
        "ETH" -> setOf("ethereum", "ether", "eth")
        "ADA" -> setOf("cardano", "ada")
        "SOL" -> setOf("solana", "sol")
        "XRP" -> setOf("xrp", "ripple")
        "DOGE" -> setOf("dogecoin", "doge")
        "BNB" -> setOf("bnb", "binance coin")
        "LINK" -> setOf("chainlink", "link")
        else -> setOf(symbol.lowercase(Locale.ROOT))
    }
}
