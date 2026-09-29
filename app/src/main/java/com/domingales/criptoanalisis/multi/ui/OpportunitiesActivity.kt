package com.domingales.criptoanalisis.multi.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.ScrollView
import com.domingales.criptoanalisis.multi.data.AppDatabase

class OpportunitiesActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val s=ScrollView(this); val r=Ui.root(this); s.addView(r)
        r.addView(Ui.title(this,"Radar de oportunidades"))
        r.addView(Ui.subtitle(this,"Ordenado por confianza. Distingue oportunidad, proximidad y ausencia de señal."))
        val items=AppDatabase.get(this).dashboard().sortedByDescending{it.confidence}
        addGroup(r, "SEÑALES EN SEGUIMIENTO", items.filter { it.activeFollowups > 0 }, Ui.statusColor("FOLLOWING"))
        addGroup(r, "SEÑALES / ALTA PRIORIDAD", items.filter { it.activeFollowups == 0 && it.state == "SIGNAL" }, Color.rgb(70,205,130))
        addGroup(r, "CERCA DEL UMBRAL / PRESEÑAL", items.filter { it.activeFollowups == 0 && it.state == "PRESIGNAL" }, Ui.statusColor("PRESIGNAL"))
        addGroup(r, "VIGILANDO / NO OPERAR", items.filter { it.activeFollowups == 0 && it.state != "SIGNAL" && it.state != "PRESIGNAL" }, Color.LTGRAY)
        if(items.isEmpty())r.addView(Ui.subtitle(this,"Aún no hay evaluaciones."))
        setContentView(s)
    }
    private fun addGroup(root: android.widget.LinearLayout, title:String, items:List<com.domingales.criptoanalisis.multi.domain.DashboardItem>, color:Int){
        root.addView(Ui.section(this,title))
        if(items.isEmpty()){root.addView(Ui.subtitle(this,"Ninguna cripto en este grupo."));return}
        items.forEachIndexed{idx,item->
            val decision=when(item.state){"SIGNAL"->item.direction.replace('_',' ');"PRESIGNAL"->"PRESEÑAL ${item.direction.replace('_',' ')}";else->"NO OPERAR • ${item.direction.replace('_',' ')} candidata"}
            val c=Ui.card(this);c.addView(Ui.badge(this,"${idx+1}. ${item.symbol} • $decision • ${item.confidence}%",color))
            c.addView(Ui.text(this,"Score ${item.score}/100 • Prob. ${item.probability?.let{x->"$x%"}?:"sin calibrar"} • ${item.timeframe} • Riesgo ${item.risk}",13f))
            c.addView(Ui.text(this,"Régimen ${item.regime} • Precio ${if(item.price>=1000)"%.2f".format(item.price) else "%.6f".format(item.price)} • Variación ${"%+.2f".format(item.changePct)}%",12f))
            c.setOnClickListener{startActivity(Intent(this,CryptoDetailActivity::class.java).putExtra("symbol",item.symbol))};root.addView(c)
        }
    }
}
