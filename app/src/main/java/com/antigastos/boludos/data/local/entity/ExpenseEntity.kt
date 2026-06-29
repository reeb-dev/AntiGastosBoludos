package com.antigastos.boludos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("categoryId"),
        Index("occurredAt"),
    ]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Monto entero en pesos argentinos (sin centavos). */
    val amountPesos: Long,
    val categoryId: Long,
    val note: String?,
    val occurredAt: Long,
    /** Id estable para Firestore; vacío hasta el primer push. */
    val cloudId: String = "",
    /** Para resolver conflictos al sincronizar. */
    val updatedAtMillis: Long = 0L,
    /**
     * Modo pareja v1: si no es null, este gasto es "compartido" con la
     * pareja del usuario y este número (0-100) es el porcentaje que
     * carga el usuario actual. La diferencia (100 - x) es lo que la
     * pareja le debe sobre este gasto.
     *
     * null  = gasto solo del usuario (default).
     * 50    = mitad y mitad.
     */
    val sharedSplitPercent: Int? = null,
)
