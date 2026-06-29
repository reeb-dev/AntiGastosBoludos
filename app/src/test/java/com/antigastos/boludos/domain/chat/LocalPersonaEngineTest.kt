package com.antigastos.boludos.domain.chat

import com.antigastos.boludos.domain.CopyContext
import com.antigastos.boludos.domain.CopyMood
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalPersonaEngineTest {

    private val ctx = CopyContext(
        monthLabel = "junio",
        totalLucas = 120,
        topCategoryName = "Delivery",
        topCategorySlug = "delivery",
        topCategoryLucas = 45,
        deltaLucasVsPrevMonth = 20,
        pctOfMonthlyGoal = 0.92,
        daySpentLucas = 8,
        dayOfMonth = 25,
        daysInMonth = 30,
        expenseCount = 12,
        lastExpenseLucas = 15,
    )

    @Test
    fun detect_howAmI() {
        assertEquals(ChatIntent.HOW_AM_I, ChatIntent.detect("Decime cómo voy este mes"))
    }

    @Test
    fun detect_delivery() {
        assertEquals(ChatIntent.DELIVERY, ChatIntent.detect("Otra vez Rappi, qué hago?"))
    }

    @Test
    fun chatReply_usesContextForHowAmI() {
        val reply = LocalPersonaEngine.chatReply("tachero", ctx, "¿Cómo voy?")
        assertTrue(reply.contains("120"))
        assertTrue(reply.contains("lucas"))
    }

    @Test
    fun chatReply_differsByPersona() {
        val tachero = LocalPersonaEngine.chatReply("tachero", ctx, "hola")
        val cheta = LocalPersonaEngine.chatReply("cheta", ctx, "hola")
        assertFalse(tachero == cheta)
    }

    @Test
    fun moodFor_alarmWhenOverGoal() {
        val alarmCtx = ctx.copy(pctOfMonthlyGoal = 1.1)
        assertEquals(CopyMood.ALARM, LocalPersonaEngine.moodFor(alarmCtx))
    }

    @Test
    fun shortPhrase_includesPackContent() {
        val phrase = LocalPersonaEngine.shortPhrase("termo", ctx, CopyMood.BURN)
        assertTrue(phrase.isNotBlank())
    }
}
