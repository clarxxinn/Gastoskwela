package com.clarxxinn.gastoskwela

import com.clarxxinn.gastoskwela.data.Allowance
import com.clarxxinn.gastoskwela.data.Expense
import com.clarxxinn.gastoskwela.data.SchoolPayment
import com.clarxxinn.gastoskwela.utils.FinanceCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class FinanceCalculatorTest {

    @Test
    fun paidSchoolPaymentsReduceAvailableBalance() {
        val allowances = listOf(
            Allowance(
                amountCentavos = 200_000L,
                source = "Parents",
                date = "2026-10-10"
            )
        )

        val expenses = listOf(
            Expense(
                amountCentavos = 40_000L,
                category = "Food",
                description = "Meals",
                date = "2026-10-10"
            )
        )

        val payments = listOf(
            SchoolPayment(
                title = "School Project",
                category = "Projects",
                amountCentavos = 80_000L,
                paidCentavos = 50_000L,
                dueDate = "2026-10-15"
            )
        )

        val result = FinanceCalculator.calculate(
            allowances,
            expenses,
            payments
        )

        assertEquals(
            90_000L,
            result.totalSpentCentavos
        )

        assertEquals(
            110_000L,
            result.remainingBalanceCentavos
        )
    }

    @Test
    fun unpaidSchoolPaymentsDoNotReduceBalance() {
        val result = FinanceCalculator.calculate(
            allowances = listOf(
                Allowance(
                    amountCentavos = 100_000L,
                    source = "Parents",
                    date = "2026-10-10"
                )
            ),
            expenses = emptyList(),
            payments = listOf(
                SchoolPayment(
                    title = "Tuition",
                    category = "Tuition",
                    amountCentavos = 50_000L,
                    paidCentavos = 0L,
                    dueDate = "2026-10-20"
                )
            )
        )

        assertEquals(
            100_000L,
            result.remainingBalanceCentavos
        )
    }
}