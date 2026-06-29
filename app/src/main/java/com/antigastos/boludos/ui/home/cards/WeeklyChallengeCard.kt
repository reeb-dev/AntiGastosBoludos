package com.antigastos.boludos.ui.home.cards

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antigastos.boludos.domain.MoneyFormat
import com.antigastos.boludos.ui.home.WeeklyChallengeUi
import com.antigastos.boludos.ui.theme.AgColors

@Composable
internal fun WeeklyChallengeCard(challenge: WeeklyChallengeUi) {
    val limit = challenge.limitPesos.coerceAtLeast(1L)
    val progress = (challenge.spentPesos.toFloat() / limit.toFloat()).coerceIn(0f, 1.2f)
    val ok = challenge.spentPesos <= challenge.limitPesos
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (ok) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Desafío de la semana",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                challenge.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "${MoneyFormat.formatPesos(challenge.spentPesos)} de ${MoneyFormat.formatPesos(challenge.limitPesos)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                color = if (ok) AgColors.CheerGreen else MaterialTheme.colorScheme.error,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Text(
                if (ok) "Dentro del tope. Seguí así." else "Te pasaste del tope del desafío — la semana sigue, ajustá el ritmo.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
