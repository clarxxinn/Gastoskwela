package com.clarxxinn.gastoskwela.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "school_payment_transactions",
    foreignKeys = [
        ForeignKey(
            entity = SchoolPayment::class,
            parentColumns = ["id"],
            childColumns = ["schoolPaymentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["schoolPaymentId"])
    ]
)
data class SchoolPaymentTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val schoolPaymentId: Int,

    val amountCentavos: Long,

    val paymentDate: String,

    val paymentMethod: String = "Unspecified",

    val notes: String = ""
)