package com.clarxxinn.gastoskwela.fragments

import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
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
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

class ReportsFragment : Fragment(R.layout.fragment_reports) {

    private val expenseDao by lazy {
        GastoskwelaDatabase.getDatabase(requireContext())
            .expenseDao()
    }

    private var selectedPeriod = "Monthly"
    private var selectedDate = LocalDate.now()
    private var allExpenses: List<Expense> = emptyList()

    private val grayColor = Color.rgb(107, 114, 128)

    private val categoryColors = listOf(
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

        if (savedInstanceState != null) {
            selectedPeriod = savedInstanceState.getString(
                "selectedPeriod",
                "Monthly"
            )

            selectedDate = runCatching {
                LocalDate.parse(
                    savedInstanceState.getString(
                        "selectedDate",
                        LocalDate.now().toString()
                    )
                )
            }.getOrDefault(LocalDate.now())
        }

        setupPeriodFilter(view)
        setupDateFilter(view)
        renderReports(view)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {
                expenseDao.getAllExpenses().collect { expenses ->
                    allExpenses = expenses
                    renderReports(view)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(
            "selectedPeriod",
            selectedPeriod
        )

        outState.putString(
            "selectedDate",
            selectedDate.toString()
        )

        super.onSaveInstanceState(outState)
    }

    private fun setupPeriodFilter(view: View) {
        val periodButton = view.findViewById<MaterialButton>(
            R.id.btnReportPeriod
        )

        periodButton.setOnClickListener {
            val popup = PopupMenu(
                requireContext(),
                periodButton
            )

            val periods = listOf(
                "Daily",
                "Weekly",
                "Monthly",
                "Yearly"
            )

            periods.forEach { period ->
                popup.menu.add(period)
            }

            popup.setOnMenuItemClickListener { item ->
                selectedPeriod = item.title.toString()
                renderReports(view)
                true
            }

            popup.show()
        }
    }

    private fun setupDateFilter(view: View) {
        val dateButton = view.findViewById<MaterialButton>(
            R.id.btnReportMonth
        )

        dateButton.setOnClickListener {
            val datePicker = DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    selectedDate = LocalDate.of(
                        year,
                        month + 1,
                        day
                    )

                    renderReports(view)
                },
                selectedDate.year,
                selectedDate.monthValue - 1,
                selectedDate.dayOfMonth
            )

            datePicker.show()
        }
    }

    private fun getDateRange(): Pair<LocalDate, LocalDate> {
        return when (selectedPeriod) {
            "Daily" -> {
                Pair(selectedDate, selectedDate)
            }

            "Weekly" -> {
                val start = selectedDate.with(
                    TemporalAdjusters.previousOrSame(
                        DayOfWeek.MONDAY
                    )
                )

                Pair(
                    start,
                    start.plusDays(6)
                )
            }

            "Yearly" -> {
                val start = selectedDate.withDayOfYear(1)

                Pair(
                    start,
                    start.plusYears(1).minusDays(1)
                )
            }

            else -> {
                val month = YearMonth.from(selectedDate)

                Pair(
                    month.atDay(1),
                    month.atEndOfMonth()
                )
            }
        }
    }

    private fun getPreviousDateRange(
        currentStart: LocalDate
    ): Pair<LocalDate, LocalDate> {
        return when (selectedPeriod) {
            "Daily" -> {
                val previous = currentStart.minusDays(1)
                Pair(previous, previous)
            }

            "Weekly" -> {
                val start = currentStart.minusWeeks(1)
                Pair(start, start.plusDays(6))
            }

            "Yearly" -> {
                val start = currentStart.minusYears(1)
                Pair(
                    start,
                    start.plusYears(1).minusDays(1)
                )
            }

            else -> {
                val month = YearMonth.from(
                    currentStart.minusMonths(1)
                )

                Pair(
                    month.atDay(1),
                    month.atEndOfMonth()
                )
            }
        }
    }

    private fun renderReports(view: View) {
        val periodButton = view.findViewById<MaterialButton>(
            R.id.btnReportPeriod
        )

        val dateButton = view.findViewById<MaterialButton>(
            R.id.btnReportMonth
        )

        periodButton.text = "$selectedPeriod  ⌄"

        val currentRange = getDateRange()
        val start = currentRange.first
        val end = currentRange.second

        dateButton.text = "${getDateLabel(start, end)}  ⌄"

        val previousRange = getPreviousDateRange(start)

        val currentExpenses = getExpensesBetween(
            start,
            end
        )

        val previousExpenses = getExpensesBetween(
            previousRange.first,
            previousRange.second
        )

        val currentTotal = currentExpenses.sumOf { expense ->
            expense.amountCentavos
        }

        val previousTotal = previousExpenses.sumOf { expense ->
            expense.amountCentavos
        }

        view.findViewById<TextView>(
            R.id.tvReportTotalExpenses
        ).text = MoneyUtils.format(currentTotal)

        updateComparison(
            view,
            currentTotal,
            previousTotal
        )

        updateCategoryChart(
            view,
            currentExpenses
        )

        updateSpendingTrend(
            view,
            currentExpenses,
            start
        )
    }

    private fun getDateLabel(
        start: LocalDate,
        end: LocalDate
    ): String {
        return when (selectedPeriod) {
            "Daily" -> {
                selectedDate.format(
                    DateTimeFormatter.ofPattern(
                        "MMM d, yyyy",
                        Locale.ENGLISH
                    )
                )
            }

            "Weekly" -> {
                val firstFormat = DateTimeFormatter.ofPattern(
                    "MMM d",
                    Locale.ENGLISH
                )

                val lastFormat = DateTimeFormatter.ofPattern(
                    "MMM d, yyyy",
                    Locale.ENGLISH
                )

                "${start.format(firstFormat)} - ${end.format(lastFormat)}"
            }

            "Yearly" -> {
                selectedDate.year.toString()
            }

            else -> {
                selectedDate.format(
                    DateTimeFormatter.ofPattern(
                        "MMMM yyyy",
                        Locale.ENGLISH
                    )
                )
            }
        }
    }

    private fun parseExpenseDate(
        value: String
    ): LocalDate? {
        return runCatching {
            LocalDate.parse(value.take(10))
        }.getOrNull()
    }

    private fun getExpensesBetween(
        start: LocalDate,
        end: LocalDate
    ): List<Expense> {
        return allExpenses.filter { expense ->
            val date = parseExpenseDate(expense.date)

            date != null &&
                    !date.isBefore(start) &&
                    !date.isAfter(end)
        }
    }

    private fun updateComparison(
        view: View,
        currentTotal: Long,
        previousTotal: Long
    ) {
        val comparisonText = view.findViewById<TextView>(
            R.id.tvReportComparison
        )

        val previousPeriodName = when (selectedPeriod) {
            "Daily" -> "previous day"
            "Weekly" -> "previous week"
            "Yearly" -> "previous year"
            else -> "previous month"
        }

        if (previousTotal == 0L) {
            comparisonText.text = if (currentTotal == 0L) {
                "No expenses for this period"
            } else {
                "No previous period data"
            }

            comparisonText.setTextColor(grayColor)
            return
        }

        val percentage = (
                (currentTotal - previousTotal).toDouble() /
                        previousTotal.toDouble() * 100.0
                ).roundToInt()

        comparisonText.text = when {
            percentage > 0 -> {
                "↑ ${abs(percentage)}% from $previousPeriodName"
            }

            percentage < 0 -> {
                "↓ ${abs(percentage)}% from $previousPeriodName"
            }

            else -> {
                "0% change from $previousPeriodName"
            }
        }

        val textColor = when {
            percentage > 0 -> Color.rgb(239, 68, 68)
            percentage < 0 -> Color.rgb(34, 197, 94)
            else -> grayColor
        }

        comparisonText.setTextColor(textColor)
    }

    private fun updateCategoryChart(
        view: View,
        expenses: List<Expense>
    ) {
        val legendContainer = view.findViewById<LinearLayout>(
            R.id.categoryLegendContainer
        )

        val emptyText = view.findViewById<TextView>(
            R.id.tvReportNoCategories
        )

        val donutChart = view.findViewById<ReportsChartView>(
            R.id.donutChart
        )

        legendContainer.removeAllViews()

        val categoryTotals = expenses
            .groupBy { expense ->
                expense.category.trim().ifBlank {
                    "Others"
                }
            }
            .mapValues { entry ->
                entry.value.sumOf { expense ->
                    expense.amountCentavos
                }
            }
            .filterValues { amount ->
                amount > 0L
            }
            .toList()
            .sortedByDescending { entry ->
                entry.second
            }

        val totalAmount = categoryTotals.sumOf { entry ->
            entry.second
        }

        emptyText.visibility = if (totalAmount == 0L) {
            View.VISIBLE
        } else {
            View.GONE
        }

        val slices = categoryTotals.mapIndexed { index, entry ->
            ReportsChartView.Slice(
                amount = entry.second,
                color = categoryColors[
                    index % categoryColors.size
                ]
            )
        }

        donutChart.setDonutData(slices)

        categoryTotals.forEachIndexed { index, entry ->
            val categoryName = entry.first
            val categoryAmount = entry.second

            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                setPadding(
                    0,
                    dp(5),
                    0,
                    dp(5)
                )
            }

            val colorDot = View(requireContext()).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(
                        categoryColors[
                            index % categoryColors.size
                        ]
                    )
                }
            }

            row.addView(
                colorDot,
                LinearLayout.LayoutParams(
                    dp(8),
                    dp(8)
                ).apply {
                    marginEnd = dp(7)
                }
            )

            val nameText = TextView(requireContext()).apply {
                text = categoryName
                textSize = 10f
                setTextColor(grayColor)
                maxLines = 1
                ellipsize =
                    android.text.TextUtils.TruncateAt.END
            }

            row.addView(
                nameText,
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )

            val percentage = if (totalAmount > 0L) {
                (
                        categoryAmount.toDouble() /
                                totalAmount.toDouble() * 100.0
                        ).roundToInt()
            } else {
                0
            }

            val percentageText = TextView(
                requireContext()
            ).apply {
                text = "$percentage%"
                textSize = 10f
                setTextColor(
                    Color.rgb(55, 65, 81)
                )
            }

            row.addView(percentageText)
            legendContainer.addView(row)
        }
    }

    private fun updateSpendingTrend(
        view: View,
        expenses: List<Expense>,
        start: LocalDate
    ) {
        val trendTitle = view.findViewById<TextView>(
            R.id.tvTrendTitle
        )

        val barChart = view.findViewById<ReportsChartView>(
            R.id.weeklyChart
        )

        val chartData: Pair<List<Long>, List<String>> =
            when (selectedPeriod) {

                "Daily" -> {
                    trendTitle.text = "Daily Spending Trend"

                    val values = listOf(
                        expenses.sumOf { expense ->
                            expense.amountCentavos
                        }
                    )

                    val labels = listOf(
                        start.format(
                            DateTimeFormatter.ofPattern(
                                "MMM d",
                                Locale.ENGLISH
                            )
                        )
                    )

                    Pair(values, labels)
                }

                "Weekly" -> {
                    trendTitle.text = "Daily Spending Trend"

                    val values = MutableList(7) { 0L }

                    val labels = listOf(
                        "Mon", "Tue", "Wed", "Thu",
                        "Fri", "Sat", "Sun"
                    )

                    expenses.forEach { expense ->
                        val date = parseExpenseDate(
                            expense.date
                        )

                        if (date != null) {
                            val index = ChronoUnit.DAYS.between(
                                start,
                                date
                            ).toInt()

                            if (index in values.indices) {
                                values[index] +=
                                    expense.amountCentavos
                            }
                        }
                    }

                    Pair(values, labels)
                }

                "Yearly" -> {
                    trendTitle.text = "Monthly Spending Trend"

                    val values = MutableList(12) { 0L }

                    val labels = listOf(
                        "Jan", "Feb", "Mar", "Apr",
                        "May", "Jun", "Jul", "Aug",
                        "Sep", "Oct", "Nov", "Dec"
                    )

                    expenses.forEach { expense ->
                        val date = parseExpenseDate(
                            expense.date
                        )

                        if (date != null) {
                            val index = date.monthValue - 1

                            if (index in values.indices) {
                                values[index] +=
                                    expense.amountCentavos
                            }
                        }
                    }

                    Pair(values, labels)
                }

                else -> {
                    trendTitle.text = "Weekly Spending Trend"

                    val values = MutableList(5) { 0L }

                    val labels = listOf(
                        "W1", "W2", "W3", "W4", "W5"
                    )

                    expenses.forEach { expense ->
                        val date = parseExpenseDate(
                            expense.date
                        )

                        if (date != null) {
                            val index = (
                                    (date.dayOfMonth - 1) / 7
                                    ).coerceIn(0, 4)

                            values[index] +=
                                expense.amountCentavos
                        }
                    }

                    Pair(values, labels)
                }
            }

        barChart.setBarData(
            chartData.first,
            chartData.second
        )
    }

    private fun dp(value: Int): Int {
        return (
                value * resources.displayMetrics.density
                ).toInt()
    }
}