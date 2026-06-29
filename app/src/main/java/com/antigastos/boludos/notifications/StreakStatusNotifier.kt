package com.antigastos.boludos.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.MainActivity
import com.antigastos.boludos.R
import com.antigastos.boludos.domain.MoneyFormat
import com.antigastos.boludos.domain.SpendingBudgetEngine
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.YearMonth

/**
 * Notificación **ongoing** con racha, gasto de hoy y atajos +5k/+10k pesos.
 * Opt-in vía [com.antigastos.boludos.data.local.entity.SettingsEntity.persistStreakNotification].
 */
object StreakStatusNotifier {

    const val NOTIFICATION_ID = 4242

    suspend fun refresh(app: AntiGastosApplication) {
        val ctx = app.applicationContext
        if (!Notifier.hasPostNotificationsPermission(ctx)) {
            NotificationManagerCompat.from(ctx).cancel(NOTIFICATION_ID)
            return
        }
        val settings = app.settingsRepository.getSnapshot()
        if (!settings.persistStreakNotification) {
            NotificationManagerCompat.from(ctx).cancel(NOTIFICATION_ID)
            return
        }

        val ym = YearMonth.now()
        val home = runCatching { app.repository.observeHome(ym).first() }.getOrNull()
        if (home == null) {
            NotificationManagerCompat.from(ctx).cancel(NOTIFICATION_ID)
            return
        }

        val today = LocalDate.now()
        val budget = SpendingBudgetEngine.evaluate(home, today)
        val streak = home.streakDaysNoSpend
        val spent = home.daySpentPesos
        val allowance = budget.allowanceTodayPesos

        val title = buildString {
            append("🔥 $streak día${if (streak == 1) "" else "s"} sin gastar hoy")
            append(" · ")
            append(if (spent == 0L) "Hoy \$0" else MoneyFormat.formatPesos(spent))
        }
        val body = buildString {
            if (home.globalGoalPesos != null && home.globalGoalPesos > 0) {
                append("Tope sugerido hoy: ${MoneyFormat.formatPesos(allowance)}. ")
            }
            if (streak >= 2) {
                append("Si mañana arrancás en cero, perdés una racha que ya te costó días. ")
            }
            append("Tocá la app o usá los atajos para cargar rápido.")
        }

        val openPi = PendingIntent.getActivity(
            ctx,
            NOTIFICATION_ID,
            Intent(ctx, MainActivity::class.java).apply {
                data = Uri.parse(Notifier.DEEPLINK_HOME)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            piFlags(),
        )

        val add5 = QuickExpenseReceiver.pendingIntent(ctx, 5_000L, 4401)
        val add10 = QuickExpenseReceiver.pendingIntent(ctx, 10_000L, 4402)

        val noti = NotificationCompat.Builder(ctx, Notifier.CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_notification_trophy)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openPi)
            .addAction(0, "+5 mil", add5)
            .addAction(0, "+10 mil", add10)
            .build()

        runCatching {
            NotificationManagerCompat.from(ctx).notify(NOTIFICATION_ID, noti)
        }
    }

    private fun piFlags(): Int =
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
}
