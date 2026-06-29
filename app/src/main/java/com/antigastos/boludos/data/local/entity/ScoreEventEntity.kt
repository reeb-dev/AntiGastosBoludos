package com.antigastos.boludos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Historial de eventos de score: premios (REWARD) o fundazos (FAIL).
 * Lo usamos para mostrar al usuario qué le sumó/restó puntos.
 */
@Entity(
    tableName = "score_events",
    indices = [Index("createdAt")],
)
data class ScoreEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** REWARD o FAIL (string para evitar TypeConverters). */
    val kind: String,
    /** Etiqueta breve, sin emoji. Ej: "Día limpio", "Te pasaste la meta". */
    val label: String,
    /** Emoji para mostrar en la lista. */
    val emoji: String,
    /** Delta aplicado al puntaje. Negativo si fue fundazo. */
    val delta: Long,
    /** Fecha (yyyy-MM-dd) a la que corresponde el evento. */
    val day: String,
    /** Timestamp del registro. */
    val createdAt: Long = System.currentTimeMillis(),
)
