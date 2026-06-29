package com.antigastos.boludos.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.antigastos.boludos.MainActivity
import com.antigastos.boludos.R

/**
 * Push notifications mejoradas:
 *  - Múltiples canales (rewards, fails, alerts, social, reminders) para que
 *    el usuario los pueda silenciar individualmente.
 *  - Iconos blancos monocromos vectoriales (requeridos en Android 5+) +
 *    color del sistema tinted con `setColor`.
 *  - Deep links: cada noti abre la sección relevante (logros, top amigos,
 *    home) usando un Uri con scheme propio.
 *  - BigTextStyle por defecto + group keys para que se agrupen visualmente.
 */
object Notifier {

    // -------- Canales --------
    /** Avisos de gasto / suscripciones / fin de mes. */
    const val CHANNEL_GASTOS = "gastos"
    /** Premios y rachas. */
    const val CHANNEL_REWARDS = "rewards"
    /** Fundazos y excedidos de meta. */
    const val CHANNEL_FAILS = "fails"
    /** Notificaciones de movimientos de amigos / leaderboard. */
    const val CHANNEL_SOCIAL = "social"
    /** Recordatorios genéricos (workers, pactos, etc.). */
    const val CHANNEL_REMINDERS = "reminders"
    /** Notificación fija de racha (baja prioridad, ongoing). */
    const val CHANNEL_STATUS = "streak_status"

    // -------- Group keys --------
    private const val GROUP_REWARDS = "group_rewards"
    private const val GROUP_FAILS = "group_fails"
    private const val GROUP_SOCIAL = "group_social"

    private const val NOTI_EXPENSE_SAVED = 1010
    private const val NOTI_REWARDS_SUMMARY = 2000
    private const val NOTI_FAILS_SUMMARY = 2100
    private const val NOTI_SOCIAL_SUMMARY = 2200
    private var rollingId = 5000

    /** Crea todos los canales (idempotente). Llamar en Application.onCreate. */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return

        ensureChannel(
            mgr, CHANNEL_GASTOS, "Gastos & meta",
            "Cuando agregás un gasto, te queda poco margen, o el mes está apretado.",
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        ensureChannel(
            mgr, CHANNEL_REWARDS, "Premios",
            "Logros, rachas y subas de rango.",
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        ensureChannel(
            mgr, CHANNEL_FAILS, "Fundazos",
            "Cuando te fundís, pasás la meta o vas mal.",
            NotificationManager.IMPORTANCE_HIGH,
        )
        ensureChannel(
            mgr, CHANNEL_SOCIAL, "Social",
            "Avisos sociales de la app.",
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        ensureChannel(
            mgr, CHANNEL_REMINDERS, "Recordatorios",
            "Pactos, suscripciones que vencen, evaluación diaria.",
            NotificationManager.IMPORTANCE_LOW,
        )
        ensureChannel(
            mgr, CHANNEL_STATUS, "Racha en pantalla",
            "Resumen fijo de racha y gasto de hoy (podés apagarlo en Ajustes).",
            NotificationManager.IMPORTANCE_LOW,
        )
    }

    private fun ensureChannel(
        mgr: NotificationManager,
        id: String,
        name: String,
        desc: String,
        importance: Int,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val existing = mgr.getNotificationChannel(id)
        if (existing != null) return
        val ch = NotificationChannel(id, name, importance).apply {
            description = desc
            enableLights(true)
            enableVibration(importance >= NotificationManager.IMPORTANCE_DEFAULT)
            setShowBadge(true)
        }
        mgr.createNotificationChannel(ch)
    }

    // -------- API pública --------

    /** Recordatorio genérico (workers, alertas de meta, pactos). */
    fun postReminder(context: Context, title: String, body: String) {
        post(
            context,
            channelId = CHANNEL_GASTOS,
            id = nextId(),
            title = title,
            body = body,
            iconRes = R.drawable.ic_notification_warning,
            colorRes = R.color.notification_warning,
            deeplink = DEEPLINK_HOME,
        )
    }

    /** Aviso nocturno de racha / cierre (22 h). */
    fun postStreakNudge(context: Context, title: String, body: String) {
        post(
            context,
            channelId = CHANNEL_REMINDERS,
            id = nextId(),
            title = title,
            body = body,
            iconRes = R.drawable.ic_notification_trophy,
            colorRes = R.color.notification_reward,
            deeplink = DEEPLINK_HOME,
        )
    }

    /** CTA lotería diaria (~21 h). */
    fun postDailyGift(context: Context, title: String, body: String) {
        post(
            context,
            channelId = CHANNEL_REWARDS,
            id = nextId(),
            title = title,
            body = body,
            iconRes = R.drawable.ic_notification_trophy,
            colorRes = R.color.notification_reward,
            deeplink = DEEPLINK_LOTERIA,
            group = GROUP_REWARDS,
        )
        postSummary(
            context,
            CHANNEL_REWARDS,
            NOTI_REWARDS_SUMMARY,
            "Tenés premios nuevos",
            R.drawable.ic_notification_trophy,
            R.color.notification_reward,
            GROUP_REWARDS,
            DEEPLINK_ACHIEVEMENTS,
        )
    }

    fun postExpenseSaved(context: Context, title: String, body: String) {
        post(
            context,
            channelId = CHANNEL_GASTOS,
            id = NOTI_EXPENSE_SAVED,
            title = title,
            body = body,
            iconRes = R.drawable.ic_notification_money,
            colorRes = R.color.notification_accent,
            deeplink = DEEPLINK_LIST,
        )
    }

    /** Premio (logro nuevo, racha, subida de rango). */
    fun postReward(context: Context, title: String, body: String) {
        post(
            context,
            channelId = CHANNEL_REWARDS,
            id = nextId(),
            title = title,
            body = body,
            iconRes = R.drawable.ic_notification_trophy,
            colorRes = R.color.notification_reward,
            deeplink = DEEPLINK_ACHIEVEMENTS,
            group = GROUP_REWARDS,
        )
        postSummary(
            context,
            CHANNEL_REWARDS,
            NOTI_REWARDS_SUMMARY,
            "Tenés premios nuevos",
            R.drawable.ic_notification_trophy,
            R.color.notification_reward,
            GROUP_REWARDS,
            DEEPLINK_ACHIEVEMENTS,
        )
    }

    /** Fundazo (gasto grande, meta rota, racha caída). */
    fun postFail(context: Context, title: String, body: String) {
        post(
            context,
            channelId = CHANNEL_FAILS,
            id = nextId(),
            title = title,
            body = body,
            iconRes = R.drawable.ic_notification_warning,
            colorRes = R.color.notification_fail,
            deeplink = DEEPLINK_ACHIEVEMENTS,
            priority = NotificationCompat.PRIORITY_HIGH,
            group = GROUP_FAILS,
        )
        postSummary(
            context,
            CHANNEL_FAILS,
            NOTI_FAILS_SUMMARY,
            "Hay fundazos en tu historial",
            R.drawable.ic_notification_warning,
            R.color.notification_fail,
            GROUP_FAILS,
            DEEPLINK_ACHIEVEMENTS,
        )
    }

    /** Movimiento social (reservado; sin ranking de amigos en esta versión). */
    @Suppress("unused")
    fun postFriendUpdate(context: Context, title: String, body: String) {
        post(
            context,
            channelId = CHANNEL_SOCIAL,
            id = nextId(),
            title = title,
            body = body,
            iconRes = R.drawable.ic_notification_friends,
            colorRes = R.color.notification_friend,
            deeplink = DEEPLINK_HOME,
            group = GROUP_SOCIAL,
        )
    }

    // -------- Internals --------

    private fun post(
        context: Context,
        channelId: String,
        id: Int,
        title: String,
        body: String,
        iconRes: Int,
        colorRes: Int,
        deeplink: String,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT,
        group: String? = null,
    ) {
        if (!hasPermission(context)) return
        val pi = pendingIntentFor(context, deeplink, requestCode = id)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(iconRes)
            .setColor(ContextCompat.getColor(context, colorRes))
            .setColorized(false)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(priority)
            .setCategory(categoryFor(channelId))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pi)
        if (group != null) builder.setGroup(group)

        runCatching {
            NotificationManagerCompat.from(context).notify(id, builder.build())
        }
    }

    private fun postSummary(
        context: Context,
        channelId: String,
        summaryId: Int,
        summaryTitle: String,
        iconRes: Int,
        colorRes: Int,
        group: String,
        deeplink: String,
    ) {
        if (!hasPermission(context)) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        val pi = pendingIntentFor(context, deeplink, requestCode = summaryId)
        val summary = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(iconRes)
            .setColor(ContextCompat.getColor(context, colorRes))
            .setStyle(NotificationCompat.InboxStyle().setSummaryText(summaryTitle))
            .setContentTitle(summaryTitle)
            .setGroup(group)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(summaryId, summary)
        }
    }

    private fun categoryFor(channelId: String): String = when (channelId) {
        CHANNEL_FAILS, CHANNEL_GASTOS -> NotificationCompat.CATEGORY_ALARM
        CHANNEL_SOCIAL -> NotificationCompat.CATEGORY_SOCIAL
        CHANNEL_REWARDS -> NotificationCompat.CATEGORY_PROMO
        else -> NotificationCompat.CATEGORY_REMINDER
    }

    private fun pendingIntentFor(
        context: Context,
        deeplink: String,
        requestCode: Int,
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            data = Uri.parse(deeplink)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            pendingIntentFlags(),
        )
    }

    /** `POST_NOTIFICATIONS` concedido (Android 13+) o no requerido. */
    fun hasPostNotificationsPermission(context: Context): Boolean = hasPermission(context)

    private fun hasPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun pendingIntentFlags(): Int =
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

    private fun nextId(): Int {
        rollingId += 1
        if (rollingId > 9000) rollingId = 5000
        return rollingId
    }

    /** Deep link: resolver un “antojo” pendiente (24 h). */
    fun crushDeeplink(id: Long): String = "antigastos://crush/$id"

    fun postCrushReminder(context: Context, crushId: Long, title: String, body: String) {
        val deeplink = crushDeeplink(crushId)
        val nid = (3_000_000 + (crushId % 99_000).toInt())
        post(
            context,
            channelId = CHANNEL_REMINDERS,
            id = nid,
            title = title,
            body = body,
            iconRes = R.drawable.ic_notification_trophy,
            colorRes = R.color.notification_reward,
            deeplink = deeplink,
        )
    }

    // Deep links que MainActivity sabe interpretar.
    const val DEEPLINK_HOME = "antigastos://home"
    const val DEEPLINK_LIST = "antigastos://list"
    const val DEEPLINK_RECAP = "antigastos://recap"
    const val DEEPLINK_ACHIEVEMENTS = "antigastos://achievements"
    const val DEEPLINK_LOTERIA = "antigastos://loteria"
}
