package com.antigastos.boludos.domain

/** Niveles sugeridos de cafecito (ARS) para transferencia por CBU. */
data class DonationTier(
    val key: String,
    val title: String,
    val amountPesos: Long,
    val emoji: String,
    val blurb: String,
)

object DonationCatalog {

    val tiers: List<DonationTier> = listOf(
        DonationTier(
            key = "chico",
            title = "Café chico",
            amountPesos = 3_500L,
            emoji = "☕",
            blurb = "Un gesto que ayuda mucho. ¡Gracias!",
        ),
        DonationTier(
            key = "mediano",
            title = "Café mediano",
            amountPesos = 7_000L,
            emoji = "☕☕",
            blurb = "Doble cafecito, doble cariño.",
        ),
        DonationTier(
            key = "grande",
            title = "Café grande",
            amountPesos = 15_000L,
            emoji = "🧉",
            blurb = "Sos un/a capo/a del mate y del apoyo.",
        ),
        DonationTier(
            key = "super",
            title = "Súper fan",
            amountPesos = 25_000L,
            emoji = "👑",
            blurb = "Leyenda. Gracias de corazón.",
        ),
    )

    fun tierForAmount(amountPesos: Long): DonationTier? =
        tiers.find { it.amountPesos == amountPesos }
}
