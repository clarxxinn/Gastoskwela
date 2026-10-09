package com.clarxxinn.gastoskwela.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "school_payments")
data class SchoolPayment(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val category: String,
    val amountCentavos: Long,
    val paidCentavos: Long = 0,
    val dueDate: String,
    val notes: String = ""
) {
    val remainingCentavos: Long
        get() = amountCentavos - paidCentavos

    val status: String
        get() = when {
            paidCentavos >= amountCentavos -> "Paid"
            paidCentavos > 0 -> "Partial"
            else -> "Pending"
        }
}