package com.antigastos.boludos.ui.pact

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.data.local.entity.SettingsEntity
import android.content.Intent
import com.antigastos.boludos.ui.theme.AgColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PactScreen(viewModel: PactViewModel) {
    val state by viewModel.uiState.collectAsState()
    var slug by remember { mutableStateOf<String?>(null) }
    var days by remember { mutableStateOf(7) }
    var friendName by remember { mutableStateOf("") }
    var stake by remember { mutableStateOf("") }
    val ctx = LocalContext.current
    val app = ctx.applicationContext as AntiGastosApplication
    val settings by app.settingsRepository.flow.collectAsState(initial = SettingsEntity())

    val fmt = DateTimeFormatter.ofPattern("dd/MM").withZone(ZoneId.systemDefault())

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Modo pacto", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Te comprometés con vos mismo: \"no compro en X durante Y días\". " +
                "Si cargás un gasto en esa categoría, el pacto queda roto.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Nuevo pacto", fontWeight = FontWeight.SemiBold)
                Text("Categoría", style = MaterialTheme.typography.bodySmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.categories.forEach { c ->
                        FilterChip(
                            selected = c.slug == slug,
                            onClick = { slug = c.slug },
                            label = { Text(c.name) },
                        )
                    }
                }
                Text("Cantidad de días", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(3, 7, 14, 30).forEach { d ->
                        FilterChip(
                            selected = d == days,
                            onClick = { days = d },
                            label = { Text("$d días") },
                        )
                    }
                }

                Text(
                    "Pacto con un amigo (opcional)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                OutlinedTextField(
                    value = friendName,
                    onValueChange = { friendName = it.take(40) },
                    label = { Text("Nombre del amigo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = stake,
                    onValueChange = { stake = it.take(80) },
                    label = { Text("¿Qué se juegan? (ej. una pizza)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Button(
                    enabled = slug != null,
                    onClick = {
                        slug?.let {
                            viewModel.create(
                                it,
                                days,
                                friendName.takeIf { f -> f.isNotBlank() },
                                stake.takeIf { s -> s.isNotBlank() },
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Comprometerme") }

                if (slug != null && (friendName.isNotBlank() || stake.isNotBlank())) {
                    OutlinedButton(
                        onClick = {
                            val msg = buildPactInviteMessage(
                                friend = friendName,
                                slug = slug.orEmpty(),
                                days = days,
                                stake = stake,
                            )
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, msg)
                            }
                            ctx.startActivity(Intent.createChooser(intent, "Invitar al pacto"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("📲 Invitar a ${friendName.ifBlank { "tu amigo" }} a copiar el pacto") }
                }
            }
        }

        Text("Tus pactos", style = MaterialTheme.typography.titleMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.pacts, key = { it.id }) { p ->
                val (label, color) = when {
                    p.brokenAt != null -> "❌ Roto" to AgColors.AlarmRed
                    p.completedAt != null -> "🏆 Cumplido" to AgColors.CheerGreen
                    else -> "⏳ En curso" to AgColors.NeutralBlue
                }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("No gastar en \"${p.categorySlug}\"", fontWeight = FontWeight.SemiBold)
                            Text(label, color = color, fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            "Del ${fmt.format(Instant.ofEpochMilli(p.startEpochMillis))} al ${fmt.format(Instant.ofEpochMilli(p.endEpochMillis))}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        val social = listOfNotNull(
                            p.friendName?.let { "🤝 Apostado con $it" },
                            p.stakeText?.let { "🎯 Premio: $it" },
                        )
                        if (social.isNotEmpty()) {
                            social.forEach { line ->
                                Text(line, style = MaterialTheme.typography.bodySmall)
                            }
                            // Solo cuando se cierra el pacto (cumplido o roto) ofrecemos
                            // share del resultado, para usar la cuenta como "vincha".
                            if (p.brokenAt != null || p.completedAt != null) {
                                OutlinedButton(
                                    onClick = {
                                        val msg = buildPactResultMessage(p)
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, msg)
                                        }
                                        ctx.startActivity(
                                            Intent.createChooser(intent, "Cobrar el pacto"),
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        if (p.completedAt != null) {
                                            "📲 Avisar que cumplí (a cobrar)"
                                        } else {
                                            "📲 Avisar que perdí (a pagar)"
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun buildPactInviteMessage(
    friend: String,
    slug: String,
    days: Int,
    stake: String,
): String = buildString {
    appendLine("Eh ${friend.ifBlank { "loco" }}, te apuesto algo:")
    append("No gasto en ")
    append(slug)
    append(" durante ")
    append(days)
    appendLine(" días.")
    if (stake.isNotBlank()) {
        appendLine("Si pierdo, va $stake. Si ganás vos, vos pagás.")
    } else {
        appendLine("El que pierde paga lo que se le ocurra al otro.")
    }
    appendLine()
    appendLine("Lo estoy llevando con Anti-gastos boludos. Copialo en tu app.")
}

private fun buildPactResultMessage(
    p: com.antigastos.boludos.data.local.entity.PactEntity,
): String = buildString {
    val friend = p.friendName ?: "che"
    if (p.completedAt != null) {
        append("Listo $friend, cumplí: ")
        append(p.categorySlug)
        append(" cero gastos durante ese tiempo. ")
        if (!p.stakeText.isNullOrBlank()) append("Vení que cobro: ${p.stakeText}.") else append("A cobrar.")
    } else {
        append("Caí $friend. Me mandé un gasto en ")
        append(p.categorySlug)
        append(" antes de tiempo. ")
        if (!p.stakeText.isNullOrBlank()) append("Te debo: ${p.stakeText}.") else append("Pago lo que digas.")
    }
}
