package com.antigastos.boludos.ui.personas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.data.local.entity.SettingsEntity
import com.antigastos.boludos.domain.Persona
import com.antigastos.boludos.domain.PersonaCatalog

/**
 * Lista de personajes para chatear. Mostrá un teaser de cada uno con su última
 * frase de muestra y un check si es el seleccionado como voz principal.
 */
@Composable
fun PersonaListScreen(onOpen: (String) -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as AntiGastosApplication
    val settings by app.settingsRepository.flow.collectAsState(initial = SettingsEntity())
    val personas = remember { PersonaCatalog.all }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Header(aiEnabled = settings.aiPhrasesEnabled)
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(items = personas, key = { it.key }) { persona ->
                PersonaCard(
                    persona = persona,
                    isPrimary = persona.key == settings.personaKey,
                    onOpen = { onOpen(persona.key) },
                )
            }
        }
    }
}

@Composable
private fun Header(aiEnabled: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "💬 Chatealos",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                if (aiEnabled) {
                    "Hablales en serio: la IA hace de cada personaje. Pueden cargarte, aconsejarte o psicoanalizarte."
                } else {
                    "Activá la IA en Ajustes y tirales un mensaje. Con IA apagada igual te responden con frases tipo, pero más cortitas."
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun PersonaCard(persona: Persona, isPrimary: Boolean, onOpen: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpen,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(56.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(persona.emoji, style = MaterialTheme.typography.headlineMedium)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        persona.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (isPrimary) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Text(
                                "tu voz",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
                Text(
                    persona.tagline,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                )
                if (persona.samples.isNotEmpty()) {
                    Spacer(Modifier.size(4.dp))
                    Text(
                        "“${persona.samples.first()}”",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}
