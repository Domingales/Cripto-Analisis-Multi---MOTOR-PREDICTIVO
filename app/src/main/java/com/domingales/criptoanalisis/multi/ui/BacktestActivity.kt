package com.domingales.criptoanalisis.multi.ui

import android.app.Activity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.domingales.criptoanalisis.multi.data.MarketRepository
import com.domingales.criptoanalisis.multi.domain.BacktestEngine
import com.domingales.criptoanalisis.multi.domain.Candle
import com.domingales.criptoanalisis.multi.util.Prefs
import java.util.concurrent.Executors

class BacktestActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = Ui.root(this)
        root.addView(Ui.title(this, "Backtest multitemporal"))
        val symbolSpinner = Spinner(this)
        val symbols = Prefs.watchlist(this).sorted().ifEmpty { listOf("BTC") }
        symbolSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, symbols)
        root.addView(symbolSpinner)
        val timeframeSpinner = Spinner(this)
        val timeframes = listOf("15m", "30m", "1h", "4h", "1d")
        timeframeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, timeframes)
        timeframeSpinner.setSelection(timeframes.indexOf(Prefs.timeframe(this)).coerceAtLeast(0))
        root.addView(timeframeSpinner)
        val output = Ui.text(this, "Usa hasta 1000 velas por intervalo. Ejecuta aprendizaje walk-forward, contexto BTC histórico, cooldown y primer toque dentro de una ventana adaptada al intervalo. Nunca usa datos posteriores a la fecha evaluada.", 14f)
        root.addView(output)
        root.addView(Ui.button(this, "EJECUTAR BACKTEST MTF") {
            output.text = "Descargando históricos y calculando sin anticipación de datos…"
            executor.execute {
                try {
                    val symbol = symbolSpinner.selectedItem.toString()
                    val timeframe = timeframeSpinner.selectedItem.toString()
                    val repository = MarketRepository(this)
                    val series = linkedMapOf<String, List<Candle>>()
                    for (tf in timeframes) series[tf] = repository.rawCandles(symbol, tf, 1000)
                    val btc4h = if (symbol == "BTC") emptyList() else repository.rawCandles("BTC", "4h", 1000)
                    val result = BacktestEngine.run(
                        symbol = symbol,
                        timeframe = timeframe,
                        candlesByTimeframe = series,
                        threshold = Prefs.symbolThreshold(this, symbol),
                        cooldownMinutes = Prefs.cooldownMinutes(this),
                        btcCandles4h = btc4h,
                        allowBuy = Prefs.symbolAllowBuy(this, symbol),
                        allowSell = Prefs.symbolAllowSell(this, symbol)
                    )
                    runOnUiThread {
                        output.text = "$symbol $timeframe\n" +
                            "Ventanas MTF válidas ${result.evaluatedWindows} • omitidas sin histórico MTF ${result.skippedWithoutMtf}\n" +
                            "Ventanas con contexto BTC ${result.btcContextWindows} • casos walk-forward utilizados ${result.walkForwardCasesUsed}\n" +
                            "Señales ${result.signals}\n" +
                            "Aciertos ${result.hits} • Fallos ${result.fails} • Neutras ${result.neutral}\n" +
                            "Precisión resueltas ${"%.1f".format(result.accuracy)}%\n\n" +
                            "BUY: ${result.buy.signals} señales • ${result.buy.hits} aciertos • ${result.buy.fails} fallos • ${result.buy.neutral} neutras • precisión ${"%.1f".format(result.buy.accuracy)}%\n" +
                            "SELL: ${result.sell.signals} señales • ${result.sell.hits} aciertos • ${result.sell.fails} fallos • ${result.sell.neutral} neutras • precisión ${"%.1f".format(result.sell.accuracy)}%\n\n" +
                            result.contextNote
                    }
                } catch (t: Throwable) {
                    runOnUiThread { output.text = "Error: ${t.message}" }
                }
            }
        })
        setContentView(root)
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
