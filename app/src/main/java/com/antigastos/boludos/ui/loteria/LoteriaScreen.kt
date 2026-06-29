package com.antigastos.boludos.ui.loteria

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antigastos.boludos.AntiGastosApplication
import com.antigastos.boludos.AppFeedback
import com.antigastos.boludos.ads.AdHelper
import com.antigastos.boludos.data.local.entity.SettingsEntity
import com.antigastos.boludos.domain.CopyMood
import com.antigastos.boludos.ui.common.ArgSticker
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.random.Random

private val rarePhrases = listOf(
    "Hoy te tocó filosofía barata: si no está en el bolsillo, no existe.",
    "Frase rara del día: el delivery no te ama, te quiere la billetera.",
    "Mantra meme: anotá antes de que tu yo del futuro llore.",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoteriaScreen(
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as AntiGastosApplication
    val settings by app.settingsRepository.flow.collectAsState(initial = SettingsEntity())
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val today = remember { LocalDate.now().toString() }

    var rewardText by remember { mutableStateOf<String?>(null) }
    var stickerSeed by remember { mutableStateOf<Long?>(null) }
    var grantedShield by remember { mutableStateOf(false) }
    /** True si el usuario ya duplicó el premio con un video rewarded. */
    var doubledPrize by remember { mutableStateOf(false) }
    /** Tipo de premio que salió (0=escudo, 1=frase, 2=sticker). */
    var lastRoll by remember { mutableStateOf(-1) }

    val already = settings.lastDailyGiftClaimDay == today

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Regalo del día 🎁") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Una recompensa al día",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Tocá el botón y te toca una de tres: una frase meme, un sticker random o +1 escudo de racha (tope 5).",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (already) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(
                        Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            "✅ Regalo del día reclamado",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "Volvé mañana a partir de las 21:00 para una nueva recompensa.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            } else {
                Button(
                    onClick = {
                        AppFeedback.rouletteReveal(ctx, settings.soundEnabled)
                        scope.launch {
                            val roll = Random.nextInt(3)
                            lastRoll = roll
                            doubledPrize = false
                            when (roll) {
                                0 -> {
                                    app.settingsRepository.update {
                                        it.copy(
                                            lastDailyGiftClaimDay = today,
                                            streakFreezeCharges = (it.streakFreezeCharges + 1).coerceAtMost(5),
                                        )
                                    }
                                    grantedShield = true
                                    rewardText = "Te sumé +1 escudo de racha 🔥"
                                }
                                1 -> {
                                    app.settingsRepository.update {
                                        it.copy(lastDailyGiftClaimDay = today)
                                    }
                                    rewardText = rarePhrases.random()
                                }
                                else -> {
                                    app.settingsRepository.update {
                                        it.copy(lastDailyGiftClaimDay = today)
                                    }
                                    stickerSeed = Random.nextLong()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text("Reclamar regalo de hoy")
                }
            }

            rewardText?.let { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    ),
                ) {
                    Text(
                        msg,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            if (grantedShield) {
                Text(
                    "Escudos actuales: hasta 5. Aprovechá un escudo cuando ayer metiste gasto y hoy querés salvar la racha.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            stickerSeed?.let { seed ->
                ArgSticker(
                    mood = CopyMood.CHEER,
                    key = seed,
                    subline = "Sticker del día — pasáselo a un amigo fundido.",
                    modifier = Modifier.height(220.dp),
                )
            }

            // Botón opcional: mirar video para duplicar el premio
            val hasPrize = rewardText != null || stickerSeed != null
            if (hasPrize && !doubledPrize && com.antigastos.boludos.domain.AdFreePolicy.shouldShowAds(settings)) {
                OutlinedButton(
                    onClick = {
                        val activity = ctx as? Activity
                        if (activity == null) {
                            scope.launch {
                                snackbar.showSnackbar("No se pudo abrir el video. Reintentá.")
                            }
                            return@OutlinedButton
                        }
                        val shown = AdHelper.showRewarded(
                            activity,
                            AdHelper.RewardedSlot.DOUBLE_LOTERIA,
                        ) {
                            doubledPrize = true
                            scope.launch {
                                when (lastRoll) {
                                    0 -> {
                                        app.settingsRepository.update {
                                            it.copy(
                                                streakFreezeCharges = (it.streakFreezeCharges + 1).coerceAtMost(5),
                                            )
                                        }
                                        rewardText = "¡Duplicado! +1 escudo extra de racha 🔥🔥"
                                    }
                                    else -> {
                                        app.settingsRepository.update {
                                            it.copy(
                                                streakFreezeCharges = (it.streakFreezeCharges + 1).coerceAtMost(5),
                                            )
                                        }
                                        rewardText = (rewardText ?: "") + "\n\n🎁 Bonus: +1 escudo de racha gratis"
                                    }
                                }
                                snackbar.showSnackbar("¡Premio duplicado!")
                            }
                        }
                        if (!shown) {
                            scope.launch {
                                snackbar.showSnackbar(AdHelper.REWARDED_NOT_READY_MESSAGE)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("🎬 Mirá un video → duplicá tu premio")
                }
            }
            if (doubledPrize) {
                Text(
                    "✅ Premio duplicado. ¡Bien ahí!",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
