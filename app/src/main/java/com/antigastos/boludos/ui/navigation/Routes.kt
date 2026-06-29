package com.antigastos.boludos.ui.navigation

object Routes {
    const val HOME = "home"
    const val ADD = "add"
    const val EDIT = "edit/{expenseId}"

    fun edit(expenseId: Long): String = "edit/$expenseId"

    const val LIST = "list"
    const val STATS = "stats"
    const val GOALS = "goals"
    const val BOLSILLO = "bolsillo"
    const val SETTINGS = "settings"
    const val PRIVACY_POLICY = "privacy_policy"
    const val SUBSCRIPTIONS = "subscriptions"
    const val ACHIEVEMENTS = "achievements"
    const val PACT = "pact"
    const val CATEGORIES = "categories"
    const val RULETA = "ruleta"
    const val PERSONAS = "personas"
    const val DONATE = "donate"
    const val RECAP = "recap"
    const val PERSONA_QUIZ = "persona_quiz"
    const val LOTERIA = "loteria"
    const val PERSONA_CHAT = "persona_chat/{key}"
    const val CRUSH_RESOLVE = "crush/{crushId}"

    /** Extra en Intent: navegar a una ruta ([HOME], [ADD], etc.). */
    const val EXTRA_OPEN_ROUTE = "com.antigastos.OPEN_ROUTE"

    fun personaChat(key: String): String = "persona_chat/$key"

    fun crushResolve(crushId: Long): String = "crush/$crushId"

    val mainBarRoutes: Set<String> = setOf(HOME, LIST, BOLSILLO, RULETA, PERSONAS)
}
