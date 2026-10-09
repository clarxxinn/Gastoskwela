package com.clarxxinn.gastoskwela.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.clarxxinn.gastoskwela.utils.MoneyUtils
import kotlin.math.ceil
import kotlin.math.max

class ReportsChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    data class Slice(
        val amount: Long,
        val color: Int
    )

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var chartMode = 0

    private var slices: List<Slice> = emptyList()
    private var totalCentavos = 0L
    private var barValues: List<Long> = List(5) { 0L }
    private var barLabels: List<String> =
        listOf("W1", "W2", "W3", "W4", "W5")

    private val purple = Color.rgb(101, 71, 237)
    private val gray = Color.rgb(142, 151, 172)
    private val dark = Color.rgb(31, 41, 55)

    fun setDonutData(data: List<Slice>) {
        chartMode = 0
        slices = data.filter { it.amount > 0L }
        totalCentavos = slices.sumOf { it.amount }
        invalidate()
    }

    fun setBarData(
        values: List<Long>,
        labels: List<String>
    ) {
        chartMode = 1
        barValues = values
        barLabels = labels
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (chartMode == 0) {
            drawDonut(canvas)
        } else {
            drawBars(canvas)
        }
    }

    private fun drawDonut(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(width, height) * 0.41f
        val stroke = radius * 0.39f

        val bounds = RectF(
            cx - radius + stroke / 2,
            cy - radius + stroke / 2,
            cx + radius - stroke / 2,
            cy + radius - stroke / 2
        )

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = stroke
        paint.strokeCap = Paint.Cap.BUTT
        paint.color = Color.rgb(232, 234, 247)

        canvas.drawArc(
            bounds, 0f, 360f, false, paint
        )

        if (totalCentavos > 0L) {
            var angle = -90f

            slices.forEach { slice ->
                val sweep = (
                        slice.amount.toDouble() /
                                totalCentavos.toDouble() * 360.0
                        ).toFloat()

                paint.color = slice.color

                canvas.drawArc(
                    bounds,
                    angle,
                    sweep,
                    false,
                    paint
                )

                angle += sweep
            }
        }

        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.CENTER
        paint.color = dark
        paint.typeface = android.graphics.Typeface.create(
            "sans-serif",
            android.graphics.Typeface.BOLD
        )
        paint.textSize = sp(16f)

        val amount = MoneyUtils.format(totalCentavos)

        canvas.drawText(
            amount,
            cx,
            cy + sp(2f),
            paint
        )

        paint.color = gray
        paint.textSize = sp(11f)
        paint.typeface = android.graphics.Typeface.DEFAULT

        canvas.drawText(
            "Total",
            cx,
            cy + sp(18f),
            paint
        )
    }

    private fun drawBars(canvas: Canvas) {
        val left = dp(35f)
        val right = width - dp(6f)
        val top = dp(10f)
        val bottom = height - dp(27f)

        if (right <= left || bottom <= top) return

        val values = barValues
        if (values.isEmpty()) return

        val maxValue = max(1L, values.maxOrNull() ?: 0L)

        val maxPesos = maxValue / 100.0
        val rawStep = maxPesos / 4.0
        val magnitude = when {
            rawStep <= 1 -> 1.0
            rawStep <= 10 -> 10.0
            rawStep <= 100 -> 100.0
            rawStep <= 1000 -> 1000.0
            else -> 10000.0
        }

        val niceStep = max(
            magnitude,
            ceil(rawStep / magnitude) * magnitude
        )

        val axisMax = niceStep * 4.0
        val chartHeight = bottom - top

        paint.style = Paint.Style.FILL
        paint.typeface = android.graphics.Typeface.DEFAULT
        paint.textSize = sp(10f)
        paint.textAlign = Paint.Align.RIGHT

        for (i in 0..4) {
            val y = bottom - chartHeight * i / 4f
            val tick = niceStep * i

            paint.color = gray
            canvas.drawText(
                tick.toLong().toString(),
                left - dp(7f),
                y + dp(3f),
                paint
            )

            paint.color = Color.rgb(232, 235, 246)
            paint.strokeWidth = dp(1f)

            canvas.drawLine(
                left, y, right, y, paint
            )
        }

        val count = values.size
        val cellWidth = (right - left) / count
        val barWidth = minOf(dp(25f), cellWidth * 0.55f)

        values.forEachIndexed { index, value ->
            val x = left + cellWidth * (index + 0.5f)

            val fraction = (
                    value / 100.0 / axisMax
                    ).toFloat().coerceIn(0f, 1f)

            val barHeight = chartHeight * fraction

            if (barHeight > 0f) {
                paint.color = purple

                canvas.drawRoundRect(
                    RectF(
                        x - barWidth / 2f,
                        bottom - barHeight,
                        x + barWidth / 2f,
                        bottom
                    ),
                    dp(4f),
                    dp(4f),
                    paint
                )
            }

            paint.color = gray
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = sp(10f)

            canvas.drawText(
                barLabels.getOrElse(index) {
                    "W${index + 1}"
                },
                x,
                height - dp(8f),
                paint
            )
        }
    }

    private fun dp(value: Float): Float {
        return value * resources.displayMetrics.density
    }

    private fun sp(value: Float): Float {
        return value * resources.displayMetrics.scaledDensity
    }
}