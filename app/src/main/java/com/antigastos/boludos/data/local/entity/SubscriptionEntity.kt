package com.antigastos.boludos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Suscripción / gasto fijo recurrente (Netflix, alquiler, internet…) o cuota
 * en N cuotas (tele, viaje, electro).
 *
 * - `dayOfMonth` 1..28 para que valga cualquier mes.
 * - `lastSeededPeriod` guarda el último "YYYY-MM" en el que ya se generó el gasto.
 * - `installmentsTotal`: si es `null` = suscripción infinita (legacy). Si es N
 *   = cuotas; cuando `installmentsPaid` llega a N el seeder marca `active=false`.
 */
@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amountPesos: Long,
    val categoryId: Long,
    val dayOfMonth: Int = 1,
    val active: Boolean = true,
    val lastSeededPeriod: String? = null,
    val installmentsTotal: Int? = null,
    val installmentsPaid: Int = 0,
)
