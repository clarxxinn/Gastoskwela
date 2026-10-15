package com.clarxxinn.gastoskwela.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SchoolPaymentTransactionDao {

    @Query(
        """
        SELECT *
        FROM school_payment_transactions
        ORDER BY paymentDate DESC, id DESC
        """
    )
    fun getAllTransactions():
            Flow<List<SchoolPaymentTransaction>>

    @Query(
        """
        SELECT *
        FROM school_payment_transactions
        WHERE schoolPaymentId = :paymentId
        ORDER BY paymentDate DESC, id DESC
        """
    )
    fun getTransactionsForPayment(
        paymentId: Int
    ): Flow<List<SchoolPaymentTransaction>>

    @Query(
        """
        SELECT COALESCE(SUM(amountCentavos), 0)
        FROM school_payment_transactions
        WHERE schoolPaymentId = :paymentId
        """
    )
    fun getTotalPaidForPayment(
        paymentId: Int
    ): Flow<Long>

    @Insert
    suspend fun insert(
        transaction: SchoolPaymentTransaction
    ): Long

    @Delete
    suspend fun delete(
        transaction: SchoolPaymentTransaction
    )
}