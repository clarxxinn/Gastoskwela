package com.clarxxinn.gastoskwela.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import com.clarxxinn.gastoskwela.utils.MoneyUtils
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.pow

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

    private var barValues: List<Long> = emptyList()
    private var barLabels: List<String> = emptyList()

    private val purple = Color.rgb(101, 71, 237)
    private val dark = Color.rgb(31, 41, 55)
    private val gray = Color.rgb(142, 151, 172)
    private val gridColor = Color.rgb(232, 235, 246)
    private val trackColor = Color.rgb(241, 242, 250)

    private val numberFormatter =
        NumberFormat.getNumberInstance(Locale.US).apply {
            maximumFractionDigits = 0
        }

    fun setDonutData(data: List<Slice>) {
        chartMode = 0

        slices = data.filter {
            it.amount > 0L
        }

        totalCentavos = slices.sumOf {
            it.amount
        }

        invalidate()
    }

    fun setBarData(
        values: List<Long>,
        labels: List<String>
    ) {
        chartMode = 1

        barValues = values.map {
            it.coerceAtLeast(0L)
        }

        barLabels = labels

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (chartMode == 0) {
            drawDonutChart(canvas)
        } else {
            drawBarChart(canvas)
        }
    }

    private fun drawDonutChart(canvas: Canvas) {
        val centerX = width / 2f
        val centerY = height / 2f

        val radius = min(
            width.toFloat(),
            height.toFloat()
        ) * 0.40f

        val strokeWidth = radius * 0.36f

        val bounds = RectF(
            centerX - radius + strokeWidth / 2f,
            centerY - radius + strokeWidth / 2f,
            centerX + radius - strokeWidth / 2f,
            centerY + radius - strokeWidth / 2f
        )

        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth
        paint.strokeCap = Paint.Cap.BUTT
        paint.color = trackColor

        canvas.drawArc(
            bounds,
            0f,
            360f,
            false,
            paint
        )

        if (totalCentavos > 0L) {
            var startAngle = -90f

            slices.forEach { slice ->
                val sweepAngle = (
                        slice.amount.toDouble() /
                                totalCentavos.toDouble() * 360.0
                        ).toFloat()

                paint.color = slice.color

                canvas.drawArc(
                    bounds,
                    startAngle,
                    sweepAngle,
                    false,
                    paint
                )

                startAngle += sweepAngle
            }
        }

        paint.style = Paint.Style.FILL
        paint.color = dark
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(
            "sans-serif",
            Typeface.BOLD
        )
        paint.textSize = sp(16f)

        val amountText = MoneyUtils.format(
            totalCentavos
        )

        val maxTextWidth = radius * 1.65f

        while (
            paint.measureText(amountText) > maxTextWidth &&
            paint.textSize > sp(10f)
        ) {
            paint.textSize -= sp(0.5f)
        }

        canvas.drawText(
            amountText,
            centerX,
            centerY + sp(1f),
            paint
        )

        paint.color = gray
        paint.typeface = Typeface.DEFAULT
        paint.textSize = sp(10f)

        canvas.drawText(
            "Total",
            centerX,
            centerY + sp(17f),
            paint
        )
    }

    private fun drawBarChart(canvas: Canvas) {
        if (barValues.isEmpty()) {
            drawEmptyChart(canvas)
            return
        }

        val chartLeft = dp(48f)
        val chartRight = width.toFloat() - dp(12f)
        val chartTop = dp(30f)
        val chartBottom = height.toFloat() - dp(33f)

        if (
            chartRight <= chartLeft ||
            chartBottom <= chartTop
        ) {
            return
        }

        val chartHeight = chartBottom - chartTop
        val chartWidth = chartRight - chartLeft

        // Amounts are stored in centavos.
        val highestValue = (
                barValues.maxOrNull() ?: 0L
                ).toDouble() / 100.0

        // Example: 445 pesos -> axis maximum 500.
        val axisMaximum = calculateAxisMaximum(
            highestValue
        )

        drawGridLines(
            canvas,
            chartLeft,
            chartRight,
            chartTop,
            chartBottom,
            axisMaximum
        )

        val barCount = barValues.size
        val cellWidth = chartWidth / barCount

        val barWidth = when {
            barCount <= 1 -> min(
                dp(52f),
                cellWidth * 0.25f
            )

            barCount <= 5 -> min(
                dp(38f),
                cellWidth * 0.55f
            )

            barCount <= 7 -> min(
                dp(30f),
                cellWidth * 0.58f
            )

            else -> min(
                dp(18f),
                cellWidth * 0.62f
            )
        }

        barValues.forEachIndexed { index, value ->
            val centerX = chartLeft +
                    cellWidth * (index + 0.5f)

            val valuePesos = value.toDouble() / 100.0

            val fraction = (
                    valuePesos / axisMaximum
                    ).toFloat().coerceIn(0f, 1f)

            val barHeight = chartHeight * fraction

            if (barHeight > 0f) {
                drawBar(
                    canvas,
                    centerX,
                    chartBottom,
                    barWidth,
                    barHeight
                )

                drawValueLabel(
                    canvas,
                    centerX,
                    chartBottom - barHeight,
                    valuePesos,
                    cellWidth
                )
            }

            drawBottomLabel(
                canvas,
                centerX,
                chartBottom,
                index,
                cellWidth
            )
        }
    }

    private fun calculateAxisMaximum(
        highestValue: Double
    ): Double {
        if (highestValue <= 0.0) {
            return 100.0
        }

        // Round up to a readable maximum without
        // adding excessive empty space above the bars.
        val magnitude = 10.0.pow(
            floor(log10(highestValue))
        )

        val normalizedValue = highestValue / magnitude

        val multiplier = when {
            normalizedValue <= 1.0 -> 1.0
            normalizedValue <= 2.0 -> 2.0
            normalizedValue <= 2.5 -> 2.5
            normalizedValue <= 5.0 -> 5.0
            normalizedValue <= 8.0 -> 8.0
            else -> 10.0
        }

        return multiplier * magnitude
    }

    private fun drawGridLines(
        canvas: Canvas,
        left: Float,
        right: Float,
        top: Float,
        bottom: Float,
        axisMaximum: Double
    ) {
        val chartHeight = bottom - top

        for (index in 0..4) {
            val y = bottom -
                    chartHeight * index / 4f

            val tickValue = axisMaximum * index / 4.0

            paint.reset()
            paint.isAntiAlias = true
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = dp(0.8f)
            paint.color = gridColor

            canvas.drawLine(
                left,
                y,
                right,
                y,
                paint
            )

            paint.style = Paint.Style.FILL
            paint.color = gray
            paint.textAlign = Paint.Align.RIGHT
            paint.textSize = sp(9f)
            paint.typeface = Typeface.DEFAULT

            canvas.drawText(
                formatAxisValue(tickValue),
                left - dp(8f),
                y + dp(3f),
                paint
            )
        }
    }

    private fun drawBar(
        canvas: Canvas,
        centerX: Float,
        bottom: Float,
        barWidth: Float,
        barHeight: Float
    ) {
        val left = centerX - barWidth / 2f
        val right = centerX + barWidth / 2f
        val top = bottom - barHeight

        val barRect = RectF(
            left,
            top,
            right,
            bottom
        )

        val radius = min(
            dp(6f),
            barWidth / 3f
        )

        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.color = purple

        // Solid purple, no gradient or overlay.
        canvas.drawRoundRect(
            barRect,
            radius,
            radius,
            paint
        )
    }

    private fun drawValueLabel(
        canvas: Canvas,
        centerX: Float,
        barTop: Float,
        valuePesos: Double,
        cellWidth: Float
    ) {
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.color = dark
        paint.typeface = Typeface.create(
            "sans-serif-medium",
            Typeface.NORMAL
        )
        paint.textAlign = Paint.Align.CENTER

        paint.textSize = when {
            barValues.size >= 12 -> sp(7f)
            barValues.size >= 7 -> sp(8f)
            else -> sp(9f)
        }

        val label = formatBarValue(valuePesos)

        if (
            paint.measureText(label) >
            cellWidth * 0.95f
        ) {
            paint.textSize = sp(7f)
        }

        canvas.drawText(
            label,
            centerX,
            barTop - dp(7f),
            paint
        )
    }

    private fun drawBottomLabel(
        canvas: Canvas,
        centerX: Float,
        chartBottom: Float,
        index: Int,
        cellWidth: Float
    ) {
        val label = barLabels.getOrElse(index) {
            "${index + 1}"
        }

        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.color = gray
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.DEFAULT

        paint.textSize = when {
            barValues.size >= 12 -> sp(8f)
            barValues.size >= 7 -> sp(9f)
            else -> sp(10f)
        }

        while (
            paint.measureText(label) > cellWidth * 0.92f &&
            paint.textSize > sp(7f)
        ) {
            paint.textSize -= sp(0.5f)
        }

        canvas.drawText(
            label,
            centerX,
            chartBottom + dp(19f),
            paint
        )
    }

    private fun drawEmptyChart(canvas: Canvas) {
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.color = gray
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = sp(12f)

        canvas.drawText(
            "No spending data available",
            width / 2f,
            height / 2f,
            paint
        )
    }

    private fun formatAxisValue(
        value: Double
    ): String {
        return when {
            value >= 1000000.0 -> {
                String.format(
                    Locale.US,
                    "%.1fM",
                    value / 1000000.0
                )
            }

            value >= 1000.0 -> {
                String.format(
                    Locale.US,
                    "%.1fk",
                    value / 1000.0
                )
            }

            value >= 1.0 -> {
                if (value % 1.0 == 0.0) {
                    numberFormatter.format(value)
                } else {
                    String.format(
                        Locale.US,
                        "%.2f",
                        value
                    )
                }
            }

            else -> {
                String.format(
                    Locale.US,
                    "%.2f",
                    value
                )
            }
        }
    }

    private fun formatBarValue(
        value: Double
    ): String {
        return when {
            value >= 1000000.0 -> {
                String.format(
                    Locale.US,
                    "₱%.1fM",
                    value / 1000000.0
                )
            }

            value >= 1000.0 -> {
                String.format(
                    Locale.US,
                    "₱%.1fk",
                    value / 1000.0
                )
            }

            value >= 1.0 -> {
                "₱${numberFormatter.format(value)}"
            }

            else -> {
                String.format(
                    Locale.US,
                    "₱%.2f",
                    value
                )
            }
        }
    }

    private fun dp(value: Float): Float {
        return value * resources.displayMetrics.density
    }

    private fun sp(value: Float): Float {
        return value * resources.displayMetrics.scaledDensity
    }
}