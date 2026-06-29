package com.antigastos.boludos

import com.antigastos.boludos.domain.CopyContext
import com.antigastos.boludos.domain.CopyEngine
import com.antigastos.boludos.domain.CopyMood
import com.antigastos.boludos.domain.DonationCatalog
import com.antigastos.boludos.domain.PersonaCatalog
import com.antigastos.boludos.domain.PersonaCopyPacks
import com.antigastos.boludos.domain.chat.ChatIntent
import com.antigastos.boludos.domain.chat.LocalPersonaEngine
import com.antigastos.boludos.domain.chat.PersonaQuickSuggestions
import com.antigastos.boludos.ui.navigation.Routes
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Smoke tests de producción: valida invariantes de dominio y rutas sin dispositivo.
 */
class ProductionSmokeTest {

    private val sampleCtx = CopyContext(
        monthLabel = "junio",
        totalLucas = 85,
        topCategoryName = "Delivery",
        topCategorySlug = "delivery",
        topCategoryLucas = 40,
        deltaLucasVsPrevMonth = 10,
        pctOfMonthlyGoal = 0.88,
        daySpentLucas = 12,
        dayOfMonth = 15,
        daysInMonth = 30,
        expenseCount = 9,
        lastExpenseSlug = "delivery",
        lastExpenseLucas = 8,
    )

    @Test
    fun allPersonas_haveCopyPacksForEveryMood() {
        for (persona in PersonaCatalog.all) {
            for (mood in CopyMood.entries) {
                val pool = PersonaCopyPacks.pool(persona.key, mood, sampleCtx)
                assertTrue(
                    "Persona ${persona.key} sin frases para $mood",
                    pool.isNotEmpty(),
                )
            }
        }
    }

    @Test
    fun allPersonas_chatReplyNonBlank() {
        for (persona in PersonaCatalog.all) {
            val reply = LocalPersonaEngine.chatReply(
                personaKey = persona.key,
                ctx = sampleCtx,
                userMessage = "¿Cómo voy este mes?",
            )
            assertTrue("Chat vacío para ${persona.key}", reply.isNotBlank())
        }
    }

    @Test
    fun allPersonas_shortPhraseNonBlank() {
        for (persona in PersonaCatalog.all) {
            val phrase = LocalPersonaEngine.shortPhrase(persona.key, sampleCtx, CopyMood.NEUTRAL)
            assertTrue("Frase vacía para ${persona.key}", phrase.isNotBlank())
        }
    }

    @Test
    fun quickSuggestions_allIntentsRecognized() {
        val chips = PersonaQuickSuggestions.build(sampleCtx, CopyMood.BURN)
        assertTrue(chips.isNotEmpty())
        val actionable = chips.count { ChatIntent.detect(it.message) != ChatIntent.DEFAULT }
        assertTrue("Pocos chips con intención clara: $actionable/${chips.size}", actionable >= chips.size / 2)
        chips.forEach { assertTrue(it.message.isNotBlank()) }
    }

    @Test
    fun copyEngine_producesTextForAllMoods() {
        for (mood in CopyMood.entries) {
            val ctx = sampleCtx.copy(
                totalLucas = if (mood == CopyMood.CHEER) 20 else 200,
                pctOfMonthlyGoal = when (mood) {
                    CopyMood.ALARM -> 1.2
                    else -> 0.9
                },
            )
            val choice = CopyEngine.pickWithPersona(ctx, emptyMap(), "termo")
            assertTrue(choice.text.isNotBlank())
        }
    }

    @Test
    fun donationCatalog_hasTiers() {
        assertTrue(DonationCatalog.tiers.isNotEmpty())
    }

    @Test
    fun routes_constantsAreNonBlank() {
        listOf(
            Routes.HOME, Routes.LIST, Routes.STATS, Routes.GOALS,
            Routes.SETTINGS, Routes.DONATE, Routes.RULETA, Routes.LOTERIA,
            Routes.PERSONAS, Routes.PACT, Routes.CATEGORIES,
        ).forEach { assertTrue(it.isNotBlank()) }
    }

    @Test
    fun chatIntent_coversCommonUserPhrases() {
        val cases = mapOf(
            "¿cómo voy?" to ChatIntent.HOW_AM_I,
            "delivery otra vez" to ChatIntent.DELIVERY,
            "pasé la meta" to ChatIntent.META,
            "consejo por favor" to ChatIntent.ADVICE,
            "analizame" to ChatIntent.ANALYZE,
            "hola" to ChatIntent.GREETING,
            "gracias capo" to ChatIntent.THANKS,
        )
        cases.forEach { (msg, expected) ->
            assertTrue(
                "Esperaba $expected para '$msg', got ${ChatIntent.detect(msg)}",
                ChatIntent.detect(msg) == expected,
            )
        }
    }

    @Test
    fun localEngine_personasSoundDifferent() {
        val keys = listOf("termo", "tachero", "gato", "cheta")
        val replies = keys.map {
            LocalPersonaEngine.chatReply(it, sampleCtx, "hola")
        }
        assertTrue(replies.distinct().size >= 3)
    }
}
