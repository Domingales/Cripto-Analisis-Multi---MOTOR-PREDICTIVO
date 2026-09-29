package com.domingales.criptoanalisis.multi.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import com.domingales.criptoanalisis.multi.util.Prefs
import kotlin.math.min

object AppNavigation {
    private val gold = Color.rgb(212, 175, 55)

    private fun dp(activity: Activity, value: Int): Int =
        (value * activity.resources.displayMetrics.density).toInt()

    fun show(activity: Activity, anchor: View) {
        val destinations = listOf(
            "INICIO" to MainActivity::class.java,
            "MIS CRIPTOS Y CONFIGURACIÓN INDIVIDUAL" to WatchlistActivity::class.java,
            "OPORTUNIDADES" to OpportunitiesActivity::class.java,
            "ANÁLISIS POR CRIPTOMONEDA" to AnalysisSelectorActivity::class.java,
            "ALARMAS DE PRECIO" to PriceAlarmsActivity::class.java,
            "POSIBLES EXPLOSIONES — HISTORIAL INDEPENDIENTE" to ExplosionActivity::class.java,
            "HISTORIAL Y SEGUIMIENTO DE SEÑALES" to HistoryActivity::class.java,
            "RENDIMIENTO / ESTADÍSTICAS" to PerformanceActivity::class.java,
            "BACKTEST HISTÓRICO" to BacktestActivity::class.java,
            "EXPORTAR INFORME COMPLETO" to ExportActivity::class.java,
            "DIAGNÓSTICO DETALLADO" to DiagnosticsActivity::class.java,
            "CONFIGURACIÓN GLOBAL" to SettingsActivity::class.java,
            "MAPA DE LOS 59 PUNTOS" to CoverageActivity::class.java,
            "AYUDA / ACERCA DE" to InfoActivity::class.java
        )

        val column = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(activity, 10), dp(activity, 10), dp(activity, 10), dp(activity, 10))
            background = GradientDrawable().apply {
                setColor(Color.rgb(8, 8, 9))
                cornerRadius = dp(activity, 16).toFloat()
                setStroke(dp(activity, 2), gold)
            }
        }

        val popupWidth = min(dp(activity, 360), activity.resources.displayMetrics.widthPixels - dp(activity, 24))
        val popupHeight = min(dp(activity, 660), (activity.resources.displayMetrics.heightPixels * 0.82f).toInt())
        val scroll = ScrollView(activity).apply {
            isFillViewport = true
            addView(column, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        val popup = PopupWindow(scroll, popupWidth, popupHeight, true).apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isOutsideTouchable = true
            isClippingEnabled = true
            elevation = dp(activity, 12).toFloat()
        }

        destinations.forEach { item ->
            val option = Button(activity).apply {
                text = item.first
                isAllCaps = false
                textSize = 13f
                setTextColor(gold)
                gravity = Gravity.CENTER
                minHeight = dp(activity, 52)
                setPadding(dp(activity, 12), dp(activity, 8), dp(activity, 12), dp(activity, 8))
                background = GradientDrawable().apply {
                    setColor(Color.BLACK)
                    cornerRadius = dp(activity, 12).toFloat()
                    setStroke(dp(activity, 2), gold)
                }
                setOnClickListener {
                    popup.dismiss()
                    if (activity.javaClass != item.second) activity.startActivity(Intent(activity, item.second))
                }
            }
            column.addView(option, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, dp(activity, 4), 0, dp(activity, 4)) })
        }

        val horizontalOffset = if (Prefs.menuOnLeft(activity)) 0 else -(popupWidth - anchor.width)
        popup.showAsDropDown(anchor, horizontalOffset, dp(activity, 6))
    }
}
