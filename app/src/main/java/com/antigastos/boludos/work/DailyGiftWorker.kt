package com.antigastos.boludos.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.domain.PersonaCatalog
import com.antigastos.boludos.notifications.Notifier
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * ~21:00 — CTA para abrir la lotería diaria (sticker / frase / escudo).
 */
class DailyGiftWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? AntiGastosApplication ?: return Result.success()
        val settings = app.settingsRepository.getSnapshot()
        if (!settings.streakNudgeEnabled) return Result.success()
        if (!WorkerNotificationState.tryConsumeDailyGift(applicationContext)) {
            return Result.success()
        }

        val today = LocalDate.now()
        val persona = PersonaCatalog.byKey(settings.personaKey)
        val fri = today.dayOfWeek == DayOfWeek.FRIDAY
        val postPay = today.dayOfMonth == 2

        val title = "${persona.emoji} Regalo del día"
        val body = buildString {
            append(persona.displayName)
            append(" te dejó un regalo: abrí la app y tocá «Regalo del día» en Inicio. ")
            if (fri) append("Es viernes a la noche: hora peligrosa de delivery — anotá antes de mandarte una de más. ")
            if (postPay) append("Si cobraste ayer, moderá el impulso de gastar todo hoy. ")
        }

        Notifier.postDailyGift(applicationContext, title, body.trim())
        DailyGiftScheduler.scheduleNext(applicationContext)
        return Result.success()
    }
}
