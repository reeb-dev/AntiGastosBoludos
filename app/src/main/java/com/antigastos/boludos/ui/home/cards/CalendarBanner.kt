package com.antigastos.boludos.ui.home.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antigastos.boludos.domain.HolidayCalendar
import com.antigastos.boludos.domain.MoneyFormat
import com.antigastos.boludos.domain.SpendingBudgetEngine
import com.antigastos.boludos.ui.theme.AgColors

/**
 * Banner contextual con feriado de hoy / próximo feriado y el "permiso"
 * que da el `SpendingBudgetEngine`. Si no hay nada que mostrar, no
 * renderiza nada (devuelve antes de armar la card).
 */
@Composable
internal fun CalendarBanner(
    budget: SpendingBudgetEngine.DailyBudget?,
    holiday: HolidayCalendar.Holiday?,
    nextHoliday: HolidayCalendar.Holiday?,
) {
    if (budget == null && holiday == null && nextHoliday == null) return
    val accent = when (budget?.permission) {
        SpendingBudgetEngine.Permission.GREEN -> AgColors.PermissionGreen
        SpendingBudgetEngine.Permission.YELLOW -> AgColors.PermissionYellow
        SpendingBudgetEngine.Permission.RED -> AgColors.PermissionRed
        SpendingBudgetEngine.Permission.FORBIDDEN -> AgColors.PermissionForbidden
        null -> MaterialTheme.colorScheme.primary
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = accent.copy(alpha = 0.08f),
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (holiday != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(holiday.emoji, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.size(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Hoy: ${holiday.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = accent,
                        )
                        Text(holiday.tagline, style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else if (nextHoliday != null) {
                Text(
                    "${nextHoliday.emoji} Próximo: ${nextHoliday.name} (${nextHoliday.date})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (budget != null) {
                Text(
                    when (budget.permission) {
                        SpendingBudgetEngine.Permission.GREEN -> "✅ ${budget.message}"
                        SpendingBudgetEngine.Permission.YELLOW -> "⚠️ ${budget.message}"
                        SpendingBudgetEngine.Permission.RED -> "🚨 ${budget.message}"
                        SpendingBudgetEngine.Permission.FORBIDDEN -> "💣 ${budget.message}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = accent,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Gastado hoy: ${MoneyFormat.formatPesos(budget.spentTodayPesos)} · Tope sugerido: ${MoneyFormat.formatPesos(budget.allowanceTodayPesos)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
