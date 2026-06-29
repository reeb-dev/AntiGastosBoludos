package com.antigastos.boludos.domain

import com.antigastos.boludos.domain.model.HomeUiModel

/**
 * Reglas para sumar/restar puntos a partir del estado del Home y de eventos puntuales
 * (un gasto recién insertado, un logro desbloqueado).
 *
 * Pure: sin acceso a DB ni IO. Devuelve la lista de [Delta] a aplicar.
 */
object ScoreEngine {

    enum class Kind(val storage: String, val emoji: String) {
        REWARD("REWARD", "🏆"),
        FAIL("FAIL", "💸"),
    }

    data class Delta(
        val kind: Kind,
        val label: String,
        val emoji: String,
        val points: Long,
    )

    /**
     * Evaluación diaria. La misma firma del día devuelve los mismos deltas, así que
     * el repositorio puede deduplicar con `lastEvaluatedSig`.
     */
    fun evaluateDay(home: HomeUiModel): List<Delta> {
        val deltas = mutableListOf<Delta>()
        val daySpentLucas = MoneyFormat.pesosToLucas(home.daySpentPesos)
        val pctGoal = home.globalProgress?.toDouble()
        val streak = home.streakDaysNoSpend

        when {
            home.expenseCount == 0 -> Unit
            daySpentLucas == 0 -> deltas += Delta(Kind.REWARD, "Día limpio", "🧼", +5)
            daySpentLucas in 1..15 -> deltas += Delta(Kind.REWARD, "Gastaste poquito", "🪙", +1)
            daySpentLucas in 16..30 -> Unit
            daySpentLucas in 31..50 -> deltas += Delta(Kind.FAIL, "Te zarpaste un poco hoy", "⚠️", -2)
            daySpentLucas in 51..100 -> deltas += Delta(Kind.FAIL, "Fundazo del día", "🔥", -5)
            else -> deltas += Delta(Kind.FAIL, "Catástrofe diaria", "💣", -10)
        }

        when {
            pctGoal == null -> Unit
            pctGoal >= 1.10 -> deltas += Delta(Kind.FAIL, "Rompiste la meta del mes", "🚨", -15)
            pctGoal >= 1.0 -> deltas += Delta(Kind.FAIL, "Te pasaste la meta", "📉", -10)
            pctGoal <= 0.5 -> deltas += Delta(Kind.REWARD, "Vas re bien con la meta", "📈", +3)
        }

        when {
            streak >= 30 -> deltas += Delta(Kind.REWARD, "30 días de racha", "🏆", +25)
            streak >= 14 -> deltas += Delta(Kind.REWARD, "14 días sin gastar", "🧘", +12)
            streak >= 7 -> deltas += Delta(Kind.REWARD, "Una semana en modo rata", "🐀", +6)
            streak >= 3 -> deltas += Delta(Kind.REWARD, "3 días limpios", "🔥", +3)
        }

        return deltas
    }

    /** Cuando se inserta un gasto puntual, evaluamos si fue fundazo. */
    fun evaluateExpense(amountLucas: Long): Delta? = when {
        amountLucas in 1..9 -> null
        amountLucas in 10..29 -> Delta(Kind.REWARD, "Gasto chico, controlado", "🪙", +1)
        amountLucas in 30..49 -> null
        amountLucas in 50..99 -> Delta(Kind.FAIL, "Gasto importante", "💸", -3)
        amountLucas >= 100 -> Delta(Kind.FAIL, "Gastonazo de lucas", "💣", -8)
        else -> null
    }

    /** Cada logro nuevo suma un bonus fijo. */
    fun evaluateAchievementUnlock(achievementKey: String, achievementTitle: String): Delta =
        Delta(
            kind = Kind.REWARD,
            label = "Trofeo: $achievementTitle",
            emoji = "🏅",
            points = 20,
        )
}
