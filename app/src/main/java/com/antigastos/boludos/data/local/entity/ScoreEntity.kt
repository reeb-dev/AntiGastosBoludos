package com.antigastos.boludos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Singleton (id=1) con el puntaje acumulado del usuario y métricas para
 * competir consigo mismo / con otros (cuando se sincronice a Firestore).
 */
@Entity(tableName = "score")
data class ScoreEntity(
    @PrimaryKey val id: Long = 1L,
    /** Puntaje total acumulado en toda la vida de la app. Puede bajar (fallos). */
    val points: Long = 0L,
    /** Mes actual para tracking mensual: yyyy-MM. */
    val currentMonth: String = "",
    /** Puntos del mes en curso (se resetea al cambiar de mes). */
    val monthPoints: Long = 0L,
    /** Mejor mes histórico (puntos). */
    val bestMonthPoints: Long = 0L,
    /** Última fecha en la que se evaluó el día (yyyy-MM-dd). */
    val lastEvaluatedDay: String = "",
    /** Firma de la última evaluación para no doble-contar. */
    val lastEvaluatedSig: String = "",
    /** Total de fundazos (eventos negativos grandes). */
    val failsCount: Int = 0,
    /** Total de premios desbloqueados (eventos positivos relevantes). */
    val rewardsCount: Int = 0,
)
