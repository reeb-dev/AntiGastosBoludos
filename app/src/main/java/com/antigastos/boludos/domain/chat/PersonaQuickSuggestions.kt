package com.antigastos.boludos.domain.chat

import com.antigastos.boludos.domain.CopyContext
import com.antigastos.boludos.domain.CopyMood

/**
 * Chips de sugerencia rápida en el chat de personajes.
 * Los [message] usan keywords que [ChatIntent] reconoce en modo offline.
 */
data class PersonaQuickSuggestion(
    val emoji: String,
    val label: String,
    val message: String,
)

object PersonaQuickSuggestions {

    fun build(ctx: CopyContext?, mood: CopyMood): List<PersonaQuickSuggestion> {
        val out = mutableListOf<PersonaQuickSuggestion>()

        out += PersonaQuickSuggestion(
            emoji = "🎯",
            label = "¿Cómo voy?",
            message = "¿Cómo voy este mes con la guita?",
        )

        out += PersonaQuickSuggestion(
            emoji = "💡",
            label = "Consejo",
            message = "Tirame un consejo financiero corto para hoy.",
        )

        val topSlug = ctx?.topCategorySlug?.lowercase().orEmpty()
        val topName = ctx?.topCategoryName
        if (ctx != null && ctx.topCategoryLucas > 0 && topName != null) {
            out += PersonaQuickSuggestion(
                emoji = "💸",
                label = "¿Y mi $topName?",
                message = "Mi categoría más cara es $topName. ¿Qué hago para bajarla?",
            )
        }

        if (topSlug.contains("delivery") || topSlug.contains("comida") ||
            (ctx?.lastExpenseSlug?.contains("delivery") == true)
        ) {
            out += PersonaQuickSuggestion(
                emoji = "🍔",
                label = "Delivery",
                message = "El delivery me está matando, ¿qué hago?",
            )
        }

        when (mood) {
            CopyMood.ALARM -> out += PersonaQuickSuggestion(
                emoji = "🚨",
                label = "Hasta las manos",
                message = "Estoy fundido, ¿qué hago para no pedir prestado?",
            )
            CopyMood.BURN -> out += PersonaQuickSuggestion(
                emoji = "🔥",
                label = "Bardéame",
                message = "Bardéame por lo que estoy gastando este mes.",
            )
            CopyMood.CHEER -> out += PersonaQuickSuggestion(
                emoji = "🏆",
                label = "Felicitame",
                message = "Hoy no gasté nada, decime algo lindo.",
            )
            CopyMood.NEUTRAL -> { /* consejo universal arriba */ }
        }

        val day = ctx?.dayOfMonth ?: 0
        if (day in 22..31) {
            out += PersonaQuickSuggestion(
                emoji = "📅",
                label = "Fin de mes",
                message = "¿Cómo llego al fin de mes sin fundirme?",
            )
        }

        val pct = ctx?.pctOfMonthlyGoal ?: 0.0
        when {
            pct >= 1.0 -> out += PersonaQuickSuggestion(
                emoji = "🎯",
                label = "Pasé la meta",
                message = "Ya pasé la meta del mes, ¿qué corto?",
            )
            pct in 0.85..0.999 -> out += PersonaQuickSuggestion(
                emoji = "⚠️",
                label = "Casi al límite",
                message = "Estoy al ${(pct * 100).toInt()}% de la meta, ¿qué pinta?",
            )
        }

        out += PersonaQuickSuggestion(
            emoji = "🧠",
            label = "Analizame",
            message = "Analizame mis gastos, ¿qué patrón ves?",
        )

        return out.distinctBy { it.label }
    }

    fun defaults(): List<PersonaQuickSuggestion> = listOf(
        PersonaQuickSuggestion("🎯", "¿Cómo voy?", "¿Cómo voy con la guita este mes?"),
        PersonaQuickSuggestion("💡", "Consejo", "Tirame un consejo para ahorrar."),
        PersonaQuickSuggestion("🔥", "Bardéame", "Bardéame por mis gastos."),
        PersonaQuickSuggestion("🧠", "Analizame", "Analizame mis gastos del mes."),
    )
}
