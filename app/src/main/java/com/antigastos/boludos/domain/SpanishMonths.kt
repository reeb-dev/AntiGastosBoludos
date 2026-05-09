package com.antigastos.boludos.domain

object SpanishMonths {
    private val names = listOf(
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
    )

    fun name(monthValue1Based: Int): String =
        names.getOrNull(monthValue1Based - 1) ?: ""
}
