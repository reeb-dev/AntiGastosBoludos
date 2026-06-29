package com.antigastos.boludos.work

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Programa la notificación de "regalo del día" a una **hora aleatoria**
 * entre 11:00 y 22:59 cada día (re-scheduling al terminar el worker).
 */
object DailyGiftScheduler {

    private const val UNIQUE = "antigastos_daily_gift_ot"

    fun scheduleNext(context: Context) {
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        val hour = Random.nextInt(11, 23)
        val minute = Random.nextInt(0, 60)
        var target = now.toLocalDate().atTime(LocalTime.of(hour, minute))
        if (!target.isAfter(now)) {
            target = target.plusDays(1)
        }
        val delayMs = Duration.between(now, target).toMillis().coerceAtLeast(60_000L)

        val req = OneTimeWorkRequestBuilder<DailyGiftWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE,
            ExistingWorkPolicy.REPLACE,
            req,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE)
    }
}
