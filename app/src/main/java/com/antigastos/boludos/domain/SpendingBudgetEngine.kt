package com.antigastos.boludos.domain

import com.antigastos.boludos.domain.model.HomeUiModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * "¿Cuánto puedo gastar HOY?".
 *
 * Toma la meta global del mes, lo ya gastado y los días que faltan para
 * fin de mes, y devuelve un allowance diario en pesos. Después aplica
 * un factor por feriado/finde para ajustar la expectativa real:
 *
 *  - Feriado de "estar en familia" (Navidad, día del padre/madre, año nuevo) → bumpea.
 *  - Día solemne (Viernes Santo, 24 de marzo) → bajamos el allowance.
 *  - Sábado / domingo → leve bump (todos sabemos que se gasta más).
 *
 * El resultado se usa para:
 *  1. Mostrar un banner en Home: "Hoy podés gastar hasta $X (es Navidad, dale)".
 *  2. Avisar en el editor de gasto si te estás zarpando: "Estás cargando 3x
 *     el promedio de un día como hoy".
 */
object SpendingBudgetEngine {

    enum class Permission { GREEN, YELLOW, RED, FORBIDDEN }

    data class DailyBudget(
        val date: LocalDate,
        val allowanceTodayPesos: Long,
        val spentTodayPesos: Long,
        val spentMonthPesos: Long,
        val goalPesos: Long?,
        val daysLeftInMonth: Int,
        val permission: Permission,
        val message: String,
        val holiday: HolidayCalendar.Holiday?,
        val isWeekend: Boolean,
        val factor: Double,
    )

    fun evaluate(home: HomeUiModel, today: LocalDate = LocalDate.now()): DailyBudget {
        val ym = YearMonth.from(today)
        val daysInMonth = ym.lengthOfMonth()
        val daysLeft = (daysInMonth - today.dayOfMonth + 1).coerceAtLeast(1)
        val holiday = HolidayCalendar.forDate(today)
        val isWeekend = today.dayOfWeek == DayOfWeek.SATURDAY ||
            today.dayOfWeek == DayOfWeek.SUNDAY

        // Allowance base: lo que queda de la meta repartido en los días
        // que quedan. Si no hay meta, usamos un proxy: 3% del total
        // gastado del mes (autodisciplina suave).
        val goal = home.globalGoalPesos
        val spentMonth = home.totalPesos
        val baseAllowance: Long = if (goal != null && goal > 0L) {
            ((goal - spentMonth).coerceAtLeast(0L) / daysLeft)
        } else {
            (spentMonth / daysInMonth).coerceAtLeast(2_000L)
        }

        val factor = computeFactor(holiday, isWeekend, today)
        val allowanceToday = (baseAllowance * factor).toLong().coerceAtLeast(0L)
        val spentToday = home.daySpentPesos

        val permission = when {
            goal != null && goal > 0L && spentMonth >= goal -> Permission.FORBIDDEN
            allowanceToday <= 0L -> Permission.FORBIDDEN
            spentToday <= allowanceToday * 0.5 -> Permission.GREEN
            spentToday <= allowanceToday -> Permission.YELLOW
            spentToday <= allowanceToday * 1.5 -> Permission.RED
            else -> Permission.FORBIDDEN
        }

        val msg = composeMessage(permission, allowanceToday, spentToday, holiday, isWeekend)
        return DailyBudget(
            date = today,
            allowanceTodayPesos = allowanceToday,
            spentTodayPesos = spentToday,
            spentMonthPesos = spentMonth,
            goalPesos = goal,
            daysLeftInMonth = daysLeft,
            permission = permission,
            message = msg,
            holiday = holiday,
            isWeekend = isWeekend,
            factor = factor,
        )
    }

    /**
     * Decide si un gasto puntual conviene o no. Útil para mostrar un
     * dialog de confirmación en el editor.
     */
    fun checkSpend(
        home: HomeUiModel,
        candidatePesos: Long,
        today: LocalDate = LocalDate.now(),
    ): SpendCheck {
        val budget = evaluate(home, today)
        val totalIfSaved = budget.spentTodayPesos + candidatePesos
        val ratio = if (budget.allowanceTodayPesos > 0L) {
            totalIfSaved.toDouble() / budget.allowanceTodayPesos.toDouble()
        } else {
            10.0
        }
        val verdict = when {
            ratio <= 0.7 -> Permission.GREEN
            ratio <= 1.0 -> Permission.YELLOW
            ratio <= 1.5 -> Permission.RED
            else -> Permission.FORBIDDEN
        }
        val tip = when (verdict) {
            Permission.GREEN -> "✅ Gasto razonable para hoy."
            Permission.YELLOW -> "⚠️ Te comés ${(ratio * 100).toInt()}% del allowance del día."
            Permission.RED -> "🚨 Estás gastando ${(ratio * 100).toInt()}% del límite. ¿Seguro?"
            Permission.FORBIDDEN -> "💣 Estás muy zarpado. Hoy te alcanza solo para ${budget.allowanceTodayPesos} pesos."
        }
        return SpendCheck(
            verdict = verdict,
            tip = tip,
            allowanceTodayPesos = budget.allowanceTodayPesos,
            ratio = ratio,
            holiday = budget.holiday,
        )
    }

    data class SpendCheck(
        val verdict: Permission,
        val tip: String,
        val allowanceTodayPesos: Long,
        val ratio: Double,
        val holiday: HolidayCalendar.Holiday?,
    )

    // ---------- Internos ----------

    private fun computeFactor(
        holiday: HolidayCalendar.Holiday?,
        isWeekend: Boolean,
        today: LocalDate,
    ): Double {
        val baseHoliday = holiday?.spendingFactor ?: 1.0
        val weekendBump = if (isWeekend) 1.15 else 1.0
        // Día 1 del mes / día post-cobro (presumimos los 5 primeros) un bump leve.
        val payday = if (today.dayOfMonth in 1..5) 1.10 else 1.0
        return baseHoliday * weekendBump * payday
    }

    private fun composeMessage(
        permission: Permission,
        allowance: Long,
        spent: Long,
        holiday: HolidayCalendar.Holiday?,
        isWeekend: Boolean,
    ): String {
        val holidayPrefix = if (holiday != null) "${holiday.emoji} ${holiday.name}: " else ""
        val weekendNote = if (isWeekend && holiday == null) "Es finde, " else ""
        return when (permission) {
            Permission.GREEN ->
                "${holidayPrefix}podés gastar tranquilo hasta \$${pretty(allowance)} hoy."
            Permission.YELLOW ->
                "${holidayPrefix}${weekendNote}vas medio fuerte. Te quedan \$${pretty((allowance - spent).coerceAtLeast(0L))} para hoy."
            Permission.RED ->
                "${holidayPrefix}${weekendNote}te zarpaste un poco. Frená un cambio."
            Permission.FORBIDDEN ->
                "${holidayPrefix}🚨 hoy ya estás muy excedido. Encerrate sin tarjeta."
        }
    }

    private fun pretty(amount: Long): String =
        amount.toString().reversed().chunked(3).joinToString(".").reversed()
}
