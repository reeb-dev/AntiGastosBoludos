package com.antigastos.boludos.domain

/**
 * Niveles ("rangos") según puntaje acumulado. Cada rango tiene un piso de puntos,
 * un nombre con onda argentina, un emoji y un color sugerido para la UI.
 */
data class ScoreRank(
    val key: String,
    val title: String,
    val tagline: String,
    val emoji: String,
    val minPoints: Long,
    /** Color ARGB para la insignia. */
    val colorArgb: Int,
)

object ScoreCatalog {

    /** Ordenado por minPoints ascendente. */
    val ranks: List<ScoreRank> = listOf(
        ScoreRank(
            key = "fundido",
            title = "Fundido total",
            tagline = "Empezamos en cero. Bancate las cargadas.",
            emoji = "💀",
            minPoints = -1_000_000L,
            colorArgb = 0xFF8E1A1A.toInt(),
        ),
        ScoreRank(
            key = "termo",
            title = "Termo financiero",
            tagline = "Todavía pagás todo en cuotas. Hay laburo por hacer.",
            emoji = "🧉",
            minPoints = 0L,
            colorArgb = 0xFFB58B00.toInt(),
        ),
        ScoreRank(
            key = "aprendiz",
            title = "Aprendiz de la guita",
            tagline = "Ya entendiste cómo se anota un gasto. Vamos viendo.",
            emoji = "🤓",
            minPoints = 100L,
            colorArgb = 0xFF1F4E8A.toInt(),
        ),
        ScoreRank(
            key = "rata",
            title = "Modo rata activado",
            tagline = "Ahorrás como si no hubiera mañana. Bien ahí.",
            emoji = "🐀",
            minPoints = 300L,
            colorArgb = 0xFF6A5ACD.toInt(),
        ),
        ScoreRank(
            key = "capo",
            title = "Capo de barrio",
            tagline = "Tenés todo controlado. Te vienen a pedir consejos.",
            emoji = "🧠",
            minPoints = 700L,
            colorArgb = 0xFF2E7D32.toInt(),
        ),
        ScoreRank(
            key = "buffeton",
            title = "Buffeton de Constitución",
            tagline = "Warren Buffett porteño. Hasta los plazos fijos te tienen miedo.",
            emoji = "🏛️",
            minPoints = 1500L,
            colorArgb = 0xFF1B5E20.toInt(),
        ),
        ScoreRank(
            key = "intocable",
            title = "Intocable",
            tagline = "Nivel mítico. Tu billetera es leyenda.",
            emoji = "👑",
            minPoints = 3000L,
            colorArgb = 0xFFB8860B.toInt(),
        ),
    )

    fun forPoints(points: Long): ScoreRank =
        ranks.lastOrNull { points >= it.minPoints } ?: ranks.first()

    /** El próximo rango después de [current] (null si ya está en el tope). */
    fun next(current: ScoreRank): ScoreRank? {
        val idx = ranks.indexOf(current)
        return if (idx < 0 || idx == ranks.lastIndex) null else ranks[idx + 1]
    }

    /** Progreso 0..1 hacia el próximo rango. 1f si ya estás en el tope. */
    fun progressToNext(points: Long): Float {
        val current = forPoints(points)
        val next = next(current) ?: return 1f
        val span = (next.minPoints - current.minPoints).coerceAtLeast(1L)
        val pos = (points - current.minPoints).coerceIn(0L, span)
        return pos.toFloat() / span.toFloat()
    }
}
