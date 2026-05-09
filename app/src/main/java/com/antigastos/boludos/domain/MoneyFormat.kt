package com.antigastos.boludos.domain

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object MoneyFormat {
    private val pesosFormat: NumberFormat by lazy {
        NumberFormat.getCurrencyInstance(Locale("es", "AR")).apply {
            currency = Currency.getInstance("ARS")
        }
    }

    fun formatPesos(amountPesos: Long): String = pesosFormat.format(amountPesos)

    /** 1 luca ≈ 1000 pesos (uso coloquial). */
    fun pesosToLucas(amountPesos: Long): Int =
        (amountPesos / 1000L).toInt().coerceAtLeast(0)
}
