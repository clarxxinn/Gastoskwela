package com.clarxxinn.gastoskwela.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "allowances")
data class Allowance(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val amountCentavos: Long,
    val source: String,
    val date: String,
    val notes: String = ""
)