package com.clarxxinn.gastoskwela.fragments

import android.os.Bundle
import android.view.View
import android.widget.ImageView
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
            "'Today,' MMM d, yyyy",
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
                    database.expenseDao().getAllExpenses()
                ) { allowances, expenses ->
                    DashboardData(
                        allowances = allowances,
                        expenses = expenses
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
            R.id.btnHomeReports
        ).setOnClickListener {
            openTab(R.id.nav_reports)
        }

        view.findViewById<View>(
            R.id.btnSeeAllExpenses
        ).setOnClickListener {
            openTab(R.id.nav_expenses)
        }
    }

    private fun openTab(tabId: Int) {
        (requireActivity() as MainActivity).selectTab(tabId)
    }

    private data class DashboardData(
        val allowances: List<Allowance>,
        val expenses: List<Expense>
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

        val balanceView = view.findViewById<TextView>(
            R.id.tvHomeBalance
        )

        balanceView.text = money(remaining)

        balanceView.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                android.R.color.white
            )
        )

        view.findViewById<TextView>(
            R.id.tvHomeAllowance
        ).text = money(totalAllowance)

        view.findViewById<TextView>(
            R.id.tvHomeExpenses
        ).text = money(todayExpenses)

        updateBudgetProgress(
            view = view,
            allowance = totalAllowance,
            spending = totalExpenses
        )

        updateRecentExpenses(
            view = view,
            expenses = data.expenses
        )
    }

    private fun updateBudgetProgress(
        view: View,
        allowance: Long,
        spending: Long
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
            percentageView.text = "0%"
            progressBar.progress = 0

            insightView.text = if (spending > 0L) {
                "Over Budget! Add allowance to cover your spending."
            } else {
                "Add an allowance to start tracking your budget."
            }

            return
        }

        val remaining = allowance - spending

        val remainingPercent =
            remaining.toDouble() / allowance.toDouble() * 100.0

        val displayPercent = remainingPercent
            .coerceIn(0.0, 100.0)
            .toInt()

        progressBar.progress = displayPercent
        percentageView.text = "$displayPercent%"

        insightView.text = when {
            remaining < 0L ->
                "Over Budget! You've exceeded your allowance."

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

    private fun categoryIcon(category: String): Int {
        return when (category) {

            "Food", "Pagkain" ->
                R.drawable.ic_expense_food

            "Transportation", "Pamasahe" ->
                R.drawable.ic_expense_transport

            "Groceries" ->
                R.drawable.ic_expense_groceries

            "Subscriptions" ->
                R.drawable.ic_expense_subscriptions

            "School Supplies", "School" ->
                R.drawable.ic_expense_supplies

            "Projects" ->
                R.drawable.ic_expense_projects

            "Load/Internet", "Load/Data" ->
                R.drawable.ic_expense_load

            "Others", "Miscellaneous" ->
                R.drawable.ic_expense_misc

            else ->
                R.drawable.ic_expense_misc
        }
    }

    private fun displayCategory(category: String): String {
        return when (category) {
            "Food" -> "Pagkain"
            "Transportation" -> "Pamasahe"
            "Load/Internet" -> "Load/Data"
            else -> category
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

        emptyView.visibility =
            if (recent.isEmpty()) View.VISIBLE else View.GONE

        emptyView.text =
            "No expenses yet. Tap Add Expense to get started."

        val today = LocalDate.now().toString()

        recent.forEach { expense ->
            val itemView = layoutInflater.inflate(
                R.layout.item_home_transaction,
                container,
                false
            )

            val iconView = itemView.findViewById<ImageView>(
                R.id.ivTransactionIcon
            )

            iconView.setImageResource(
                categoryIcon(expense.category)
            )

            itemView.findViewById<TextView>(
                R.id.tvTransactionTitle
            ).text = displayCategory(expense.category)

            itemView.findViewById<TextView>(
                R.id.tvTransactionDetails
            ).text = if (expense.date == today) {
                "Today"
            } else {
                expense.date
            }

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
}