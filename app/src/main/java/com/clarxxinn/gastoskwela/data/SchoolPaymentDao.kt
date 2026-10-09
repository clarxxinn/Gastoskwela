package com.clarxxinn.gastoskwela.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SchoolPaymentDao {

    @Query("SELECT * FROM school_payments ORDER BY dueDate ASC, id DESC")
    fun getAllPayments(): Flow<List<SchoolPayment>>

    @Query("SELECT COALESCE(SUM(amountCentavos), 0) FROM school_payments")
    fun getTotalDue(): Flow<Long>

    @Query("SELECT COALESCE(SUM(paidCentavos), 0) FROM school_payments")
    fun getTotalPaid(): Flow<Long>

    @Query(
        "SELECT COALESCE(SUM(amountCentavos - paidCentavos), 0) FROM school_payments"
    )
    fun getTotalOutstanding(): Flow<Long>

    @Insert
    suspend fun insertPayment(payment: SchoolPayment)

    @Update
    suspend fun updatePayment(payment: SchoolPayment)

    @Delete
    suspend fun deletePayment(payment: SchoolPayment)
}