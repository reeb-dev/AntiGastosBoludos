package com.antigastos.boludos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Presupuesto mensual del usuario: cuánta plata entra, cuánta sale fija sí o sí.
 * `period` con formato "YYYY-MM" identifica el mes.
 */
@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val period: String,
    val salaryPesos: Long = 0L,
    val otherIncomePesos: Long = 0L,
    val fixedExpensesPesos: Long = 0L,
)
