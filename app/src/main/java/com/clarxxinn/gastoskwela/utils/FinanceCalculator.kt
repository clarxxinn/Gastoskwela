package com.clarxxinn.gastoskwela.utils

import com.clarxxinn.gastoskwela.data.Allowance
import com.clarxxinn.gastoskwela.data.Expense
import com.clarxxinn.gastoskwela.data.SchoolPayment

data class FinanceSummary(
    val totalAllowanceCentavos: Long,
    val regularExpensesCentavos: Long,
    val schoolPaymentsPaidCentavos: Long,
    val totalSpentCentavos: Long,
    val remainingBalanceCentavos: Long
)

object FinanceCalculator {

    fun calculate(
        allowances: List<Allowance>,
        expenses: List<Expense>,
        payments: List<SchoolPayment>
    ): FinanceSummary {

        val totalAllowance = allowances.sumOf {
            it.amountCentavos
        }

        val regularExpenses = expenses.sumOf {
            it.amountCentavos
        }

        val schoolPaymentsPaid = payments.sumOf {
            it.paidCentavos
        }

        val totalSpent = Math.addExact(
            regularExpenses,
            schoolPaymentsPaid
        )

        val remainingBalance = Math.subtractExact(
            totalAllowance,
            totalSpent
        )

        return FinanceSummary(
            totalAllowanceCentavos = totalAllowance,
            regularExpensesCentavos = regularExpenses,
            schoolPaymentsPaidCentavos = schoolPaymentsPaid,
            totalSpentCentavos = totalSpent,
            remainingBalanceCentavos = remainingBalance
        )
    }
}