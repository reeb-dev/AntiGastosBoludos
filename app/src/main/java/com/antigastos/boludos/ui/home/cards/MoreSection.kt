package com.antigastos.boludos.ui.home.cards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Sección colapsable "Más" del Home: agrupa los CTAs secundarios
 * (ruleta, regalo, quiz, recap, invitar) para no saturar la vista.
 */
@Composable
internal fun MoreSection(
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clickableNoIndication(onClick = onToggle),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        "Más",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Ruleta, regalo del día, quiz y recap.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Cerrar más" else "Abrir más",
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(top = 8.dp, bottom = 8.dp)) {
                    content()
                }
            }
        }
    }
}

/**
 * Chips con accesos rápidos a las pantallas secundarias del Home.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun QuickLinks(
    onOpenRecap: () -> Unit,
    onQuickAdd: () -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenRuleta: () -> Unit,
    onOpenQuiz: () -> Unit,
    onOpenLoteria: () -> Unit,
    onShareMonthStory: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Atajos",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val chipModifier = Modifier.height(48.dp)
            FilterChip(
                selected = false,
                onClick = onQuickAdd,
                label = { Text("➕ Cargar gasto rápido") },
                modifier = chipModifier,
            )
            FilterChip(
                selected = false,
                onClick = onOpenRecap,
                label = { Text("📊 Recap del mes") },
                modifier = chipModifier,
            )
            FilterChip(
                selected = false,
                onClick = onShareMonthStory,
                label = { Text("📱 Historia 9:16 del mes") },
                modifier = chipModifier,
            )
            FilterChip(
                selected = false,
                onClick = onOpenAchievements,
                label = { Text("🏆 Trofeos") },
                modifier = chipModifier,
            )
            FilterChip(
                selected = false,
                onClick = onOpenRuleta,
                label = { Text("🎰 Tirar ruleta") },
                modifier = chipModifier,
            )
            FilterChip(
                selected = false,
                onClick = onOpenLoteria,
                label = { Text("🎁 Regalo diario") },
                modifier = chipModifier,
            )
            FilterChip(
                selected = false,
                onClick = onOpenQuiz,
                label = { Text("🧠 Quiz personaje") },
                modifier = chipModifier,
            )
        }
        Text(
            "Tip: compartí la historia 9:16 del mes o el quiz con tus amigos.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

/**
 * Versión "limpia" de `clickable` sin ripple/indicación. La usamos
 * para áreas grandes que ya tienen su propio feedback visual.
 */
@Composable
private fun Modifier.clickableNoIndication(onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    return this.clickable(
        interactionSource = interaction,
        indication = null,
        onClick = onClick,
    )
}
