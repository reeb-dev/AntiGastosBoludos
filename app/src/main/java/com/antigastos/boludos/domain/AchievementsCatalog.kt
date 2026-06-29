package com.antigastos.boludos.domain

data class AchievementMeta(
    val key: String,
    val title: String,
    val description: String,
    val emoji: String,
)

object AchievementsCatalog {

    const val FIRST_EXPENSE = "first_expense"
    const val FIFTY_EXPENSES = "fifty_expenses"
    const val STREAK_3 = "streak_3"
    const val STREAK_7 = "streak_7"
    const val STREAK_14 = "streak_14"
    const val STREAK_30 = "streak_30"
    const val MONTH_GREEN = "month_green"
    const val PACT_DONE = "pact_done"
    const val SUBS_LOADED = "subs_loaded"
    const val USD_TRACKER = "usd_tracker"

    // === Logros absurdos ===
    const val SOBREVIVISTE_2_LUCAS = "sobreviviste_2_lucas"
    const val NO_DELIVERY_3 = "no_delivery_3"
    const val NO_MELI = "no_meli"
    const val TOCASTE_PASTO = "tocaste_pasto"
    const val DORMISTE_TEMPRANO = "dormiste_temprano"
    const val CACHO_VOZ = "cacho_voz"
    const val RULETA_VICIOSO = "ruleta_vicioso"
    const val PERSONA_TODOS = "persona_todos"

    val all: List<AchievementMeta> = listOf(
        AchievementMeta(FIRST_EXPENSE, "Primer gasto", "Cargaste tu primer gasto. Bienvenido al club.", "🎉"),
        AchievementMeta(FIFTY_EXPENSES, "Tracker pro", "50 gastos cargados. Sos el contador de tus mangos.", "📊"),
        AchievementMeta(STREAK_3, "3 días limpios", "Tres días sin gastar un mango. Bien ahí.", "🔥"),
        AchievementMeta(STREAK_7, "Semana modo rata", "Una semana entera sin patinar la guita.", "🐀"),
        AchievementMeta(STREAK_14, "14 días monje", "Dos semanas sin tocar la billetera. Inimputable.", "🧘"),
        AchievementMeta(STREAK_30, "Un mes entero", "30 días sin gastar. Te cambió la cara.", "🏆"),
        AchievementMeta(MONTH_GREEN, "Mes en verde", "Cerraste el mes sin romper la meta global.", "✅"),
        AchievementMeta(PACT_DONE, "Pacto cumplido", "Bancaste un pacto de no gastar. Palabra de capo.", "🤝"),
        AchievementMeta(SUBS_LOADED, "Suscripciones al día", "Cargaste tus gastos fijos en la app.", "📅"),
        AchievementMeta(USD_TRACKER, "Verde verde", "Configuraste cotización del dólar.", "💵"),
        AchievementMeta(SOBREVIVISTE_2_LUCAS, "Sobreviviste con 2 lucas", "Te quedaste con menos de 2 lucas pero la remaste.", "🆘"),
        AchievementMeta(NO_DELIVERY_3, "Sin delivery 3 días", "Tres días sin pedir Rappi/PedidosYa. Cocinaste.", "🍳"),
        AchievementMeta(NO_MELI, "Sin Mercado Libre", "Una semana sin abrir Mercado Libre. Reza por ti.", "📦"),
        AchievementMeta(TOCASTE_PASTO, "Tocaste pasto", "Saliste a la calle. Bien parado.", "🌱"),
        AchievementMeta(DORMISTE_TEMPRANO, "Dormiste antes de las 2 AM", "No abriste la app entre las 2 y las 6 AM esta semana.", "😴"),
        AchievementMeta(CACHO_VOZ, "Cacho voz", "Activaste TTS y la app empezó a hablarte. Bienvenido al manicomio.", "🔊"),
        AchievementMeta(RULETA_VICIOSO, "Ludópata simulado", "Tiraste la ruleta 7 días seguidos.", "🎰"),
        AchievementMeta(PERSONA_TODOS, "Multipersonalidad", "Probaste todos los personajes.", "🎭"),
    )
}
