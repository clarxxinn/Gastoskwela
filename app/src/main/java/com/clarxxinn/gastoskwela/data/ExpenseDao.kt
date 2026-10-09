package com.clarxxinn.gastoskwela.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class ExpenseCategoryTotal(
    val category: String,
    val totalCentavos: Long
)

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses ORDER BY date DESC, id DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT COALESCE(SUM(amountCentavos), 0) FROM expenses")
    fun getTotalExpenses(): Flow<Long>

    @Query("""
        SELECT COALESCE(SUM(amountCentavos), 0)
        FROM expenses
        WHERE date LIKE :month || '%'
    """)
    fun getMonthlyExpenses(month: String): Flow<Long>

    @Query("""
        SELECT category, SUM(amountCentavos) AS totalCentavos
        FROM expenses
        GROUP BY category
        ORDER BY totalCentavos DESC
    """)
    fun getExpensesByCategory(): Flow<List<ExpenseCategoryTotal>>

    @Insert
    suspend fun insertExpense(expense: Expense)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)
}