package com.clarxxinn.gastoskwela.data

import androidx.room.withTransaction
import java.time.LocalDate

class SchoolPaymentRepository(
    private val database: GastoskwelaDatabase
) {

    suspend fun recordPayment(
        schoolPaymentId: Int,
        amountCentavos: Long,
        paymentDate: String = LocalDate.now().toString(),
        paymentMethod: String = "Cash",
        notes: String = ""
    ): Long {

        require(amountCentavos > 0L) {
            "Payment amount must be greater than zero."
        }

        require(paymentMethod.isNotBlank()) {
            "Payment method is required."
        }

        LocalDate.parse(paymentDate)

        return database.withTransaction {

            val payment = database.schoolPaymentDao()
                .getPaymentById(schoolPaymentId)
                ?: throw IllegalArgumentException(
                    "School payment not found."
                )

            val remaining = Math.subtractExact(
                payment.amountCentavos,
                payment.paidCentavos
            )

            require(remaining >= amountCentavos) {
                "Payment exceeds the remaining balance."
            }

            val newPaid = Math.addExact(
                payment.paidCentavos,
                amountCentavos
            )

            val transactionId =
                database.schoolPaymentTransactionDao()
                    .insert(
                        SchoolPaymentTransaction(
                            schoolPaymentId = schoolPaymentId,
                            amountCentavos = amountCentavos,
                            paymentDate = paymentDate,
                            paymentMethod = paymentMethod,
                            notes = notes
                        )
                    )

            database.schoolPaymentDao().update(
                payment.copy(
                    paidCentavos = newPaid
                )
            )

            transactionId
        }
    }
}