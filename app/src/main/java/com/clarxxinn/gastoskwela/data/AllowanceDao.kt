package com.clarxxinn.gastoskwela.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AllowanceDao {

    @Query("SELECT * FROM allowances ORDER BY date DESC, id DESC")
    fun getAllAllowances(): Flow<List<Allowance>>

    @Query("SELECT COALESCE(SUM(amountCentavos), 0) FROM allowances")
    fun getTotalAllowance(): Flow<Long>

    @Insert
    suspend fun insertAllowance(allowance: Allowance)

    @Update
    suspend fun updateAllowance(allowance: Allowance)

    @Delete
    suspend fun deleteAllowance(allowance: Allowance)
}