package com.antigastos.boludos.ui.home.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Banner de "tips para arrancar" la primera vez que el usuario
 * entra al Home. Se cierra con la cruz y guarda el flag en settings.
 */
@Composable
internal fun HomeTipsBanner(onClose: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
        ),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Tips para arrancar",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text("• Tocá ➕ para cargar un gasto en 5 segundos.", style = MaterialTheme.typography.bodySmall)
                Text("• La racha 🔥 sube cuando cerrás el día sin gastar.", style = MaterialTheme.typography.bodySmall)
                Text("• Abrí “Más” acá abajo para ruleta, regalo y quiz.", style = MaterialTheme.typography.bodySmall)
                Text("• ¿Te aburre el personaje? Cambialo en Ajustes → Personaje.", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(Icons.Default.Close, contentDescription = "No mostrar más")
            }
        }
    }
}
