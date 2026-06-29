package com.antigastos.boludos.ui.home.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antigastos.boludos.domain.MoneyFormat
import com.antigastos.boludos.domain.SpendingBudgetEngine
import com.antigastos.boludos.ui.theme.AgColors

/**
 * Card "Estado del día" arriba del Home. Muestra el gasto del día,
 * el tope sugerido si hay presupuesto y la racha + CTA de cargar gasto.
 */
@Composable
internal fun TodayHero(
    daySpentPesos: Long,
    budget: SpendingBudgetEngine.DailyBudget?,
    streakDays: Int,
    scoreLevel: Int,
    scorePoints: Long,
    lifeHoursLine: String?,
    onAdd: () -> Unit,
) {
    val zero = daySpentPesos == 0L
    val headline = if (zero) "Hoy vas en cero" else "Hoy gastaste ${MoneyFormat.formatPesos(daySpentPesos)}"
    val subline = when {
        budget != null -> {
            val tope = MoneyFormat.formatPesos(budget.allowanceTodayPesos)
            if (zero) "Meta del día: $tope · cuanto más bajo, mejor."
            else "Tope sugerido del día: $tope"
        }
        zero -> "Si seguís así sumás racha. Cargá lo que entre, mantenete fiel."
        else -> "Cargá los gastos a medida que pasan: el cuento te lo armamos solos."
    }
    val accent = if (zero) AgColors.CheerGreen else MaterialTheme.colorScheme.primary
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Estado del día",
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                fontWeight = FontWeight.Bold,
            )
            Text(
                headline,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                subline,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (!lifeHoursLine.isNullOrBlank()) {
                Text(
                    lifeHoursLine,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            if (streakDays > 0) {
                Text(
                    "🔥 Racha: $streakDays día${if (streakDays == 1) "" else "s"} sin gastar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AgColors.CheerGreen,
                )
            }
            if (scorePoints != 0L) {
                Text(
                    "⭐ Nivel $scoreLevel · $scorePoints pts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onAdd,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) { Text("➕ Agregar gasto") }
        }
    }
}
