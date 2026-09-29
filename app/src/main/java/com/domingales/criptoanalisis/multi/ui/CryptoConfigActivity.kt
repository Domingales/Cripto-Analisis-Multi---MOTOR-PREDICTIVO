package com.domingales.criptoanalisis.multi.ui

import android.app.Activity
import android.os.Bundle
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ScrollView
import android.widget.Toast
import com.domingales.criptoanalisis.multi.util.Prefs

class CryptoConfigActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val symbol = intent.getStringExtra("symbol") ?: return finish()
        val s = ScrollView(this); val r = Ui.root(this); s.addView(r)
        r.addView(Ui.title(this, "Configurar $symbol"))
        r.addView(Ui.subtitle(this, "Cada cripto puede tener intervalos, umbral, direcciones, sonido y vibración propios."))
        val selected = Prefs.symbolTimeframes(this, symbol)
        val boxes = listOf("15m","30m","1h","4h","1d").associateWith { tf -> CheckBox(this).apply { text=tf; isChecked=selected.contains(tf); textSize=16f } }
        boxes.values.forEach { r.addView(it) }
        r.addView(Ui.text(this,"Umbral individual (55–95 %)",16f,true))
        val th = EditText(this).apply { inputType=2; setText(Prefs.symbolThreshold(this@CryptoConfigActivity,symbol).toString()) }; r.addView(th)
        val buy = CheckBox(this).apply { text="Permitir alertas de COMPRA"; isChecked=Prefs.symbolAllowBuy(this@CryptoConfigActivity,symbol) }; r.addView(buy)
        val sell = CheckBox(this).apply { text="Permitir alertas de VENTA"; isChecked=Prefs.symbolAllowSell(this@CryptoConfigActivity,symbol) }; r.addView(sell)
        val sound = CheckBox(this).apply { text="Sonido para $symbol"; isChecked=Prefs.symbolSoundEnabled(this@CryptoConfigActivity,symbol) }; r.addView(sound)
        val vibration = CheckBox(this).apply { text="Vibración para $symbol"; isChecked=Prefs.symbolVibrationEnabled(this@CryptoConfigActivity,symbol) }; r.addView(vibration)
        r.addView(Ui.button(this,"GUARDAR") {
            Prefs.setSymbolTimeframes(this,symbol,boxes.filterValues{it.isChecked}.keys)
            Prefs.setSymbolThreshold(this,symbol,th.text.toString().toIntOrNull()?:Prefs.threshold(this))
            Prefs.setSymbolAllowBuy(this,symbol,buy.isChecked); Prefs.setSymbolAllowSell(this,symbol,sell.isChecked)
            Prefs.setSymbolSoundEnabled(this,symbol,sound.isChecked); Prefs.setSymbolVibrationEnabled(this,symbol,vibration.isChecked)
            Toast.makeText(this,"Configuración de $symbol guardada",Toast.LENGTH_SHORT).show(); finish()
        })
        setContentView(s)
    }
}
