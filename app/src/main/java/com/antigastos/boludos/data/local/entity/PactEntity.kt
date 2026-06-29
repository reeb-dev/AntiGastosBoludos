package com.antigastos.boludos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * "Modo pacto": el usuario se compromete a no gastar en una categoría
 * por X días. Si carga un gasto en esa categoría dentro del rango → roto.
 *
 * V1 social (sin backend):
 *  - [friendName] guarda el nombre del amigo con quien se hace la apuesta.
 *  - [stakeText] es lo que se juegan (ej. "una pizza", "$5000", "lavar el auto").
 *  - El "lado social" es local y se materializa por compartir un mensaje
 *    sugerido al amigo y/o el resultado al cierre.
 */
@Entity(tableName = "pacts")
data class PactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categorySlug: String,
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val brokenAt: Long? = null,
    val completedAt: Long? = null,
    val friendName: String? = null,
    val stakeText: String? = null,
)
