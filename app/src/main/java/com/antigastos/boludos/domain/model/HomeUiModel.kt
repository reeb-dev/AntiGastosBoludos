package com.antigastos.boludos.domain.model

import java.time.YearMonth

data class TopCategorySpend(
    val name: String,
    val slug: String,
    val amountPesos: Long,
)

data class HomeUiModel(
    val yearMonth: YearMonth,
    val totalPesos: Long,
    val topCategories: List<TopCategorySpend>,
    val globalGoalPesos: Long?,
    val globalProgress: Float?,
    val personalityMessage: String,
)
