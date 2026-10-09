package com.clarxxinn.gastoskwela.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val amountCentavos: Long,
    val category: String,
    val description: String,
    val date: String,
    val notes: String = ""
)