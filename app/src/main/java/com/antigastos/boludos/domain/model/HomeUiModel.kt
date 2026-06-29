package com.antigastos.boludos.domain.model

import com.antigastos.boludos.domain.CopyMood
import com.antigastos.boludos.domain.ShameLevel
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
    val personalityTemplateId: String,
    val personalityMood: CopyMood,
    val personalityUpVotes: Int,
    val personalityDownVotes: Int,
    val expenseCount: Int,
    val lastExpenseSlug: String?,
    val daySpentPesos: Long,
    val streakDaysNoSpend: Int,
    val shameLevel: ShameLevel = ShameLevel.EJEMPLAR,
    val personaKey: String = "termo",
    val personaEmoji: String = "🧉",
    /** Ayer hubo al menos un gasto (mes actual). Sirve para ofrecer escudo de racha. */
    val yesterdayHadExpenses: Boolean = false,
    /** Gasto por día del mes (1..31) para heatmap; vacío si no hay filas. */
    val dailySpendByDay: Map<Int, Long> = emptyMap(),
)
