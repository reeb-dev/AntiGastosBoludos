package com.antigastos.boludos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * [categoryId] == 0L representa meta global del mes.
 * Otras filas: tope por categoría.
 */
@Entity(
    tableName = "goals",
    indices = [Index(value = ["period", "categoryId"], unique = true)]
)
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val period: String,
    val limitPesos: Long,
)
