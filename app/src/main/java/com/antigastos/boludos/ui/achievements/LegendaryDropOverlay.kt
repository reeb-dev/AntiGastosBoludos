package com.antigastos.boludos.ui.achievements

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.antigastos.boludos.AppFeedback
import com.antigastos.boludos.domain.CopyMood
import com.antigastos.boludos.ui.common.ArgSticker

@Composable
fun LegendaryDropOverlay(
    soundEnabled: Boolean,
    onDismiss: () -> Unit,
) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) {
        AppFeedback.legendaryDrop(ctx, soundEnabled)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        ) {
            Column(
                Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "¡Drop legendario! (~1%)",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Se te prendió la app. Mostráselo a alguien antes de que te lo creas solo vos.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                ArgSticker(
                    mood = CopyMood.CHEER,
                    key = 0xC0FFEE00L,
                    subline = "Sticker de oro — compartilo y sumá leyenda.",
                    forceLegendary = true,
                    modifier = Modifier.height(240.dp),
                )
                Button(
                    onClick = {
                        AppFeedback.shareAction(ctx)
                        val text =
                            "Me tocó un DROP LEGENDARIO en Anti-gastos boludos (1%). 🇦🇷 #AntiGastosBoludos"
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        ctx.startActivity(Intent.createChooser(send, "Compartir drop"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Text("  Compartir")
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cerrar")
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
