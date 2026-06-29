package com.antigastos.boludos.domain.model

import java.time.LocalDate
import java.time.YearMonth

data class CategorySlice(
    val label: String,
    val slug: String,
    val amountPesos: Long,
)

data class TopDay(
    val date: LocalDate,
    val amountPesos: Long,
)

data class StatsSnapshot(
    val yearMonth: YearMonth,
    val totalPesos: Long,
    val previousMonthTotalPesos: Long,
    val categorySlices: List<CategorySlice>,
    val dailyTotals: Map<Int, Long> = emptyMap(),
    val topDays: List<TopDay> = emptyList(),
    /** Comparación con el mismo mes hace 3 meses (null si no hay datos viejos). */
    val pastSelfLabel: String? = null,
    val pastSelfTotalPesos: Long? = null,
    val pastSelfDiffPesos: Long? = null,
)
