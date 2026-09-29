package com.domingales.criptoanalisis.multi.ui
import android.app.Activity
import android.os.Bundle
import android.widget.ScrollView
import com.domingales.criptoanalisis.multi.data.AppDatabase
class HistoryActivity:Activity(){override fun onCreate(b:Bundle?){super.onCreate(b);val s=ScrollView(this);val r=Ui.root(this);s.addView(r);r.addView(Ui.title(this,"Historial de señales"));r.addView(Ui.subtitle(this,"Cada señal conserva estado, seguimiento, MFE y MAE."));val h=AppDatabase.get(this).history(500);if(h.isEmpty())r.addView(Ui.subtitle(this,"Todavía no hay señales registradas."));h.forEach{val c=Ui.card(this);c.addView(Ui.text(this,it,14f));r.addView(c)};setContentView(s)}}
