package com.antigastos.boludos.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.domain.HolidayCalendar
import com.antigastos.boludos.domain.MoneyFormat
import com.antigastos.boludos.domain.WeeklyChallengeSeeder
import com.antigastos.boludos.notifications.Notifier
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class DailyReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? AntiGastosApplication ?: return Result.success()
        val settings = app.settingsRepository.getSnapshot()
        if (!settings.dailyReminderEnabled) return Result.success()
        if (!WorkerNotificationState.tryConsumeDailyReminder(applicationContext)) {
            return Result.success()
        }

        val persona = com.antigastos.boludos.domain.PersonaCatalog.byKey(settings.personaKey)
        val today = LocalDate.now()
        val ym = YearMonth.from(today)
        val home = runCatching { app.repository.observeHome(ym).first() }.getOrNull()
        val holiday = HolidayCalendar.forDate(today)
        val fri = today.dayOfWeek == DayOfWeek.FRIDAY
        val paydayHint = today.dayOfMonth in 1..5

        val title = buildString {
            append("${persona.emoji} Anotá los gastos")
            if (holiday != null) append(" · ${holiday.emoji}")
        }
        val body = buildString {
            append("${persona.displayName} te tira la posta: ")
            if (holiday != null) {
                append("${holiday.name}: ${holiday.tagline} ")
            }
            if (paydayHint) {
                append("Arranque de mes: mejor 5 minutos hoy que drama el día 30. ")
            }
            if (fri) {
                append("Viernes a la noche pica: cargá antes de que se te borre. ")
            }
            if (today.dayOfMonth in 25..30) {
                append("Se viene el cierre: mirá el recap del mes desde Inicio. ")
            }
            if (home != null && home.globalGoalPesos != null && home.globalProgress != null) {
                val pct = (home.globalProgress * 100).toInt().coerceIn(0, 150)
                append("Meta del mes: ~$pct% (${MoneyFormat.formatPesos(home.totalPesos)}). ")
            }
            append(home?.personalityMessage ?: "Cargá lo que gastaste hoy. Después no te quejes.")
        }

        Notifier.postReminder(applicationContext, title, body.trim())
        WeeklyChallengeSeeder.maybeSeed(app)
        return Result.success()
    }
}
