package com.antigastos.boludos.domain

import kotlin.random.Random

data class CopyContext(
    val monthLabel: String,
    val totalLucas: Int,
    val topCategoryName: String?,
    val topCategorySlug: String?,
    val topCategoryLucas: Int,
    val deltaLucasVsPrevMonth: Int?,
    val pctOfMonthlyGoal: Double?,
)

/**
 * Motor de textos con tono rioplatense. Sin Android para poder testear en JVM.
 */
object CopyEngine {
    fun pickMessage(ctx: CopyContext, rng: Random = Random.Default): String {
        if (ctx.totalLucas == 0) {
            return "Este mes no registraste gastos. ¿Arrancamos antes de que vuele todo?"
        }

        val bucket = rng.nextInt(100)

        val rules = buildList {
            if (ctx.topCategorySlug == "delivery" && ctx.topCategoryLucas >= 30) {
                add("Mirá vo': ${ctx.topCategoryLucas} lucas solo en delivery este mes 💀")
                add("Te la pasás pidiendo comida: ${ctx.topCategoryLucas} lucas en delivery.")
            }
            if (ctx.topCategorySlug == "delivery") {
                add("${ctx.topCategoryLucas} lucas en delivery en ${ctx.monthLabel}. El horno no existe, ¿no?")
            }
            if (ctx.topCategorySlug == "boludeces") {
                add("El podrío ganó: ${ctx.topCategoryLucas} lucas en boludeces.")
            }
            if (ctx.topCategorySlug == "salidas") {
                add("Salidas ${ctx.topCategoryLucas} lucas. La previa sale cara.")
            }
            val pct = ctx.pctOfMonthlyGoal
            if (pct != null) {
                when {
                    pct >= 1.15 -> add("Pasaste la meta un ${((pct - 1) * 100).toInt()}%. tranqui nomás 😅")
                    pct >= 1.0 -> add("Rompiste la meta del mes. Next: ahorrar o asumirlo.")
                    pct >= 0.85 -> add("Te queda poquito cupo de meta: ${(pct * 100).toInt()}% usado.")
                    pct <= 0.5 -> add("Vas bien contra la meta: todavía te queda aire.")
                }
            }
            val delta = ctx.deltaLucasVsPrevMonth
            if (delta != null && kotlin.math.abs(delta) >= 5) {
                if (delta > 0) {
                    add("Subiste ~${delta} lucas vs el mes pasado. Ojo ahí.")
                } else {
                    add("Bajaste ~${-delta} lucas vs el mes pasado. Capeón.")
                }
            }
        }

        if (rules.isNotEmpty() && bucket < 88) {
            return rules[rng.nextInt(rules.size)]
        }

        return when {
            ctx.totalLucas >= 200 -> "Este mes se te fue como ${ctx.totalLucas} lucas. Acá no juzgamos… mucho."
            ctx.totalLucas >= 80 -> "Van ${ctx.totalLucas} lucas en ${ctx.monthLabel}. Ni tan mal ni tan bien."
            else -> "Llevás ${ctx.totalLucas} lucas en ${ctx.monthLabel}. Controlado."
        }
    }
}
