package com.antigastos.boludos.domain

import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.data.SettingsRepository
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/**
 * Cada semana (clave = lunes ISO) genera un mini-desafío local sobre la
 * categoría top o delivery, con tope en pesos basado en la semana anterior.
 */
object WeeklyChallengeSeeder {

    suspend fun maybeSeed(app: AntiGastosApplication) {
        val repo = app.repository
        val settingsRepo: SettingsRepository = app.settingsRepository
        val today = LocalDate.now()
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekKey = monday.toString()
        val before = settingsRepo.getSnapshot()
        if (before.weeklyChallengeWeekStart == weekKey) return

        val home = repo.observeHome(YearMonth.now()).first()
        val slug = home.topCategories.firstOrNull()?.slug
            ?: home.lastExpenseSlug
            ?: "delivery"
        val label = home.topCategories.firstOrNull()?.name
            ?: slug.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        val lastWeek = repo.sumCategorySlugCalendarWeek(slug, weeksAgo = 1)
        val limit = when {
            lastWeek > 0L -> (lastWeek * 0.90).toLong().coerceIn(15_000L, 900_000L)
            else -> 40_000L
        }
        val title = "Esta semana: menos de ${MoneyFormat.formatPesos(limit)} en $label"

        settingsRepo.update {
            it.copy(
                weeklyChallengeWeekStart = weekKey,
                weeklyChallengeSlug = slug,
                weeklyChallengeLimitPesos = limit,
                weeklyChallengeTitle = title,
            )
        }
    }
}
