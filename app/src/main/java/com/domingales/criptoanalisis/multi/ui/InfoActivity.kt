package com.domingales.criptoanalisis.multi.ui

import android.app.Activity
import android.os.Bundle
import android.widget.ScrollView

class InfoActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val s=ScrollView(this);val r=Ui.root(this);s.addView(r)
        r.addView(Ui.title(this,"CriptoAnálisis Multi 3.8.0"))
        r.addView(Ui.subtitle(this,"Vigilancia técnica multcripto con trazabilidad completa."))
        fun block(t:String,d:String){r.addView(Ui.section(this,t));val c=Ui.card(this);c.addView(Ui.text(this,d));r.addView(c)}
        block("Qué hace","Vigila varias criptomonedas, calcula indicadores, estructura, régimen, contexto multitemporal y BTC; separa score, confianza y probabilidad; filtra, registra y alerta sólo cuando una señal válida supera todos los controles.")
        block("Qué no hace","No garantiza movimientos futuros ni convierte un porcentaje en certeza. Una evaluación alta puede terminar en NO OPERAR si faltan volumen, ADX, alineación MTF u otras condiciones.")
        block("Aprendizaje","Cada señal guarda dos medidas: resultado operativo por primer toque y persistencia mediante cierres adaptados al intervalo. Sólo el resultado operativo compatible con el backtest calibra probabilidades; también se conservan MFE y MAE.")
        block("Auditoría","Diagnóstico registra ciclos, errores, rechazos, preseñales, duplicados y alertas. La ficha de cada cripto muestra últimas evaluaciones y seguimientos.")
        block("Posibles explosiones","Módulo independiente que exige cinco condiciones extremas simultáneas y usa historial, seguimiento, estadísticas y notificaciones propios. No cambia las señales ni sus porcentajes y permanece sin calibrar hasta reunir resultados suficientes.")
        block("59 puntos","El menú MAPA DE LOS 59 PUNTOS permite comprobar dónde aparece cada función definida para esta nueva aplicación.")
        block("Aviso","Herramienta informativa de análisis de mercado. No constituye asesoramiento financiero. Las criptomonedas pueden sufrir pérdidas rápidas y significativas.")
        setContentView(s)
    }
}
