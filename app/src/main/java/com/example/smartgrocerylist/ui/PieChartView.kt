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

    private val palette = intArrayOf(
        0xFF4CAF50.toInt(), // green
        0xFF2196F3.toInt(), // blue
        0xFFFFC107.toInt(), // amber
        0xFFFF5722.toInt(), // deep orange
        0xFF9C27B0.toInt(), // purple
        0xFF00BCD4.toInt(), // cyan
        0xFF795548.toInt()  // brown
    )

    fun setData(map: Map<String, Double>) {
        data = map.entries
            .sortedByDescending { it.value }
            .map { it.key to it.value }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) return

        val total = data.sumOf { it.second }
        if (total <= 0.0) return

        val size = min(width, height).toFloat()
        val padding = size * 0.08f
        val rect = RectF(padding, padding, size - padding, size - padding)

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
