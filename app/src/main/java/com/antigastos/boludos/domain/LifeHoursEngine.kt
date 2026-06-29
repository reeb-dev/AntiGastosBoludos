package com.antigastos.boludos.domain

import kotlin.math.roundToLong

/**
 * Traduce pesos a “tiempo de laburo” con una tarifa horaria derivada del sueldo.
 */
object LifeHoursEngine {

    /** Horas de trabajo en un mes “promedio” a partir de horas semanales. */
    fun hoursPerMonth(weeklyHours: Int): Double {
        val w = weeklyHours.coerceIn(1, 80)
        return w * 52.0 / 12.0
    }

    /** Pesos por hora, o null si falta dato. */
    fun hourlyRatePesos(monthlyNetPesos: Long, weeklyHours: Int): Double? {
        if (monthlyNetPesos <= 0L) return null
        val hm = hoursPerMonth(weeklyHours)
        if (hm <= 0.0) return null
        return monthlyNetPesos.toDouble() / hm
    }

    /**
     * Texto corto para UI, ej. "≈ 2 h 5 min" o "≈ 40 min".
     */
    fun formatLifeHours(pesos: Long, hourlyRatePesos: Double): String {
        if (pesos <= 0L || hourlyRatePesos <= 0.0) return "≈ 0 min"
        val hoursExact = pesos.toDouble() / hourlyRatePesos
        val totalMinutes = (hoursExact * 60.0).roundToLong().coerceAtLeast(1L)
        val h = totalMinutes / 60L
        val m = totalMinutes % 60L
        return when {
            h == 0L -> "≈ ${m} min"
            m == 0L -> "≈ ${h} h"
            else -> "≈ ${h} h ${m} min"
        }
    }

    fun formatHourlyRateLabel(hourlyRatePesos: Double): String =
        "~${MoneyFormat.formatPesos(hourlyRatePesos.toLong().coerceAtLeast(0L))}/h"
}
