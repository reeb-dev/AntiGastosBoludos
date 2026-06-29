package com.antigastos.boludos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Gasto pospuesto (“antojo”) que el usuario mandó a la heladera 24 h.
 * No es un [ExpenseEntity] hasta que confirma desde la notificación o la pantalla.
 */
@Entity(tableName = "pending_crushes")
data class PendingCrushEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountPesos: Long,
    val categoryId: Long,
    val note: String? = null,
    val createdAtMillis: Long,
    /** Momento en que debería avisar (normalmente +24 h). */
    val remindAtMillis: Long,
)
