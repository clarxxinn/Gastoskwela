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
import com.clarxxinn.gastoskwela.MainActivity
import com.clarxxinn.gastoskwela.R
import com.clarxxinn.gastoskwela.data.Allowance
import com.clarxxinn.gastoskwela.data.Expense
import com.clarxxinn.gastoskwela.data.GastoskwelaDatabase
import com.clarxxinn.gastoskwela.data.SchoolPayment
import com.clarxxinn.gastoskwela.utils.MoneyUtils
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val database by lazy {
        GastoskwelaDatabase.getDatabase(requireContext())
    }

    private fun money(value: Long): String {
        return MoneyUtils.format(value)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val dateFormatter = DateTimeFormatter.ofPattern(
            "EEEE, MMMM d, yyyy",
            Locale.ENGLISH
        )

        view.findViewById<TextView>(
            R.id.tvHomeDate
        ).text = LocalDate.now().format(dateFormatter)

        setupNavigation(view)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {
                combine(
                    database.allowanceDao().getAllAllowances(),
                    database.expenseDao().getAllExpenses(),
                    database.schoolPaymentDao().getAllPayments()
                ) { allowances, expenses, payments ->
                    DashboardData(
                        allowances,
                        expenses,
                        payments
                    )
                }.collect { data ->
                    updateDashboard(view, data)
                }
            }
        }
    }

    private fun setupNavigation(view: View) {

        view.findViewById<View>(
            R.id.btnHomeExpenses
        ).setOnClickListener {
            openTab(R.id.nav_expenses)
        }

        view.findViewById<View>(
            R.id.btnHomeAllowance
        ).setOnClickListener {
            openTab(R.id.nav_allowance)
        }

        view.findViewById<View>(
            R.id.btnHomePayments
        ).setOnClickListener {
            openTab(R.id.nav_payments)
        }

        view.findViewById<View>(
            R.id.btnHomeReports
        ).setOnClickListener {
            openTab(R.id.nav_reports)
        }

        view.findViewById<View>(
            R.id.btnSeeAllExpenses
        ).setOnClickListener {
            openTab(R.id.nav_expenses)
        }

        view.findViewById<View>(
            R.id.btnSeeAllPayments
        ).setOnClickListener {
            openTab(R.id.nav_payments)
        }
    }

    private fun openTab(tabId: Int) {
        (requireActivity() as MainActivity).selectTab(tabId)
    }

    private data class DashboardData(
        val allowances: List<Allowance>,
        val expenses: List<Expense>,
        val payments: List<SchoolPayment>
    )

    private fun updateDashboard(
        view: View,
        data: DashboardData
    ) {
        val totalAllowance = data.allowances.sumOf {
            it.amountCentavos
        }

        val totalExpenses = data.expenses.sumOf {
            it.amountCentavos
        }

        val remaining = totalAllowance - totalExpenses

        val today = LocalDate.now().toString()

        val todayExpenses = data.expenses
            .filter { it.date == today }
            .sumOf { it.amountCentavos }

        view.findViewById<TextView>(
            R.id.tvHomeBalance
        ).text = money(remaining)

        view.findViewById<TextView>(
            R.id.tvHomeAllowance
        ).text = money(totalAllowance)

        view.findViewById<TextView>(
            R.id.tvHomeExpenses
        ).text = money(todayExpenses)

        updateBudgetProgress(
            view,
            totalAllowance,
            totalExpenses
        )

        updateRecentExpenses(
            view,
            data.expenses
        )

        updateUpcomingPayments(
            view,
            data.payments
        )
    }

    private fun updateBudgetProgress(
        view: View,
        allowance: Long,
        expenses: Long
    ) {
        val percentageView = view.findViewById<TextView>(
            R.id.tvHomeBudgetPercentage
        )

        val insightView = view.findViewById<TextView>(
            R.id.tvHomeBudgetInsight
        )

        val progressBar = view.findViewById<ProgressBar>(
            R.id.progressHomeBudget
        )

        if (allowance <= 0L) {
            percentageView.text = "0% remaining"
            insightView.text =
                "Add an allowance to start tracking your budget."
            progressBar.progress = 0
            return
        }

        val remaining = allowance - expenses

        val remainingPercent = (
                remaining.toDouble() /
                        allowance.toDouble() * 100.0
                )

        val displayPercent = remainingPercent
            .coerceIn(0.0, 100.0)
            .toInt()

        progressBar.progress = displayPercent

        percentageView.text =
            "$displayPercent% of allowance remaining"

        insightView.text = when {
            remaining < 0L ->
                "You've exceeded your available allowance."

            remaining == 0L ->
                "You've used all your available allowance."

            remainingPercent <= 20.0 ->
                "Budget alert! Your allowance is running low."

            remainingPercent <= 50.0 ->
                "You've used more than half your allowance."

            else ->
                "You're doing great! Keep tracking your gastos."
        }
    }

    private fun updateRecentExpenses(
        view: View,
        expenses: List<Expense>
    ) {
        val container = view.findViewById<LinearLayout>(
            R.id.homeTransactionsContainer
        )

        val emptyView = view.findViewById<TextView>(
            R.id.tvHomeEmptyTransactions
        )

        container.removeAllViews()

        val recent = expenses
            .sortedWith(
                compareByDescending<Expense> { it.date }
                    .thenByDescending { it.id }
            )
            .take(5)

        emptyView.visibility = if (recent.isEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }

        emptyView.text =
            "No expenses yet. Tap Add Expense to get started."

        recent.forEach { expense ->
            val itemView = layoutInflater.inflate(
                R.layout.item_home_transaction,
                container,
                false
            )

            val categoryIcon = when (
                expense.category.lowercase()
            ) {
                "food", "pagkain" -> "🍔"
                "transportation", "pamasahe" -> "🚌"
                "school supplies" -> "📚"
                "projects" -> "📋"
                "load/internet" -> "📱"
                else -> "🧾"
            }

            val title = expense.description
                .takeIf { it.isNotBlank() }
                ?: expense.category

            itemView.findViewById<TextView>(
                R.id.tvTransactionTitle
            ).text = "$categoryIcon  $title"

            itemView.findViewById<TextView>(
                R.id.tvTransactionDetails
            ).text = "${expense.category} · ${expense.date}"

            val amountView = itemView.findViewById<TextView>(
                R.id.tvTransactionAmount
            )

            amountView.text =
                "-${money(expense.amountCentavos)}"

            amountView.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.expense_red
                )
            )

            container.addView(itemView)
        }
    }

    private fun updateUpcomingPayments(
        view: View,
        payments: List<SchoolPayment>
    ) {
        val container = view.findViewById<LinearLayout>(
            R.id.homePaymentsContainer
        )

        val emptyView = view.findViewById<TextView>(
            R.id.tvHomeEmptyPayments
        )

        container.removeAllViews()

        val today = LocalDate.now().toString()

        val upcoming = payments
            .filter { it.remainingCentavos > 0L }
            .sortedBy { it.dueDate }
            .take(3)

        emptyView.visibility = if (upcoming.isEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }

        emptyView.text =
            "No outstanding school payments. You're all caught up!"

        upcoming.forEach { payment ->
            val itemView = layoutInflater.inflate(
                R.layout.item_home_payment,
                container,
                false
            )

            itemView.findViewById<TextView>(
                R.id.tvHomePaymentTitle
            ).text = payment.title

            itemView.findViewById<TextView>(
                R.id.tvHomePaymentAmount
            ).text = money(payment.remainingCentavos)

            val isOverdue = payment.dueDate < today

            val dueView = itemView.findViewById<TextView>(
                R.id.tvHomePaymentDue
            )

            dueView.text = if (isOverdue) {
                "Overdue: ${payment.dueDate}"
            } else {
                "Due: ${payment.dueDate}"
            }

            dueView.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (isOverdue) {
                        R.color.expense_red
                    } else {
                        R.color.text_secondary
                    }
                )
            )

            itemView.findViewById<TextView>(
                R.id.tvHomePaymentStatus
            ).text = if (isOverdue) {
                "Overdue · ${payment.status}"
            } else {
                payment.status
            }

            container.addView(itemView)
        }
    }
}