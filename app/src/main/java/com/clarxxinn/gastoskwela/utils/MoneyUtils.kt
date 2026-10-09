package com.clarxxinn.gastoskwela.utils

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

object MoneyUtils {

    fun parseCentavos(
        input: String,
        allowZero: Boolean = false
    ): Long? {
        return try {
            val amount = BigDecimal(input.trim())

            if (amount.signum() < 0) return null
            if (!allowZero && amount.signum() == 0) return null

            amount.movePointRight(2).longValueExact()
        } catch (_: Exception) {
            null
        }
    }

    fun format(centavos: Long): String {
        val locale = Locale.Builder()
            .setLanguage("en")
            .setRegion("PH")
            .build()

        return NumberFormat.getCurrencyInstance(locale)
            .format(BigDecimal.valueOf(centavos, 2))
    }
}