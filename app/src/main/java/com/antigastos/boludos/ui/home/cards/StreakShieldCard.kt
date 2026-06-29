package com.antigastos.boludos.ui.home.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antigastos.boludos.data.local.entity.SettingsEntity
import com.antigastos.boludos.domain.AdFreePolicy
import com.antigastos.boludos.domain.model.HomeUiModel
import java.time.LocalDate

/**
 * Card que ofrece "perdonar" un gasto de ayer para no romper la racha.
 * Solo se muestra si el usuario tiene escudos cargados y ayer hubo gasto.
 */
@Composable
internal fun StreakShieldCard(
    charges: Int,
    onUse: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE8EAF6),
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "🛡️ Escudo de racha",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF283593),
            )
            Text(
                "Ayer metiste gasto y hoy venís en cero. Podés perdonar ese día una vez y seguir sumando días limpios.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Escudos disponibles: $charges",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF3949AB),
            )
            Button(onClick = onUse) {
                Text("Usar escudo en ayer")
            }
        }
    }
}

/**
 * Card que ofrece mirar un video rewarded para ganar un escudo gratis
 * cuando el usuario se quedó sin cargas.
 */
@Composable
internal fun StreakShieldRewardedCard(
    onWatchVideo: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0),
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "🎬 Ganá un escudo gratis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE65100),
            )
            Text(
                "Se te acabaron los escudos, pero podés mirar un video cortito y ganar uno para salvar tu racha.",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(onClick = onWatchVideo) {
                Text("▶ Ver video y ganar escudo")
            }
        }
    }
}

/**
 * Decide si conviene mostrar la card del escudo: necesita escudos disponibles,
 * gasto ayer, hoy en cero y que ayer no haya sido ya perdonado.
 */
internal fun showStreakShield(state: HomeUiModel, settings: SettingsEntity): Boolean {
    if (settings.streakFreezeCharges <= 0) return false
    if (!state.yesterdayHadExpenses) return false
    if (state.streakDaysNoSpend != 0) return false
    if (state.daySpentPesos != 0L) return false
    val y = LocalDate.now().minusDays(1).toEpochDay().toString()
    val forg = settings.streakForgivenEpochDaysCsv.split(",").map { it.trim() }
    return y !in forg
}

/**
 * True si el usuario no tiene escudos pero estaría en condiciones de usar uno
 * (aplica la misma lógica que [showStreakShield] salvo el check de charges).
 */
internal fun showStreakRewardedOffer(state: HomeUiModel, settings: SettingsEntity): Boolean {
    if (settings.streakFreezeCharges > 0) return false
    if (AdFreePolicy.isAdFreeActive(settings)) return false
    if (!state.yesterdayHadExpenses) return false
    if (state.streakDaysNoSpend != 0) return false
    if (state.daySpentPesos != 0L) return false
    val y = LocalDate.now().minusDays(1).toEpochDay().toString()
    val forg = settings.streakForgivenEpochDaysCsv.split(",").map { it.trim() }
    return y !in forg
}
