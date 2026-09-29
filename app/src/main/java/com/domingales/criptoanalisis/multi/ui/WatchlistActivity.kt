package com.domingales.criptoanalisis.multi.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import com.domingales.criptoanalisis.multi.data.BinanceMarketDataSource
import com.domingales.criptoanalisis.multi.domain.Assets
import com.domingales.criptoanalisis.multi.domain.CryptoAsset
import com.domingales.criptoanalisis.multi.util.Prefs
import java.util.concurrent.Executors

class WatchlistActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val selected = linkedSetOf<String>()
    private val favorites = linkedSetOf<String>()
    private val catalog = linkedMapOf<String, CryptoAsset>()
    private lateinit var list: LinearLayout
    private lateinit var search: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selected += Prefs.watchlist(this); favorites += Prefs.favorites(this)
        Assets.supported.forEach { catalog[it.symbol] = it }
        Prefs.catalogSymbols(this).forEach { catalog.putIfAbsent(it, CryptoAsset(it, it)) }

        val scroll = ScrollView(this); val root = Ui.root(this); scroll.addView(root)
        root.addView(Ui.title(this, "Mis criptos"))
        root.addView(Ui.subtitle(this, "Busca en el catálogo Spot USDT, marca varias monedas y usa ★ para conservar favoritas."))
        search = EditText(this).apply { hint = "Buscar BTC, Cardano, SOL…" }
        root.addView(search)
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(Ui.button(this, "MARCAR VISIBLES") { visibleSymbols().forEach(selected::add); renderList() }, LinearLayout.LayoutParams(0, -2, 1f))
        actions.addView(Ui.button(this, "DESMARCAR VISIBLES") { visibleSymbols().forEach(selected::remove); renderList() }, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(actions)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; root.addView(list)
        root.addView(Ui.button(this, "GUARDAR SELECCIÓN") {
            Prefs.setWatchlist(this, selected.toSet()); Prefs.setFavorites(this, favorites.toSet())
            Toast.makeText(this, "${selected.size} criptomonedas guardadas", Toast.LENGTH_SHORT).show(); finish()
        })
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = renderList()
            override fun afterTextChanged(s: Editable?) = Unit
        })
        renderList(); setContentView(scroll); refreshCatalog()
    }

    private fun visibleSymbols(): List<String> {
        val term = if (::search.isInitialized) search.text.toString().trim().lowercase() else ""
        return catalog.values.filter { term.isBlank() || it.symbol.lowercase().contains(term) || it.name.lowercase().contains(term) }
            .sortedWith(compareByDescending<CryptoAsset> { it.symbol in favorites }.thenBy { it.symbol }).map { it.symbol }
    }

    private fun renderList() {
        if (!::list.isInitialized) return
        list.removeAllViews()
        val visible = visibleSymbols()
        if (visible.isEmpty()) list.addView(Ui.subtitle(this, "No hay coincidencias."))
        visible.forEach { symbol ->
            val asset = catalog.getValue(symbol)
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val check = CheckBox(this).apply {
                text = if (asset.name == asset.symbol) asset.symbol else "${asset.symbol}  ${asset.name}"
                textSize = 15f; isChecked = symbol in selected
                setOnCheckedChangeListener { _, checked -> if (checked) selected += symbol else selected -= symbol }
            }
            row.addView(check, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(Ui.button(this, if (symbol in favorites) "★" else "☆") { if (!favorites.add(symbol)) favorites.remove(symbol); renderList() })
            row.addView(Ui.button(this, "CONFIG.") { startActivity(Intent(this, CryptoConfigActivity::class.java).putExtra("symbol", symbol)) })
            list.addView(row)
        }
    }

    private fun refreshCatalog() {
        executor.execute {
            try {
                val remote = BinanceMarketDataSource().availableAssets()
                Prefs.setCatalogSymbols(this, remote.map { it.symbol }.toSet())
                remote.forEach { catalog[it.symbol] = it }
                runOnUiThread { renderList(); Toast.makeText(this, "Catálogo actualizado: ${catalog.size} criptomonedas", Toast.LENGTH_SHORT).show() }
            } catch (_: Throwable) {
                runOnUiThread { Toast.makeText(this, "Se usa el catálogo guardado; Binance no respondió", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    override fun onDestroy() { executor.shutdownNow(); super.onDestroy() }
}
