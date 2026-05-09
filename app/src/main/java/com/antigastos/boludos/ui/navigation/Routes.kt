package com.antigastos.boludos.ui.navigation

object Routes {
    const val HOME = "home"
    const val ADD = "add"
    const val EDIT = "edit/{expenseId}"

    fun edit(expenseId: Long): String = "edit/$expenseId"

    const val LIST = "list"
    const val STATS = "stats"
    const val GOALS = "goals"
}
