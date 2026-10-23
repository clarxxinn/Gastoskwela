package com.clarxxinn.gastoskwela

import com.clarxxinn.gastoskwela.data.Allowance
import com.clarxxinn.gastoskwela.data.Expense
import com.clarxxinn.gastoskwela.utils.FinanceCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class FinanceCalculatorTest {

    @Test
    fun expensesReduceAvailableBalance() {
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

        val result = FinanceCalculator.calculate(
            allowances = allowances,
            expenses = expenses
        )

        assertEquals(
            200_000L,
            result.totalAllowanceCentavos
        )

        assertEquals(
            40_000L,
            result.regularExpensesCentavos
        )

        assertEquals(
            40_000L,
            result.totalSpentCentavos
        )

        assertEquals(
            160_000L,
            result.remainingBalanceCentavos
        )
    }

    @Test
    fun noExpensesKeepFullAllowance() {
        val result = FinanceCalculator.calculate(
            allowances = listOf(
                Allowance(
                    amountCentavos = 100_000L,
                    source = "Parents",
                    date = "2026-10-10"
                )
            ),
            expenses = emptyList()
        )

        assertEquals(
            0L,
            result.totalSpentCentavos
        )

        assertEquals(
            100_000L,
            result.remainingBalanceCentavos
        )
    }

    @Test
    fun expensesCanExceedAllowance() {
        val result = FinanceCalculator.calculate(
            allowances = listOf(
                Allowance(
                    amountCentavos = 50_000L,
                    source = "Parents",
                    date = "2026-10-10"
                )
            ),
            expenses = listOf(
                Expense(
                    amountCentavos = 75_000L,
                    category = "Food",
                    description = "Meals",
                    date = "2026-10-10"
                )
            )
        )

        assertEquals(
            75_000L,
            result.totalSpentCentavos
        )

        assertEquals(
            -25_000L,
            result.remainingBalanceCentavos
        )
    }

    @Test
    fun emptyRecordsReturnZeroBalance() {
        val result = FinanceCalculator.calculate(
            allowances = emptyList(),
            expenses = emptyList()
        )

        assertEquals(
            0L,
            result.totalAllowanceCentavos
        )

        assertEquals(
            0L,
            result.totalSpentCentavos
        )

        assertEquals(
            0L,
            result.remainingBalanceCentavos
        )
    }
}