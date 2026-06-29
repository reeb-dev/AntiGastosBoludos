package com.antigastos.boludos.domain

/**
 * Categorías donde tiene sentido ofrecer la “heladera 24 h” (antojos).
 */
object CrushCooldownRules {

    val IMPULSE_SLUGS: Set<String> = setOf(
        "delivery",
        "salidas",
        "boludeces",
    )

    fun qualifies(
        categorySlug: String,
        amountPesos: Long,
        minPesos: Long,
        featureEnabled: Boolean,
        isNewExpense: Boolean,
    ): Boolean {
        if (!featureEnabled || !isNewExpense) return false
        if (amountPesos < minPesos.coerceAtLeast(0L)) return false
        return categorySlug in IMPULSE_SLUGS
    }
}
