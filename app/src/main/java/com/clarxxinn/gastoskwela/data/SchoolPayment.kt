package com.clarxxinn.gastoskwela.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "school_payments")
data class SchoolPayment(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val title: String,

    val category: String,

    val amountCentavos: Long,

    val paidCentavos: Long = 0L,

    val dueDate: String,

    val notes: String = "",

    @ColumnInfo(defaultValue = "0")
    val legacyPaidCentavos: Long = 0L
) {
    val remainingCentavos: Long
        get() = (amountCentavos - paidCentavos)
            .coerceAtLeast(0L)

    val status: String
        get() = when {
            paidCentavos >= amountCentavos -> "Paid"
            paidCentavos > 0L -> "Partial"
            else -> "Pending"
        }
}