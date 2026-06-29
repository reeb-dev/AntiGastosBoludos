package com.antigastos.boludos.domain

import com.antigastos.boludos.domain.model.HomeUiModel
import kotlin.math.max
import kotlin.random.Random

/**
 * Tipo de consejo. Define cuánto dura el modal y qué color/mood usar.
 */
enum class ConsejoKind {
    /** Recompensa por ahorrar, día sin gastar, autoregalo. */
    PREMIO,

    /** Aviso suave: "te queda margen para salir, etc." Se reusa para tips de
     *  educación financiera y crecimiento (UVA, FCI, MEP, Cedears, etc.). */
    SUGERENCIA,

    /** Te zarpaste hoy, frená la mano. */
    FRENO,

    /** Pasaste la meta, modo emergencia. */
    EMERGENCIA,
}

data class Consejo(
    val id: String,
    val kind: ConsejoKind,
    val title: String,
    val body: String,
    /** Cuánto dura el modal en pantalla. */
    val durationMs: Long,
    /** Personalidad que pone la voz al consejo (random, ruleteable). null = sin voz. */
    val personaKey: String? = null,
    val personaEmoji: String? = null,
    val personaName: String? = null,
)

object ConsejoEngine {

    /**
     * Devuelve el consejo más relevante para el estado actual, o null si no hay
     * nada que mostrar.
     *
     * Prioridad:
     *  1) Emergencia real (te pasaste la meta) → mensaje fijo.
     *  2) Te zarpaste HOY (>= 30 lucas) → mensaje fijo de freno.
     *  3) Día completo sin gastar con varios gastos previos → premio.
     *  4) Default: sale algo aleatorio del catálogo financiero ([FinancialAdviceCatalog]),
     *     filtrado por las condiciones del estado (top categoría, racha, meta, etc.).
     *  5) Si nada matchea, tip ligero de mindset.
     */
    fun pick(
        state: HomeUiModel,
        rng: Random = Random.Default,
    ): Consejo? {
        val pctGoal = state.globalProgress?.toDouble()

        // 1) Pasaste la meta → emergencia (siempre prioritario).
        if (pctGoal != null && pctGoal >= 1.0) {
            return Consejo(
                id = "emergency_over_goal",
                kind = ConsejoKind.EMERGENCIA,
                title = "🚨 Modo emergencia",
                body = listOf(
                    "Te pasaste la meta del mes. Apretá el cinturón unos días y bajale al delivery.",
                    "Te corriste de la meta. Hasta el 30 viví con lo justo y mete vianda.",
                    "Saliste de presupuesto. No te castigues, pero acá frenamos los gustos hasta fin de mes.",
                ).random(rng),
                durationMs = 6000,
            )
        }

        val daySpentLucas = MoneyFormat.pesosToLucas(state.daySpentPesos)

        // 2) Te zarpaste HOY (≥ 30 lucas en el día) → freno duro.
        if (daySpentLucas >= 30) {
            return Consejo(
                id = "freno_dia_alto",
                kind = ConsejoKind.FRENO,
                title = "🛑 Frená la mano",
                body = "Hoy ya te volaste $daySpentLucas lucas. " + listOf(
                    "Probá cocinar mañana.",
                    "Cero delivery por 48hs.",
                    "Modo rata activado 🐀.",
                    "Un día sin tarjeta y se acomoda.",
                    "Mandá lo que sobra a una cuenta remunerada antes de seguir gastando.",
                ).random(rng),
                durationMs = 5500,
            )
        }

        // 3) Día sin gastar con varios gastos previos → premio fuerte.
        if (state.daySpentPesos == 0L && state.expenseCount >= 2) {
            val rewards = listOf(
                "te ganaste un cafecito gratis ☕",
                "te merecés una birra a fin de mes 🍺",
                "guardate un canuto chico, lo lograste 🤫",
                "andá comprate un alfajor sin culpa 🍫",
                "premio simbólico: una galletita extra 🍪",
                "mete esos pesos en un FCI antes de que te tienten 📈",
            )
            return Consejo(
                id = "premio_zero_today",
                kind = ConsejoKind.PREMIO,
                title = "🏆 Día sin gastar",
                body = "Hoy no tocaste la billetera. Bien ahí — ${rewards.random(rng)}.",
                durationMs = 5000,
            )
        }

        // 4) Catálogo financiero (consejos contextuales + crecimiento).
        FinancialAdviceCatalog.pick(state, rng)?.let { return it }

        // 5) Fallback ligero si no hay nada en el catálogo (caso raro).
        val totalLucas = MoneyFormat.pesosToLucas(state.totalPesos)
        if (totalLucas >= 1) {
            return Consejo(
                id = "tip_general_fallback",
                kind = ConsejoKind.SUGERENCIA,
                title = "💡 Tip del día",
                body = listOf(
                    "Llevás la guita controlada. Seguí así.",
                    "Andá guardando un canuto chico cada semana 🤫.",
                    "Cocinar 2 días extras te ahorra una luca por mes.",
                    "Hacer vaquita con amigos siempre conviene 🐄.",
                    "Si te sobra plata, no la dejes en caja de ahorro: cuenta remunerada o FCI.",
                ).random(rng),
                durationMs = 4000,
            )
        }

        return null
    }

    /**
     * Aproxima el día actual del mes. Si el HomeUiModel es del mes corriente
     * usamos hoy; si no, devolvemos el último día. Lo dejamos por si otros
     * componentes quieren hacer cuentas con el día.
     */
    @Suppress("unused")
    private fun nowDayOfMonthOrEnd(state: HomeUiModel, daysInMonth: Int): Int {
        val today = java.time.LocalDate.now()
        return if (today.year == state.yearMonth.year && today.monthValue == state.yearMonth.monthValue) {
            today.dayOfMonth
        } else {
            daysInMonth
        }
    }

    @Suppress("unused")
    private fun marginPesosLeft(state: HomeUiModel, daysInMonth: Int): Long {
        val goal = state.globalGoalPesos ?: return 0L
        val today = java.time.LocalDate.now()
        val todayDay = if (today.year == state.yearMonth.year && today.monthValue == state.yearMonth.monthValue) today.dayOfMonth else daysInMonth
        val daysLeft = max(1, daysInMonth - todayDay + 1)
        val remaining = (goal - state.totalPesos).coerceAtLeast(0L)
        return remaining / daysLeft
    }
}
