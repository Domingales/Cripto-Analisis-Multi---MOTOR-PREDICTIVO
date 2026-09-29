package com.domingales.criptoanalisis.multi.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ScrollView
import com.domingales.criptoanalisis.multi.data.AppDatabase
import com.domingales.criptoanalisis.multi.util.Prefs

class AnalysisSelectorActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this); val root = Ui.root(this); scroll.addView(root)
        root.addView(Ui.title(this, "Análisis por criptomoneda"))
        root.addView(Ui.subtitle(this, "Abre la ficha técnica, el gráfico, la predicción y el seguimiento de cada activo."))
        val analyses = AppDatabase.get(this).dashboard().associateBy { it.symbol }
        Prefs.watchlist(this).sorted().forEach { symbol ->
            val item = analyses[symbol]
            val card = Ui.card(this)
            card.addView(Ui.text(this, symbol, 18f, true))
            card.addView(Ui.text(this, item?.let { "${it.timeframe} • ${it.state} • confianza ${it.confidence}% • variación ${"%+.2f".format(it.changePct)}%" } ?: "Pendiente del primer análisis"))
            card.setOnClickListener { startActivity(Intent(this, CryptoDetailActivity::class.java).putExtra("symbol", symbol)) }
            root.addView(card)
        }
        setContentView(scroll)
    }
}
