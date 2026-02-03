package com.example.smartgrocerylist.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class PieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var data: List<Pair<String, Double>> = emptyList()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val rect = RectF()
    private var padding = 0f
    private var size = 0f

    private val palette = intArrayOf(
        0xFF4CAF50.toInt(),
        0xFF2196F3.toInt(),
        0xFFFFC107.toInt(),
        0xFFFF5722.toInt(),
        0xFF9C27B0.toInt(),
        0xFF00BCD4.toInt(),
        0xFF795548.toInt()
    )

    fun setData(map: Map<String, Double>) {
        data = map.entries
            .sortedByDescending { it.value }
            .map { it.key to it.value }
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        size = min(w, h).toFloat()
        padding = size * 0.08f
        rect.set(padding, padding, size - padding, size - padding)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) return

        val total = data.sumOf { it.second }
        if (total <= 0.0) return

        var startAngle = -90f
        var i = 0

        for ((_, value) in data) {
            val sweep = ((value / total) * 360f).toFloat()
            paint.color = palette[i % palette.size]
            canvas.drawArc(rect, startAngle, sweep, true, paint)
            startAngle += sweep
            i++
        }
    }
}
