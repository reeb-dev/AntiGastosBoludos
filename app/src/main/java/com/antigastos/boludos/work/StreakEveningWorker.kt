package com.antigastos.boludos.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.domain.HolidayCalendar
import com.antigastos.boludos.domain.PersonaCatalog
import com.antigastos.boludos.notifications.Notifier
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * Gancho tipo “no pierdas la racha”: ~22 h, si el usuario habilitó el aviso.
 */
class StreakEveningWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? AntiGastosApplication ?: return Result.success()
        val settings = app.settingsRepository.getSnapshot()
        if (!settings.streakNudgeEnabled) return Result.success()
        if (!WorkerNotificationState.tryConsumeStreakNudge(applicationContext)) {
            return Result.success()
        }

        val persona = PersonaCatalog.byKey(settings.personaKey)

        val today = LocalDate.now()
        val ym = YearMonth.from(today)
        val home = runCatching { app.repository.observeHome(ym).first() }.getOrNull()
            ?: return Result.success()

        val holiday = HolidayCalendar.forDate(today)
        val fri = today.dayOfWeek == DayOfWeek.FRIDAY
        val title = buildString {
            append("${persona.emoji} ")
            when {
                home.streakDaysNoSpend >= 3 -> append("Racha fuerte")
                home.streakDaysNoSpend > 0 -> append("Racha del día")
                else -> append("Cerrá el día")
            }
            if (holiday != null) append(" · ${holiday.emoji}")
        }
        val body = buildString {
            append("${persona.displayName} te recuerda: ")
            if (holiday != null) append("${holiday.emoji} ${holiday.name}. ")
            if (fri) append("Es viernes: anotá antes que se te vaya la mano. ")
            if (home.streakDaysNoSpend >= 2 && home.daySpentPesos == 0L) {
                append("Perder ${home.streakDaysNoSpend} días de racha por no cargar 30 segundos es un robo. ")
            }
            when {
                home.streakDaysNoSpend >= 2 && home.daySpentPesos == 0L -> {
                    append("Llevás ${home.streakDaysNoSpend} días sin gastar hoy. ")
                    append("Si no cerrás el día, mañana la racha vuelve a cero: no regales lo que ya construiste. ")
                }
                home.daySpentPesos == 0L && home.expenseCount > 0 ->
                    append("¿Hoy no gastaste nada? Sumá el cierre (aunque sea un mate) para no romper el hábito.")
                else ->
                    append("Revisá el resumen y cargá lo que falte. Mañana te lo agradecés.")
            }
        }

        Notifier.postStreakNudge(applicationContext, title, body.trim())
        return Result.success()
    }
}
