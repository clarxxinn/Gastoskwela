package com.clarxxinn.gastoskwela.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SchoolPaymentDao {

    // Get all school payments
    @Query(
        """
        SELECT * FROM school_payments
        ORDER BY dueDate ASC, id DESC
        """
    )
    fun getAllPayments(): Flow<List<SchoolPayment>>

    // Get a specific school payment by ID
    @Query(
        """
        SELECT * FROM school_payments
        WHERE id = :id
        LIMIT 1
        """
    )
    suspend fun getPaymentById(id: Int): SchoolPayment?

    // Total school payment obligations
    @Query(
        """
        SELECT COALESCE(SUM(amountCentavos), 0)
        FROM school_payments
        """
    )
    fun getTotalDue(): Flow<Long>

    // Total amount already paid
    @Query(
        """
        SELECT COALESCE(SUM(paidCentavos), 0)
        FROM school_payments
        """
    )
    fun getTotalPaid(): Flow<Long>

    // Total remaining school payment balance
    @Query(
        """
        SELECT COALESCE(
            SUM(amountCentavos - paidCentavos),
            0
        )
        FROM school_payments
        """
    )
    fun getTotalOutstanding(): Flow<Long>

    // Insert school payment (repository)
    @Insert
    suspend fun insert(
        payment: SchoolPayment
    ): Long

    // Insert school payment (PaymentsFragment compatibility)
    @Insert
    suspend fun insertPayment(
        payment: SchoolPayment
    ): Long

    // Update school payment (repository)
    @Update
    suspend fun update(
        payment: SchoolPayment
    )

    // Update school payment (PaymentsFragment compatibility)
    @Update
    suspend fun updatePayment(
        payment: SchoolPayment
    )

    // Delete school payment (repository)
    @Delete
    suspend fun delete(
        payment: SchoolPayment
    )

    // Delete school payment (PaymentsFragment compatibility)
    @Delete
    suspend fun deletePayment(
        payment: SchoolPayment
    )
}