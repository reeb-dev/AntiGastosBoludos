package com.antigastos.boludos.data

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate

/**
 * Cupo diario duro para llamadas de IA. Persistido en SharedPreferences así
 * sobrevive al cierre del proceso. Se resetea automáticamente al cambiar el día.
 *
 * Pensado para que **nunca** se pase del free tier de Gemini Developer API
 * (1500 req/día), sumando todos los pedidos del Home + Ruleta + Personas.
 */
class AiQuota(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Devuelve true si todavía hay cupo y descuenta uno. False si ya está al tope. */
    @Synchronized
    fun tryConsume(): Boolean {
        val today = LocalDate.now().toString()
        val storedDay = prefs.getString(KEY_DAY, null)
        val current = if (storedDay == today) prefs.getInt(KEY_COUNT, 0) else 0
        if (current >= DAILY_CAP) return false
        prefs.edit()
            .putString(KEY_DAY, today)
            .putInt(KEY_COUNT, current + 1)
            .apply()
        return true
    }

    /** Lo usa la UI para informar "te quedan X frases IA por hoy". */
    fun remaining(): Int {
        val today = LocalDate.now().toString()
        val storedDay = prefs.getString(KEY_DAY, null)
        val current = if (storedDay == today) prefs.getInt(KEY_COUNT, 0) else 0
        return (DAILY_CAP - current).coerceAtLeast(0)
    }

    fun resetForTesting() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS = "antigastos_ai_quota"
        private const val KEY_DAY = "day"
        private const val KEY_COUNT = "count"

        /**
         * Tope diario por dispositivo. 40 con caché agresiva nunca debería
         * llegarse en uso normal, y deja muchísimo margen vs el free tier
         * de 1500/día de Gemini Developer API.
         */
        const val DAILY_CAP = 40
    }
}
