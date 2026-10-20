package com.domingales.criptoanalisis.multi.ui

import android.app.Activity
import android.os.Bundle
import android.widget.ScrollView
import com.domingales.criptoanalisis.multi.data.AppDatabase
import com.domingales.criptoanalisis.multi.data.ForecastCriterionStore
import com.domingales.criptoanalisis.multi.domain.PerformanceSummary
import com.domingales.criptoanalisis.multi.util.Prefs

class PerformanceActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this)
        val root = Ui.root(this)
        scroll.addView(root)
        root.addView(Ui.title(this, "Rendimiento y aprendizaje"))
        root.addView(Ui.subtitle(this, "Mide el resultado operativo por primer toque. La persistencia por cierres se conserva como una medida distinta y no entrena la probabilidad."))
        val db = AppDatabase.get(this)

        fun card(name: String, summary: PerformanceSummary) {
            val view = Ui.card(this)
            view.addView(Ui.text(this, name, 17f, true))
            view.addView(Ui.text(this, "Señales ${summary.totalSignals} • Pendientes ${summary.pending}"))
            view.addView(Ui.text(this, "Aciertos ${summary.hits} • Fallos ${summary.fails} • Neutras ${summary.neutral}"))
            view.addView(Ui.text(this, "Precisión resueltas: ${"%.1f".format(summary.resolvedAccuracy)}%", 15f, true))
            root.addView(view)
        }

        fun section(title: String, rows: Map<String, PerformanceSummary>) {
            root.addView(Ui.section(this, title))
            if (rows.isEmpty()) root.addView(Ui.subtitle(this, "Aún no hay resultados exactos suficientes para este desglose."))
            rows.forEach { (name, summary) -> card(name, summary) }
        }

        root.addView(Ui.section(this, "Resumen global"))
        card("TOTAL", db.performance())
        root.addView(Ui.section(this, "Comparación: magnitud y plazo V1"))
        root.addView(Ui.subtitle(this, "Mismo predictor, otro criterio de evaluación experimental. Objetivo: centro del rango original, fijado al registrar cada señal nueva. Acierto: 100% del objetivo en 24h; aproximado: al menos 75% en 48h; fuera de plazo hasta 120h; si no, fallo. Los datos ausentes mantienen el caso pendiente."))
        root.addView(Ui.subtitle(this, "No demuestra una mejora del predictor ni sustituye su probabilidad V2. MFE/MAE muestran el riesgo observado; una caída previa no invalida este criterio de recorrido. Velas completas de 5m posteriores al registro; pueden omitir hasta 5m iniciales. Las señales solapadas no son pruebas independientes."))
        val criterionRows = ForecastCriterionStore(db).report()
        if (criterionRows.isEmpty()) root.addView(Ui.subtitle(this, "Todavía no hay contratos nuevos. No se reclasifican señales anteriores a esta actualización."))
        criterionRows.forEach { line ->
            val comparison = Ui.card(this)
            comparison.addView(Ui.text(this, line, 13f))
            root.addView(comparison)
        }
        val legacy = db.legacyOutcomeCount()
        if (legacy > 0) root.addView(Ui.subtitle(this, "$legacy señales heredadas se conservan en Historial, pero se excluyen de la calibración V2."))

        section("Por criptomoneda", Prefs.watchlist(this).sorted().associateWith { db.performance(it) }.filterValues { it.totalSignals > 0 })
        section("Por intervalo", db.performanceByTimeframe())
        section("Por dirección", db.performanceByDirection())
        section("Por régimen", db.performanceByRegime())
        section("Por riesgo", db.performanceByRisk())

        root.addView(Ui.subtitle(this, "La calibración sólo usa resultados V2 por primer toque, reconstruidos desde el cierre confirmado con velas posteriores de 5 minutos y muestras BUY/SELL separadas."))
        setContentView(scroll)
    }
}

