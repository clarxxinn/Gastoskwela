package com.clarxxinn.gastoskwela.fragments

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.clarxxinn.gastoskwela.R
import com.clarxxinn.gastoskwela.data.Allowance
import com.clarxxinn.gastoskwela.data.Expense
import com.clarxxinn.gastoskwela.data.GastoskwelaDatabase
import com.clarxxinn.gastoskwela.data.SchoolPayment
import com.clarxxinn.gastoskwela.utils.MoneyUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class ReportsFragment : Fragment(R.layout.fragment_reports) {

    private val database by lazy {
        GastoskwelaDatabase.getDatabase(requireContext())
    }

    private val selectedMonth = MutableStateFlow(YearMonth.now())

    private val monthFormatter = DateTimeFormatter.ofPattern(
        "MMMM yyyy",
        Locale.ENGLISH
    )

    private val purple = Color.rgb(91, 63, 217)
    private val green = Color.rgb(22, 163, 74)
    private val red = Color.rgb(239, 68, 68)
    private val gray = Color.rgb(107, 114, 128)

    private data class ReportData(
        val allowances: List<Allowance>,
        val expenses: List<Expense>,
        val payments: List<SchoolPayment>,
        val month: YearMonth
    )

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        savedInstanceState
            ?.getString("selected_month")
            ?.let { savedMonth ->
                runCatching {
                    YearMonth.parse(savedMonth)
                }.getOrNull()?.let {
                    selectedMonth.value = it
                }
            }

        view.findViewById<View>(
            R.id.btnPreviousMonth
        ).setOnClickListener {
            selectedMonth.value =
                selectedMonth.value.minusMonths(1)
        }

        view.findViewById<View>(
            R.id.btnNextMonth
        ).setOnClickListener {
            selectedMonth.value =
                selectedMonth.value.plusMonths(1)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {
                combine(
                    database.allowanceDao().getAllAllowances(),
                    database.expenseDao().getAllExpenses(),
                    database.schoolPaymentDao().getAllPayments(),
                    selectedMonth
                ) { allowances, expenses, payments, month ->
                    ReportData(
                        allowances = allowances,
                        expenses = expenses,
                        payments = payments,
                        month = month
                    )
                }.collect { report ->
                    updateReport(view, report)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(
            "selected_month",
            selectedMonth.value.toString()
        )

        super.onSaveInstanceState(outState)
    }

    private fun percentage(
        numerator: Long,
        denominator: Long
    ): Int {
        if (denominator <= 0L) return 0

        return (
                numerator.toDouble() /
                        denominator.toDouble() * 100.0
                ).toInt().coerceIn(0, 100)
    }

    private fun updateReport(
        view: View,
        report: ReportData
    ) {
        val totalAllowance = report.allowances.sumOf {
            it.amountCentavos
        }

        val totalExpenses = report.expenses.sumOf {
            it.amountCentavos
        }

        val balance = totalAllowance - totalExpenses

        updateText(
            view,
            R.id.tvReportAllowance,
            MoneyUtils.format(totalAllowance)
        )

        updateText(
            view,
            R.id.tvReportExpenses,
            MoneyUtils.format(totalExpenses)
        )

        updateText(
            view,
            R.id.tvReportBalance,
            MoneyUtils.format(balance)
        )

        updateSavings(
            view,
            totalAllowance,
            balance
        )

        val monthPrefix = report.month.toString()

        val monthlyAllowance = report.allowances
            .filter { it.date.startsWith(monthPrefix) }
            .sumOf { it.amountCentavos }

        val monthlyExpenseRecords = report.expenses
            .filter { it.date.startsWith(monthPrefix) }

        val monthlyExpenses = monthlyExpenseRecords.sumOf {
            it.amountCentavos
        }

        val monthlyNet = monthlyAllowance - monthlyExpenses

        updateText(
            view,
            R.id.tvSelectedMonth,
            report.month.format(monthFormatter)
        )

        updateText(
            view,
            R.id.tvMonthlyAllowance,
            MoneyUtils.format(monthlyAllowance)
        )

        updateText(
            view,
            R.id.tvMonthlyExpenses,
            MoneyUtils.format(monthlyExpenses)
        )

        updateText(
            view,
            R.id.tvMonthlyNet,
            MoneyUtils.format(monthlyNet)
        )

        view.findViewById<TextView>(
            R.id.tvMonthlyNet
        ).setTextColor(
            if (monthlyNet < 0L) red else purple
        )

        val monthlyInsight = when {
            monthlyAllowance == 0L && monthlyExpenses == 0L ->
                "No allowance or expenses recorded for this month."

            monthlyNet < 0L ->
                "Your expenses exceeded this month's allowance."

            monthlyNet == 0L ->
                "Your recorded allowance and expenses are equal."

            else ->
                "Great! Your recorded allowance is higher than your expenses."
        }

        updateText(
            view,
            R.id.tvMonthlyInsight,
            monthlyInsight
        )

        updateCategories(
            view,
            monthlyExpenseRecords
        )

        updateSchoolPayments(
            view,
            report.payments
        )
    }

    private fun updateSavings(
        view: View,
        totalAllowance: Long,
        balance: Long
    ) {
        val availableSavings = balance.coerceAtLeast(0L)

        val savingsRate = percentage(
            availableSavings,
            totalAllowance
        )

        updateText(
            view,
            R.id.tvSavingsAmount,
            MoneyUtils.format(availableSavings)
        )

        updateText(
            view,
            R.id.tvSavingsRate,
            "Available savings rate: $savingsRate%"
        )

        view.findViewById<ProgressBar>(
            R.id.progressSavings
        ).progress = savingsRate

        val insight = when {
            totalAllowance <= 0L ->
                "Add allowance records to start tracking your budget."

            balance < 0L ->
                "Your recorded expenses are higher than your allowance."

            savingsRate >= 50 ->
                "You're keeping at least half of your allowance unspent."

            savingsRate >= 20 ->
                "You still have part of your allowance available."

            balance > 0L ->
                "Your remaining allowance is getting low."

            else ->
                "Your recorded allowance has been fully spent."
        }

        updateText(
            view,
            R.id.tvSavingsInsight,
            insight
        )
    }

    private fun updateCategories(
        view: View,
        expenses: List<Expense>
    ) {
        val container = view.findViewById<LinearLayout>(
            R.id.categoryReportContainer
        )

        val emptyText = view.findViewById<TextView>(
            R.id.tvEmptyCategories
        )

        container.removeAllViews()

        val totals = expenses
            .groupBy { it.category }
            .mapValues { (_, records) ->
                records.sumOf { it.amountCentavos }
            }
            .toList()
            .sortedByDescending { it.second }

        emptyText.visibility = if (totals.isEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }

        if (totals.isEmpty()) {
            emptyText.text =
                "No expenses recorded for this month."
            return
        }

        val grandTotal = totals.sumOf { it.second }

        totals.forEach { (category, amount) ->
            val categoryPercentage = percentage(
                amount,
                grandTotal
            )

            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(19)
                }
            }

            val header = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            val categoryLabel = TextView(requireContext()).apply {
                text = category
                textSize = 14f
                setTextColor(Color.rgb(31, 41, 55))

                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            val amountLabel = TextView(requireContext()).apply {
                text = MoneyUtils.format(amount)
                textSize = 14f
                setTextColor(purple)
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            header.addView(categoryLabel)
            header.addView(amountLabel)

            val progressBar = ProgressBar(
                requireContext(),
                null,
                android.R.attr.progressBarStyleHorizontal
            ).apply {
                max = 100
                progress = categoryPercentage
                progressTintList = ColorStateList.valueOf(
                    categoryColor(category)
                )
                progressBackgroundTintList =
                    ColorStateList.valueOf(
                        Color.rgb(236, 240, 255)
                    )

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(9)
                ).apply {
                    topMargin = dp(9)
                }
            }

            val percentLabel = TextView(requireContext()).apply {
                text = "$categoryPercentage% of monthly expenses"
                textSize = 11f
                setTextColor(gray)

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(6)
                }
            }

            row.addView(header)
            row.addView(progressBar)
            row.addView(percentLabel)

            container.addView(row)
        }
    }

    private fun categoryColor(category: String): Int {
        return when (category.lowercase(Locale.ENGLISH)) {
            "food", "pagkain" ->
                Color.rgb(245, 158, 11)

            "transportation", "pamasahe" ->
                Color.rgb(59, 130, 246)

            "school supplies" ->
                Color.rgb(139, 92, 246)

            "projects" ->
                Color.rgb(236, 72, 153)

            "load/internet" ->
                Color.rgb(6, 182, 212)

            else ->
                Color.rgb(91, 63, 217)
        }
    }

    private fun updateSchoolPayments(
        view: View,
        payments: List<SchoolPayment>
    ) {
        val totalDue = payments.sumOf {
            it.amountCentavos
        }

        val totalPaid = payments.sumOf {
            it.paidCentavos
        }

        val outstanding = payments.sumOf {
            it.remainingCentavos
        }

        val paymentRate = percentage(
            totalPaid,
            totalDue
        )

        updateText(
            view,
            R.id.tvReportPaymentDue,
            MoneyUtils.format(totalDue)
        )

        updateText(
            view,
            R.id.tvReportPaymentPaid,
            MoneyUtils.format(totalPaid)
        )

        updateText(
            view,
            R.id.tvReportPaymentOutstanding,
            MoneyUtils.format(outstanding)
        )

        view.findViewById<ProgressBar>(
            R.id.progressReportPayments
        ).progress = paymentRate

        updateText(
            view,
            R.id.tvReportPaymentProgress,
            "$paymentRate% paid"
        )
    }

    private fun updateText(
        view: View,
        id: Int,
        value: String
    ) {
        view.findViewById<TextView>(id).text = value
    }

    private fun dp(value: Int): Int {
        return (
                value * resources.displayMetrics.density
                ).toInt()
    }
}