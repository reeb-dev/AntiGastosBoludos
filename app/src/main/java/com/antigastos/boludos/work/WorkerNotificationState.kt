package com.antigastos.boludos.work

import android.content.Context
import java.time.LocalDate

/**
 * Evita spam si WorkManager dispara más de una vez el mismo día (reintentos,
 * desfases de hora, etc.).
 */
object WorkerNotificationState {

    private const val PREFS = "antigastos_worker_noti"

    fun tryConsumeDailyReminder(ctx: Context): Boolean =
        tryConsume(ctx, "daily_reminder", LocalDate.now())

    fun tryConsumeStreakNudge(ctx: Context): Boolean =
        tryConsume(ctx, "streak_nudge", LocalDate.now())

    fun tryConsumeDailyGift(ctx: Context): Boolean =
        tryConsume(ctx, "daily_gift", LocalDate.now())

    fun tryConsumeBudgetCheck(ctx: Context): Boolean =
        tryConsume(ctx, "budget_check", LocalDate.now())

    fun tryConsumeSubsSeed(ctx: Context): Boolean =
        tryConsume(ctx, "subs_seed", LocalDate.now())

    /** Para tests: limpia el estado en memoria. */
    fun clearAll(ctx: Context) {
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    private fun tryConsume(ctx: Context, kind: String, day: LocalDate): Boolean {
        val p = ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = computeKey(kind, day)
        val prev = p.getString(kind, null)
        if (prev == key) return false
        p.edit().putString(kind, key).apply()
        return true
    }

    /**
     * Núcleo testeable: dado el `prev` (lo último que se guardó para ese kind)
     * y el `day` actual, decide si la notificación se debe disparar y devuelve
     * la nueva clave a persistir.
     *
     * Esto permite testear desde JVM puro sin SharedPreferences ni Robolectric.
     */
    fun decideConsume(prev: String?, kind: String, day: LocalDate): Pair<Boolean, String> {
        val key = computeKey(kind, day)
        val shouldFire = prev != key
        return shouldFire to key
    }

    fun computeKey(kind: String, day: LocalDate): String = "${kind}_${day}"
}
