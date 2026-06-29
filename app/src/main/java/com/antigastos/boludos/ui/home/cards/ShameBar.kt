package com.antigastos.boludos.ui.home.cards

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antigastos.boludos.domain.ShameLevel
import com.antigastos.boludos.domain.model.HomeUiModel
import com.antigastos.boludos.ui.theme.AgColors

/**
 * Barra del "nivel de vergüenza" del Home. Toma color y descripción
 * del `ShameLevel` actual del modelo.
 */
@Composable
internal fun ShameBar(state: HomeUiModel) {
    val level = state.shameLevel
    val accent = when (level) {
        ShameLevel.EJEMPLAR -> AgColors.ShameOk
        ShameLevel.NORMAL -> MaterialTheme.colorScheme.primary
        ShameLevel.DESASTRE -> AgColors.ShameAware
        ShameLevel.DELIVERY -> AgColors.ShameDanger
        ShameLevel.LUDOPATA -> AgColors.ShameLudopata
        ShameLevel.REY_FIADO -> AgColors.LegendaryPurple
    }
    val animated by animateFloatAsState(
        targetValue = level.score / 100f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "shameProgress",
    )
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(level.emoji, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.size(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Nivel de vergüenza", style = MaterialTheme.typography.titleSmall)
                    Text(
                        level.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = accent,
                    )
                }
            }
            LinearProgressIndicator(
                progress = { animated },
                modifier = Modifier.fillMaxWidth(),
                color = accent,
            )
        }
    }
}
