package com.antigastos.boludos.domain.model

import java.time.YearMonth

data class CategorySlice(
    val label: String,
    val slug: String,
    val amountPesos: Long,
)

data class StatsSnapshot(
    val yearMonth: YearMonth,
    val totalPesos: Long,
    val previousMonthTotalPesos: Long,
    val categorySlices: List<CategorySlice>,
)
