package com.domingales.criptoanalisis.multi.ui

import android.content.Context
import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*

object Ui {
    private val bg = Color.rgb(12, 13, 15)
    private val ink = Color.rgb(242, 239, 225)
    private val muted = Color.rgb(180, 177, 163)
    private val accent = Color.rgb(212, 175, 55)
    private val panel = Color.rgb(25, 26, 29)

    private fun dp(c: Context, value: Int): Int =
        (value * c.resources.displayMetrics.density).toInt()

    fun root(c: Context): LinearLayout = LinearLayout(c).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(22, 22, 22, 30)
        setBackgroundColor(bg)
    }

    fun title(c: Context, s: String): LinearLayout = LinearLayout(c).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 5, 0, 10)
        val label = TextView(c).apply { text = s; textSize = 25f; setTypeface(typeface, Typeface.BOLD); setTextColor(ink); gravity = Gravity.CENTER_VERTICAL }
        val menu = ImageButton(c).apply {
            setImageResource(com.domingales.criptoanalisis.multi.R.drawable.ic_menu_gold)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            contentDescription = "Abrir menú principal"
            setPadding(dp(c, 10), dp(c, 10), dp(c, 10), dp(c, 10))
            background = GradientDrawable().apply {
                setColor(Color.BLACK)
                cornerRadius = dp(c, 14).toFloat()
                setStroke(dp(c, 2), accent)
            }
            setOnClickListener { if (c is Activity) AppNavigation.show(c, this) }
        }
        val menuParams = LinearLayout.LayoutParams(dp(c, 64), dp(c, 58))
        val labelParams = LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(12, 0, 12, 0) }
        if (com.domingales.criptoanalisis.multi.util.Prefs.menuOnLeft(c)) { addView(menu, menuParams); addView(label, labelParams) }
        else { addView(label, labelParams); addView(menu, menuParams) }
    }

    fun subtitle(c: Context, s: String) = TextView(c).apply {
        text = s; textSize = 14f; setTextColor(muted); setPadding(0, 3, 0, 10)
    }

    fun section(c: Context, s: String) = TextView(c).apply {
        text = s.uppercase(); textSize = 13f; setTypeface(typeface, Typeface.BOLD); setTextColor(accent); setPadding(2, 18, 0, 7)
    }

    fun button(c: Context, s: String, on: () -> Unit) = Button(c).apply {
        text = s; isAllCaps = false; setTypeface(typeface, Typeface.BOLD); setTextColor(accent)
        background = GradientDrawable().apply { setColor(Color.rgb(31, 32, 36)); cornerRadius = 14f; setStroke(1, accent) }
        setPadding(12, 8, 12, 8); setOnClickListener { on() }
    }

    fun card(c: Context): LinearLayout = LinearLayout(c).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(20, 17, 20, 17)
        background = GradientDrawable().apply { setColor(panel); cornerRadius = 18f; setStroke(2, accent) }
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 5, 0, 8) }
        elevation = 2f
    }

    fun metricCard(c: Context, big: String, label: String): LinearLayout = card(c).apply {
        gravity = Gravity.CENTER_HORIZONTAL
        addView(text(c, big, 23f, true).apply { gravity = Gravity.CENTER })
        addView(text(c, label, 12f, false).apply { setTextColor(muted); gravity = Gravity.CENTER })
    }

    fun text(c: Context, s: String, size: Float = 15f, bold: Boolean = false) = TextView(c).apply {
        text = s; textSize = size; setTextColor(ink); if (bold) setTypeface(typeface, Typeface.BOLD); setPadding(0, 3, 0, 3)
    }

    fun badge(c: Context, s: String, color: Int): TextView = text(c, s, 12f, true).apply {
        setTextColor(color); setPadding(16, 8, 16, 8)
        background = GradientDrawable().apply { setColor(Color.rgb(18, 19, 22)); cornerRadius = 30f; setStroke(2, color) }
    }

    fun divider(c: Context) = View(c).apply {
        setBackgroundColor(Color.rgb(90, 77, 35)); layoutParams = LinearLayout.LayoutParams(-1, 1).apply { setMargins(0, 9, 0, 9) }
    }

    fun twoColumn(c: Context, left: View, right: View): LinearLayout = LinearLayout(c).apply {
        orientation = LinearLayout.HORIZONTAL
        addView(left, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(0, 0, 5, 0) })
        addView(right, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(5, 0, 0, 0) })
    }

    fun statusColor(status: String): Int = when (status) {
        "ACTIVE", "SIGNAL_BUY" -> Color.rgb(70, 205, 130)
        "TRIGGERED", "SIGNAL_SELL" -> Color.rgb(245, 95, 85)
        "FOLLOWING" -> Color.rgb(175, 120, 235)
        "PRESIGNAL" -> Color.rgb(235, 185, 55)
        else -> muted
    }
}
