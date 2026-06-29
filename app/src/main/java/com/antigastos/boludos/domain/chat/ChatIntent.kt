package com.antigastos.boludos.domain.chat

/**
 * Intención inferida del mensaje del usuario en el chat offline.
 * No es NLP: keywords rioplatenses + heurísticas simples.
 */
enum class ChatIntent {
    HOW_AM_I,
    DELIVERY,
    META,
    ADVICE,
    CATEGORY,
    FIN_MONTH,
    ANALYZE,
    GREETING,
    THANKS,
    COMPLAINT,
    DEFAULT,
    ;

    companion object {
        fun detect(message: String): ChatIntent {
            val m = message.lowercase()
                .replace('á', 'a').replace('é', 'e').replace('í', 'i')
                .replace('ó', 'o').replace('ú', 'u')

            if (matchesAny(m, "hola", "buen dia", "buenas", "que onda", "epa", "che")) {
                return GREETING
            }
            if (matchesAny(m, "gracias", "te banco", "genial", "joya", "crack", "capo")) {
                return THANKS
            }
            if (matchesAny(
                    m,
                    "como voy",
                    "como ando",
                    "como estoy",
                    "resumen",
                    "cuanto gaste",
                    "cuanto llevo",
                    "estado del mes",
                    "numeros",
                )
            ) {
                return HOW_AM_I
            }
            if (matchesAny(
                    m,
                    "delivery",
                    "rappi",
                    "pedidos ya",
                    "uber eats",
                    "pedi",
                    "pedir comida",
                    "sushi",
                    "empanada",
                )
            ) {
                return DELIVERY
            }
            if (matchesAny(m, "meta", "objetivo", "presupuesto", "limite", "tope", "pasé la meta", "pase la meta")) {
                return META
            }
            if (matchesAny(m, "fin de mes", "llegar al 30", "ultimo dia", "cierre de mes", "hasta fin de mes")) {
                return FIN_MONTH
            }
            if (matchesAny(
                    m,
                    "categoria",
                    "en que me fundi",
                    "donde gasto",
                    "patron",
                    "analiza",
                    "analizame",
                    "psicologo",
                    "que ves",
                )
            ) {
                return if (matchesAny(m, "analiza", "patron", "psicologo", "que ves")) ANALYZE else CATEGORY
            }
            if (matchesAny(
                    m,
                    "consejo",
                    "tip",
                    "que hago",
                    "ayuda",
                    "sugerencia",
                    "truco",
                    "como ahorro",
                    "como ahorrar",
                )
            ) {
                return ADVICE
            }
            if (matchesAny(m, "no puedo", "estoy fundido", "me quede sin", "hasta las manos", "me mato", "me fundi")) {
                return COMPLAINT
            }
            return DEFAULT
        }

        private fun matchesAny(text: String, vararg needles: String): Boolean =
            needles.any { text.contains(it) }
    }
}
