package com.antigastos.boludos.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.antigastos.boludos.MainActivity
import com.antigastos.boludos.R
import com.antigastos.boludos.data.local.AppDatabase
import com.antigastos.boludos.domain.MoneyFormat
import com.antigastos.boludos.ui.navigation.Routes
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Widget de escritorio: total gastado hoy + atajo para cargar gasto.
 */
class HomeAppWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val zone = java.time.ZoneId.systemDefault()
                val today = LocalDate.now(zone)
                val dayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
                val dayEnd = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val ym = java.time.YearMonth.from(today)
                val monthStart = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val monthEnd = ym.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val db = AppDatabase.create(context.applicationContext)
                val daySum = db.expenseDao().sumTotal(dayStart, dayEnd) ?: 0L
                val monthSum = db.expenseDao().sumTotal(monthStart, monthEnd) ?: 0L
                val lucas = MoneyFormat.pesosToLucas(daySum)
                val label = "$lucas lucas"
                val monthLabel = "Mes: ${MoneyFormat.formatPesos(monthSum)}"

                val openHome = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(Routes.EXTRA_OPEN_ROUTE, Routes.HOME)
                }
                val openAdd = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(Routes.EXTRA_OPEN_ROUTE, Routes.ADD)
                }
                val piHome = PendingIntent.getActivity(
                    context,
                    0,
                    openHome,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                val piAdd = PendingIntent.getActivity(
                    context,
                    1,
                    openAdd,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

                val views = RemoteViews(context.packageName, R.layout.widget_home).apply {
                    setTextViewText(R.id.widget_amount, label)
                    setTextViewText(R.id.widget_month, monthLabel)
                    setOnClickPendingIntent(R.id.widget_root, piHome)
                    setOnClickPendingIntent(R.id.widget_add, piAdd)
                }
                withContext(Dispatchers.Main) {
                    appWidgetIds.forEach { id ->
                        appWidgetManager.updateAppWidget(id, views)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
