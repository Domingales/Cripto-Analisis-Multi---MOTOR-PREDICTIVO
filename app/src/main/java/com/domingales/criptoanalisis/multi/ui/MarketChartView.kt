package com.domingales.criptoanalisis.multi.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import com.domingales.criptoanalisis.multi.domain.Candle
import kotlin.math.max

class MarketChartView(context: Context) : View(context) {
    private var candles: List<Candle> = emptyList()
    private var support: Double? = null
    private var resistance: Double? = null
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(212,175,55); strokeWidth = 4f; style = Paint.Style.STROKE }
    private val supportPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(70,205,130); strokeWidth = 2f }
    private val resistancePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(245,95,85); strokeWidth = 2f }
    private val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(58,58,62); strokeWidth = 1f }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; textSize = 28f }

    init { minimumHeight = (250 * resources.displayMetrics.density).toInt(); setPadding(12, 18, 12, 18) }

    fun setData(values: List<Candle>, supportValue: Double?, resistanceValue: Double?) {
        candles = values.takeLast(100); support = supportValue; resistance = resistanceValue; invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(18,19,22))
        if (candles.size < 2) { canvas.drawText("Gráfico pendiente de datos", 24f, height / 2f, label); return }
        val values = candles.map { it.close } + listOfNotNull(support, resistance)
        val low = values.minOrNull() ?: return
        val high = values.maxOrNull() ?: return
        val range = max(high - low, max(high, 1.0) * 0.001)
        val left = paddingLeft.toFloat(); val right = width - paddingRight.toFloat()
        val top = paddingTop.toFloat(); val bottom = height - paddingBottom.toFloat()
        repeat(4) { index -> val y = top + (bottom - top) * index / 3f; canvas.drawLine(left, y, right, y, grid) }
        fun y(value: Double) = bottom - ((value - low) / range * (bottom - top)).toFloat()
        support?.let { canvas.drawLine(left, y(it), right, y(it), supportPaint); canvas.drawText("S ${price(it)}", left + 8, y(it) - 6, label) }
        resistance?.let { canvas.drawLine(left, y(it), right, y(it), resistancePaint); canvas.drawText("R ${price(it)}", left + 8, y(it) - 6, label) }
        val path = Path()
        candles.forEachIndexed { index, candle ->
            val x = left + (right - left) * index / (candles.size - 1).toFloat()
            if (index == 0) path.moveTo(x, y(candle.close)) else path.lineTo(x, y(candle.close))
        }
        canvas.drawPath(path, line)
        canvas.drawText("${candles.size} cierres • ${price(candles.last().close)}", left + 8, top + 28, label)
    }

    private fun price(value: Double) = if (value >= 1000) "%.2f".format(value) else "%.6f".format(value)
}
