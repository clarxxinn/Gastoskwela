package com.clarxxinn.gastoskwela.fragments

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.clarxxinn.gastoskwela.R
import com.clarxxinn.gastoskwela.data.Expense
import com.clarxxinn.gastoskwela.data.GastoskwelaDatabase
import com.clarxxinn.gastoskwela.utils.MoneyUtils
import com.clarxxinn.gastoskwela.views.ReportsChartView
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

class ReportsFragment : Fragment(R.layout.fragment_reports) {

    private val expenseDao by lazy {
        GastoskwelaDatabase.getDatabase(requireContext())
            .expenseDao()
    }

    private var selectedMonth = YearMonth.now()
    private var selectedPeriod = "Monthly"
    private var allExpenses: List<Expense> = emptyList()

    private val monthFormatter = DateTimeFormatter.ofPattern(
        "MMMM yyyy",
        Locale.ENGLISH
    )

    private val colors = listOf(
        Color.rgb(239, 68, 68),
        Color.rgb(59, 130, 246),
        Color.rgb(34, 197, 94),
        Color.rgb(20, 184, 166),
        Color.rgb(139, 92, 246),
        Color.rgb(245, 158, 11),
        Color.rgb(99, 102, 241),
        Color.rgb(236, 72, 153)
    )

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        savedInstanceState?.let { state ->
            selectedPeriod = state.getString(
                "report_period",
                "Monthly"
            )

            selectedMonth = runCatching {
                YearMonth.parse(
                    state.getString("report_month")
                )
            }.getOrDefault(YearMonth.now())
        }

        val periodButton = view.findViewById<View>(
            R.id.btnReportPeriod
        )

        val monthButton = view.findViewById<View>(
            R.id.btnReportMonth
        )

        periodButton.setOnClickListener {
            val popup = PopupMenu(requireContext(), periodButton)

            popup.menu.add("Monthly")
            popup.menu.add("Weekly")

            popup.setOnMenuItemClickListener { item ->
                selectedPeriod = item.title.toString()
                render(view)
                true
            }

            popup.show()
        }

        monthButton.setOnClickListener {
            val initialDate = selectedMonth.atDay(1)

            DatePickerDialog(
                requireContext(),
                { _, year, month, _ ->
                    selectedMonth = YearMonth.of(
                        year,
                        month + 1
                    )

                    render(view)
                },
                initialDate.year,
                initialDate.monthValue - 1,
                1
            ).show()
        }

        render(view)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {
                expenseDao.getAllExpenses().collect { expenses ->
                    allExpenses = expenses
                    render(view)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(
            "report_period",
            selectedPeriod
        )

        outState.putString(
            "report_month",
            selectedMonth.toString()
        )

        super.onSaveInstanceState(outState)
    }

    private fun render(view: View) {
        view.findViewById<TextView>(
            R.id.btnReportPeriod
        ).text = "$selectedPeriod  ⌄"

        view.findViewById<TextView>(
            R.id.btnReportMonth
        ).text = "${selectedMonth.format(monthFormatter)}  ⌄"

        val monthStart = selectedMonth.atDay(1)
        val monthEnd = selectedMonth.atEndOfMonth()

        // Weekly shows the last seven days of the selected month.
        // For the current month, it ends on today's date.
        val rangeEnd = if (
            selectedPeriod == "Weekly" &&
            selectedMonth == YearMonth.now()
        ) {
            LocalDate.now()
        } else {
            monthEnd
        }

        val rangeStart = if (selectedPeriod == "Weekly") {
            rangeEnd.minusDays(6)
        } else {
            monthStart
        }

        val previousStart: LocalDate
        val previousEnd: LocalDate

        if (selectedPeriod == "Weekly") {
            previousEnd = rangeStart.minusDays(1)
            previousStart = previousEnd.minusDays(6)
        } else {
            val previousMonth = selectedMonth.minusMonths(1)
            previousStart = previousMonth.atDay(1)
            previousEnd = previousMonth.atEndOfMonth()
        }

        val currentExpenses = expensesBetween(
            rangeStart,
            rangeEnd
        )

        val previousExpenses = expensesBetween(
            previousStart,
            previousEnd
        )

        val total = currentExpenses.sumOf {
            it.amountCentavos
        }

        val previousTotal = previousExpenses.sumOf {
            it.amountCentavos
        }

        view.findViewById<TextView>(
            R.id.tvReportTotalExpenses
        ).text = MoneyUtils.format(total)

        updateComparison(
            view,
            total,
            previousTotal
        )

        updateCategoryChart(
            view,
            currentExpenses
        )

        updateWeeklyChart(
            view,
            currentExpenses,
            rangeStart
        )
    }

    private fun expensesBetween(
        start: LocalDate,
        end: LocalDate
    ): List<Expense> {
        return allExpenses.filter { expense ->
            val date = runCatching {
                LocalDate.parse(expense.date.take(10))
            }.getOrNull()

            date != null &&
                    !date.isBefore(start) &&
                    !date.isAfter(end)
        }
    }

    private fun updateComparison(
        view: View,
        current: Long,
        previous: Long
    ) {
        val comparison = view.findViewById<TextView>(
            R.id.tvReportComparison
        )

        val periodName = if (selectedPeriod == "Weekly") {
            "last week"
        } else {
            "last month"
        }

        if (previous == 0L) {
            comparison.text = if (current == 0L) {
                "No expenses for this period"
            } else {
                "No previous period data"
            }

            comparison.setTextColor(
                Color.rgb(107, 114, 128)
            )
            return
        }

        val difference = (
                (current.toDouble() - previous.toDouble()) /
                        previous.toDouble() * 100.0
                ).roundToInt()

        comparison.text = when {
            difference > 0 ->
                "↑ ${abs(difference)}% from $periodName"
            difference < 0 ->
                "↓ ${abs(difference)}% from $periodName"
            else ->
                "0% change from $periodName"
        }

        comparison.setTextColor(
            when {
                difference > 0 ->
                    Color.rgb(239, 68, 68)
                difference < 0 ->
                    Color.rgb(34, 197, 94)
                else ->
                    Color.rgb(107, 114, 128)
            }
        )
    }

    private fun updateCategoryChart(
        view: View,
        expenses: List<Expense>
    ) {
        val donut = view.findViewById<ReportsChartView>(
            R.id.donutChart
        )

        val legend = view.findViewById<LinearLayout>(
            R.id.categoryLegendContainer
        )

        val empty = view.findViewById<TextView>(
            R.id.tvReportNoCategories
        )

        legend.removeAllViews()

        val categoryTotals = expenses
            .groupBy { it.category.trim() }
            .mapValues { (_, records) ->
                records.sumOf { it.amountCentavos }
            }
            .filterValues { it > 0L }
            .toList()
            .sortedByDescending { it.second }

        val total = categoryTotals.sumOf { it.second }

        empty.visibility = if (total == 0L) {
            View.VISIBLE
        } else {
            View.GONE
        }

        val slices = categoryTotals.mapIndexed { index, entry ->
            ReportsChartView.Slice(
                amount = entry.second,
                color = colors[index % colors.size]
            )
        }

        donut.setDonutData(slices)

        categoryTotals.forEachIndexed { index, entry ->
            val category = entry.first
            val amount = entry.second
            val color = colors[index % colors.size]

            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(12)
                }
            }

            val dot = View(requireContext()).apply {
                background = android.graphics.drawable
                    .GradientDrawable().apply {
                        shape = android.graphics.drawable
                            .GradientDrawable.OVAL
                        setColor(color)
                    }

                layoutParams = LinearLayout.LayoutParams(
                    dp(8),
                    dp(8)
                ).apply {
                    marginEnd = dp(7)
                }
            }

            val label = TextView(requireContext()).apply {
                text = category
                textSize = 10f
                setTextColor(Color.rgb(107, 114, 128))
                typeface = resources.getFont(
                    R.font.poppins_medium
                )

                maxLines = 1
                ellipsize = android.text.TextUtils
                    .TruncateAt.END

                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            val percent = if (total > 0L) {
                (
                        amount.toDouble() /
                                total.toDouble() * 100.0
                        ).roundToInt()
            } else {
                0
            }

            val percentText = TextView(requireContext()).apply {
                text = "$percent%"
                textSize = 10f
                setTextColor(Color.rgb(55, 65, 81))
                typeface = resources.getFont(
                    R.font.poppins_medium
                )
            }

            row.addView(dot)
            row.addView(label)
            row.addView(percentText)

            legend.addView(row)
        }
    }

    private fun updateWeeklyChart(
        view: View,
        expenses: List<Expense>,
        rangeStart: LocalDate
    ) {
        val chart = view.findViewById<ReportsChartView>(
            R.id.weeklyChart
        )

        val title = view.findViewById<TextView>(
            R.id.tvTrendTitle
        )

        if (selectedPeriod == "Weekly") {
            title.text = "Daily Spending Trend"

            val totals = LongArray(7)

            expenses.forEach { expense ->
                val date = runCatching {
                    LocalDate.parse(expense.date.take(10))
                }.getOrNull() ?: return@forEach

                val dayIndex = java.time.temporal.ChronoUnit
                    .DAYS.between(rangeStart, date)
                    .toInt()

                if (dayIndex in 0..6) {
                    totals[dayIndex] += expense.amountCentavos
                }
            }

            val labels = (0..6).map { index ->
                rangeStart.plusDays(index.toLong())
                    .dayOfWeek.name.take(3)
                    .lowercase()
                    .replaceFirstChar { it.uppercase() }
            }

            chart.setBarData(
                totals.toList(),
                labels
            )
        } else {
            title.text = "Weekly Spending Trend"

            val totals = LongArray(5)

            expenses.forEach { expense ->
                val date = runCatching {
                    LocalDate.parse(expense.date.take(10))
                }.getOrNull() ?: return@forEach

                val weekIndex = (
                        (date.dayOfMonth - 1) / 7
                        ).coerceIn(0, 4)

                totals[weekIndex] += expense.amountCentavos
            }

            chart.setBarData(
                totals.toList(),
                listOf("W1", "W2", "W3", "W4", "W5")
            )
        }
    }

    private fun dp(value: Int): Int {
        return (
                value * resources.displayMetrics.density
                ).toInt()
    }
}