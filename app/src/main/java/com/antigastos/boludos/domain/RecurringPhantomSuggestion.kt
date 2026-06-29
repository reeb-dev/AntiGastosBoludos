package com.antigastos.boludos.domain

/**
 * Patrón detectado: el usuario cargó a mano varias veces un gasto parecido
 * en meses distintos, pero no tiene una suscripción equivalente.
 */
data class RecurringPhantomSuggestion(
    /** Clave estable para “no volver a mostrar” hasta que cambie el patrón. */
    val suggestionKey: String,
    val categoryId: Long,
    val categoryName: String,
    val suggestedName: String,
    val typicalAmountPesos: Long,
    val distinctMonths: Int,
    val hitCount: Int,
    val suggestedDayOfMonth: Int,
)
