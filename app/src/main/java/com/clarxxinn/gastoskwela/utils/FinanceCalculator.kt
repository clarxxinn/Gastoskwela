package com.clarxxinn.gastoskwela.utils

import com.clarxxinn.gastoskwela.data.Allowance
import com.clarxxinn.gastoskwela.data.Expense

data class FinanceSummary(
    val totalAllowanceCentavos: Long,
    val regularExpensesCentavos: Long,
    val totalSpentCentavos: Long,
    val remainingBalanceCentavos: Long
)

object FinanceCalculator {

    fun calculate(
        allowances: List<Allowance>,
        expenses: List<Expense>
    ): FinanceSummary {

        val totalAllowance = allowances.sumOf {
            it.amountCentavos
        }

        val regularExpenses = expenses.sumOf {
            it.amountCentavos
        }

        val totalSpent = regularExpenses

        val remainingBalance = Math.subtractExact(
            totalAllowance,
            totalSpent
        )

        return FinanceSummary(
            totalAllowanceCentavos = totalAllowance,
            regularExpensesCentavos = regularExpenses,
            totalSpentCentavos = totalSpent,
            remainingBalanceCentavos = remainingBalance
        )
    }
}