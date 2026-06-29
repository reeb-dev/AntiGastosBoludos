package com.antigastos.boludos.domain

import com.antigastos.boludos.domain.model.HomeUiModel
import java.time.LocalDate

data class Forecast(
    val projectedTotalPesos: Long,
    val daysElapsed: Int,
    val daysInMonth: Int,
    val verdict: String,
    val ratioVsGoal: Double?,
)

object ForecastEngine {

    fun forecast(home: HomeUiModel, today: LocalDate = LocalDate.now()): Forecast {
        val daysInMonth = home.yearMonth.lengthOfMonth()
        val sameMonth = today.year == home.yearMonth.year &&
            today.monthValue == home.yearMonth.monthValue
        val daysElapsed = if (sameMonth) today.dayOfMonth else daysInMonth
        val perDay = if (daysElapsed > 0) home.totalPesos.toDouble() / daysElapsed else 0.0
        val projected = (perDay * daysInMonth).toLong().coerceAtLeast(home.totalPesos)

        val ratio = home.globalGoalPesos?.takeIf { it > 0L }?.let {
            projected.toDouble() / it.toDouble()
        }

        val verdict = when {
            ratio == null -> "A este ritmo terminás gastando ${MoneyFormat.formatPesos(projected)}."
            ratio >= 1.2 -> "🚨 Vas para hacer mierda la meta. Proyección: ${MoneyFormat.formatPesos(projected)}."
            ratio >= 1.0 -> "⚠️ A este ritmo te pasás de la meta. Proyección: ${MoneyFormat.formatPesos(projected)}."
            ratio >= 0.85 -> "👌 Vas justito. Proyección: ${MoneyFormat.formatPesos(projected)}."
            else -> "✅ Vas para terminar el mes en verde: ${MoneyFormat.formatPesos(projected)}."
        }

        return Forecast(
            projectedTotalPesos = projected,
            daysElapsed = daysElapsed,
            daysInMonth = daysInMonth,
            verdict = verdict,
            ratioVsGoal = ratio,
        )
    }
}
