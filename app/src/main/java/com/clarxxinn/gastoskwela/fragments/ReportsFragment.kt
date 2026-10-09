package com.clarxxinn.gastoskwela.fragments

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.clarxxinn.gastoskwela.R
import com.clarxxinn.gastoskwela.data.Allowance
import com.clarxxinn.gastoskwela.data.Expense
import com.clarxxinn.gastoskwela.data.GastoskwelaDatabase
import com.clarxxinn.gastoskwela.data.SchoolPayment
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.NumberFormat
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

    private fun formatMoney(centavos: Long): String {
        val formatter = NumberFormat.getCurrencyInstance(
            Locale.Builder()
                .setLanguage("en")
                .setRegion("PH")
                .build()
        )

        return formatter.format(BigDecimal.valueOf(centavos, 2))
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState != null) {
            savedInstanceState.getString("selected_month")?.let {
                selectedMonth.value = YearMonth.parse(it)
            }
        }

        view.findViewById<MaterialButton>(
            R.id.btnPreviousMonth
        ).setOnClickListener {
            selectedMonth.value = selectedMonth.value.minusMonths(1)
        }

        view.findViewById<MaterialButton>(
            R.id.btnNextMonth
        ).setOnClickListener {
            selectedMonth.value = selectedMonth.value.plusMonths(1)
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

    private data class ReportData(
        val allowances: List<Allowance>,
        val expenses: List<Expense>,
        val payments: List<SchoolPayment>,
        val month: YearMonth
    )

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

        // Financial Summary
        view.findViewById<TextView>(
            R.id.tvReportAllowance
        ).text = formatMoney(totalAllowance)

        view.findViewById<TextView>(
            R.id.tvReportExpenses
        ).text = formatMoney(totalExpenses)

        view.findViewById<TextView>(
            R.id.tvReportBalance
        ).text = formatMoney(balance)

        // Estimated Savings
        val availableSavings = balance.coerceAtLeast(0L)

        val savingsRate = if (totalAllowance > 0L) {
            (
                    availableSavings.toDouble() /
                            totalAllowance.toDouble() * 100.0
                    ).coerceIn(0.0, 100.0).toInt()
        } else {
            0
        }

        view.findViewById<TextView>(
            R.id.tvSavingsAmount
        ).text = formatMoney(availableSavings)

        view.findViewById<TextView>(
            R.id.tvSavingsRate
        ).text = "Available savings rate: $savingsRate%"

        view.findViewById<ProgressBar>(
            R.id.progressSavings
        ).progress = savingsRate

        // Monthly Report
        val monthPrefix = report.month.toString()

        val monthlyAllowance = report.allowances
            .filter { it.date.startsWith(monthPrefix) }
            .sumOf { it.amountCentavos }

        val monthlyExpenses = report.expenses
            .filter { it.date.startsWith(monthPrefix) }
            .sumOf { it.amountCentavos }

        view.findViewById<TextView>(
            R.id.tvSelectedMonth
        ).text = report.month.format(monthFormatter)

        view.findViewById<TextView>(
            R.id.tvMonthlyAllowance
        ).text = formatMoney(monthlyAllowance)

        view.findViewById<TextView>(
            R.id.tvMonthlyExpenses
        ).text = formatMoney(monthlyExpenses)

        view.findViewById<TextView>(
            R.id.tvMonthlyNet
        ).text = formatMoney(
            monthlyAllowance - monthlyExpenses
        )

        // Category Breakdown (all-time)
        updateCategories(view, report.expenses)

        // School Payments Summary (all-time)
        val paymentDue = report.payments.sumOf {
            it.amountCentavos
        }

        val paymentPaid = report.payments.sumOf {
            it.paidCentavos
        }

        val outstanding = paymentDue - paymentPaid

        view.findViewById<TextView>(
            R.id.tvReportPaymentDue
        ).text = formatMoney(paymentDue)

        view.findViewById<TextView>(
            R.id.tvReportPaymentPaid
        ).text = formatMoney(paymentPaid)

        view.findViewById<TextView>(
            R.id.tvReportPaymentOutstanding
        ).text = formatMoney(outstanding)
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
            .mapValues { (_, items) ->
                items.sumOf { it.amountCentavos }
            }
            .toList()
            .sortedByDescending { it.second }

        emptyText.visibility =
            if (totals.isEmpty()) View.VISIBLE else View.GONE

        val grandTotal = totals.sumOf { it.second }

        totals.forEach { (category, amount) ->

            val percentage = if (grandTotal > 0L) {
                (
                        amount.toDouble() /
                                grandTotal.toDouble() * 100.0
                        ).coerceIn(0.0, 100.0).toInt()
            } else {
                0
            }

            val categoryLabel = TextView(requireContext()).apply {
                text = "$category — ${formatMoney(amount)} ($percentage%)"
                textSize = 14f
                setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.text_primary
                    )
                )
            }

            val progressBar = ProgressBar(
                requireContext(),
                null,
                android.R.attr.progressBarStyleHorizontal
            ).apply {
                max = 100
                progress = percentage
                progressTintList =
                    ContextCompat.getColorStateList(
                        requireContext(),
                        R.color.purple_primary
                    )

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(12)
                ).apply {
                    topMargin = dp(8)
                    bottomMargin = dp(18)
                }
            }

            container.addView(categoryLabel)
            container.addView(progressBar)
        }
    }

    private fun dp(value: Int): Int {
        return (
                value * resources.displayMetrics.density
                ).toInt()
    }
}