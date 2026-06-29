package com.antigastos.boludos.domain.chat

import com.antigastos.boludos.domain.CopyContext
import com.antigastos.boludos.domain.CopyMood
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonaQuickSuggestionsTest {

    private val ctx = CopyContext(
        monthLabel = "junio",
        totalLucas = 90,
        topCategoryName = "Delivery",
        topCategorySlug = "delivery",
        topCategoryLucas = 40,
        deltaLucasVsPrevMonth = null,
        pctOfMonthlyGoal = 0.95,
        dayOfMonth = 28,
        daysInMonth = 30,
        expenseCount = 8,
        lastExpenseSlug = "delivery",
    )

    @Test
    fun build_includesDeliveryWhenTopCategory() {
        val chips = PersonaQuickSuggestions.build(ctx, CopyMood.BURN)
        assertTrue(chips.any { it.label == "Delivery" })
    }

    @Test
    fun build_messagesMatchChatIntent() {
        val chip = PersonaQuickSuggestions.build(ctx, CopyMood.NEUTRAL)
            .first { it.label == "¿Cómo voy?" }
        assertTrue(ChatIntent.detect(chip.message) == ChatIntent.HOW_AM_I)
    }
}
