package com.antigastos.boludos.domain

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CopyEngineTest {

    private fun ctx(
        total: Int = 50,
        topSlug: String? = null,
        topLucas: Int = 0,
        delta: Int? = null,
        pct: Double? = null,
    ) = CopyContext(
        monthLabel = "mayo",
        totalLucas = total,
        topCategoryName = topSlug,
        topCategorySlug = topSlug,
        topCategoryLucas = topLucas,
        deltaLucasVsPrevMonth = delta,
        pctOfMonthlyGoal = pct,
    )

    @Test
    fun zeroSpend_pickedAlone() {
        val choice = CopyEngine.pickWeighted(ctx(total = 0), emptyMap(), Random(1))
        assertEquals("zero_spend", choice.templateId)
    }

    @Test
    fun heavilyVoted_winsRunoff() {
        val c = ctx(total = 120, topSlug = "delivery", topLucas = 40, pct = 1.2)
        // Peso "absurdamente alto" para que `delivery_high` domine sin importar
        // cuántos otros templates haya matcheando ese contexto. Antes usábamos
        // 1024.0 y el test se volvió frágil al ir creciendo el catálogo.
        val weights = mapOf("delivery_high" to 1_000_000.0)
        val choice = CopyEngine.pickWeighted(c, weights, Random(7))
        assertEquals("delivery_high", choice.templateId)
        assertTrue(choice.text.contains("delivery", ignoreCase = true))
    }

    @Test
    fun weightForVotes_isExponentialAndClipped() {
        assertEquals(1.0, CopyEngine.weightForVotes(0, 0), 0.0001)
        assertEquals(2.0, CopyEngine.weightForVotes(1, 0), 0.0001)
        assertEquals(0.5, CopyEngine.weightForVotes(0, 1), 0.0001)
        assertEquals(32.0, CopyEngine.weightForVotes(10, 0), 0.0001)
        assertEquals(0.125, CopyEngine.weightForVotes(0, 10), 0.0001)
    }
}
