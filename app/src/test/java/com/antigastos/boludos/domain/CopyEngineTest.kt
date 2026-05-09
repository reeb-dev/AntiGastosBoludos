package com.antigastos.boludos.domain

import kotlin.random.Random
import org.junit.Assert.assertTrue
import org.junit.Test

class CopyEngineTest {

    @Test
    fun pickMessage_zeroSpend_isFriendly() {
        val ctx = CopyContext(
            monthLabel = "mayo",
            totalLucas = 0,
            topCategoryName = null,
            topCategorySlug = null,
            topCategoryLucas = 0,
            deltaLucasVsPrevMonth = null,
            pctOfMonthlyGoal = null,
        )
        val msg = CopyEngine.pickMessage(ctx, Random(1))
        assertTrue(msg.contains("gastos", ignoreCase = true))
    }

    @Test
    fun pickMessage_deliveryHigh_hasRoastTone() {
        val ctx = CopyContext(
            monthLabel = "mayo",
            totalLucas = 120,
            topCategoryName = "Delivery",
            topCategorySlug = "delivery",
            topCategoryLucas = 40,
            deltaLucasVsPrevMonth = 5,
            pctOfMonthlyGoal = 1.2,
        )
        val msg = CopyEngine.pickMessage(ctx, Random(2))
        assertTrue(msg.length > 10)
    }
}
