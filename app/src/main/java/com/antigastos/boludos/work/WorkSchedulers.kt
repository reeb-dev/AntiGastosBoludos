package com.antigastos.boludos.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.antigastos.boludos.data.local.entity.SettingsEntity
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

object WorkSchedulers {

    private const val DAILY_REMINDER = "antigastos_daily_reminder"
    private const val STREAK_EVENING = "antigastos_streak_evening"
    private const val MONTHLY_BUDGET_CHECK = "antigastos_monthly_check"
    private const val SUBS_SEEDER = "antigastos_subs_seeder"

    fun applyFromSettings(context: Context, settings: SettingsEntity) {
        val wm = WorkManager.getInstance(context)
        // Migración: backup mensual y regalo diario legacy.
        wm.cancelUniqueWork("antigastos_monthly_backup")
        wm.cancelUniqueWork("antigastos_daily_gift")
        if (settings.dailyReminderEnabled) {
            scheduleDailyReminder(wm, settings.dailyReminderHour)
        } else {
            wm.cancelUniqueWork(DAILY_REMINDER)
        }
        if (settings.streakNudgeEnabled) {
            scheduleStreakEvening(wm)
            DailyGiftScheduler.scheduleNext(context)
        } else {
            wm.cancelUniqueWork(STREAK_EVENING)
            DailyGiftScheduler.cancel(context)
        }
        if (settings.budgetAlertsEnabled) {
            scheduleMonthlyCheck(wm)
        } else {
            wm.cancelUniqueWork(MONTHLY_BUDGET_CHECK)
        }
        scheduleSubsSeeder(wm)
    }

    private fun scheduleDailyReminder(wm: WorkManager, hour: Int) {
        val req = PeriodicWorkRequestBuilder<DailyReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelayUntil(hour, 0), TimeUnit.MILLISECONDS)
            .build()
        wm.enqueueUniquePeriodicWork(
            DAILY_REMINDER,
            ExistingPeriodicWorkPolicy.UPDATE,
            req,
        )
    }

    /** ~22:00 recordatorio de racha / cierre del día. */
    private fun scheduleStreakEvening(wm: WorkManager) {
        val req = PeriodicWorkRequestBuilder<StreakEveningWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelayUntil(22, 0), TimeUnit.MILLISECONDS)
            .build()
        wm.enqueueUniquePeriodicWork(
            STREAK_EVENING,
            ExistingPeriodicWorkPolicy.UPDATE,
            req,
        )
    }

    private fun scheduleMonthlyCheck(wm: WorkManager) {
        val req = PeriodicWorkRequestBuilder<BudgetCheckWorker>(24, TimeUnit.HOURS).build()
        wm.enqueueUniquePeriodicWork(
            MONTHLY_BUDGET_CHECK,
            ExistingPeriodicWorkPolicy.UPDATE,
            req,
        )
    }

    private fun scheduleSubsSeeder(wm: WorkManager) {
        val req = PeriodicWorkRequestBuilder<SubsSeederWorker>(1, TimeUnit.DAYS).build()
        wm.enqueueUniquePeriodicWork(
            SUBS_SEEDER,
            ExistingPeriodicWorkPolicy.UPDATE,
            req,
        )
    }

    private fun initialDelayUntil(hour: Int, minute: Int): Long {
        val now = LocalDateTime.now()
        var target = now.toLocalDate().atTime(LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59)))
        if (!target.isAfter(now)) target = target.plusDays(1)
        val zone = ZoneId.systemDefault()
        val ms = Duration.between(now.atZone(zone), target.atZone(zone)).toMillis()
        return ms.coerceAtLeast(60_000L)
    }
}
