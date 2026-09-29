package com.domingales.criptoanalisis.multi.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import com.domingales.criptoanalisis.multi.service.NotificationHelper
import com.domingales.criptoanalisis.multi.service.MonitoringScheduler
import com.domingales.criptoanalisis.multi.util.Prefs

class SettingsActivity : Activity() {
    override fun onCreate(b:Bundle?){
        super.onCreate(b);val s=ScrollView(this);val r=Ui.root(this);s.addView(r);r.addView(Ui.title(this,"Configuración global"))
        r.addView(Ui.subtitle(this,"Los ajustes individuales de una cripto prevalecen sobre estos valores globales."))
        r.addView(Ui.section(this,"Análisis"));r.addView(Ui.text(this,"Intervalo principal",16f,true));val sp=Spinner(this);val tfs=listOf("15m","30m","1h","4h","1d");sp.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,tfs);sp.setSelection(tfs.indexOf(Prefs.timeframe(this)).coerceAtLeast(0));r.addView(sp)
        r.addView(Ui.text(this,"Umbral global de señal (55–95 %)",16f,true));val th=EditText(this).apply{inputType=2;setText(Prefs.threshold(this@SettingsActivity).toString())};r.addView(th)
        r.addView(Ui.text(this,"Revisión solicitada cada N minutos (1–60)",16f,true));val min=EditText(this).apply{inputType=2;setText(Prefs.intervalMinutes(this@SettingsActivity).toString())};r.addView(min)
        r.addView(Ui.text(this,"Cooldown señales equivalentes (15–1440 min)",16f,true));val cool=EditText(this).apply{inputType=2;setText(Prefs.cooldownMinutes(this@SettingsActivity).toString())};r.addView(cool)
        val closed=CheckBox(this).apply{text="Confirmar con VELA CERRADA (más fiable)";isChecked=Prefs.closedCandleMode(this@SettingsActivity)};r.addView(closed)
        val news=CheckBox(this).apply{text="Usar contexto de noticias auxiliar";isChecked=Prefs.newsEnabled(this@SettingsActivity)};r.addView(news)
        val sound=CheckBox(this).apply{text="Sonido global de alertas";isChecked=Prefs.soundEnabled(this@SettingsActivity)};r.addView(sound)
        val vibration=CheckBox(this).apply{text="Vibración global de alertas";isChecked=Prefs.vibrationEnabled(this@SettingsActivity)};r.addView(vibration)
        r.addView(Ui.section(this,"Detector independiente de posibles explosiones"))
        val explosionEnabled=CheckBox(this).apply{text="Activar detector extremo 5/5";isChecked=Prefs.explosionDetectorEnabled(this@SettingsActivity)};r.addView(explosionEnabled)
        val explosionSound=CheckBox(this).apply{text="Sonido propio para posibles explosiones";isChecked=Prefs.explosionSoundEnabled(this@SettingsActivity)};r.addView(explosionSound)
        val explosionVibration=CheckBox(this).apply{text="Vibración propia para posibles explosiones";isChecked=Prefs.explosionVibrationEnabled(this@SettingsActivity)};r.addView(explosionVibration)
        r.addView(Ui.subtitle(this,"Este módulo sólo observa evaluaciones de vela cerrada. No cambia BUY/SELL, score, confianza, probabilidad, aprendizaje, umbral ni contadores existentes."))
        r.addView(Ui.button(this,"PROBAR AVISO DE POSIBLE EXPLOSIÓN"){Prefs.setExplosionSoundEnabled(this,explosionSound.isChecked);Prefs.setExplosionVibrationEnabled(this,explosionVibration.isChecked);NotificationHelper.createChannels(this);NotificationHelper.testExplosion(this)})
        val menuLeft=CheckBox(this).apply{text="Menú hamburguesa a la izquierda";isChecked=Prefs.menuOnLeft(this@SettingsActivity)};r.addView(menuLeft)
        r.addView(Ui.subtitle(this,"El motor MTF puede consultar 15m, intervalo propio, 4h y 1d. Las noticias tienen peso limitado y no crean señales por sí solas."))
        r.addView(Ui.button(this,"GUARDAR CONFIGURACIÓN"){
            Prefs.setTimeframe(this,sp.selectedItem.toString());Prefs.setThreshold(this,th.text.toString().toIntOrNull()?:80);Prefs.setIntervalMinutes(this,min.text.toString().toIntOrNull()?:5);Prefs.setCooldownMinutes(this,cool.text.toString().toIntOrNull()?:60);Prefs.setClosedCandleMode(this,closed.isChecked);Prefs.setNewsEnabled(this,news.isChecked);Prefs.setSoundEnabled(this,sound.isChecked);Prefs.setVibrationEnabled(this,vibration.isChecked);Prefs.setExplosionDetectorEnabled(this,explosionEnabled.isChecked);Prefs.setExplosionSoundEnabled(this,explosionSound.isChecked);Prefs.setExplosionVibrationEnabled(this,explosionVibration.isChecked);Prefs.setMenuOnLeft(this,menuLeft.isChecked);MonitoringScheduler.reconfigure(this);Toast.makeText(this,"Configuración guardada",Toast.LENGTH_SHORT).show();recreate()
        })
        r.addView(Ui.section(this,"Continuidad híbrida gratuita"))
        r.addView(Ui.subtitle(this,"Android programa revisiones locales con un mínimo efectivo de 15 minutos. Para activaciones más frecuentes y continuidad tras reposo/reinicio, configura Cloudflare Workers Free + Firebase Spark siguiendo GUIA_HIBRIDA_GRATUITA.md."))
        r.addView(Ui.text(this,"URL del Worker HTTPS",16f,true));val endpoint=EditText(this).apply{setText(Prefs.remoteEndpoint(this@SettingsActivity));hint="https://tu-worker.workers.dev"};r.addView(endpoint)
        r.addView(Ui.text(this,"Clave de registro",16f,true));val remoteKey=EditText(this).apply{setText(Prefs.remoteRegistrationKey(this@SettingsActivity));hint="La misma APP_REGISTRATION_SECRET del Worker"};r.addView(remoteKey)
        r.addView(Ui.button(this,"GUARDAR CONEXIÓN HÍBRIDA"){
            Prefs.setRemoteEndpoint(this,endpoint.text.toString());Prefs.setRemoteRegistrationKey(this,remoteKey.text.toString());MonitoringScheduler.reconfigure(this);Toast.makeText(this,"Conexión híbrida guardada",Toast.LENGTH_SHORT).show()
        })
        r.addView(Ui.section(this,"Alertas Android"));val a=Ui.card(this);a.addView(Ui.text(this,"Las señales usan un canal Android de alta prioridad. Sonido/vibración dependen también de los ajustes del canal y del modo No molestar del teléfono."));a.addView(Ui.button(this,"PROBAR SONIDO Y VIBRACIÓN"){NotificationHelper.testSignal(this)});a.addView(Ui.button(this,"ABRIR AJUSTES DE NOTIFICACIONES"){try{startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,packageName))}catch(_:Throwable){Toast.makeText(this,"Abre Ajustes > Notificaciones",Toast.LENGTH_SHORT).show()}});r.addView(a)
        r.addView(Ui.section(this,"Fuente y seguridad de datos"));r.addView(Ui.subtitle(this,"Fuente principal: Binance Spot público USDT. Si faltan datos, están viejos o son incoherentes, esa evaluación se suspende y queda anotada en Diagnóstico. El motor no reutiliza silenciosamente datos antiguos para crear una alerta."))
        setContentView(s)
    }
}
