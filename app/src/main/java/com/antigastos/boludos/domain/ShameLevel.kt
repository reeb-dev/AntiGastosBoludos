package com.antigastos.boludos.domain

/**
 * Nivel de "vergüenza" que se muestra como badge en Home.
 * Va de "ciudadano ejemplar" a "rey del fiado".
 *
 * Calculado a partir de: % de meta, día del mes, plata en delivery, plata en boludeces.
 */
enum class ShameLevel(
    val emoji: String,
    val label: String,
    /** Puntaje numérico para barras de progreso (0..100). */
    val score: Int,
) {
    EJEMPLAR("😇", "Ciudadano ejemplar", 5),
    NORMAL("😐", "Mortal común", 25),
    DESASTRE("💸", "Desastre económico", 50),
    DELIVERY("🍕", "Gordo delivery", 70),
    LUDOPATA("🎰", "Ludópata financiero", 85),
    REY_FIADO("👑", "Rey del fiado", 100);

    companion object {
        fun from(
            pctOfMonthlyGoal: Double?,
            dayOfMonth: Int,
            lucasByCategory: Map<String, Int>,
            totalLucas: Int,
        ): ShameLevel {
            val pct = pctOfMonthlyGoal ?: 0.0
            val deliveryLucas = lucasByCategory["delivery"] ?: 0
            val boludecesLucas = lucasByCategory["boludeces"] ?: 0
            val salidasLucas = lucasByCategory["salidas"] ?: 0

            // Reyes del fiado: revientan meta y van por mucho más
            if (pct >= 1.3) return REY_FIADO
            // Ludópata: meta superada con boludeces y salidas pesadas
            if (pct >= 1.1 && (boludecesLucas + salidasLucas) >= 60) return LUDOPATA
            // Gordo delivery: delivery domina la torta
            if (deliveryLucas >= 40 && deliveryLucas >= totalLucas * 0.4) return DELIVERY
            // Desastre: ya gastó la meta y todavía falta mes
            if (pct >= 1.0 || (pct >= 0.85 && dayOfMonth <= 20)) return DESASTRE
            if (pct >= 0.6) return NORMAL
            return EJEMPLAR
        }
    }
}
