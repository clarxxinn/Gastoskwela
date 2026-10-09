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
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val database by lazy {
        GastoskwelaDatabase.getDatabase(requireContext())
    }

    private fun formatMoney(centavos: Long): String {
        return MoneyUtils.format(centavos)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        // Quick action: Allowance
        view.findViewById<MaterialButton>(
            R.id.btnHomeAllowance
        ).setOnClickListener {
            (requireActivity() as MainActivity).selectTab(
                R.id.nav_allowance
            )
        }

        // Quick action: Expenses
        view.findViewById<MaterialButton>(
            R.id.btnHomeExpenses
        ).setOnClickListener {
            (requireActivity() as MainActivity).selectTab(
                R.id.nav_expenses
            )
        }

        // Quick action: Payments
        view.findViewById<MaterialButton>(
            R.id.btnHomePayments
        ).setOnClickListener {
            (requireActivity() as MainActivity).selectTab(
                R.id.nav_payments
            )
        }

        // Observe Room database
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
                        allowances = allowances,
                        expenses = expenses,
                        payments = payments
                    )
                }.collect { data ->
                    updateDashboard(view, data)
                }
            }
        }
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

        val balance = totalAllowance - totalExpenses

        // Financial summary
        view.findViewById<TextView>(
            R.id.tvHomeBalance
        ).text = formatMoney(balance)

        view.findViewById<TextView>(
            R.id.tvHomeAllowance
        ).text = formatMoney(totalAllowance)

        view.findViewById<TextView>(
            R.id.tvHomeExpenses
        ).text = formatMoney(totalExpenses)

        updateBudgetInsight(
            view,
            totalAllowance,
            totalExpenses
        )

        updateRecentTransactions(
            view,
            data.allowances,
            data.expenses
        )

        updateUpcomingPayments(
            view,
            data.payments
        )
    }

    private fun updateBudgetInsight(
        view: View,
        allowance: Long,
        expenses: Long
    ) {
        val insightText = view.findViewById<TextView>(
            R.id.tvHomeBudgetInsight
        )

        val percentageText = view.findViewById<TextView>(
            R.id.tvHomeBudgetPercentage
        )

        val progress = view.findViewById<ProgressBar>(
            R.id.progressHomeBudget
        )

        if (allowance <= 0L) {
            insightText.text = if (expenses > 0L) {
                "You have expenses recorded but no allowance yet."
            } else {
                "Add your allowance to start tracking your budget."
            }

            percentageText.text = "No allowance available"
            progress.progress = 0
            return
        }

        val spentPercent =
            expenses.toDouble() / allowance.toDouble() * 100.0

        val displayPercent = spentPercent.toInt()

        progress.progress = spentPercent
            .coerceIn(0.0, 100.0)
            .toInt()

        percentageText.text =
            "$displayPercent% of allowance spent"

        insightText.text = when {
            spentPercent >= 100.0 ->
                "Your expenses have reached or exceeded your allowance."

            spentPercent >= 80.0 ->
                "Budget alert! You have spent most of your allowance."

            spentPercent >= 50.0 ->
                "You have used more than half of your allowance."

            else ->
                "Great! You still have room in your budget."
        }
    }

    private data class HomeTransaction(
        val title: String,
        val category: String,
        val date: String,
        val amountCentavos: Long,
        val isIncome: Boolean,
        val id: Int
    )

    private fun updateRecentTransactions(
        view: View,
        allowances: List<Allowance>,
        expenses: List<Expense>
    ) {
        val container = view.findViewById<LinearLayout>(
            R.id.homeTransactionsContainer
        )

        val emptyText = view.findViewById<TextView>(
            R.id.tvHomeEmptyTransactions
        )

        container.removeAllViews()

        val incomeTransactions = allowances.map { allowance ->
            HomeTransaction(
                title = allowance.source,
                category = "Allowance",
                date = allowance.date,
                amountCentavos = allowance.amountCentavos,
                isIncome = true,
                id = allowance.id
            )
        }

        val expenseTransactions = expenses.map { expense ->
            HomeTransaction(
                title = expense.description,
                category = expense.category,
                date = expense.date,
                amountCentavos = expense.amountCentavos,
                isIncome = false,
                id = expense.id
            )
        }

        val recentTransactions = (
                incomeTransactions + expenseTransactions
                )
            .sortedWith(
                compareByDescending<HomeTransaction> { it.date }
                    .thenByDescending { it.id }
            )
            .take(5)

        emptyText.visibility = if (recentTransactions.isEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }

        emptyText.text =
            "No transactions yet. Add an allowance or expense to get started."

        recentTransactions.forEach { transaction ->
            val itemView = layoutInflater.inflate(
                R.layout.item_home_transaction,
                container,
                false
            )

            itemView.findViewById<TextView>(
                R.id.tvTransactionTitle
            ).text = transaction.title

            itemView.findViewById<TextView>(
                R.id.tvTransactionDetails
            ).text = "${transaction.category} · ${transaction.date}"

            val amountView = itemView.findViewById<TextView>(
                R.id.tvTransactionAmount
            )

            amountView.text = if (transaction.isIncome) {
                "+${formatMoney(transaction.amountCentavos)}"
            } else {
                "-${formatMoney(transaction.amountCentavos)}"
            }

            val amountColor = if (transaction.isIncome) {
                R.color.success_green
            } else {
                R.color.expense_red
            }

            amountView.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    amountColor
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

        val emptyText = view.findViewById<TextView>(
            R.id.tvHomeEmptyPayments
        )

        container.removeAllViews()

        val today = LocalDate.now().toString()

        // Overdue payments first, then nearest due dates
        val upcoming = payments
            .filter { it.remainingCentavos > 0L }
            .sortedBy { it.dueDate }
            .take(5)

        emptyText.visibility = if (upcoming.isEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }

        emptyText.text =
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
            ).text = formatMoney(payment.remainingCentavos)

            val dueText = itemView.findViewById<TextView>(
                R.id.tvHomePaymentDue
            )

            val isOverdue = payment.dueDate < today

            dueText.text = if (isOverdue) {
                "Overdue: ${payment.dueDate}"
            } else {
                "Due: ${payment.dueDate}"
            }

            dueText.setTextColor(
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